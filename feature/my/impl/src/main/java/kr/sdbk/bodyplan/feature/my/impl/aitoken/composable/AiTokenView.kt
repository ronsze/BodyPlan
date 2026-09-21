package kr.sdbk.bodyplan.feature.my.impl.aitoken.composable

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcon
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanIcons
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.SectionRow
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
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
import kr.sdbk.bodyplan.core.domain.model.OnDeviceModelStatus
import kr.sdbk.bodyplan.core.domain.model.requiresToken
import kr.sdbk.bodyplan.core.ui.components.AiProviderMark
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.aitoken.AiTokenEffect
import kr.sdbk.bodyplan.feature.my.impl.aitoken.AiTokenIntent
import kr.sdbk.bodyplan.feature.my.impl.aitoken.AiTokenState
import kr.sdbk.bodyplan.feature.my.impl.aitoken.AiTokenViewModel

internal data class AiTokenEvents(val goBack: () -> Unit)

internal data class AiTokenUiEvents(
    val onBackPressed: () -> Unit,
    val onClickProvider: (AiProvider) -> Unit,
    val onChangeInput: (String) -> Unit,
    val onClickConnect: () -> Unit,
    val onClickDownloadModel: () -> Unit,
    val onClickCancelConnect: () -> Unit,
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
        onClickProvider = { viewModel.handleIntent(AiTokenIntent.ClickProvider(it)) },
        onChangeInput = { viewModel.handleIntent(AiTokenIntent.ChangeInput(it)) },
        onClickConnect = { viewModel.handleIntent(AiTokenIntent.ClickConnect) },
        onClickDownloadModel = { viewModel.handleIntent(AiTokenIntent.ClickDownloadModel) },
        onClickCancelConnect = { viewModel.handleIntent(AiTokenIntent.ClickCancelConnect) },
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
        BodyPlanTopBar(title = "AI 연동", onBack = uiEvents.onBackPressed)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                // 온디바이스 바로 연결은 화면 없이 검증만 돈다. 그동안 목록을 보이면 다른 제공자를 누를 수 있다.
                state.isBusyWithoutScreen -> LoadingContent()

                state.connected != null -> ConnectedContent(state, uiEvents)

                state.connectingProvider?.requiresToken == false -> OnDeviceDownloadContent(state, uiEvents)

                state.connectingProvider != null -> ConnectingContent(state, uiEvents)

                else -> ProviderPicker(state.providers, uiEvents.onClickProvider)
            }
        }
    }
}

@Composable
private fun ProviderPicker(providers: List<AiProvider>, onClickProvider: (AiProvider) -> Unit) {
    BaseText(
        text = "분석에 쓸 AI를 하나 골라 연동하세요.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
    )
    providers.forEach { provider ->
        ProviderButton(provider = provider, onClick = { onClickProvider(provider) })
    }
    BaseText(
        text = "키는 이 기기에만 저장되고, 사진과 기록이 분석을 위해 외부로 전송됩니다. 온디바이스는 기기 안에서 처리됩니다.",
        style = MaterialTheme.typography.bodySmall,
        color = TextTertiary,
    )
}

/** 설정 줄과 같은 얼개다 — 아이콘·이름·화살표 한 줄이 곧 그 제공자로 연동하기다. */
@Composable
private fun ProviderButton(provider: AiProvider, onClick: () -> Unit) {
    SectionRow(
        title = "${provider.label}로 연동하기",
        onClick = onClick,
        description = if (provider.requiresToken) null else ON_DEVICE_DESCRIPTION,
        leading = { AiProviderMark(provider) },
    )
}

/** 모델을 내려받아 연결하는 화면. 키 넣기 화면과 버튼 자리를 맞춰 흐름이 같아 보이게 한다. */
@Composable
private fun OnDeviceDownloadContent(state: AiTokenState, uiEvents: AiTokenUiEvents) {
    val provider = state.connectingProvider ?: return
    BodyPlanCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AiProviderMark(provider)
            BaseText(
                text = "온디바이스 모델 내려받기",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
        }
        VerticalSpacer(space = 8.dp)
        BaseText(
            text = "Gemini Nano 모델을 이 기기에 내려받습니다. 기록과 사진이 외부로 나가지 않습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        if (state.isConnecting) {
            VerticalSpacer(space = 16.dp)
            DownloadProgress(ratio = state.downloadRatio)
        }
    }

    state.errorMessage?.let { message ->
        BaseText(text = message, style = MaterialTheme.typography.bodyMedium, color = Danger)
    }

    PrimaryButton(
        text = if (state.isConnecting) "내려받는 중" else "내려받기",
        onClick = uiEvents.onClickDownloadModel,
        enabled = state.canConnect,
    )
    OutlinedActionButton(text = "취소", onClick = uiEvents.onClickCancelConnect)
}

