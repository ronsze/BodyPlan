package kr.sdbk.bodyplan.feature.my.impl.inbody.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.File
import java.time.LocalDate
import kr.sdbk.bodyplan.core.designsystem.component.Badge
import kr.sdbk.bodyplan.core.designsystem.component.BaseImage
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Accent
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.SurfaceMuted
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.InbodyEntry
import kr.sdbk.bodyplan.core.domain.model.InbodyEntryStatus
import kr.sdbk.bodyplan.core.domain.model.InbodyMetricKind
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.usecase.InbodyMetrics
import kr.sdbk.bodyplan.core.ui.components.label
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.SecondaryFilledButton
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.fullText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.label
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.tenthsText
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.title
import kr.sdbk.bodyplan.feature.my.impl.inbodycommon.unit

/**
 * 사진 기록 한 건. 분석 상태에 따라 값, 진행 단계, 실패 사유, 안내 중 하나를 보인다.
 *
 * 분석 중에는 이전 결과가 있어도 값을 숨긴다. 새 결과가 나오면 값이 바뀔 것이라 옛 값을 같이
 * 보이면 어느 것이 맞는지 흐려진다.
 */
@Composable
internal fun InbodyRecordCard(
    entry: InbodyEntry,
    isLatest: Boolean,
    onClickDetail: () -> Unit,
    onClickAnalyze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = entry.status
    BodyPlanCard(modifier = modifier, contentPadding = 16.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BaseImage(
                file = File(entry.record.imagePath),
                contentDescription = "인바디 사진",
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceMuted),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BaseText(
                        text = entry.record.measuredOn.fullText(),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    if (isLatest) Badge(text = "최근 측정")
                }
                BaseText(text = status.label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        }
        VerticalSpacer(space = 12.dp)

        when (status) {
            InbodyEntryStatus.RUNNING -> RunningBody(entry)

            InbodyEntryStatus.NOT_ANALYZED -> BaseText(
                text = "아직 분석하지 않았어요. 새로 분석하기를 눌러 값을 읽어 보세요.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )

            InbodyEntryStatus.FAILED -> {
                BaseText(
                    text = entry.record.failureReason.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Danger,
                )
                if (entry.measurement != null) {
                    VerticalSpacer(space = 8.dp)
                    Values(entry)
                }
            }

            InbodyEntryStatus.ANALYZED ->
                if (entry.measurement != null) {
                    Values(entry)
                } else {
                    BaseText(
                        text = "사진에서 읽힌 값이 없어요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
        }

        VerticalSpacer(space = 12.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BaseText(
                text = "상세 보기 >",
                modifier = Modifier
                    .clickable(onClick = onClickDetail)
                    .width(DETAIL_WIDTH),
                style = MaterialTheme.typography.bodySmall,
                color = Accent,
            )
            SecondaryFilledButton(
                text = if (status == InbodyEntryStatus.RUNNING) "분석 중" else "새로 분석하기",
                onClick = onClickAnalyze,
                modifier = Modifier.weight(1f),
                enabled = status != InbodyEntryStatus.RUNNING,
            )
        }
    }
}

@Composable
private fun Values(entry: InbodyEntry) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        InbodyMetricKind.entries.forEach { kind ->
            val tenths = InbodyMetrics.tenthsOf(entry.measurement, kind)
            Column {
                BaseText(text = kind.title, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                BaseText(
                    text = tenths?.let { "${it.tenthsText()} ${kind.unit}" } ?: "-",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun RunningBody(entry: InbodyEntry) {
    val stage = entry.run?.stage
    BaseText(
        text = stage?.label ?: "분석을 시작하는 중",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
    )
    VerticalSpacer(space = 6.dp)
    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = Accent,
    )
    VerticalSpacer(space = 6.dp)
    BaseText(
        text = "진행 중에는 다시 요청할 수 없어요.",
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
    )
}

private val DETAIL_WIDTH = 88.dp

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun InbodyRecordCardNotAnalyzedPreview() {
    BodyPlanTheme {
        InbodyRecordCard(
            entry = InbodyEntry(
                record = InbodyRecord(1L, "/none.jpg", LocalDate.of(2026, 9, 29), 0L),
                result = null,
                run = null,
            ),
            isLatest = false,
            onClickDetail = {},
            onClickAnalyze = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
