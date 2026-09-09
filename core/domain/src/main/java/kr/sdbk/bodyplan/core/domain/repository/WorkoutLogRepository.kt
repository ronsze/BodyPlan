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

    /**
     * [from]부터 [to]까지의 기록을 날짜별로 묶어 낸다. 기록이 없는 날짜는 담지 않는다.
     *
     * 하루씩 [observeLog]를 여러 번 여는 대신 한 번에 읽는다 — 기간이 길어질수록 여는 Flow가 늘어난다.
     */
    fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>>

    suspend fun getEntry(id: Long): WorkoutEntry?

    suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long

    /** 종목까지 바꿀 수 있으므로 [exercise]를 받아 스냅샷을 갱신하고 세트를 갈아끼운다. */
    suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>)

    suspend fun deleteEntry(id: Long)

    /** 그날의 메모를 덮어쓴다. [text]가 공백뿐이면 메모를 지운다. */
    suspend fun saveMemo(date: LocalDate, text: String)
}
