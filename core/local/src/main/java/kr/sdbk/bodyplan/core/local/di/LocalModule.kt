package kr.sdbk.bodyplan.core.local.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kr.sdbk.bodyplan.core.local.BodyPlanDatabase
import kr.sdbk.bodyplan.core.local.DefaultExercises
import kr.sdbk.bodyplan.core.local.dao.DietEntryDao
import kr.sdbk.bodyplan.core.local.dao.ExerciseDao
import kr.sdbk.bodyplan.core.local.dao.WorkoutEntryDao

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BodyPlanDatabase =
        Room.databaseBuilder(context, BodyPlanDatabase::class.java, DATABASE_NAME)
            .addCallback(SeedExercisesCallback)
            // 배포 전까지만 쓴다. 스키마가 자주 바뀌는 동안 실행될 일 없는 마이그레이션이 쌓이는 것을 막는다.
            // 개발 기기의 기존 데이터는 스키마가 바뀔 때마다 지워진다. 첫 배포 시점에 떼고 정식 마이그레이션으로 바꾼다.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideExerciseDao(database: BodyPlanDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideWorkoutEntryDao(database: BodyPlanDatabase): WorkoutEntryDao = database.workoutEntryDao()

    @Provides
    fun provideDietEntryDao(database: BodyPlanDatabase): DietEntryDao = database.dietEntryDao()
}

/**
 * 첫 생성 때만 기본 종목을 넣는다.
 * DAO는 이 시점에 아직 쓸 수 없어 원시 SQL로 넣는다.
 */
private object SeedExercisesCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultExercises.all.forEach { exercise ->
            db.execSQL(
                "INSERT INTO exercise (bodyPart, name, intensityType, isDeleted) VALUES (?, ?, ?, 0)",
                arrayOf(exercise.bodyPart, exercise.name, exercise.intensityType),
            )
        }
    }
}

private const val DATABASE_NAME = "bodyplan.db"
