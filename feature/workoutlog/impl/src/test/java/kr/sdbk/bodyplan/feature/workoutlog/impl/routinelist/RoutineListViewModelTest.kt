package kr.sdbk.bodyplan.feature.workoutlog.impl.routinelist

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Routine
import kr.sdbk.bodyplan.feature.workoutlog.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeRoutineRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class RoutineListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun routine(id: Long, name: String, bodyPart: BodyPart) =
        Routine(id = id, name = name, bodyPart = bodyPart, entries = emptyList())

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: RoutineListViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: RoutineListViewModel,
    ): List<RoutineListEffect> {
        val effects = mutableListOf<RoutineListEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.effect.collect { effects += it } }
        return effects
    }

    @Test
    fun `루틴 목록은 부위 순서로 묶인다`() = runTest {
        val repository = FakeRoutineRepository(
            listOf(
                routine(1L, "하체A", BodyPart.LEG),
                routine(2L, "가슴A", BodyPart.CHEST),
            ),
        )
        val viewModel = RoutineListViewModel(repository)

        subscribe(viewModel)

        assertEquals(
            BodyPart.entries.filter { it == BodyPart.CHEST || it == BodyPart.LEG },
            viewModel.uiState.value.sections.map { it.first },
        )
    }

    @Test
    fun `루틴이 없는 부위는 묶음에 나오지 않는다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine(1L, "가슴A", BodyPart.CHEST)))
        val viewModel = RoutineListViewModel(repository)

        subscribe(viewModel)

        assertEquals(listOf(BodyPart.CHEST), viewModel.uiState.value.sections.map { it.first })
    }

    @Test
    fun `루틴이 하나도 없으면 목록이 비어 있다`() = runTest {
        val viewModel = RoutineListViewModel(FakeRoutineRepository())

        subscribe(viewModel)

        assertTrue(viewModel.uiState.value.routines.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `루틴 추가를 누르면 대화상자가 초기값으로 열린다`() = runTest {
        val viewModel = RoutineListViewModel(FakeRoutineRepository())
        subscribe(viewModel)

        viewModel.handleIntent(RoutineListIntent.ClickAddRoutine)

        assertTrue(viewModel.uiState.value.isDialogVisible)
        assertEquals("", viewModel.uiState.value.dialogName)
        assertEquals(BodyPart.CHEST, viewModel.uiState.value.dialogBodyPart)
    }

    @Test
    fun `이름이 공백뿐이면 안내 메시지가 나가고 대화상자가 유지된다`() = runTest {
        val viewModel = RoutineListViewModel(FakeRoutineRepository())
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(RoutineListIntent.ClickAddRoutine)
        viewModel.handleIntent(RoutineListIntent.ChangeDialogName("   "))

        viewModel.handleIntent(RoutineListIntent.ConfirmDialog)
        advanceUntilIdle()

        assertTrue(
            effects.filterIsInstance<RoutineListEffect.ShowMessage>().any { it.message == "루틴 이름을 입력하세요" },
        )
        assertTrue(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun `만들기를 누르면 루틴이 생기고 상세로 이동한다`() = runTest {
        val repository = FakeRoutineRepository()
        val viewModel = RoutineListViewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(RoutineListIntent.ClickAddRoutine)
        viewModel.handleIntent(RoutineListIntent.ChangeDialogName("하체 루틴"))
        viewModel.handleIntent(RoutineListIntent.SelectDialogBodyPart(BodyPart.LEG))

        viewModel.handleIntent(RoutineListIntent.ConfirmDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDialogVisible)
        assertEquals("하체 루틴", repository.lastAddedRoutineName)
        assertEquals(BodyPart.LEG, repository.lastAddedRoutineBodyPart)
        assertTrue(effects.any { it is RoutineListEffect.NavigateToRoutineDetail })
    }

    @Test
    fun `이름 입력은 20자를 넘기면 잘린다`() = runTest {
        val viewModel = RoutineListViewModel(FakeRoutineRepository())
        subscribe(viewModel)

        viewModel.handleIntent(RoutineListIntent.ChangeDialogName("가".repeat(30)))

        assertEquals(20, viewModel.uiState.value.dialogName.length)
    }

    @Test
    fun `생성이 실패하면 메시지가 나가고 대화상자와 입력이 유지된다`() = runTest {
        val repository = FakeRoutineRepository()
        repository.mutateFailure = IllegalStateException("boom")
        val viewModel = RoutineListViewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(RoutineListIntent.ClickAddRoutine)
        viewModel.handleIntent(RoutineListIntent.ChangeDialogName("하체 루틴"))

        viewModel.handleIntent(RoutineListIntent.ConfirmDialog)
        advanceUntilIdle()

        assertTrue(
            effects.filterIsInstance<RoutineListEffect.ShowMessage>().any { it.message == "루틴을 만들지 못했습니다" },
        )
        assertTrue(viewModel.uiState.value.isDialogVisible)
        assertEquals("하체 루틴", viewModel.uiState.value.dialogName)
        assertFalse(viewModel.uiState.value.isCreating)
    }

    @Test
    fun `루틴을 지우면 목록에서 사라진다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine(1L, "하체A", BodyPart.LEG)))
        val viewModel = RoutineListViewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(RoutineListIntent.ClickDeleteRoutine(1L))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.routines.isEmpty())
    }

    @Test
    fun `삭제가 실패하면 메시지가 나가고 목록은 그대로다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine(1L, "하체A", BodyPart.LEG)))
        repository.mutateFailure = IllegalStateException("boom")
        val viewModel = RoutineListViewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(RoutineListIntent.ClickDeleteRoutine(1L))
        advanceUntilIdle()

        assertTrue(
            effects.filterIsInstance<RoutineListEffect.ShowMessage>().any { it.message == "삭제하지 못했습니다" },
        )
        assertEquals(1, viewModel.uiState.value.routines.size)
    }

    @Test
    fun `조회가 실패하면 에러가 실리고 재시도가 다시 조회한다`() = runTest {
        val repository = FakeRoutineRepository(listOf(routine(1L, "하체A", BodyPart.LEG)))
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = RoutineListViewModel(repository)

        subscribe(viewModel)

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        repository.observeFailure = null
        viewModel.handleIntent(RoutineListIntent.ClickRetry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.routines.size)
    }

    @Test
    fun `루틴 클릭은 그 루틴의 상세로 이동한다`() = runTest {
        val viewModel = RoutineListViewModel(FakeRoutineRepository())
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(RoutineListIntent.ClickRoutine(7L))
        advanceUntilIdle()

        val effect = effects.filterIsInstance<RoutineListEffect.NavigateToRoutineDetail>().single()
        assertEquals(7L, effect.routineId)
    }
}
