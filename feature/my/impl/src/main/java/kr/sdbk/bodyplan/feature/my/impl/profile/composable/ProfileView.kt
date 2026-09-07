package kr.sdbk.bodyplan.feature.my.impl.profile.composable

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.ui.coordinator.CollectEffect
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileEffect
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileInput
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileIntent
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileState
import kr.sdbk.bodyplan.feature.my.impl.profile.ProfileViewModel

internal data class ProfileEvents(val goBack: () -> Unit)

internal data class ProfileUiEvents(
    val onBackPressed: () -> Unit,
    val onChangeInput: (ProfileInput) -> Unit,
    val onClickSave: () -> Unit,
)

@Composable
internal fun ProfileView(events: ProfileEvents, viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)
    val context = LocalContext.current

    ProfileViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is ProfileEffect.GoBack -> events.goBack()

            is ProfileEffect.ShowMessage ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun rememberUiEvents(events: ProfileEvents, viewModel: ProfileViewModel): ProfileUiEvents = remember {
    ProfileUiEvents(
        onBackPressed = events.goBack,
        onChangeInput = { viewModel.handleIntent(ProfileIntent.ChangeInput(it)) },
        onClickSave = { viewModel.handleIntent(ProfileIntent.ClickSave) },
    )
}

@Composable
internal fun ProfileViewImpl(state: ProfileState, uiEvents: ProfileUiEvents) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = "내 정보", onBack = uiEvents.onBackPressed)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            ProfileForm(input = state.input, onChange = uiEvents.onChangeInput)
        }

        PrimaryButton(
            text = "저장",
            onClick = uiEvents.onClickSave,
            modifier = Modifier.padding(16.dp),
            enabled = !state.isSaving,
        )
    }
}

private val previewUiEvents = ProfileUiEvents(
    onBackPressed = {},
    onChangeInput = {},
    onClickSave = {},
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ProfileViewImplPreview() {
    BodyPlanTheme {
        ProfileViewImpl(
            state = ProfileState(
                input = ProfileInput(
                    ageYears = "30",
                    heightCm = "175",
                    weightKg = "72",
                    gender = Gender.MALE,
                    goals = setOf(Goal.DIET),
                ),
            ),
            uiEvents = previewUiEvents,
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ProfileViewImplEmptyPreview() {
    BodyPlanTheme {
        ProfileViewImpl(state = ProfileState(), uiEvents = previewUiEvents)
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ProfileViewImplLoadingPreview() {
    BodyPlanTheme {
        ProfileViewImpl(state = ProfileState(isLoading = true), uiEvents = previewUiEvents)
    }
}
