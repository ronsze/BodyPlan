package kr.sdbk.bodyplan.feature.my.impl.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.sdbk.bodyplan.core.domain.repository.OnboardingRepository

internal class FakeOnboardingRepository(initial: Boolean = false) : OnboardingRepository {
    private val completed = MutableStateFlow(initial)

    var markFailure: Throwable? = null

    val isCompleted: Boolean get() = completed.value

    override fun observeCompleted(): Flow<Boolean> = completed

    override suspend fun markCompleted() {
        markFailure?.let { throw it }
        completed.value = true
    }
}
