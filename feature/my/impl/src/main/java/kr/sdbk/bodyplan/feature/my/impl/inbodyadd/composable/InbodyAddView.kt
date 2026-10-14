package kr.sdbk.bodyplan.feature.my.impl.inbodyadd.composable

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.ui.components.AiTokenRequiredDialog
import kr.sdbk.bodyplan.core.ui.components.requestNotificationThen
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.inbodyadd.InbodyAddEffect
import kr.sdbk.bodyplan.feature.my.impl.inbodyadd.InbodyAddIntent
import kr.sdbk.bodyplan.feature.my.impl.inbodyadd.InbodyAddState
import kr.sdbk.bodyplan.feature.my.impl.inbodyadd.InbodyAddViewModel
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.NoticeBox
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.SecondaryFilledButton

internal data class InbodyAddEvents(val goBack: () -> Unit, val goToAiToken: () -> Unit)

internal data class InbodyAddUiEvents(
    val onBackPressed: () -> Unit,
    val onClickCamera: () -> Unit,
    val onClickAlbum: () -> Unit,
    val onClickPickDate: () -> Unit,
    val onSelectDate: (LocalDate) -> Unit,
    val onChangePickerMonth: (YearMonth) -> Unit,
    val onDismissDatePicker: () -> Unit,
    val onClickAddOnly: () -> Unit,
    val onClickAddAndAnalyze: () -> Unit,
    val onConfirmTokenDialog: () -> Unit,
    val onDismissTokenDialog: () -> Unit,
)

@Composable
internal fun InbodyAddView(events: InbodyAddEvents, viewModel: InbodyAddViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 사진 선택기는 저장소 권한을 요구하지 않는다. 취소하면 null이 오고 아무 것도 하지 않는다.
    val pickAlbum = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.handleIntent(InbodyAddIntent.PickImage(uri.toString()))
    }

    // 카메라 앱이 끝났을 때 어느 파일에 찍었는지 알아야 해서 주소를 들고 있는다. 화면이 다시 만들어져도 남긴다.
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) viewModel.handleIntent(InbodyAddIntent.PickImage(uri.toString()))
    }

    val uiEvents = remember(viewModel, events, pickAlbum, takePicture) {
        InbodyAddUiEvents(
            onBackPressed = events.goBack,
            onClickCamera = {
                val uri = newCameraUri(context)
                cameraUri = uri
                takePicture.launch(uri)
            },
            onClickAlbum = {
                pickAlbum.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onClickPickDate = { viewModel.handleIntent(InbodyAddIntent.ClickPickDate) },
            onSelectDate = { viewModel.handleIntent(InbodyAddIntent.SelectDate(it)) },
            onChangePickerMonth = { viewModel.handleIntent(InbodyAddIntent.ChangePickerMonth(it)) },
            onDismissDatePicker = { viewModel.handleIntent(InbodyAddIntent.DismissDatePicker) },
            onClickAddOnly = { viewModel.handleIntent(InbodyAddIntent.ClickAddOnly) },
            onClickAddAndAnalyze = { viewModel.handleIntent(InbodyAddIntent.ClickAddAndAnalyze) },
            onConfirmTokenDialog = { viewModel.handleIntent(InbodyAddIntent.ConfirmTokenDialog) },
            onDismissTokenDialog = { viewModel.handleIntent(InbodyAddIntent.DismissTokenDialog) },
        )
    }

    InbodyAddViewImpl(state = state, uiEvents = uiEvents)

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is InbodyAddEffect.Close -> events.goBack()
            is InbodyAddEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            is InbodyAddEffect.NavigateToAiToken -> events.goToAiToken()
        }
    }
}

/**
 * 촬영 결과를 받을 임시 파일의 주소.
 *
 * 앞서 찍은 임시 파일은 지운다. 사진 보관소로 복사된 뒤에는 쓸 곳이 없다.
 */
private fun newCameraUri(context: android.content.Context): Uri {
    val directory = File(context.cacheDir, CAMERA_DIRECTORY).apply { mkdirs() }
    directory.listFiles()?.forEach { it.delete() }
    val file = File(directory, "inbody-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private const val CAMERA_DIRECTORY = "camera"

@Composable
internal fun InbodyAddViewImpl(state: InbodyAddState, uiEvents: InbodyAddUiEvents) {
    // 알림 권한을 묻고 나서 분석을 요청한다. 거절해도 분석은 그대로 돈다.
    val addAndAnalyze = requestNotificationThen(uiEvents.onClickAddAndAnalyze)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "인바디 사진 추가", onBack = uiEvents.onBackPressed)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("1. 결과지 사진 선택")
            PhotoSourceButtons(onClickCamera = uiEvents.onClickCamera, onClickAlbum = uiEvents.onClickAlbum)
            PhotoPreviewCard(imageUri = state.pickedImageUri, onClickChange = uiEvents.onClickAlbum)
            Hint("결과지 전체와 숫자가 선명하게 보이도록 촬영해 주세요.")

            VerticalSpacer(space = 4.dp)
            SectionTitle("2. 측정 날짜 확인")
            MeasuredDateField(measuredOn = state.measuredOn, onClick = uiEvents.onClickPickDate)
            Hint("사진을 올린 날짜가 아닌 실제 측정 날짜를 선택해 주세요.")

            VerticalSpacer(space = 4.dp)
            SectionTitle("3. 사진 추가 또는 분석 요청")
            NoticeBox(
                title = "사진만 추가해도 괜찮아요",
                body = "저장한 사진은 기록 목록에서 언제든 분석을 요청할 수 있어요.",
            )

            state.errorMessage?.let { message ->
                BaseText(text = message, style = MaterialTheme.typography.bodyMedium, color = Danger)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(text = "사진 추가하고 분석 요청", onClick = addAndAnalyze, enabled = state.canSubmit)
            SecondaryFilledButton(text = "사진만 추가", onClick = uiEvents.onClickAddOnly, enabled = state.canSubmit)
        }
    }

    if (state.isDatePickerVisible) {
        MeasuredDatePickerDialog(
            selected = state.measuredOn,
            today = LocalDate.now(),
            yearMonth = state.pickerYearMonth,
            onSelect = uiEvents.onSelectDate,
            onChangeMonth = uiEvents.onChangePickerMonth,
            onDismiss = uiEvents.onDismissDatePicker,
        )
    }
    if (state.isTokenDialogVisible) {
        AiTokenRequiredDialog(
            onConfirm = uiEvents.onConfirmTokenDialog,
            onDismiss = uiEvents.onDismissTokenDialog,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    BaseText(text = text, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
}

@Composable
private fun Hint(text: String) {
    BaseText(text = text, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
}
