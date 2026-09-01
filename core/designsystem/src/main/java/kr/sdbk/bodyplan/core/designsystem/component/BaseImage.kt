package kr.sdbk.bodyplan.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme

/** 네트워크 URL 이미지. 로드 중에는 [placeholder], 실패하거나 [url]이 null이면 [error]를 그린다. */
@Composable
fun BaseImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = placeholder,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = error,
    )
}

/** 로컬 파일 이미지. 로드 중에는 [placeholder], 실패하면 [error]를 그린다. */
@Composable
fun BaseImage(
    file: File,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = placeholder,
) {
    AsyncImage(
        model = file,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = error,
    )
}

/**
 * 드로어블 리소스 이미지. 앱에 포함된 리소스라 실패·로딩 상태가 없으므로 동기로 그린다 —
 * placeholder/error 슬롯이 없는 이유다.
 */
@Composable
fun BaseImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    BaseImage(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/**
 * 이미 만들어진 [Painter]를 그린다. 로딩 방식을 호출부가 직접 고를 때 쓴다 —
 * 큰 비트맵을 다운샘플링해 받으려면 `rememberAsyncImagePainter`, 벡터·아이콘이면 `painterResource`.
 */
@Composable
fun BaseImage(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@Preview(showBackground = true)
@Composable
private fun BaseImagePreview() {
    BodyPlanTheme {
        BaseImage(
            url = null,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            placeholder = ColorPainter(Color.LightGray),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BaseImagePainterPreview() {
    BodyPlanTheme {
        BaseImage(
            painter = ColorPainter(Color.Gray),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
    }
}
