package kr.sdbk.bodyplan.feature.my.impl.onboarding.composable

import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.OutlinedActionButton
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.onboarding.OnboardingEffect
import kr.sdbk.bodyplan.feature.my.impl.onboarding.OnboardingIntent
import kr.sdbk.bodyplan.feature.my.impl.onboarding.OnboardingState
import kr.sdbk.bodyplan.feature.my.impl.onboarding.OnboardingViewModel
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileInput
import kr.sdbk.bodyplan.feature.my.impl.profile.composable.ProfileForm

internal data class OnboardingUiEvents(
    val onChangeInput: (ProfileInput) -> Unit,
    val onClickStart: () -> Unit,
    val onClickSkip: () -> Unit,
)

@Composable
internal fun OnboardingView(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(viewModel)
    val context = LocalContext.current

    OnboardingViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is OnboardingEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(viewModel: OnboardingViewModel): OnboardingUiEvents = remember {
    OnboardingUiEvents(
        onChangeInput = { viewModel.handleIntent(OnboardingIntent.ChangeInput(it)) },
        onClickStart = { viewModel.handleIntent(OnboardingIntent.ClickStart) },
        onClickSkip = { viewModel.handleIntent(OnboardingIntent.ClickSkip) },
    )
}

@Composable
internal fun OnboardingViewImpl(state: OnboardingState, uiEvents: OnboardingUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "시작하기")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            BaseText(
                text = NOTICE,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            VerticalSpacer(space = 20.dp)
            ProfileForm(input = state.input, onChange = uiEvents.onChangeInput)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(
                text = "시작하기",
                onClick = uiEvents.onClickStart,
                enabled = !state.isSaving,
            )
            OutlinedActionButton(text = "건너뛰기", onClick = uiEvents.onClickSkip)
        }
    }
}

private const val NOTICE =
    "분석이 참고할 정보입니다. 지금 비워 두어도 되고, 나중에 마이 탭에서 채울 수 있습니다."

private val previewUiEvents = OnboardingUiEvents(
    onChangeInput = {},
    onClickStart = {},
    onClickSkip = {},
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun OnboardingViewImplPreview() {
    BodyPlanTheme {
        OnboardingViewImpl(state = OnboardingState(), uiEvents = previewUiEvents)
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun OnboardingViewImplFilledPreview() {
    BodyPlanTheme {
        OnboardingViewImpl(
            state = OnboardingState(
                input = ProfileInput(
                    ageYears = "28",
                    heightCm = "168",
                    weightKg = "60",
                    goals = setOf(Goal.DIET, Goal.TARGET_WEIGHT),
                    targetWeightKg = "55",
                ),
            ),
            uiEvents = previewUiEvents,
        )
    }
}
