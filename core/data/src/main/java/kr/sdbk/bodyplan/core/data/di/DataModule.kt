package kr.sdbk.bodyplan.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kr.sdbk.bodyplan.core.data.image.DietImageStore
import kr.sdbk.bodyplan.core.data.image.DietImageStoreImpl
import kr.sdbk.bodyplan.core.data.repository.DietLogRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.ExerciseRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.WorkoutLogRepositoryImpl
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutLogRepository(impl: WorkoutLogRepositoryImpl): WorkoutLogRepository

    @Binds
    @Singleton
    abstract fun bindDietLogRepository(impl: DietLogRepositoryImpl): DietLogRepository

    @Binds
    @Singleton
    abstract fun bindDietImageStore(impl: DietImageStoreImpl): DietImageStore

    companion object {
        /** 날짜 판정 UseCase가 오늘을 읽는 창구. 테스트가 고정 시각을 넣을 수 있게 주입한다. */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
