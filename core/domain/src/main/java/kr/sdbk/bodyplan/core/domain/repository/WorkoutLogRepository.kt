package kr.sdbk.bodyplan.core.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet

/** 운동 기록 저장소. 실패는 삼키지 않고 호출부로 던진다. */
interface WorkoutLogRepository {
    fun observeLog(date: LocalDate): Flow<WorkoutLog>

    /** 기록이 있는 날짜만 담긴 맵을 낸다. 캘린더가 쓴다. */
    fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>>

    suspend fun getEntry(id: Long): WorkoutEntry?

    suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long

    /** 종목까지 바꿀 수 있으므로 [exercise]를 받아 스냅샷을 갱신하고 세트를 갈아끼운다. */
    suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>)

    suspend fun deleteEntry(id: Long)
}
