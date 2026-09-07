package kr.sdbk.bodyplan.core.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.data.mapper.toEntity
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.local.dao.UserProfileDao

internal class UserProfileRepositoryImpl
@Inject
constructor(private val userProfileDao: UserProfileDao) :
    UserProfileRepository {
    // 저장된 적이 없으면 빈 프로필로 본다. 화면이 null을 따로 다루지 않게 한다.
    override fun observeProfile(): Flow<UserProfile> =
        userProfileDao.observeProfile().map { it?.toDomain() ?: UserProfile() }

    override suspend fun getProfile(): UserProfile = userProfileDao.getProfile()?.toDomain() ?: UserProfile()

    override suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.upsert(profile.toEntity())
    }
}
