package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.UserProfile

/** 사용자 정보는 한 벌만 있다. 저장하면 이전 값을 덮는다. */
interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfile>

    suspend fun getProfile(): UserProfile

    suspend fun saveProfile(profile: UserProfile)
}
