package kr.sdbk.bodyplan.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisEntry
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.DietDailySummary
import kr.sdbk.bodyplan.core.domain.model.DietSummaryRequest
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository

/** 분석할 기록이 하나도 없는 날을 분석하려 한 경우. 부르지 않고 되돌린다. */
class NoRecordToAnalyzeException : Exception("분석할 기록이 없습니다")

/** 종합할 날짜별 분석이 기간에 하나도 없는 경우. 날짜별 분석을 먼저 해야 한다. */
class NoDailyAnalysisException : Exception("종합할 날짜별 분석이 없습니다")

/** 인증 정보 없이 분석을 시도한 경우. 화면이 토큰 등록으로 이끈다. */
class AiCredentialMissingException : Exception("AI 인증 정보가 없습니다")

/**
 * 식단을 분석하고 결과를 저장한다.
 *
 * 날짜별은 사진과 메모를 보내 먹은 음식·칼로리·영양 성분을 받고, 주간·월간은 사진을 다시
 * 보내지 않고 그 기간의 날짜별 분석을 모아 종합한다. 어느 쪽인지는 [kind]가 정하며 화면은
 * 이 갈림을 모른다.
 */
class AnalyzeDietUseCase
@Inject
constructor(
    private val dietLogRepository: DietLogRepository,
    private val userProfileRepository: UserProfileRepository,
    private val aiCredentialRepository: AiCredentialRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val analysisResultRepository: AnalysisResultRepository,
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

        val content = if (kind == AnalysisKind.DIET_DAILY) {
            val entries = collectEntries(from, to)
            if (entries.isEmpty()) throw NoRecordToAnalyzeException()
            // 사진을 읽어 base64로 바꾸는 일은 저장소 안에서 일어나 여기서는 끝을 알 수 없다.
            // 알 수 없는 단계를 내면 실제로 하는 일과 어긋나므로, 호출 단계 하나로 묶는다.
            onStage(AnalysisStage.CALLING)
            aiAnalysisRepository.analyzeDiet(
                DietAnalysisRequest(
                    credential = credential,
                    profile = profile,
                    periodLabel = periodLabel,
                    entries = entries,
                ),
            )
        } else {
            val dailyResults = collectDailyResults(from, to)
            if (dailyResults.isEmpty()) throw NoDailyAnalysisException()
            onStage(AnalysisStage.CALLING)
            aiAnalysisRepository.summarizeDiet(
                DietSummaryRequest(
                    credential = credential,
                    profile = profile,
                    periodLabel = periodLabel,
                    dailyResults = dailyResults,
                ),
            )
        }

        onStage(AnalysisStage.PARSING)
        analysisResultRepository.save(kind, scopeKey, content)
        return content
    }

    private suspend fun collectEntries(from: LocalDate, to: LocalDate): List<DietAnalysisEntry> =
        datesOf(from, to).flatMap { date ->
            dietLogRepository.observeLog(date).first().entries.map { entry ->
                DietAnalysisEntry(date = date, memo = entry.memo, imagePath = entry.imagePath)
            }
        }

    private suspend fun collectDailyResults(from: LocalDate, to: LocalDate): List<DietDailySummary> {
        val dates = datesOf(from, to)
        val saved = analysisResultRepository.getLatestOf(
            kind = AnalysisKind.DIET_DAILY,
            scopeKeys = dates.map { AnalysisScopeKey.daily(it) },
        )
        return dates.mapNotNull { date ->
            saved[AnalysisScopeKey.daily(date)]?.let { DietDailySummary(date = date, content = it.content) }
        }
    }

    private fun datesOf(from: LocalDate, to: LocalDate): List<LocalDate> = generateSequence(from) { it.plusDays(1) }
        .takeWhile { !it.isAfter(to) }
        .toList()
}
