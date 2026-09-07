package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AnalysisChild
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage
import kr.sdbk.bodyplan.core.domain.model.AnalysisSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisSet
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/**
 * 운동을 분석하고 결과를 저장한다.
 *
 * 계층이 식단과 같다. 기록 한 건이 최소 단위이고, 하루는 그 날 기록들의 분석을, 주는 날짜
 * 분석을, 달은 온전한 주의 주 분석과 잘린 주의 날짜 분석을 모아 종합한다.
 */
class AnalyzeWorkoutUseCase
@Inject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val userProfileRepository: UserProfileRepository,
    private val aiCredentialRepository: AiCredentialRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val analysisResultRepository: AnalysisResultRepository,
    private val children: AnalysisChildren,
) {

    suspend operator fun invoke(
        kind: AnalysisKind,
        scopeKey: String,
        periodLabel: String,
        from: LocalDate,
        to: LocalDate,
        onStage: suspend (AnalysisStage) -> Unit = {},
    ): AnalysisContent {
        onStage(AnalysisStage.COLLECTING)
        val credential = aiCredentialRepository.getCredential() ?: throw AiCredentialMissingException()
        val profile = userProfileRepository.getProfile()

        val content = when (kind) {
            AnalysisKind.WORKOUT_DAILY -> analyzeDay(credential, profile, periodLabel, from, onStage)
            else -> summarize(credential, profile, kind, periodLabel, from, to, onStage)
        }

        onStage(AnalysisStage.PARSING)
        analysisResultRepository.save(kind, scopeKey, content)
        return content
    }

    /** 하루는 그 날 기록 원문을 직접 본다. 위 계층이 이 결과를 모아 쓴다. */
    private suspend fun analyzeDay(
        credential: AiCredential,
        profile: UserProfile,
        periodLabel: String,
        date: LocalDate,
        onStage: suspend (AnalysisStage) -> Unit,
    ): AnalysisContent {
        val entries = workoutLogRepository.observeLog(date).first().entries.map { it.toAnalysisEntry(date) }
        if (entries.isEmpty()) throw NoRecordToAnalyzeException()

        onStage(AnalysisStage.CALLING)
        return aiAnalysisRepository.analyzeWorkout(
            WorkoutAnalysisRequest(
                credential = credential,
                profile = profile,
                periodLabel = periodLabel,
                entries = entries,
                // 하루는 그 날 하나다. 쉰 날은 기간 분석에서만 뜻이 있다.
                workoutDayCount = 1,
                restDayCount = 0,
                totalWeightVolume = entries.sumOf { it.volumeOf(IntensityType.WEIGHT) },
                totalBodyweightReps = entries.sumOf { it.repsOf(IntensityType.ANGLE) },
                totalCardioMinutes = entries.sumOf { it.minutesOf(IntensityType.DURATION) },
            ),
        )
    }

    private suspend fun summarize(
        credential: AiCredential,
        profile: UserProfile,
        kind: AnalysisKind,
        periodLabel: String,
        from: LocalDate,
        to: LocalDate,
        onStage: suspend (AnalysisStage) -> Unit,
    ): AnalysisContent {
        val gathered: List<AnalysisChild>
        val childLabel: String
        when (kind) {
            AnalysisKind.WORKOUT_WEEKLY -> {
                gathered = children.ofWeek(AnalysisKind.WORKOUT_DAILY, from, to)
                childLabel = "날짜별"
            }

            AnalysisKind.WORKOUT_MONTHLY -> {
                gathered = children.ofMonth(AnalysisKind.WORKOUT_DAILY, AnalysisKind.WORKOUT_WEEKLY, from)
                childLabel = "주간"
            }

            else -> error("운동 분석이 아닙니다: $kind")
        }
        if (gathered.isEmpty()) throw NoChildAnalysisException(childLabel)

        onStage(AnalysisStage.CALLING)
        return aiAnalysisRepository.summarize(
            AnalysisSummaryRequest(
                credential = credential,
                profile = profile,
                kind = kind,
                periodLabel = periodLabel,
                children = gathered,
            ),
        )
    }
}

private fun WorkoutEntry.toAnalysisEntry(date: LocalDate) = WorkoutAnalysisEntry(
    date = date,
    bodyPart = bodyPart,
    exerciseName = exerciseName,
    sets = sets.map { set ->
        WorkoutAnalysisSet(
            repeatCount = set.repeatCount,
            intensityValue = set.intensity.value,
            intensityType = intensityType,
        )
    },
)

/** 무게로 재는 종목만 kg 볼륨에 넣는다. 각도·시간 종목은 무게가 없어 섞으면 뜻이 없는 수가 된다. */
private fun WorkoutAnalysisEntry.volumeOf(type: IntensityType): Int =
    sets.filter { it.intensityType == type }.sumOf { it.intensityValue * it.repeatCount }

private fun WorkoutAnalysisEntry.repsOf(type: IntensityType): Int =
    sets.filter { it.intensityType == type }.sumOf { it.repeatCount }

/** 시간으로 재는 종목은 한 세트가 한 회차라 횟수를 곱하지 않는다. */
private fun WorkoutAnalysisEntry.minutesOf(type: IntensityType): Int =
    sets.filter { it.intensityType == type }.sumOf { it.intensityValue }
