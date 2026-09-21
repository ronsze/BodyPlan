package kr.sdbk.bodyplan.core.data.session

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.repository.WorkoutSessionController

/**
 * 세션 동안 앞에 떠서 알림에 경과 시간을 그린다. 상태는 갖지 않는다 — 컨트롤러의 것을 비출 뿐이다.
 *
 * 세션이 `null`이 되면 스스로 멈춘다. 휴식 종료 진동도 여기서 울린다 — 앱이 뒤에 있으면
 * 화면이 없어 화면이 울릴 수 없다.
 */
@AndroidEntryPoint
internal class WorkoutSessionService : Service() {
    @Inject
    lateinit var controller: WorkoutSessionController

    @Inject
    lateinit var notifier: WorkoutSessionNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        // startForegroundService 뒤 곧바로 올라가야 한다. 세션이 아직 없으면 빈 값으로 올린 뒤 바로 멈춘다.
        startForeground(WorkoutSessionNotifier.NOTIFICATION_ID, notifier.build(controller.session.value ?: EMPTY))
        scope.launch {
            controller.session.collect { session ->
                if (session == null) {
                    ServiceCompat.stopForeground(this@WorkoutSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    notifier.update(session)
                }
            }
        }
        scope.launch {
            controller.restEnded.collect { vibrate() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.resume()
            ACTION_END -> controller.end()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun vibrate() {
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(VIBRATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    companion object {
        const val ACTION_PAUSE = "kr.sdbk.bodyplan.session.PAUSE"
        const val ACTION_RESUME = "kr.sdbk.bodyplan.session.RESUME"
        const val ACTION_END = "kr.sdbk.bodyplan.session.END"
        private const val VIBRATION_MILLIS = 500L
        private val EMPTY =
            WorkoutSession(elapsedSeconds = 0L, isPaused = false, completedSets = emptySet(), rest = null)
    }
}
