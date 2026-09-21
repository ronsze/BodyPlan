package kr.sdbk.bodyplan.core.ui.components

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 알림 권한을 한 번 묻고, 답과 무관하게 [action]을 실행하는 onClick을 만든다.
 *
 * 알림은 곁다리다. 거절했다고 분석이나 세션을 막으면 사용자가 잃는 것이 더 크다.
 * 안드로이드 13 아래는 권한 자체가 없어 바로 실행한다.
 */
@Composable
fun requestNotificationThen(action: () -> Unit): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return action

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        action()
    }
    return remember(launcher, context, action) {
        {
            val granted = ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) action() else launcher.launch(POST_NOTIFICATIONS)
        }
    }
}

private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
