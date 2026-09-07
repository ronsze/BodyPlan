package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** 온보딩을 한 번 지나갔는지만 기억한다. 건너뛴 것도 지나간 것으로 본다. */
interface OnboardingRepository {
    fun observeCompleted(): Flow<Boolean>

    suspend fun markCompleted()
}
