package kr.sdbk.bodyplan.feature.my.impl.backup

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.domain.model.InvalidBackupFileException
import kr.sdbk.bodyplan.core.domain.model.UnsupportedBackupVersionException
import kr.sdbk.bodyplan.feature.my.impl.MainDispatcherRule
import kr.sdbk.bodyplan.feature.my.impl.fake.FakeBackupRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class BackupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-13T05:30:00Z"), ZoneOffset.UTC)

    private fun viewModel(backupRepository: FakeBackupRepository = FakeBackupRepository()): BackupViewModel =
        BackupViewModel(backupRepository, clock)

    @Test
    fun `내보내기를 누르면 고정 시각이 담긴 파일명으로 선택창을 연다`() = runTest {
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ClickExport)
        advanceUntilIdle()

        val effect = effects.filterIsInstance<BackupEffect.LaunchExportPicker>().single()
        assertEquals("bodyplan-backup-20260913-0530.zip", effect.suggestedFileName)
    }

    @Test
    fun `내보내기 대상을 취소하면 아무 일도 없다`() = runTest {
        val repository = FakeBackupRepository()
        val viewModel = viewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ExportDestinationPicked(null))
        advanceUntilIdle()

        assertTrue(repository.exportedTo.isEmpty())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `내보내기 대상을 고르면 저장소에 내보내고 성공 메시지를 낸다`() = runTest {
        val repository = FakeBackupRepository()
        val viewModel = viewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ExportDestinationPicked("content://export"))
        advanceUntilIdle()

        assertEquals(listOf("content://export"), repository.exportedTo)
        assertTrue(effects.any { it is BackupEffect.ShowMessage && it.message == "백업 파일을 저장했습니다" })
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun `내보내기가 실패하면 실패 메시지를 내고 진행 중 상태를 끈다`() = runTest {
        val repository = FakeBackupRepository().apply { exportFailure = IllegalStateException("boom") }
        val viewModel = viewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ExportDestinationPicked("content://export"))
        advanceUntilIdle()

        assertTrue(effects.any { it is BackupEffect.ShowMessage && it.message == "백업 파일을 저장하지 못했습니다" })
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun `가져오기를 누르면 가져오기 선택창을 연다`() = runTest {
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ClickImport)
        advanceUntilIdle()

        assertTrue(effects.any { it is BackupEffect.LaunchImportPicker })
    }

    @Test
    fun `가져오기 소스를 취소하면 대기 중인 파일이 비워진 채로 남는다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ImportSourcePicked(null))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingImportUri)
    }

    @Test
    fun `가져오기 소스를 고르면 대기 중인 파일로 잡힌다`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ImportSourcePicked("content://import"))
        advanceUntilIdle()

        assertEquals("content://import", viewModel.uiState.value.pendingImportUri)
    }

    @Test
    fun `가져오기 확인 대화상자를 닫으면 대기 중인 파일이 비워지고 가져오지 않는다`() = runTest {
        val repository = FakeBackupRepository()
        val viewModel = viewModel(repository)
        subscribe(viewModel)
        viewModel.handleIntent(BackupIntent.ImportSourcePicked("content://import"))

        viewModel.handleIntent(BackupIntent.DismissImport)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingImportUri)
        assertTrue(repository.importedFrom.isEmpty())
    }

    @Test
    fun `대기 중인 파일이 없는 채로 확인하면 아무 일도 없다`() = runTest {
        val repository = FakeBackupRepository()
        val viewModel = viewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ConfirmImport)
        advanceUntilIdle()

        assertTrue(repository.importedFrom.isEmpty())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `가져오기를 확인하면 저장소에서 복원하고 대기 중인 파일을 비운 뒤 성공 메시지를 낸다`() = runTest {
        val repository = FakeBackupRepository()
        val viewModel = viewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(BackupIntent.ImportSourcePicked("content://import"))

        viewModel.handleIntent(BackupIntent.ConfirmImport)
        advanceUntilIdle()

        assertEquals(listOf("content://import"), repository.importedFrom)
        assertNull(viewModel.uiState.value.pendingImportUri)
        assertTrue(effects.any { it is BackupEffect.ShowMessage && it.message == "백업을 복원했습니다" })
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun `백업 파일이 아니면 손상 메시지를 낸다`() = runTest {
        val repository = FakeBackupRepository().apply { importFailure = InvalidBackupFileException() }
        val viewModel = viewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(BackupIntent.ImportSourcePicked("content://import"))

        viewModel.handleIntent(BackupIntent.ConfirmImport)
        advanceUntilIdle()

        assertTrue(effects.any { it is BackupEffect.ShowMessage && it.message == "백업 파일이 아니거나 손상됐습니다" })
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun `지원하지 않는 버전이면 버전 미지원 메시지를 낸다`() = runTest {
        val repository = FakeBackupRepository().apply { importFailure = UnsupportedBackupVersionException(9) }
        val viewModel = viewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(BackupIntent.ImportSourcePicked("content://import"))

        viewModel.handleIntent(BackupIntent.ConfirmImport)
        advanceUntilIdle()

        assertTrue(effects.any { it is BackupEffect.ShowMessage && it.message == "이 앱 버전에서 지원하지 않는 백업 파일입니다" })
    }

    @Test
    fun `그 밖의 오류는 기본 복원 실패 메시지를 낸다`() = runTest {
        val repository = FakeBackupRepository().apply { importFailure = IllegalStateException("boom") }
        val viewModel = viewModel(repository)
        val effects = collectEffects(viewModel)
        subscribe(viewModel)
        viewModel.handleIntent(BackupIntent.ImportSourcePicked("content://import"))

        viewModel.handleIntent(BackupIntent.ConfirmImport)
        advanceUntilIdle()

        assertTrue(effects.any { it is BackupEffect.ShowMessage && it.message == "복원하지 못했습니다" })
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun `뒤로 가기는 화면을 닫는 이벤트를 낸다`() = runTest {
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        subscribe(viewModel)

        viewModel.handleIntent(BackupIntent.ClickBack)
        advanceUntilIdle()

        assertTrue(effects.any { it is BackupEffect.GoBack })
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: BackupViewModel,
    ): MutableList<BackupEffect> {
        val effects = mutableListOf<BackupEffect>()
        backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.effect.collect { effects += it }
        }
        return effects
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: BackupViewModel) {
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }
}
