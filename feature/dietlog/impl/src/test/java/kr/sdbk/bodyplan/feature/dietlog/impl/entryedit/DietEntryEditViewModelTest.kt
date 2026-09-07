package kr.sdbk.bodyplan.feature.dietlog.impl.entryedit

import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.feature.dietlog.api.DietEntryEditNavKey
import kr.sdbk.bodyplan.feature.dietlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.dietlog.impl.fake.FakeDietLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class DietEntryEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val date: LocalDate = LocalDate.of(2026, 9, 7)
    private val stored = DietEntry(id = 5L, imagePath = "/files/diet_images/old.jpg", memo = "닭가슴살")

    private fun viewModel(entryId: Long? = null, repository: FakeDietLogRepository = FakeDietLogRepository()) =
        DietEntryEditViewModel(
            dietLogRepository = repository,
            navKey = DietEntryEditNavKey(date.toEpochDay(), entryId),
        )

    @Test
    fun `사진을 고르기 전에는 저장할 수 없다`() = runTest {
        val viewModel = viewModel()

        subscribe(viewModel)

        assertFalse(viewModel.uiState.value.canSave)
        assertNull(viewModel.uiState.value.previewImage)
    }

    @Test
    fun `사진을 고르면 미리 보기가 생기고 저장할 수 있다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(DietEntryEditIntent.PickImage("content://photo/1"))

        assertEquals("content://photo/1", viewModel.uiState.value.previewImage)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `텍스트를 비워도 저장된다`() = runTest {
        val repository = FakeDietLogRepository()
        val viewModel = viewModel(repository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(DietEntryEditIntent.PickImage("content://photo/1"))
        viewModel.handleIntent(DietEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(1, repository.addedCount)
        assertNull(repository.lastSavedMemo)
    }

    @Test
    fun `공백만 있는 텍스트는 없는 것으로 저장한다`() = runTest {
        val repository = FakeDietLogRepository()
        val viewModel = viewModel(repository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(DietEntryEditIntent.PickImage("content://photo/1"))
        viewModel.handleIntent(DietEntryEditIntent.ChangeMemo("   "))
        viewModel.handleIntent(DietEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertNull(repository.lastSavedMemo)
    }

    @Test
    fun `신규 저장은 고른 사진으로 추가한다`() = runTest {
        val repository = FakeDietLogRepository()
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(DietEntryEditIntent.PickImage("content://photo/1"))
        viewModel.handleIntent(DietEntryEditIntent.ChangeMemo("닭가슴살"))
        viewModel.handleIntent(DietEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertEquals("content://photo/1", repository.lastAddedUri)
        assertEquals("닭가슴살", repository.lastSavedMemo)
        assertTrue(effects.any { it is DietEntryEditEffect.GoBack })
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `수정 진입이면 저장된 사진과 텍스트가 채워진다`() = runTest {
        val viewModel = viewModel(
            entryId = stored.id,
            repository = FakeDietLogRepository(listOf(stored)),
        )

        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(stored.imagePath, state.storedImagePath)
        assertEquals("닭가슴살", state.memo)
        assertEquals(stored.imagePath, state.previewImage)
        assertTrue(state.canSave)
    }

    @Test
    fun `사진을 바꾸지 않고 저장하면 사진 자리를 비워 넘긴다`() = runTest {
        val repository = FakeDietLogRepository(listOf(stored))
        val viewModel = viewModel(entryId = stored.id, repository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(DietEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(stored.id, repository.lastUpdatedEntryId)
        assertNull(repository.lastUpdatedUri)
        assertEquals(0, repository.addedCount)
    }

    @Test
    fun `사진을 바꾸면 새 사진으로 수정한다`() = runTest {
        val repository = FakeDietLogRepository(listOf(stored))
        val viewModel = viewModel(entryId = stored.id, repository = repository)
        subscribe(viewModel)

        viewModel.handleIntent(DietEntryEditIntent.PickImage("content://photo/2"))
        viewModel.handleIntent(DietEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertEquals("content://photo/2", repository.lastUpdatedUri)
    }

    @Test
    fun `저장이 실패하면 화면이 닫히지 않는다`() = runTest {
        val repository = FakeDietLogRepository()
        repository.mutateFailure = IllegalStateException("boom")
        val viewModel = viewModel(repository = repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(DietEntryEditIntent.PickImage("content://photo/1"))
        viewModel.handleIntent(DietEntryEditIntent.ClickSave)
        advanceUntilIdle()

        assertFalse(effects.any { it is DietEntryEditEffect.GoBack })
        assertTrue(effects.any { it is DietEntryEditEffect.ShowMessage })
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `복원에 실패하면 에러가 실린다`() = runTest {
        val viewModel = viewModel(entryId = 999L, repository = FakeDietLogRepository())

        subscribe(viewModel)

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: DietEntryEditViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: DietEntryEditViewModel,
    ): List<DietEntryEditEffect> {
        val effects = mutableListOf<DietEntryEditEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
