package kr.sdbk.bodyplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import kr.sdbk.bodyplan.core.designsystem.theme.BodyPlanTheme
import kr.sdbk.bodyplan.navigation.BodyPlanMainScreen
import kr.sdbk.bodyplan.navigation.analysisNavKey

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BodyPlanTheme {
                BodyPlanMainScreen(
                    modifier = Modifier.fillMaxSize(),
                    // 알림을 눌러 들어왔으면 그 분석 화면까지 연다.
                    startNavKey = intent?.analysisNavKey(),
                )
            }
        }
    }
}
