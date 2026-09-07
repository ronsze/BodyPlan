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
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisEntry
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository

/** 분석할 기록이 하나도 없는 대상을 분석하려 한 경우. 부르지 않고 되돌린다. */
class NoRecordToAnalyzeException : Exception("분석할 기록이 없습니다")

/** 종합할 아래 계층의 분석이 하나도 없는 경우. 아래부터 먼저 해야 한다. */
class NoChildAnalysisException(val childLabel: String) : Exception("먼저 $childLabel 분석을 해주세요")

/** 인증 정보 없이 분석을 시도한 경우. 화면이 토큰 등록으로 이끈다. */
class AiCredentialMissingException : Exception("AI 인증 정보가 없습니다")

/**
 * 식단을 분석하고 결과를 저장한다.
 *
 * 하루는 그 날 사진과 메모를 직접 보고, 주는 날짜 분석을, 달은 온전한 주의 주 분석과
 * 잘린 주의 날짜 분석을 모아 종합한다.
 *
 * 아래 계층이 없으면 만들지 않고 되돌린다 — 자동으로 타고 내려가면 버튼 한 번에 호출이
 * 서른 번 넘게 날 수 있고, 비용이 사용자 몫이다.
 */
class AnalyzeDietUseCase
@Inject
constructor(
    private val dietLogRepository: DietLogRepository,
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
            AnalysisKind.DIET_DAILY -> analyzeDay(credential, profile, periodLabel, from, onStage)
            else -> summarize(credential, profile, kind, periodLabel, from, to, onStage)
        }

        onStage(AnalysisStage.PARSING)
        analysisResultRepository.save(kind, scopeKey, content)
        return content
    }

    /** 하루는 그 날 사진을 직접 본다. 위 계층이 이 결과를 모아 쓴다. */
    private suspend fun analyzeDay(
        credential: AiCredential,
        profile: UserProfile,
        periodLabel: String,
        date: LocalDate,
        onStage: suspend (AnalysisStage) -> Unit,
    ): AnalysisContent {
        val entries = dietLogRepository.observeLog(date).first().entries.map { entry ->
            DietAnalysisEntry(date = date, memo = entry.memo, imagePath = entry.imagePath)
        }
        if (entries.isEmpty()) throw NoRecordToAnalyzeException()

        onStage(AnalysisStage.CALLING)
        return aiAnalysisRepository.analyzeDiet(
            DietAnalysisRequest(
                credential = credential,
                profile = profile,
                periodLabel = periodLabel,
                entries = entries,
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
            AnalysisKind.DIET_WEEKLY -> {
                gathered = children.ofWeek(AnalysisKind.DIET_DAILY, from, to)
                childLabel = "날짜별"
            }

            AnalysisKind.DIET_MONTHLY -> {
                gathered = children.ofMonth(AnalysisKind.DIET_DAILY, AnalysisKind.DIET_WEEKLY, from)
                childLabel = "주간"
            }

            else -> error("식단 분석이 아닙니다: $kind")
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
