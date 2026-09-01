package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme

/**
 * [BaseText]의 기본값. 두 오버로드가 이 값을 공유한다.
 *
 * [color]는 [Color.Unspecified]가 기본이라, 색은 `style.color` → `LocalContentColor` 순으로 해석된다.
 */
object BaseTextDefaults {
    val color: Color = Color.Unspecified

    val style: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalTextStyle.current
}

/** 앱 전역에서 쓰는 텍스트. 색·스타일을 지정하지 않으면 상위가 내려준 값을 따른다. */
@Composable
fun BaseText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BaseTextDefaults.color,
    style: TextStyle = BaseTextDefaults.style,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** 구간별 서식이 붙은 텍스트. 서식 없는 구간은 [color]·[style]을 따른다. */
@Composable
fun BaseText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = BaseTextDefaults.color,
    style: TextStyle = BaseTextDefaults.style,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Preview(showBackground = true)
@Composable
private fun BaseTextPreview() {
    BodyPlanTheme {
        BaseText(text = "BodyPlan")
    }
}

@Preview(showBackground = true)
@Composable
private fun BaseTextAnnotatedPreview() {
    BodyPlanTheme {
        BaseText(
            text = buildAnnotatedString {
                append("오늘 ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("72.4kg") }
                append(" 기록됨")
            },
        )
    }
}
