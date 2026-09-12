package kr.sdbk.bodyplan.core.local.snapshot

import kotlinx.serialization.Serializable
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity
import kr.sdbk.bodyplan.core.local.entity.DietEntryEntity
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineEntryEntity
import kr.sdbk.bodyplan.core.local.entity.RoutineSetEntity
import kr.sdbk.bodyplan.core.local.entity.UserProfileEntity
import kr.sdbk.bodyplan.core.local.entity.WeightRecordEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutEntryEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutMemoEntity
import kr.sdbk.bodyplan.core.local.entity.WorkoutSetEntity

/**
 * DB 전체를 한 시점에 떠 놓은 것. 백업 파일의 본문이다.
 *
 * 표마다 행을 그대로 담는다 — 스냅샷은 스키마의 사본이라 별도 DTO를 두지 않고,
 * 대신 [dbVersion]으로 어느 스키마의 것인지 묶는다. 다른 버전의 스냅샷은 들이지 않는다.
 */
@Serializable
data class BodyPlanSnapshot(
    val formatVersion: Int = FORMAT_VERSION,
    val dbVersion: Int,
    val exportedAtMillis: Long,
    val exercises: List<ExerciseEntity>,
    val workoutEntries: List<WorkoutEntryEntity>,
    val workoutSets: List<WorkoutSetEntity>,
    val dietEntries: List<DietEntryEntity>,
    val userProfiles: List<UserProfileEntity>,
    val analysisResults: List<AnalysisResultEntity>,
    val weightRecords: List<WeightRecordEntity>,
    val workoutMemos: List<WorkoutMemoEntity>,
    val routines: List<RoutineEntity>,
    val routineEntries: List<RoutineEntryEntity>,
    val routineSets: List<RoutineSetEntity>,
) {
    companion object {
        const val FORMAT_VERSION = 1
    }
}
