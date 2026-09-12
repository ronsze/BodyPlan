package kr.sdbk.bodyplan.feature.my.impl.backup

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.InvalidBackupFileException
import kr.sdbk.bodyplan.core.domain.model.UnsupportedBackupVersionException
import kr.sdbk.bodyplan.core.domain.repository.BackupRepository
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class BackupViewModel
@Inject
constructor(
    private val backupRepository: BackupRepository,
    private val clock: Clock,
) : BaseViewModel<BackupState, BackupIntent, BackupEffect>(initialState = BackupState()) {
    override fun handleIntent(intent: BackupIntent) {
        when (intent) {
            is BackupIntent.ClickExport -> updateEffect(BackupEffect.LaunchExportPicker(suggestedFileName()))

            is BackupIntent.ExportDestinationPicked -> intent.uri?.let(::export)

            is BackupIntent.ClickImport -> updateEffect(BackupEffect.LaunchImportPicker)

            is BackupIntent.ImportSourcePicked -> intent.uri?.let { uri ->
                updateState { it.copy(pendingImportUri = uri) }
            }

            is BackupIntent.ConfirmImport -> confirmImport()

            is BackupIntent.DismissImport -> updateState { it.copy(pendingImportUri = null) }

            is BackupIntent.ClickBack -> updateEffect(BackupEffect.GoBack)
        }
    }

    /** 파일명에 시각을 넣어 여러 번 내보내도 서로 덮어쓰지 않게 한다. */
    private fun suggestedFileName(): String = "bodyplan-backup-${LocalDateTime.now(clock).format(FILE_NAME_TIME)}.zip"

    private fun export(destinationUri: String) {
        viewModelScope.launch {
            updateState { it.copy(isExporting = true) }
            runCatching { backupRepository.exportTo(destinationUri) }
                .onSuccess { updateEffect(BackupEffect.ShowMessage("백업 파일을 저장했습니다")) }
                .onFailure { updateEffect(BackupEffect.ShowMessage("백업 파일을 저장하지 못했습니다")) }
            updateState { it.copy(isExporting = false) }
        }
    }

    private fun confirmImport() {
        val sourceUri = uiState.value.pendingImportUri ?: return
        viewModelScope.launch {
            updateState { it.copy(pendingImportUri = null, isImporting = true) }
            runCatching { backupRepository.importFrom(sourceUri) }
                .onSuccess { updateEffect(BackupEffect.ShowMessage("백업을 복원했습니다")) }
                .onFailure { updateEffect(BackupEffect.ShowMessage(it.toImportMessage())) }
            updateState { it.copy(isImporting = false) }
        }
    }

    /** 파일 쪽 원인은 그 이유를 그대로 낸다 — 다른 파일을 골라야 할지 앱을 올려야 할지 사용자가 알아야 한다. */
    private fun Throwable.toImportMessage(): String = when (this) {
        is InvalidBackupFileException, is UnsupportedBackupVersionException -> message ?: "복원하지 못했습니다"
        else -> "복원하지 못했습니다"
    }
}

private val FILE_NAME_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
