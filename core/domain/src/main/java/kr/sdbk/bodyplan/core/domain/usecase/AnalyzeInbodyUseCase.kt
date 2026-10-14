package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository

/**
 * 기록 하나의 사진을 분석하고 그 기록의 결과를 새 결과로 바꾼다.
 *
 * 프로필은 그 기록이 가장 최근 측정일 때만 갱신한다 — 과거 사진을 다시 분석할 때 옛 값이
 * 프로필을 덮지 않게 하려는 것이다.
 */
class AnalyzeInbodyUseCase
@Inject
constructor(
    private val userProfileRepository: UserProfileRepository,
    private val aiCredentialRepository: AiCredentialRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val analysisResultRepository: AnalysisResultRepository,
    private val inbodyRecordRepository: InbodyRecordRepository,
) {
    suspend operator fun invoke(recordId: Long, onStage: suspend (AnalysisStage) -> Unit = {}): AnalysisContent {
        onStage(AnalysisStage.COLLECTING)
        val credential = aiCredentialRepository.getCredential() ?: throw AiCredentialMissingException()
        val profile = userProfileRepository.getProfile()
        val record = inbodyRecordRepository.getRecord(recordId) ?: error("인바디 기록을 찾을 수 없습니다: $recordId")

        onStage(AnalysisStage.PREPARING_IMAGES)
        onStage(AnalysisStage.CALLING)
        val analysis = aiAnalysisRepository.analyzeInbody(
            InbodyAnalysisRequest(credential = credential, profile = profile, imagePath = record.imagePath),
        )

        onStage(AnalysisStage.PARSING)
        analysisResultRepository.replace(
            kind = AnalysisKind.INBODY,
            scopeKey = recordId.toString(),
            content = analysis.content,
            measurement = analysis.measurement,
        )
        updateProfileIfLatest(recordId, analysis.measurement)
        return analysis.content
    }

    /**
     * 읽어 낸 값만 프로필에 덮어쓴다.
     *
     * 결과지가 키를 싣지 않는 경우가 흔해, 못 읽은 값까지 지우면 사용자가 손수 넣은 것이
     * 사라진다. 소수점은 프로필이 정수로 들고 있어 반올림한다.
     *
     * 프로필을 여기서 다시 읽는 것은, 분석이 도는 수십 초 사이에 사용자가 나이나 목적을
     * 고쳤을 수 있어서다. 부르기 전에 읽어 둔 것에 덮어쓰면 그 편집이 되돌아간다.
     *
     * 갱신에 실패해도 분석은 성공이다. 결과는 이미 저장됐고, 프로필 갱신은 곁다리다.
     */
    private suspend fun updateProfileIfLatest(recordId: Long, measurement: InbodyMeasurement) {
        if (measurement.isEmpty) return
        runCatching {
            withContext(NonCancellable) {
                // 결과를 저장한 뒤의 기록 전체로 판정한다. 두 분석이 거의 동시에 끝나도 마지막 반영이 맞다.
                val entries = inbodyRecordRepository.getRecords().map { InbodyEntry(it.record, it.result, null) }
                if (InbodyTimeline.latestMeasured(entries)?.record?.id != recordId) return@withContext

                val current = userProfileRepository.getProfile()
                val updated = current.copy(
                    heightCm = measurement.heightCm?.roundToInt() ?: current.heightCm,
                    weightKg = measurement.weightKg?.roundToInt() ?: current.weightKg,
                )
                if (updated != current) userProfileRepository.saveProfile(updated)
            }
        }
    }
}
