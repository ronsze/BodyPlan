package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisStage
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.InbodyImageRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository

/**
 * 인바디 사진을 분석하고 결과를 사진과 함께 저장한다.
 *
 * 사진을 먼저 앱 안으로 복사해야 이력에서 다시 볼 수 있다. 다만 분석이 실패하면 그 복사본을
 * 쓸 곳이 없으므로 지운다 — 남기면 아무 결과도 가리키지 않는 파일이 쌓인다.
 */
class AnalyzeInbodyUseCase
@Inject
constructor(
    private val userProfileRepository: UserProfileRepository,
    private val aiCredentialRepository: AiCredentialRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
    private val analysisResultRepository: AnalysisResultRepository,
    private val inbodyImageRepository: InbodyImageRepository,
) {
    suspend operator fun invoke(sourceUri: String, onStage: suspend (AnalysisStage) -> Unit = {}): AnalysisContent {
        onStage(AnalysisStage.COLLECTING)
        val credential = aiCredentialRepository.getCredential() ?: throw AiCredentialMissingException()
        val profile = userProfileRepository.getProfile()

        onStage(AnalysisStage.PREPARING_IMAGES)
        val fileName = inbodyImageRepository.save(sourceUri)
        onStage(AnalysisStage.CALLING)
        val analysis = try {
            aiAnalysisRepository.analyzeInbody(
                InbodyAnalysisRequest(
                    credential = credential,
                    profile = profile,
                    imagePath = inbodyImageRepository.pathOf(fileName),
                ),
            )
        } catch (throwable: Throwable) {
            // 화면을 벗어나 취소된 경우에도 지운다. 취소된 코루틴에서는 보통 일이 돌지 않아
            // NonCancellable로 감싸지 않으면 복사본만 남는다 — 가장 흔한 실패 경로다.
            withContext(NonCancellable) { inbodyImageRepository.delete(fileName) }
            throw throwable
        }

        onStage(AnalysisStage.PARSING)
        // 인바디는 매번 새 결과를 쌓는다. 같은 대상이라는 개념이 없어 열쇠가 비어 있다.
        analysisResultRepository.save(
            kind = AnalysisKind.INBODY,
            scopeKey = "",
            content = analysis.content,
            imageFileName = fileName,
            measurement = analysis.measurement,
        )
        updateProfile(analysis.measurement)
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
    private suspend fun updateProfile(measurement: InbodyMeasurement) {
        if (measurement.isEmpty) return
        runCatching {
            withContext(NonCancellable) {
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
