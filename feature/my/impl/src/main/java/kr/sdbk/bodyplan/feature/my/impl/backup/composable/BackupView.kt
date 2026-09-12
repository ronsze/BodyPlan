package kr.sdbk.bodyplan.feature.my.impl.backup.composable

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.backup.BackupEffect
import kr.sdbk.bodyplan.feature.my.impl.backup.BackupIntent
import kr.sdbk.bodyplan.feature.my.impl.backup.BackupState
import kr.sdbk.bodyplan.feature.my.impl.backup.BackupViewModel

internal data class BackupEvents(val goBack: () -> Unit)

internal data class BackupUiEvents(
    val onBackPressed: () -> Unit,
    val onClickExport: () -> Unit,
    val onClickImport: () -> Unit,
    val onConfirmImport: () -> Unit,
    val onDismissImport: () -> Unit,
)

@Composable
internal fun BackupView(events: BackupEvents, viewModel: BackupViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
    val context = LocalContext.current

    // 파일 선택창은 저장소 권한을 요구하지 않는다. 취소하면 null이 온다.
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri -> viewModel.handleIntent(BackupIntent.ExportDestinationPicked(uri?.toString())) }
    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.handleIntent(BackupIntent.ImportSourcePicked(uri?.toString())) }

    BackupViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is BackupEffect.GoBack -> events.goBack()
            is BackupEffect.LaunchExportPicker -> createDocument.launch(effect.suggestedFileName)
            is BackupEffect.LaunchImportPicker -> openDocument.launch(IMPORT_MIME_TYPES)
            is BackupEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(events: BackupEvents, viewModel: BackupViewModel): BackupUiEvents = remember {
    BackupUiEvents(
        onBackPressed = events.goBack,
        onClickExport = { viewModel.handleIntent(BackupIntent.ClickExport) },
        onClickImport = { viewModel.handleIntent(BackupIntent.ClickImport) },
        onConfirmImport = { viewModel.handleIntent(BackupIntent.ConfirmImport) },
        onDismissImport = { viewModel.handleIntent(BackupIntent.DismissImport) },
    )
}

@Composable
internal fun BackupViewImpl(state: BackupState, uiEvents: BackupUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "백업", onBack = uiEvents.onBackPressed)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackupCard(
                title = "내보내기",
                description = "운동·식단·체중·인바디·루틴 기록과 사진을 파일 하나로 저장합니다. " +
                    "저장 위치로 Google Drive를 고르면 앱을 지워도 남습니다. AI 토큰은 담지 않습니다.",
                isBusy = state.isBusy,
            ) {
                PrimaryButton(text = "백업 파일 내보내기", onClick = uiEvents.onClickExport)
            }
            BackupCard(
                title = "가져오기",
                description = "백업 파일의 내용으로 이 기기의 기록을 바꿉니다. 지금 기록은 남지 않습니다.",
                isBusy = state.isBusy,
            ) {
                OutlinedActionButton(text = "백업 파일 가져오기", onClick = uiEvents.onClickImport)
            }
        }
    }

    if (state.pendingImportUri != null) {
        ImportConfirmDialog(onConfirm = uiEvents.onConfirmImport, onDismiss = uiEvents.onDismissImport)
    }
}

/** 내보내기·가져오기는 어느 쪽이 돌든 둘 다 막는다 — 복원 중에 내보내면 반쪽 파일이 된다. */
@Composable
private fun BackupCard(title: String, description: String, isBusy: Boolean, action: @Composable () -> Unit) {
    BodyPlanCard {
        BaseText(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        VerticalSpacer(space = 8.dp)
        BaseText(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        VerticalSpacer(space = 16.dp)
        if (isBusy) LoadingContent() else action()
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ImportConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        BodyPlanCard {
            BaseText(
                text = "백업에서 복원",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            VerticalSpacer(space = 8.dp)
            BaseText(
                text = "가져오면 이 기기의 기록이 백업 파일 내용으로 모두 바뀝니다. 계속할까요?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            VerticalSpacer(space = 20.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedActionButton(text = "취소", onClick = onDismiss)
                }
                Column(modifier = Modifier.weight(1f)) {
                    PrimaryButton(text = "가져오기", onClick = onConfirm)
                }
            }
        }
    }
}

private const val BACKUP_MIME_TYPE = "application/zip"

// Drive 같은 제공자가 zip을 octet-stream으로 내는 경우가 있어 넓게 받는다.
private val IMPORT_MIME_TYPES = arrayOf(BACKUP_MIME_TYPE, "application/x-zip-compressed", "application/octet-stream")

private val previewUiEvents = BackupUiEvents(
    onBackPressed = {},
    onClickExport = {},
    onClickImport = {},
    onConfirmImport = {},
    onDismissImport = {},
)

@Preview(showBackground = true)
@Composable
private fun BackupViewImplPreview() {
    BodyPlanTheme {
        BackupViewImpl(state = BackupState(), uiEvents = previewUiEvents)
    }
}

@Preview(showBackground = true)
@Composable
private fun BackupViewImplBusyPreview() {
    BodyPlanTheme {
        BackupViewImpl(state = BackupState(isExporting = true), uiEvents = previewUiEvents)
    }
}
