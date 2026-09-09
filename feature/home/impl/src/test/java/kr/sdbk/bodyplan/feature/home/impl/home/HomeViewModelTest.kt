package kr.sdbk.bodyplan.feature.home.impl.home

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutLog
import kr.sdbk.bodyplan.core.domain.model.WorkoutSet
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.domain.repository.WeightLogRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository
import kr.sdbk.bodyplan.core.domain.usecase.GetProgressSummaryUseCase
import kr.sdbk.bodyplan.core.domain.usecase.GetWeightTrendUseCase
import kr.sdbk.bodyplan.core.domain.usecase.SummarizeWorkoutVolumeUseCase
import kr.sdbk.bodyplan.feature.home.impl.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

internal class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today: LocalDate = LocalDate.of(2026, 9, 10)
    private val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(weightLogRepository: FakeWeightLogRepository = FakeWeightLogRepository()) = HomeViewModel(
        getProgressSummary = GetProgressSummaryUseCase(
            weightLogRepository = weightLogRepository,
            workoutLogRepository = FakeWorkoutLogRepository(),
            userProfileRepository = FakeUserProfileRepository(),
            analysisResultRepository = FakeAnalysisResultRepository(),
            getWeightTrend = GetWeightTrendUseCase(),
            summarizeWorkoutVolume = SummarizeWorkoutVolumeUseCase(),
            clock = clock,
        ),
    )

    @Test
    fun `구독하면 초기 로드가 돌아 summary가 채워지고 로딩이 끝난다`() = runTest {
        val viewModel = viewModel()

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.summary)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `조회가 실패하면 에러 메시지가 실리고 summary는 그대로다`() = runTest {
        val repository = FakeWeightLogRepository()
        repository.failure = IllegalStateException("boom")
        val viewModel = viewModel(weightLogRepository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("불러오지 못했습니다", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.summary)
    }

    @Test
    fun `실패 뒤 재시도하면 에러가 지워지고 summary가 채워진다`() = runTest {
        val repository = FakeWeightLogRepository()
        repository.failure = IllegalStateException("boom")
        val viewModel = viewModel(weightLogRepository = repository)

        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        repository.failure = null
        viewModel.handleIntent(HomeIntent.ClickRetry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertNotNull(viewModel.uiState.value.summary)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeWeightLogRepository(private val records: List<WeightRecord> = emptyList()) :
        WeightLogRepository {
        var failure: Throwable? = null

        override fun observeRecordsInRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>> = flow {
            failure?.let { throw it }
            emit(records.filter { !it.date.isBefore(from) && !it.date.isAfter(to) })
        }

        override suspend fun save(date: LocalDate, weightKg: Double) = error("사용하지 않음")
    }

    private class FakeWorkoutLogRepository(
        private val entriesByDate: Map<LocalDate, List<WorkoutEntry>> = emptyMap(),
    ) : WorkoutLogRepository {
        override fun observeLog(date: LocalDate): Flow<WorkoutLog> = error("사용하지 않음")

        override fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>> =
            error("사용하지 않음")

        override fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>> =
            flowOf(entriesByDate.filterKeys { !it.isBefore(from) && !it.isAfter(to) })

        override suspend fun getEntry(id: Long): WorkoutEntry? = error("사용하지 않음")

        override suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long =
            error("사용하지 않음")

        override suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>) {
            error("사용하지 않음")
        }

        override suspend fun deleteEntry(id: Long) {
            error("사용하지 않음")
        }

        override suspend fun saveMemo(date: LocalDate, text: String) {
            error("사용하지 않음")
        }
    }

    private class FakeUserProfileRepository(private val profile: UserProfile = UserProfile()) :
        UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile> = flowOf(profile)

        override suspend fun getProfile(): UserProfile = error("사용하지 않음")

        override suspend fun saveProfile(profile: UserProfile) = error("사용하지 않음")
    }

    private class FakeAnalysisResultRepository(private val history: List<AnalysisResult> = emptyList()) :
        AnalysisResultRepository {
        override fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?> = error("사용하지 않음")

        override suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult> =
            error("사용하지 않음")

        override fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>> = flowOf(history)

        override suspend fun save(
            kind: AnalysisKind,
            scopeKey: String,
            content: AnalysisContent,
            imageFileName: String?,
            measurement: InbodyMeasurement?,
        ) = error("사용하지 않음")
    }
}
