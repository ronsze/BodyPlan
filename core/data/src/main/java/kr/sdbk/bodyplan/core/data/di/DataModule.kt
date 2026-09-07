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
import kr.sdbk.bodyplan.core.data.repository.AiAnalysisRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.AiCredentialRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.AnalysisResultRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.DietLogRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.ExerciseRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.OnboardingRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.UserProfileRepositoryImpl
import kr.sdbk.bodyplan.core.data.repository.WorkoutLogRepositoryImpl
import kr.sdbk.bodyplan.core.domain.repository.AiAnalysisRepository
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.DietLogRepository
import kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository
import kr.sdbk.bodyplan.core.domain.repository.OnboardingRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
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

    @Binds
    @Singleton
    abstract fun bindAiCredentialRepository(impl: AiCredentialRepositoryImpl): AiCredentialRepository

    @Binds
    @Singleton
    abstract fun bindAiAnalysisRepository(impl: AiAnalysisRepositoryImpl): AiAnalysisRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindAnalysisResultRepository(impl: AnalysisResultRepositoryImpl): AnalysisResultRepository

    companion object {
        /** 날짜 판정 UseCase가 오늘을 읽는 창구. 테스트가 고정 시각을 넣을 수 있게 주입한다. */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
