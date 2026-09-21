package kr.sdbk.bodyplan.feature.workoutlog.impl.log.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.RestTimer
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.model.elapsedText
import kr.sdbk.bodyplan.core.domain.model.text

/** 세션 중 하단에 붙는 한 줄. 쉬는 동안은 남은 시간이 경과 시간보다 급해 그것만 보이고, 멈춰 있으면 휴식도 숨는다. */
@Composable
internal fun SessionBar(
    session: WorkoutSession,
    onClickAdjustRest: (Int) -> Unit,
    onClickSkipRest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyPlanCard(modifier = modifier, contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (session.isPaused) {
                BaseText(
                    text = "일시정지 · ${session.elapsedText}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextTertiary,
                )
                return@Row
            }
            val rest = session.rest
            if (rest == null) {
                BaseText(
                    text = "운동 중 · ${session.elapsedText}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                return@Row
            }
            BaseText(
                text = "휴식 ${rest.text}",
                style = MaterialTheme.typography.titleMedium,
                color = Accent,
            )
            WeightSpacer()
            RestAction(text = "-30초", color = Accent, onClick = { onClickAdjustRest(-REST_STEP_SECONDS) })
            RestAction(text = "+30초", color = Accent, onClick = { onClickAdjustRest(REST_STEP_SECONDS) })
            RestAction(text = "건너뛰기", color = TextTertiary, onClick = onClickSkipRest)
        }
    }
}

@Composable
private fun RestAction(text: String, color: Color, onClick: () -> Unit) {
    BaseText(
        text = text,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = color,
    )
}

private const val REST_STEP_SECONDS = 30

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun SessionBarRestingPreview() {
    BodyPlanTheme {
        SessionBar(
            session = previewSession(rest = RestTimer(75)),
            onClickAdjustRest = {},
            onClickSkipRest = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun SessionBarPreview() {
    BodyPlanTheme {
        SessionBar(
            session = previewSession(),
            onClickAdjustRest = {},
            onClickSkipRest = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun SessionBarPausedPreview() {
    BodyPlanTheme {
        SessionBar(
            session = previewSession(isPaused = true),
            onClickAdjustRest = {},
            onClickSkipRest = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun previewSession(isPaused: Boolean = false, rest: RestTimer? = null) =
    WorkoutSession(elapsedSeconds = 754L, isPaused = isPaused, completedSets = emptySet(), rest = rest)