@Composable
private fun DownloadProgress(ratio: Float?) {
    if (ratio == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
    }
    VerticalSpacer(space = 8.dp)
    BaseText(
        text = ratio?.let { "내려받는 중 ${(it * PERCENT).toInt()}%" } ?: "내려받는 중",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
    )
}

@Composable
private fun ConnectingContent(state: AiTokenState, uiEvents: AiTokenUiEvents) {
    val provider = state.connectingProvider ?: return
    BodyPlanCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AiProviderMark(provider)
            BaseText(
                text = "${provider.label} 키 넣기",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
        }
        VerticalSpacer(space = 16.dp)
        BaseTextField(
            value = state.input,
            onValueChange = uiEvents.onChangeInput,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FIELD_CORNER))
                .background(Background)
                .border(BorderStroke(1.dp, Border), RoundedCornerShape(FIELD_CORNER))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            placeholder = "API 키를 붙여 넣으세요",
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
        )
        VerticalSpacer(space = 8.dp)
        BaseText(
            text = "연동할 때 한 번 검증합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
    }

    state.errorMessage?.let { message ->
        BaseText(text = message, style = MaterialTheme.typography.bodyMedium, color = Danger)
    }

    PrimaryButton(
        text = if (state.isConnecting) "확인 중" else "연동",
        onClick = uiEvents.onClickConnect,
        enabled = state.canConnect,
    )
    OutlinedActionButton(text = "취소", onClick = uiEvents.onClickCancelConnect)
}

@Composable
private fun ConnectedContent(state: AiTokenState, uiEvents: AiTokenUiEvents) {
    val credential = state.connected ?: return
    BodyPlanCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AiProviderMark(credential.provider, size = 36.dp)
            Column {
                BaseText(
                    text = "${credential.provider.label} 연결됨",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                VerticalSpacer(space = 4.dp)
                BaseText(
                    text = state.connectedTokenMask ?: ON_DEVICE_CONNECTED_DESCRIPTION,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }
    }
    BaseText(
        text = "다른 AI로 바꾸려면 먼저 연결을 해제하세요.",
        style = MaterialTheme.typography.bodySmall,
        color = TextTertiary,
    )
    OutlinedActionButton(text = "연결 해제", onClick = uiEvents.onClickDisconnect)
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

private val FIELD_CORNER = 12.dp
private const val PERCENT = 100
private const val ON_DEVICE_DESCRIPTION = "키 없이 기기 안에서 처리"
private const val ON_DEVICE_CONNECTED_DESCRIPTION = "기기 안에서 처리됩니다"

private val previewUiEvents = AiTokenUiEvents(
    onBackPressed = {},
    onClickProvider = {},
    onChangeInput = {},
    onClickConnect = {},
    onClickDownloadModel = {},
    onClickCancelConnect = {},
    onClickDisconnect = {},
)

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun AiTokenViewImplPickerPreview() {
    BodyPlanTheme {
        AiTokenViewImpl(
            state = AiTokenState(onDeviceStatus = OnDeviceModelStatus.DOWNLOADABLE),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun AiTokenViewImplDownloadingPreview() {
    BodyPlanTheme {
        AiTokenViewImpl(
            state = AiTokenState(
                connectingProvider = AiProvider.ON_DEVICE,
                onDeviceStatus = OnDeviceModelStatus.DOWNLOADING,
                isConnecting = true,
                downloadTotalBytes = 1000,
                downloadedBytes = 350,
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun AiTokenViewImplConnectingPreview() {
    BodyPlanTheme {
        AiTokenViewImpl(
            state = AiTokenState(
                connectingProvider = AiProvider.CLAUDE,
                input = "sk-ant-1234",
                errorMessage = "키가 올바르지 않습니다",
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun AiTokenViewImplConnectedPreview() {
    BodyPlanTheme {
        AiTokenViewImpl(
            state = AiTokenState(connected = AiCredential(AiProvider.GEMINI, "AIza12345678abcd")),
            uiEvents = previewUiEvents,
        )
    }
}
