package kr.sdbk.bodyplan.core.local

import androidx.room.Database
import androidx.room.RoomDatabase
import kr.sdbk.bodyplan.core.local.dao.AnalysisResultDao
import kr.sdbk.bodyplan.core.local.dao.DietEntryDao
import kr.sdbk.bodyplan.core.local.dao.ExerciseDao
import kr.sdbk.bodyplan.core.local.dao.RoutineDao
import kr.sdbk.bodyplan.core.local.dao.UserProfileDao
import kr.sdbk.bodyplan.core.local.dao.WeightRecordDao
import kr.sdbk.bodyplan.core.local.dao.WorkoutEntryDao
import kr.sdbk.bodyplan.core.local.dao.WorkoutMemoDao
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

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntryEntity::class,
        WorkoutSetEntity::class,
        DietEntryEntity::class,
        UserProfileEntity::class,
        AnalysisResultEntity::class,
        WeightRecordEntity::class,
        WorkoutMemoEntity::class,
        RoutineEntity::class,
        RoutineEntryEntity::class,
        RoutineSetEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
abstract class BodyPlanDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    abstract fun workoutEntryDao(): WorkoutEntryDao

    abstract fun dietEntryDao(): DietEntryDao

    abstract fun userProfileDao(): UserProfileDao

    abstract fun analysisResultDao(): AnalysisResultDao

    abstract fun weightRecordDao(): WeightRecordDao

    abstract fun workoutMemoDao(): WorkoutMemoDao

    abstract fun routineDao(): RoutineDao
}
