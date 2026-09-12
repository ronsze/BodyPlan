package kr.sdbk.bodyplan.feature.my.impl.backup

import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class BackupState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    /** 가져오기로 고른 파일. 있으면 확인 대화상자가 떠 있는 것이다. */
    val pendingImportUri: String? = null,
) : State {
    val isBusy: Boolean get() = isExporting || isImporting
}

internal sealed interface BackupIntent : Intent {
    data object ClickExport : BackupIntent

    /** 파일 만들기 선택창의 결과. 취소하면 null. */
    data class ExportDestinationPicked(val uri: String?) : BackupIntent

    data object ClickImport : BackupIntent

    /** 파일 열기 선택창의 결과. 취소하면 null. */
    data class ImportSourcePicked(val uri: String?) : BackupIntent

    data object ConfirmImport : BackupIntent

    data object DismissImport : BackupIntent

    data object ClickBack : BackupIntent
}

internal sealed interface BackupEffect : Effect {
    data object GoBack : BackupEffect

    data class LaunchExportPicker(val suggestedFileName: String) : BackupEffect

    data object LaunchImportPicker : BackupEffect

    data class ShowMessage(val message: String) : BackupEffect
}
