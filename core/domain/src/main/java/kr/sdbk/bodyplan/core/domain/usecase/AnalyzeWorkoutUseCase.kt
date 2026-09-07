package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisSet
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/**
 * 기간의 운동을 분석하고 결과를 저장한다.
 *
 * 날짜별과 주간·월간이 같은 재료를 쓴다 — 운동 기록은 글자뿐이라 한 달치를 그대로 보내도
 * 가볍고, 날짜별 분석을 먼저 하도록 묶으면 쓰지 않을 제약만 는다.
 */
class AnalyzeWorkoutUseCase
@Inject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val userProfileRepository: UserProfileRepository,
    private val aiCredentialRepository: AiCredentialRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val analysisResultRepository: AnalysisResultRepository,
    private val clock: Clock,
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

        val dates = datesOf(from, to)
        val entries = collectEntries(dates)
        if (entries.isEmpty()) throw NoRecordToAnalyzeException()

        // 아직 오지 않은 날은 쉰 날이 아니다. 남은 날을 휴식으로 세면 기간 평가가 어긋난다.
        val today = LocalDate.now(clock)
        val passedDates = dates.filter { !it.isAfter(today) }
        val workoutDates = entries.map { it.date }.toSet()

        // 운동 기록에는 사진이 없어 준비 단계를 건너뛴다.
        onStage(AnalysisStage.CALLING)
        val content = aiAnalysisRepository.analyzeWorkout(
            WorkoutAnalysisRequest(
                credential = credential,
                profile = userProfileRepository.getProfile(),
                kind = kind,
                periodLabel = periodLabel,
                entries = entries,
                workoutDayCount = workoutDates.size,
                restDayCount = passedDates.count { it !in workoutDates },
                totalWeightVolume = entries.sumOf { entry -> entry.volumeOf(IntensityType.WEIGHT) },
                totalBodyweightReps = entries.sumOf { entry -> entry.repsOf(IntensityType.ANGLE) },
                totalCardioMinutes = entries.sumOf { entry -> entry.minutesOf(IntensityType.DURATION) },
            ),
        )
        onStage(AnalysisStage.PARSING)
        analysisResultRepository.save(kind, scopeKey, content)
        return content
    }

    private suspend fun collectEntries(dates: List<LocalDate>): List<WorkoutAnalysisEntry> = dates.flatMap { date ->
        workoutLogRepository.observeLog(date).first().entries.map { entry ->
            WorkoutAnalysisEntry(
                date = date,
                bodyPart = entry.bodyPart,
                exerciseName = entry.exerciseName,
                sets = entry.sets.map { set ->
                    WorkoutAnalysisSet(
                        repeatCount = set.repeatCount,
                        intensityValue = set.intensity.value,
                        intensityType = entry.intensityType,
                    )
                },
            )
        }
    }

    private fun datesOf(from: LocalDate, to: LocalDate): List<LocalDate> = generateSequence(from) { it.plusDays(1) }
        .takeWhile { !it.isAfter(to) }
        .toList()
}

/** 무게로 재는 종목만 kg 볼륨에 넣는다. 각도 종목은 무게가 없어 섞으면 뜻이 없는 수가 된다. */
private fun WorkoutAnalysisEntry.volumeOf(type: IntensityType): Int =
    sets.filter { it.intensityType == type }.sumOf { it.intensityValue * it.repeatCount }

private fun WorkoutAnalysisEntry.repsOf(type: IntensityType): Int =
    sets.filter { it.intensityType == type }.sumOf { it.repeatCount }

/** 시간으로 재는 종목은 한 세트가 한 회차라 횟수를 곱하지 않는다. */
private fun WorkoutAnalysisEntry.minutesOf(type: IntensityType): Int =
    sets.filter { it.intensityType == type }.sumOf { it.intensityValue }
