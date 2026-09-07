package kr.sdbk.bodyplan.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanCard
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextSecondary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection

/**
 * 저장된 분석 결과를 그린다. 식단·운동·인바디가 같은 모양을 쓴다.
 *
 * 묶음의 제목과 개수는 분석 종류마다 다르고 AI가 정한다. 받은 순서 그대로 그린다 —
 * 제목이 지시문과 어긋났다고 걸러 내면 사용자가 볼 것이 없어진다.
 */
@Composable
fun AnalysisResultContent(result: AnalysisResult, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LabeledCard(title = "요약", body = result.content.summary)

        result.content.sections.forEach { section ->
            LabeledCard(title = section.title, body = section.body)
        }

        BaseText(
            text = ANALYZED_AT_FORMAT.format(
                Instant.ofEpochMilli(result.createdAtMillis).atZone(ZoneId.systemDefault()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun LabeledCard(title: String, body: String) {
    BodyPlanCard {
        BaseText(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        VerticalSpacer(space = 8.dp)
        BaseText(text = body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

private val ANALYZED_AT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm 분석")

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun AnalysisResultContentPreview() {
    BodyPlanTheme {
        AnalysisResultContent(
            result = AnalysisResult(
                id = 1L,
                kind = AnalysisKind.WORKOUT_DAILY,
                scopeKey = "20700",
                content = AnalysisContent(
                    summary = "가슴과 삼두를 함께 자극한 날입니다. 볼륨은 목표에 조금 못 미칩니다.",
                    sections = listOf(
                        AnalysisSection("수행한 운동", "벤치프레스 3세트, 딥스 3세트"),
                        AnalysisSection("볼륨", "총 2,340kg입니다."),
                        AnalysisSection("부위 균형", "가슴·삼두에 몰렸고 등은 빠졌습니다."),
                        AnalysisSection("개선 방향", "다음 날 등 운동을 넣어 균형을 맞추세요."),
                    ),
                ),
                createdAtMillis = 1_757_300_000_000L,
            ),
        )
    }
}
