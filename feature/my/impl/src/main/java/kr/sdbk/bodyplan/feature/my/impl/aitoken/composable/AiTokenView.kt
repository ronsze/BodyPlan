package kr.sdbk.bodyplan.feature.my.impl.aitoken.composable

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BaseTextField
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PillChip
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.AiCredential
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.aitoken.AiTokenEffect
import kr.sdbk.bodyplan.feature.my.impl.aitoken.AiTokenIntent
import kr.sdbk.bodyplan.feature.my.impl.aitoken.AiTokenState
import kr.sdbk.bodyplan.feature.my.impl.aitoken.AiTokenViewModel

/** 제공자 이름. 마이 화면도 같은 표기를 쓴다. */
internal val AiProvider.label: String
    get() = when (this) {
        AiProvider.CLAUDE -> "클로드"
        AiProvider.GPT -> "GPT"
        AiProvider.GEMINI -> "제미나이"
    }

internal data class AiTokenEvents(val goBack: () -> Unit)

internal data class AiTokenUiEvents(
    val onBackPressed: () -> Unit,
    val onSelectProvider: (AiProvider) -> Unit,
    val onChangeInput: (String) -> Unit,
    val onClickSave: () -> Unit,
    val onClickVerify: () -> Unit,
    val onClickDisconnect: () -> Unit,
)

@Composable
internal fun AiTokenView(events: AiTokenEvents, viewModel: AiTokenViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
    val context = LocalContext.current

    AiTokenViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is AiTokenEffect.GoBack -> events.goBack()

            is AiTokenEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(events: AiTokenEvents, viewModel: AiTokenViewModel): AiTokenUiEvents = remember {
    AiTokenUiEvents(
        onBackPressed = events.goBack,
        onSelectProvider = { viewModel.handleIntent(AiTokenIntent.SelectProvider(it)) },
        onChangeInput = { viewModel.handleIntent(AiTokenIntent.ChangeInput(it)) },
        onClickSave = { viewModel.handleIntent(AiTokenIntent.ClickSave) },
        onClickVerify = { viewModel.handleIntent(AiTokenIntent.ClickVerify) },
        onClickDisconnect = { viewModel.handleIntent(AiTokenIntent.ClickDisconnect) },
    )
}

@Composable
internal fun AiTokenViewImpl(state: AiTokenState, uiEvents: AiTokenUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "AI 토큰", onBack = uiEvents.onBackPressed)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BodyPlanCard {
                BaseText(
                    text = NOTICE,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }

            ProviderPicker(state.selectedProvider, uiEvents.onSelectProvider)
            TokenInput(state, uiEvents)

            if (state.saved != null) {
                ConnectedCard(state, uiEvents)
            }
        }
    }
}

@Composable
private fun ProviderPicker(selected: AiProvider, onSelect: (AiProvider) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BaseText(
            text = "제공자",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            color = TextPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiProvider.entries.forEach { provider ->
                PillChip(
                    text = provider.label,
                    selected = provider == selected,
                    onClick = { onSelect(provider) },
                )
            }
        }
    }
}

@Composable
private fun TokenInput(state: AiTokenState, uiEvents: AiTokenUiEvents) {
    val shape = RoundedCornerShape(12.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BaseText(
            text = "API 키",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            color = TextPrimary,
        )
        BaseTextField(
            value = state.input,
            onValueChange = uiEvents.onChangeInput,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Surface)
                .border(BorderStroke(1.dp, Border), shape)
                .padding(16.dp),
            placeholder = "API 키를 붙여 넣으세요",
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
        )
        if (state.errorMessage != null) {
            BaseText(
                text = state.errorMessage,
                style = MaterialTheme.typography.labelMedium,
                color = Danger,
            )
        }
        PrimaryButton(text = "저장", onClick = uiEvents.onClickSave, enabled = state.canSave)
    }
}

@Composable
private fun ConnectedCard(state: AiTokenState, uiEvents: AiTokenUiEvents) {
    BodyPlanCard {
        BaseText(
            text = "${state.saved?.provider?.label} 연결됨",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            color = TextPrimary,
        )
        VerticalSpacer(space = 4.dp)
        BaseText(
            text = state.savedTokenMask.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary,
        )
        VerticalSpacer(space = 16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedActionButton(
                text = if (state.isVerifying) "확인 중" else "연결 확인",
                onClick = uiEvents.onClickVerify,
                modifier = Modifier.weight(1f),
            )
            OutlinedActionButton(
                text = "연결 해제",
                onClick = uiEvents.onClickDisconnect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val NOTICE =
    "분석 기능은 고른 제공자의 키로 AI를 부릅니다. " +
        "키는 이 기기에만 저장되고, 사진과 기록이 분석을 위해 외부로 전송됩니다."

private val previewUiEvents = AiTokenUiEvents(
    onBackPressed = {},
    onSelectProvider = {},
    onChangeInput = {},
    onClickSave = {},
    onClickVerify = {},
    onClickDisconnect = {},
)

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun AiTokenViewImplEmptyPreview() {
    BodyPlanTheme {
        AiTokenViewImpl(state = AiTokenState(), uiEvents = previewUiEvents)
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun AiTokenViewImplConnectedPreview() {
    BodyPlanTheme {
        AiTokenViewImpl(
            state = AiTokenState(
                selectedProvider = AiProvider.CLAUDE,
                saved = AiCredential(AiProvider.CLAUDE, "sk-ant-1234567890abcdef"),
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun AiTokenViewImplErrorPreview() {
    BodyPlanTheme {
        AiTokenViewImpl(
            state = AiTokenState(
                selectedProvider = AiProvider.GEMINI,
                input = "wrong-key",
                errorMessage = "키가 올바르지 않습니다",
            ),
            uiEvents = previewUiEvents,
        )
    }
}
