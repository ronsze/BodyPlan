package kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.composable

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcon
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcons
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.BorderStrong
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.DietEntryEditEffect
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.DietEntryEditIntent
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.DietEntryEditState
import kr.sdbk.bodyplan.feature.dietlog.impl.entryedit.DietEntryEditViewModel

internal data class DietEntryEditEvents(val goBack: () -> Unit)

internal data class DietEntryEditUiEvents(
    val onBackPressed: () -> Unit,
    val onClickPickImage: () -> Unit,
    val onChangeMemo: (String) -> Unit,
    val onClickSave: () -> Unit,
    val onClickRetry: () -> Unit,
)

@Composable
internal fun DietEntryEditView(events: DietEntryEditEvents, viewModel: DietEntryEditViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 사진 선택기는 저장소 권한을 요구하지 않는다. 취소하면 null이 오고 아무 것도 하지 않는다.
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.handleIntent(DietEntryEditIntent.PickImage(uri.toString()))
        }
    }

    val uiEvents = remember(viewModel, pickImage) {
        DietEntryEditUiEvents(
            onBackPressed = events.goBack,
            onClickPickImage = {
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onChangeMemo = { viewModel.handleIntent(DietEntryEditIntent.ChangeMemo(it)) },
            onClickSave = { viewModel.handleIntent(DietEntryEditIntent.ClickSave) },
            onClickRetry = { viewModel.handleIntent(DietEntryEditIntent.ClickRetry) },
        )
    }

    DietEntryEditViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is DietEntryEditEffect.GoBack -> events.goBack()

            is DietEntryEditEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
internal fun DietEntryEditViewImpl(state: DietEntryEditState, uiEvents: DietEntryEditUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(
            title = if (state.editingEntryId == null) "식단 추가" else "식단 수정",
            onBack = uiEvents.onBackPressed,
        )

        if (state.errorMessage != null) {
            ErrorContent(state.errorMessage, uiEvents.onClickRetry)
            return@Column
        }

        if (state.isLoading) {
            LoadingContent()
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                // 키보드가 올라오면 남는 높이가 줄어든다. 스크롤이 있어야 입력칸이 가려지지 않는다.
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ImageSlot(previewImage = state.previewImage)
            OutlinedActionButton(
                text = if (state.previewImage == null) "사진 선택" else "사진 변경",
                onClick = uiEvents.onClickPickImage,
            )
            MemoField(memo = state.memo, onChangeMemo = uiEvents.onChangeMemo)
        }

        PrimaryButton(
            text = "저장",
            onClick = uiEvents.onClickSave,
            modifier = Modifier.padding(16.dp),
            enabled = state.canSave,
        )
    }
}

/** 사진 자리. 고르기 전에는 점선 테두리로 비어 있음을 알린다. */
@Composable
private fun ImageSlot(previewImage: String?) {
    val shape = RoundedCornerShape(16.dp)
    if (previewImage == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(shape)
                .background(Surface)
                .border(BorderStroke(2.dp, BorderStrong), shape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            BodyPlanIcon(
                painter = BodyPlanIcons.CameraOff,
                contentDescription = null,
                boxSize = 32.dp,
                iconSize = 28.dp,
                tint = TextTertiary,
            )
            BaseText(
                text = "사진을 선택하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
        }
        return
    }

    // 저장된 사진은 파일 경로, 방금 고른 사진은 content URI다. 둘 다 Coil이 다룬다.
    BaseImage(
        url = previewImage,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(shape),
        placeholder = ColorPainter(Color.LightGray),
    )
}

@Composable
private fun MemoField(memo: String, onChangeMemo: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var fieldHeight by remember { mutableIntStateOf(0) }

    // 줄이 늘어나면 입력칸이 아래로 자라고 커서도 그만큼 내려간다.
    // 자란 아래 끝을 화면 안으로 끌어와 커서를 눈으로 따라갈 수 있게 한다.
    LaunchedEffect(fieldHeight, isFocused) {
        if (!isFocused || fieldHeight == 0) return@LaunchedEffect
        bringIntoViewRequester.bringIntoView(
            Rect(
                left = 0f,
                top = (fieldHeight - CURSOR_LINE_MARGIN).toFloat(),
                right = 0f,
                bottom = fieldHeight.toFloat(),
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BaseText(
            text = "식단 메모",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            color = TextPrimary,
        )
        BaseTextField(
            value = memo,
            onValueChange = onChangeMemo,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .onSizeChanged { fieldHeight = it.height }
                .clip(shape)
                .background(Surface)
                .border(
                    BorderStroke(if (memo.isEmpty()) 1.dp else 1.5.dp, if (memo.isEmpty()) Border else Accent),
                    shape,
                )
                .padding(16.dp),
            placeholder = "무엇을 먹었는지 적어 주세요",
            textStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onClickRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BaseText(text = message, color = TextSecondary)
            OutlinedActionButton(text = "다시 시도", onClick = onClickRetry)
        }
    }
}

/** 커서가 있는 마지막 줄만 끌어온다. 입력칸 전체를 끌어오면 화면이 위로 되밀린다. */
private const val CURSOR_LINE_MARGIN = 48

private val previewUiEvents = DietEntryEditUiEvents(
    onBackPressed = {},
    onClickPickImage = {},
    onChangeMemo = {},
    onClickSave = {},
    onClickRetry = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietEntryEditViewImplEmptyPreview() {
    BodyPlanTheme {
        DietEntryEditViewImpl(
            state = DietEntryEditState(date = LocalDate.of(2026, 9, 8)),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietEntryEditViewImplFilledPreview() {
    BodyPlanTheme {
        DietEntryEditViewImpl(
            state = DietEntryEditState(
                date = LocalDate.of(2026, 9, 8),
                editingEntryId = 5L,
                storedImagePath = "/files/diet_images/a.jpg",
                memo = "단백질 가득 아보카도 샌드위치",
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DietEntryEditViewImplErrorPreview() {
    BodyPlanTheme {
        DietEntryEditViewImpl(
            state = DietEntryEditState(
                date = LocalDate.of(2026, 9, 8),
                editingEntryId = 5L,
                errorMessage = "불러오지 못했습니다",
            ),
            uiEvents = previewUiEvents,
        )
    }
}
