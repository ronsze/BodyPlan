package kr.sdbk.bodyplan.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kr.sdbk.bodyplan.core.domain.model.AiRequestFailedException
import kr.sdbk.bodyplan.core.domain.model.AiUnauthorizedException
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage
import kr.sdbk.bodyplan.core.domain.usecase.AiCredentialMissingException
import kr.sdbk.bodyplan.core.domain.usecase.AnalyzeDietUseCase
import kr.sdbk.bodyplan.core.domain.usecase.AnalyzeInbodyUseCase
import kr.sdbk.bodyplan.core.domain.usecase.AnalyzeWorkoutUseCase
import kr.sdbk.bodyplan.core.domain.usecase.NoDailyAnalysisException
import kr.sdbk.bodyplan.core.domain.usecase.NoRecordToAnalyzeException

/**
 * 분석 하나를 화면 밖에서 돌린다.
 *
 * 화면이 사라져도, 앱이 죽었다 살아나도 이어진다. 실패 사유는 출력으로 남겨 화면이 그대로
 * 보여준다 — 워커가 끝난 뒤에도 무엇이 잘못됐는지 알 수 있어야 한다.
 */
@HiltWorker
internal class AnalysisWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analyzeDiet: AnalyzeDietUseCase,
    private val analyzeWorkout: AnalyzeWorkoutUseCase,
    private val analyzeInbody: AnalyzeInbodyUseCase,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val kind = inputData.getString(AnalysisWork.KEY_KIND)
            ?.let { name -> AnalysisKind.entries.firstOrNull { it.name == name } }
            ?: return Result.failure(workDataOf(AnalysisWork.KEY_REASON to UNKNOWN_KIND))

        return try {
            analyze(kind)
            Result.success()
        } catch (cancellation: CancellationException) {
            // 취소는 실패가 아니다. WorkManager가 취소로 다루게 그대로 던진다.
            throw cancellation
        } catch (failure: Throwable) {
            Result.failure(workDataOf(AnalysisWork.KEY_REASON to failure.toReason()))
        }
    }

    private suspend fun analyze(kind: AnalysisKind) {
        val scopeKey = inputData.getString(AnalysisWork.KEY_SCOPE).orEmpty()
        val periodLabel = inputData.getString(AnalysisWork.KEY_PERIOD_LABEL).orEmpty()

        when (kind) {
            AnalysisKind.INBODY -> {
                val sourceUri = inputData.getString(AnalysisWork.KEY_SOURCE_URI)
                    ?: error("인바디 분석에 사진이 없습니다")
                analyzeInbody(sourceUri, ::publishStage)
            }

            AnalysisKind.DIET_DAILY, AnalysisKind.DIET_WEEKLY, AnalysisKind.DIET_MONTHLY ->
                analyzeDiet(
                    kind,
                    scopeKey,
                    periodLabel,
                    dateAt(AnalysisWork.KEY_FROM),
                    dateAt(AnalysisWork.KEY_TO),
                    ::publishStage,
                )

            AnalysisKind.WORKOUT_DAILY, AnalysisKind.WORKOUT_WEEKLY, AnalysisKind.WORKOUT_MONTHLY ->
                analyzeWorkout(
                    kind,
                    scopeKey,
                    periodLabel,
                    dateAt(AnalysisWork.KEY_FROM),
                    dateAt(AnalysisWork.KEY_TO),
                    ::publishStage,
                )
        }
    }

    private fun dateAt(key: String): LocalDate {
        val epochDay = inputData.getLong(key, AnalysisWork.NO_DATE)
        require(epochDay != AnalysisWork.NO_DATE) { "기간 분석에 날짜가 없습니다" }
        return LocalDate.ofEpochDay(epochDay)
    }

    /** 지금 무엇을 하고 있는지 화면이 볼 수 있는 자리에 옮긴다. */
    private suspend fun publishStage(stage: AnalysisStage) {
        setProgress(workDataOf(AnalysisWork.KEY_STAGE to stage.name))
    }

    private companion object {
        const val UNKNOWN_KIND = "분석하지 못했습니다"
    }

    private fun Throwable.toReason(): String = when (this) {
        is NoRecordToAnalyzeException -> "분석할 기록이 없습니다"
        is NoDailyAnalysisException -> "먼저 날짜별로 분석해 주세요"
        is AiCredentialMissingException -> "AI 연결이 필요해요"
        is AiUnauthorizedException -> "키가 올바르지 않습니다"
        is AiRequestFailedException -> reason ?: "분석하지 못했습니다"
        else -> "분석하지 못했습니다"
    }
}
