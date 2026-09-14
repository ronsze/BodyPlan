package kr.sdbk.bodyplan.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.sdbk.bodyplan.core.designsystem.theme.Border
import kr.sdbk.bodyplan.core.designsystem.theme.Surface
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary

/** 입력칸 위에 붙는 이름. 칸마다 라벨 모양이 갈리지 않게 한 곳에 둔다. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    BaseText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
    )
}

/** 입력 요소를 감싸는 테두리 상자. 숫자 칸·메모 칸이 같은 모양을 쓴다. */
@Composable
fun InputBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(BorderStroke(1.dp, Border), shape)
            .padding(16.dp),
        content = content,
    )
}
