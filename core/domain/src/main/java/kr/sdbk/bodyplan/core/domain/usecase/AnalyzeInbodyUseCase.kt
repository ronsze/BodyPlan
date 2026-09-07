package kr.sdbk.bodyplan.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
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
    suspend operator fun invoke(sourceUri: String): AnalysisContent {
        val credential = aiCredentialRepository.getCredential() ?: throw AiCredentialMissingException()
        val profile = userProfileRepository.getProfile()

        val fileName = inbodyImageRepository.save(sourceUri)
        val content = try {
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

        // 인바디는 매번 새 결과를 쌓는다. 같은 대상이라는 개념이 없어 열쇠가 비어 있다.
        analysisResultRepository.save(
            kind = AnalysisKind.INBODY,
            scopeKey = "",
            content = content,
            imageFileName = fileName,
        )
        return content
    }
}
