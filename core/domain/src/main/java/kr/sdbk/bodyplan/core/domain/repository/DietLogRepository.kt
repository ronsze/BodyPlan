package kr.sdbk.bodyplan.core.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.domain.model.DietLog

/** 식단 기록 저장소. 실패는 삼키지 않고 호출부로 던진다. */
interface DietLogRepository {
    fun observeLog(date: LocalDate): Flow<DietLog>

    /** 날짜마다 첫 항목의 사진 경로. 기록이 없는 날짜는 담지 않는다. */
    fun observeFirstImageInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, String>>

    suspend fun getEntry(id: Long): DietEntry?

    /** [sourceImageUri]는 사진 선택기가 준 값이다. 앱 내부 저장소로 옮기는 일은 구현이 맡는다. */
    suspend fun addEntry(date: LocalDate, sourceImageUri: String, memo: String?): Long

    /** [sourceImageUri]가 null이면 사진을 그대로 두고 [memo]만 고친다. */
    suspend fun updateEntry(entryId: Long, sourceImageUri: String?, memo: String?)

    suspend fun deleteEntry(id: Long)
}
