package kr.sdbk.bodyplan.core.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult

/** 인바디 기록과 그 분석 결과를 함께 다룬다. */
interface InbodyRecordRepository {
    fun observeRecords(): Flow<List<InbodyRecordWithResult>>

    suspend fun getRecords(): List<InbodyRecordWithResult>

    suspend fun getRecord(id: Long): InbodyRecord?

    /** [imageFileName]은 사진 보관소에 이미 복사된 파일이다. 새 기록의 번호를 낸다. */
    suspend fun insert(imageFileName: String, measuredOn: LocalDate): Long

    /** `null`이면 지운다. */
    suspend fun setFailureReason(id: Long, reason: String?)
}
