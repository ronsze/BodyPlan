package kr.sdbk.bodyplan.core.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.repository.OnboardingRepository
import kr.sdbk.bodyplan.core.local.datastore.AppPreferences

internal class OnboardingRepositoryImpl
@Inject
constructor(private val preferences: AppPreferences) :
    OnboardingRepository {
    override fun observeCompleted(): Flow<Boolean> = preferences.onboardingCompleted

    override suspend fun markCompleted() {
        preferences.markOnboardingCompleted()
    }
}
