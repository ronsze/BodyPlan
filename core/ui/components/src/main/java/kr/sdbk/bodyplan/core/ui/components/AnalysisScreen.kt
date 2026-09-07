package kr.sdbk.bodyplan.core.ui.components

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kr.sdbk.bodyplan.core.designsystem.component.BaseText
import kr.sdbk.bodyplan.core.designsystem.component.BodyPlanTopBar
import kr.sdbk.bodyplan.core.designsystem.component.PrimaryButton
import kr.sdbk.bodyplan.core.designsystem.component.VerticalSpacer
import kr.sdbk.bodyplan.core.designsystem.theme.Background
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.core.designsystem.theme.Danger
import kr.sdbk.bodyplan.core.designsystem.theme.TextPrimary
import kr.sdbk.bodyplan.core.designsystem.theme.TextTertiary
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.AnalysisSection

/** [AnalysisScreen]이 그리는 것. 화면의 State를 이 모양으로 옮겨 넘긴다. */
data class AnalysisScreenState(
    val title: String,
    /** 제목 아래 줄. 기간 표기가 없는 화면은 `null`. */
    val subtitle: String? = null,
    val result: AnalysisResult? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val canAnalyze: Boolean = true,
    val isTokenDialogVisible: Boolean = false,
    val errorMessage: String? = null,
    val emptyMessage: String = "아직 분석하지 않았어요",
    /** 도는 동안 지금 무엇을 하고 있는지. 돌고 있지 않으면 `null`. */
    val stageMessage: String? = null,
)

data class AnalysisScreenActions(
    val onBack: () -> Unit,
    val onClickAnalyze: () -> Unit,
    val onConfirmTokenDialog: () -> Unit,
    val onDismissTokenDialog: () -> Unit,
)

/**
 * 분석 화면의 껍데기. 식단·운동·인바디가 같은 얼개를 쓴다.
 *
 * 세 화면이 다른 것은 제목과 문구, 그리고 결과 앞뒤에 무엇을 더 그리는지뿐이다.
 * 상태 분기와 버튼, 토큰 팝업의 배치를 세 번 베끼지 않는다.
 */
@Composable
fun AnalysisScreen(
    state: AnalysisScreenState,
    actions: AnalysisScreenActions,
    modifier: Modifier = Modifier,
    beforeResult: @Composable ColumnScope.() -> Unit = {},
    afterResult: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
    ) {
        BodyPlanTopBar(title = state.title, onBack = actions.onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                // 스크롤 끝에서 마지막 요소가 버튼에 붙지 않게 띄운다.
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.subtitle?.let { subtitle ->
                BaseText(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }

            beforeResult()

            when {
                state.isLoading && state.result == null -> CenteredBox { CircularProgressIndicator() }

                state.result != null -> AnalysisResultContent(state.result)

                else -> CenteredBox {
                    BaseText(
                        text = state.emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                    )
                }
            }

            // 실패해도 이전 결과는 지우지 않는다. 문구만 아래에 붙인다.
            state.errorMessage?.let { message ->
                BaseText(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Danger,
                )
            }

            afterResult()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            // 답을 다 받은 뒤 한 번에 읽으므로 진행률은 없다. 대신 지금 하는 일을 적는다.
            state.stageMessage?.let { message ->
                BaseText(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
                VerticalSpacer(space = 8.dp)
            }
            PrimaryButton(
                text = analyzeButtonText(state),
                onClick = requestNotificationThen(actions.onClickAnalyze),
                enabled = state.canAnalyze,
            )
        }
    }

    if (state.isTokenDialogVisible) {
        AiTokenRequiredDialog(
            onConfirm = actions.onConfirmTokenDialog,
            onDismiss = actions.onDismissTokenDialog,
        )
    }
}

/**
 * 분석을 누를 때 알림 권한을 한 번 묻고, 답과 무관하게 분석을 시작한다.
 *
 * 알림은 곁다리다. 거절했다고 분석을 막으면 사용자가 잃는 것이 더 크다.
 * 안드로이드 13 아래는 권한 자체가 없어 바로 시작한다.
 */
@Composable
private fun requestNotificationThen(onClickAnalyze: () -> Unit): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return onClickAnalyze

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onClickAnalyze()
    }
    return remember(launcher, context, onClickAnalyze) {
        {
            val granted = ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) onClickAnalyze() else launcher.launch(POST_NOTIFICATIONS)
        }
    }
}

private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

private fun analyzeButtonText(state: AnalysisScreenState): String = when {
    state.isAnalyzing -> "분석 중"
    state.result != null -> "새로 분석하기"
    else -> "분석하기"
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val previewActions = AnalysisScreenActions(
    onBack = {},
    onClickAnalyze = {},
    onConfirmTokenDialog = {},
    onDismissTokenDialog = {},
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AnalysisScreenPreview() {
    BodyPlanTheme {
        AnalysisScreen(
            state = AnalysisScreenState(
                title = "식단 분석",
                subtitle = "2026년 9월 8일",
                result = AnalysisResult(
                    id = 1L,
                    kind = AnalysisKind.DIET_DAILY,
                    scopeKey = "20700",
                    content = AnalysisContent(
                        summary = "단백질은 충분했지만 탄수화물이 목표보다 적었어요.",
                        sections = listOf(
                            AnalysisSection("먹은 음식", "닭가슴살 샐러드, 고구마 1개"),
                            AnalysisSection("칼로리", "약 1,450kcal로 추정됩니다."),
                        ),
                    ),
                    createdAtMillis = 1_757_300_000_000L,
                ),
            ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun AnalysisScreenEmptyPreview() {
    BodyPlanTheme {
        AnalysisScreen(
            state = AnalysisScreenState(
                title = "운동 분석",
                subtitle = "2026년 9월",
                errorMessage = "분석할 기록이 없습니다",
            ),
            actions = previewActions,
        )
    }
}
