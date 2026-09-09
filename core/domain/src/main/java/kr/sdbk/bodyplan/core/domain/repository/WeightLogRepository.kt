package kr.sdbk.bodyplan.core.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.WeightRecord

/** 체중 기록 저장소. 실패는 삼키지 않고 호출부로 던진다. */
interface WeightLogRepository {
    /** [from]부터 [to]까지의 기록. 오래된 날짜부터 담고, 기록이 없는 날짜는 담지 않는다. */
    fun observeRecordsInRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>>

    /** 그 날짜의 값을 넣거나 덮어쓴다. 하루에 한 값만 남는다. */
    suspend fun save(date: LocalDate, weightKg: Double)
}
