package kr.sdbk.bodyplan.feature.workoutlog.impl

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.Exercise
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.feature.workoutlog.impl.fake.FakeExerciseRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class ExerciseManageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val benchPress = Exercise(1L, BodyPart.CHEST, "플랫 벤치프레스 머신", IntensityType.WEIGHT)
    private val pushUp = Exercise(2L, BodyPart.CHEST, "푸쉬업", IntensityType.ANGLE)
    private val latPullDown = Exercise(3L, BodyPart.BACK, "랫풀다운", IntensityType.WEIGHT)

    private fun repository() = FakeExerciseRepository(listOf(benchPress, pushUp, latPullDown))

    @Test
    fun `진입하면 가슴 종목이 실린다`() = runTest {
        val viewModel = ExerciseManageViewModel(repository())

        subscribe(viewModel)

        assertEquals(BodyPart.CHEST, viewModel.uiState.value.selectedBodyPart)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.exercises.map { it.id })
    }

    @Test
    fun `부위를 바꾸면 그 부위 종목만 실린다`() = runTest {
        val viewModel = ExerciseManageViewModel(repository())
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.SelectBodyPart(BodyPart.BACK))
        advanceUntilIdle()

        assertEquals(listOf(3L), viewModel.uiState.value.exercises.map { it.id })
    }

    @Test
    fun `종목을 추가하면 목록에 나타난다`() = runTest {
        val viewModel = ExerciseManageViewModel(repository())
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.ClickAdd)
        viewModel.handleIntent(ExerciseManageIntent.ChangeDialogName("딥스"))
        viewModel.handleIntent(ExerciseManageIntent.ConfirmDialog)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.exercises.any { it.name == "딥스" })
        assertFalse(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun `추가 다이얼로그는 빈 이름과 무게 기록으로 열린다`() = runTest {
        val viewModel = ExerciseManageViewModel(repository())
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.ClickEdit(pushUp.id))
        viewModel.handleIntent(ExerciseManageIntent.DismissDialog)
        viewModel.handleIntent(ExerciseManageIntent.ClickAdd)

        val state = viewModel.uiState.value
        assertTrue(state.isDialogVisible)
        assertNull(state.editingExercise)
        assertEquals("", state.dialogName)
        assertEquals(IntensityType.WEIGHT, state.dialogIntensityType)
    }

    @Test
    fun `수정 다이얼로그는 고른 종목의 값으로 열린다`() = runTest {
        val viewModel = ExerciseManageViewModel(repository())
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.ClickEdit(pushUp.id))

        val state = viewModel.uiState.value
        assertEquals(pushUp, state.editingExercise)
        assertEquals("푸쉬업", state.dialogName)
        assertEquals(IntensityType.ANGLE, state.dialogIntensityType)
    }

    @Test
    fun `이름을 바꾸면 목록에 반영된다`() = runTest {
        val repository = repository()
        val viewModel = ExerciseManageViewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.ClickEdit(benchPress.id))
        viewModel.handleIntent(ExerciseManageIntent.ChangeDialogName("벤치프레스"))
        viewModel.handleIntent(ExerciseManageIntent.ConfirmDialog)
        advanceUntilIdle()

        assertEquals("벤치프레스", viewModel.uiState.value.exercises.first { it.id == 1L }.name)
    }

    @Test
    fun `이름이 비면 저장하지 않고 안내한다`() = runTest {
        val viewModel = ExerciseManageViewModel(repository())
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.ClickAdd)
        viewModel.handleIntent(ExerciseManageIntent.ChangeDialogName("   "))
        viewModel.handleIntent(ExerciseManageIntent.ConfirmDialog)
        advanceUntilIdle()

        assertTrue(effects.any { it is ExerciseManageEffect.ShowMessage })
        assertTrue(viewModel.uiState.value.isDialogVisible)
        assertEquals(2, viewModel.uiState.value.exercises.size)
    }

    @Test
    fun `종목을 지우면 목록에서 사라진다`() = runTest {
        val viewModel = ExerciseManageViewModel(repository())
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.ClickDelete(benchPress.id))
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.exercises.map { it.id })
    }

    @Test
    fun `지운 종목을 수정해도 되살아나지 않는다`() = runTest {
        val repository = repository()
        val viewModel = ExerciseManageViewModel(repository)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.ClickEdit(benchPress.id))
        viewModel.handleIntent(ExerciseManageIntent.ChangeDialogName("벤치프레스"))
        viewModel.handleIntent(ExerciseManageIntent.ClickDelete(benchPress.id))
        advanceUntilIdle()
        viewModel.handleIntent(ExerciseManageIntent.ConfirmDialog)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.exercises.any { it.id == benchPress.id })
    }

    @Test
    fun `삭제가 실패하면 안내하고 목록은 그대로다`() = runTest {
        val repository = repository()
        val viewModel = ExerciseManageViewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        repository.mutateFailure = IllegalStateException("boom")
        viewModel.handleIntent(ExerciseManageIntent.ClickDelete(benchPress.id))
        advanceUntilIdle()

        assertTrue(effects.any { it is ExerciseManageEffect.ShowMessage })
        assertEquals(2, viewModel.uiState.value.exercises.size)
    }

    @Test
    fun `저장이 실패하면 안내한다`() = runTest {
        val repository = repository()
        val viewModel = ExerciseManageViewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        repository.mutateFailure = IllegalStateException("boom")
        viewModel.handleIntent(ExerciseManageIntent.ClickAdd)
        viewModel.handleIntent(ExerciseManageIntent.ChangeDialogName("딥스"))
        viewModel.handleIntent(ExerciseManageIntent.ConfirmDialog)
        advanceUntilIdle()

        assertTrue(effects.any { it is ExerciseManageEffect.ShowMessage })
    }

    @Test
    fun `조회가 실패하면 에러가 실리고 재시도가 다시 읽는다`() = runTest {
        val repository = repository()
        repository.observeFailure = IllegalStateException("boom")
        val viewModel = ExerciseManageViewModel(repository)
        subscribe(viewModel)

        assertNotNull(viewModel.uiState.value.errorMessage)

        repository.observeFailure = null
        viewModel.handleIntent(ExerciseManageIntent.ClickRetry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.exercises.size)
    }

    @Test
    fun `뒤로 가기는 화면을 닫는 이벤트를 낸다`() = runTest {
        val viewModel = ExerciseManageViewModel(repository())
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(ExerciseManageIntent.ClickBack)
        advanceUntilIdle()

        assertTrue(effects.any { it is ExerciseManageEffect.GoBack })
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: ExerciseManageViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: ExerciseManageViewModel,
    ): List<ExerciseManageEffect> {
        val effects = mutableListOf<ExerciseManageEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }
}
