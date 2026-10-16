package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kr.sdbk.bodyplan.core.domain.repository.InbodyImageRepository
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository

/**
 * 사진을 앱 안으로 복사하고 기록으로 저장한다.
 *
 * 기록 저장이 실패하면 복사본은 가리키는 곳이 없으므로 지운다.
 */
class AddInbodyRecordUseCase
@Inject
constructor(
    private val inbodyImageRepository: InbodyImageRepository,
    private val inbodyRecordRepository: InbodyRecordRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(sourceUri: String, measuredOn: LocalDate): Long {
        // 사용자가 시간대를 옮기면 고른 날이 새 오늘보다 뒤일 수 있다. 저장 직전에 다시 맞춘다.
        val date = minOf(measuredOn, LocalDate.now(clock))
        val fileName = inbodyImageRepository.save(sourceUri)
        return try {
            inbodyRecordRepository.insert(fileName, date)
        } catch (throwable: Throwable) {
            // 취소된 코루틴에서는 보통 일이 돌지 않아 NonCancellable로 감싸지 않으면 복사본만 남는다.
            withContext(NonCancellable) { inbodyImageRepository.delete(fileName) }
            throw throwable
        }
    }
}
