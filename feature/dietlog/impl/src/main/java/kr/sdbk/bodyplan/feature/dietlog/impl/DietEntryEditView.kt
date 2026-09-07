package kr.sdbk.bodyplan.feature.dietlog.impl

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect

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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = uiEvents.onBackPressed) { BaseText(text = "뒤로") }
            BaseText(
                text = if (state.editingEntryId == null) "식단 추가" else "식단 수정",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        VerticalSpacer(space = 8.dp)

        if (state.errorMessage != null) {
            ErrorContent(state.errorMessage, uiEvents.onClickRetry)
            return@Column
        }

        if (state.isLoading) {
            LoadingContent()
            return@Column
        }

        ImageSlot(previewImage = state.previewImage)
        VerticalSpacer(space = 8.dp)
        OutlinedButton(onClick = uiEvents.onClickPickImage, modifier = Modifier.fillMaxWidth()) {
            BaseText(text = if (state.previewImage == null) "사진 선택" else "사진 변경")
        }

        VerticalSpacer(space = 16.dp)
        BaseTextField(
            value = state.memo,
            onValueChange = uiEvents.onChangeMemo,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "무엇을 먹었는지 적어 주세요",
        )

        VerticalSpacer(space = 16.dp)
        Button(
            onClick = uiEvents.onClickSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BaseText(text = "저장")
        }
    }
}

@Composable
private fun ImageSlot(previewImage: String?) {
    Box(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (previewImage == null) {
            BaseText(text = "사진을 선택하세요")
            return@Box
        }
        // 저장된 사진은 파일 경로, 방금 고른 사진은 content URI다. 둘 다 Coil이 다룬다.
        BaseImage(
            url = previewImage,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            placeholder = ColorPainter(Color.LightGray),
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BaseText(text = message)
            TextButton(onClick = onClickRetry) { BaseText(text = "다시 시도") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DietEntryEditViewImplPreview() {
    BodyPlanTheme {
        DietEntryEditViewImpl(
            state = DietEntryEditState(
                date = LocalDate.of(2026, 9, 7),
                memo = "닭가슴살과 고구마",
            ),
            uiEvents = DietEntryEditUiEvents(
                onBackPressed = {},
                onClickPickImage = {},
                onChangeMemo = {},
                onClickSave = {},
                onClickRetry = {},
            ),
        )
    }
}
