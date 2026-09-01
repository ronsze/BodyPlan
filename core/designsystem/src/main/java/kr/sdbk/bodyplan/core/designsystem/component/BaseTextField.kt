package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme

/**
 * Material 장식 없는 입력 필드. 배경·테두리·패딩과 비활성 상태의 표현은 [modifier]로 호출부가 정한다.
 * 값이 비어 있는 동안 [placeholder]를 같은 자리에 흐린 색으로 그린다.
 */
@Composable
fun BaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = null,
    textStyle: TextStyle = BaseTextDefaults.style,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    val contentColor = LocalContentColor.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        textStyle = textStyle.copy(color = contentColor),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        cursorBrush = SolidColor(contentColor),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder != null) {
                    BaseText(
                        text = placeholder,
                        color = contentColor.copy(alpha = PLACEHOLDER_ALPHA),
                        style = textStyle,
                        maxLines = maxLines,
                    )
                }
                innerTextField()
            }
        },
    )
}

private const val PLACEHOLDER_ALPHA = 0.4f

@Preview(showBackground = true)
@Composable
private fun BaseTextFieldPreview() {
    BodyPlanTheme {
        Column {
            BaseTextField(
                value = "",
                onValueChange = {},
                placeholder = "체중을 입력하세요",
                singleLine = true,
            )
            VerticalSpacer(space = 8.dp)
            BaseTextField(
                value = "72.4",
                onValueChange = {},
                singleLine = true,
            )
        }
    }
}
