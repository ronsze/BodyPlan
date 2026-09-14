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
import java.util.Locale
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.WeightSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.RestTimer
import kr.sdbk.bodyplan.feature.workoutlog.impl.log.WorkoutSession

/** 세션 중 하단에 붙는 한 줄. 쉬는 동안은 남은 시간이 경과 시간보다 급해 그것만 보인다. */
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
            val rest = session.rest
            if (rest == null) {
                BaseText(
                    text = "운동 중 · ${session.elapsedSeconds / SECONDS_PER_MINUTE}분",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                return@Row
            }
            BaseText(
                text = "휴식 ${restText(rest)}",
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

private fun restText(rest: RestTimer): String = String.format(
    Locale.US,
    "%d:%02d",
    rest.remainingSeconds / SECONDS_PER_MINUTE,
    rest.remainingSeconds % SECONDS_PER_MINUTE,
)

private const val SECONDS_PER_MINUTE = 60
private const val REST_STEP_SECONDS = 30

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun SessionBarRestingPreview() {
    BodyPlanTheme {
        SessionBar(
            session = WorkoutSession(startedAtMillis = 0L, elapsedSeconds = 720L, rest = RestTimer(75)),
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
            session = WorkoutSession(startedAtMillis = 0L, elapsedSeconds = 720L),
            onClickAdjustRest = {},
            onClickSkipRest = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
