package kr.sdbk.bodyplan.feature.my.impl.home.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.SectionRow
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.AiProvider
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.ui.components.AiProviderMark
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.home.MyEffect
import kr.sdbk.bodyplan.feature.my.impl.home.MyIntent
import kr.sdbk.bodyplan.feature.my.impl.home.MyState
import kr.sdbk.bodyplan.feature.my.impl.home.MyViewModel

internal data class MyEvents(
    val goToAiToken: () -> Unit,
    val goToProfile: () -> Unit,
    val goToInbody: () -> Unit,
    val goToWeight: () -> Unit,
)

internal data class MyUiEvents(
    val onClickAiToken: () -> Unit,
    val onClickProfile: () -> Unit,
    val onClickInbody: () -> Unit,
    val onClickWeight: () -> Unit,
)

@Composable
internal fun MyView(events: MyEvents, viewModel: MyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(viewModel)

    MyViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is MyEffect.NavigateToAiToken -> events.goToAiToken()
            is MyEffect.NavigateToProfile -> events.goToProfile()
            is MyEffect.NavigateToInbody -> events.goToInbody()
            is MyEffect.NavigateToWeight -> events.goToWeight()
        }
    }
}

@Composable
private fun rememberUiEvents(viewModel: MyViewModel): MyUiEvents = remember {
    MyUiEvents(
        onClickAiToken = { viewModel.handleIntent(MyIntent.ClickAiToken) },
        onClickProfile = { viewModel.handleIntent(MyIntent.ClickProfile) },
        onClickInbody = { viewModel.handleIntent(MyIntent.ClickInbody) },
        onClickWeight = { viewModel.handleIntent(MyIntent.ClickWeight) },
    )
}

@Composable
internal fun MyViewImpl(state: MyState, uiEvents: MyUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "마이")

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileSummaryCard(
                profile = state.profile,
                fromInbody = state.profileFromInbody,
                onClick = uiEvents.onClickProfile,
            )
            SectionRow(
                title = "AI 연동",
                onClick = uiEvents.onClickAiToken,
                description = state.connectedProvider?.let { "${it.label} 연결됨" } ?: "연결되지 않음",
                leading = state.connectedProvider?.let { provider ->
                    { AiProviderMark(provider, size = 24.dp) }
                },
            )
            SectionRow(
                title = "인바디 분석",
                onClick = uiEvents.onClickInbody,
                description = "사진으로 체성분을 분석해요",
            )
            SectionRow(
                title = "체중 기록",
                onClick = uiEvents.onClickWeight,
                description = "매일 재고 변동을 확인해요",
            )
        }
    }
}

private val previewUiEvents =
    MyUiEvents(onClickAiToken = {}, onClickProfile = {}, onClickInbody = {}, onClickWeight = {})

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun MyViewImplPreview() {
    BodyPlanTheme {
        MyViewImpl(
            state = MyState(
                connectedProvider = AiProvider.CLAUDE,
                profile = UserProfile(ageYears = 30, heightCm = 175, goals = setOf(Goal.DIET)),
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun MyViewImplNotConnectedPreview() {
    BodyPlanTheme {
        MyViewImpl(state = MyState(), uiEvents = previewUiEvents)
    }
}
