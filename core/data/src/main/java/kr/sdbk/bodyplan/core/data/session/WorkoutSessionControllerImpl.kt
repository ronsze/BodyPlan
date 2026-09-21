package kr.sdbk.bodyplan.core.data.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kr.sdbk.bodyplan.core.domain.repository.WorkoutSessionController

/**
 * 엔진에 서비스 시작만 덧붙인다.
 *
 * 세션이 열리면 알림이 따라 떠야 하는데, 엔진이 그것을 알면 Android에 묶여 단위 테스트가 안 된다.
 * 서비스는 세션이 `null`이 되는 것을 보고 스스로 멈추므로 여기서 끝내지 않는다.
 */
@Singleton
internal class WorkoutSessionControllerImpl
@Inject
constructor(
    private val engine: WorkoutSessionEngine,
    @ApplicationContext private val context: Context,
) : WorkoutSessionController by engine {
    override fun start() {
        // 이미 세션 중이면 서비스도 이미 떠 있다. 다시 띄우면 startForeground 없는 onStartCommand만 돈다.
        if (engine.session.value != null) return
        engine.start()
        ContextCompat.startForegroundService(context, Intent(context, WorkoutSessionService::class.java))
    }
}
