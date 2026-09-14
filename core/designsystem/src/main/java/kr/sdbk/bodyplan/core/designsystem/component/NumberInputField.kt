package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary

/**
 * 라벨·단위가 달린 숫자 한 칸.
 *
 * 글자를 거르지 않는다 — 무엇이 유효한 입력인지는 저장 규칙을 아는 호출부가 정한다.
 */
@Composable
fun NumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        InputBox {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BaseTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = placeholder,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                )
                BaseText(
                    text = suffix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F4F6)
@Composable
private fun NumberInputFieldPreview() {
    BodyPlanTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberInputField(label = "무게", value = "60", onValueChange = {}, suffix = "kg")
            NumberInputField(label = "나이", value = "", onValueChange = {}, suffix = "세", placeholder = "비워 둘 수 있어요")
        }
    }
}
