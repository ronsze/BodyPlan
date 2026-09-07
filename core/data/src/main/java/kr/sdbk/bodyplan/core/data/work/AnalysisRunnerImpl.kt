package kr.sdbk.bodyplan.core.data.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisRun
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunRequest
import kr.sdbk.bodyplan.core.domain.model.AnalysisRunState
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage
import kr.sdbk.bodyplan.core.domain.repository.AnalysisRunner

internal class AnalysisRunnerImpl
@Inject
constructor(private val workManager: WorkManager) : AnalysisRunner {
    /**
     * 같은 대상이 돌고 있으면 그것을 그대로 둔다.
     *
     * [ExistingWorkPolicy.KEEP]이라 두 번 눌러도 호출이 두 번 나가지 않는다. 다시 분석하려면
     * 앞의 것이 끝난 뒤에 누른다 — 끝난 작업은 이 이름을 붙들고 있지 않다.
     */
    override suspend fun start(request: AnalysisRunRequest) {
        // 넣기가 실패하면 화면은 분석 중도 실패도 보지 못한 채 멈춘다. 기다려서 던진다.
        workManager.enqueueUniqueWork(
            AnalysisWork.nameOf(request.kind, request.scopeKey),
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AnalysisWorker>()
                .setInputData(
                    workDataOf(
                        AnalysisWork.KEY_KIND to request.kind.name,
                        AnalysisWork.KEY_SCOPE to request.scopeKey,
                        AnalysisWork.KEY_PERIOD_LABEL to request.periodLabel,
                        AnalysisWork.KEY_FROM to (request.from?.toEpochDay() ?: AnalysisWork.NO_DATE),
                        AnalysisWork.KEY_TO to (request.to?.toEpochDay() ?: AnalysisWork.NO_DATE),
                        AnalysisWork.KEY_SOURCE_URI to request.sourceUri,
                    ),
                )
                .build(),
        ).await()
    }

    override fun observe(kind: AnalysisKind, scopeKey: String): Flow<AnalysisRun?> =
        workManager.getWorkInfosForUniqueWorkFlow(AnalysisWork.nameOf(kind, scopeKey))
            // 고유 작업이라 한 이름에 하나만 남는다. 순서에 기대지 않는다.
            .map { infos -> infos.firstOrNull()?.toRun(kind, scopeKey) }
}

private fun WorkInfo.toRun(kind: AnalysisKind, scopeKey: String): AnalysisRun? {
    val runState = when (state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> AnalysisRunState.RUNNING

        WorkInfo.State.SUCCEEDED -> AnalysisRunState.SUCCEEDED

        WorkInfo.State.FAILED -> AnalysisRunState.FAILED

        // 취소는 사용자가 그만둔 것이라 실패로 알리지 않는다. 돌고 있지 않은 것과 같게 본다.
        WorkInfo.State.CANCELLED -> return null
    }
    return AnalysisRun(
        kind = kind,
        scopeKey = scopeKey,
        state = runState,
        stage = progress.getString(AnalysisWork.KEY_STAGE)
            ?.let { name -> AnalysisStage.entries.firstOrNull { it.name == name } },
        failureReason = outputData.getString(AnalysisWork.KEY_REASON),
    )
}
