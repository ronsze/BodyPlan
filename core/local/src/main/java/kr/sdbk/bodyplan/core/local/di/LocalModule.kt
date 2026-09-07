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
import kr.sdbk.bodyplan.core.local.dao.UserProfileDao
import kr.sdbk.bodyplan.core.local.dao.WorkoutEntryDao
import kr.sdbk.bodyplan.core.local.datastore.AppPreferences
import kr.sdbk.bodyplan.core.local.migration.MIGRATION_1_2
import kr.sdbk.bodyplan.core.local.migration.MIGRATION_2_3

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BodyPlanDatabase =
        Room.databaseBuilder(context, BodyPlanDatabase::class.java, DATABASE_NAME)
            .addCallback(SeedExercisesCallback)
            // 스키마가 바뀌어도 이미 쌓인 기록을 지우지 않는다. 버전을 올릴 때마다 마이그레이션을 더한다.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideExerciseDao(database: BodyPlanDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideWorkoutEntryDao(database: BodyPlanDatabase): WorkoutEntryDao = database.workoutEntryDao()

    @Provides
    fun provideDietEntryDao(database: BodyPlanDatabase): DietEntryDao = database.dietEntryDao()

    @Provides
    fun provideUserProfileDao(database: BodyPlanDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences = AppPreferences(context)
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
