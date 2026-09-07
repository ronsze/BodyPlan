package kr.sdbk.bodyplan.feature.dietlog.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository

internal class FakeUserProfileRepository(initial: UserProfile = UserProfile()) : UserProfileRepository {
    private val profile = MutableStateFlow(initial)

    override fun observeProfile(): Flow<UserProfile> = profile

    override suspend fun getProfile(): UserProfile = profile.value

    override suspend fun saveProfile(profile: UserProfile) {
        this.profile.value = profile
    }
}
