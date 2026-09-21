package kr.sdbk.bodyplan.core.data.session

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kr.sdbk.bodyplan.core.domain.model.WorkoutSession
import kr.sdbk.bodyplan.core.domain.model.elapsedText
import kr.sdbk.bodyplan.core.domain.model.text

/**
 * 세션 알림을 만든다. 매초 갈아 끼우므로 소리·진동 없이 조용히 바뀐다.
 *
 * [build]는 서비스가 foreground로 올라갈 때 쓰고, [update]는 그 뒤 매초 쓴다.
 * 권한이 없으면 [update]는 아무것도 하지 않는다 — 서비스와 세션은 그대로 돈다.
 */
@Singleton
internal class WorkoutSessionNotifier
@Inject
constructor(@ApplicationContext private val context: Context) {
    fun build(session: WorkoutSession): Notification {
        createChannel()
        val body = listOfNotNull(session.elapsedText, session.rest?.let { "휴식 ${it.text}" }).joinToString(" · ")
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(if (session.isPaused) "일시정지" else "운동 중")
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
        if (session.isPaused) {
            builder.addAction(0, "재개", serviceIntent(WorkoutSessionService.ACTION_RESUME))
        } else {
            builder.addAction(0, "일시정지", serviceIntent(WorkoutSessionService.ACTION_PAUSE))
        }
        builder.addAction(0, "종료", serviceIntent(WorkoutSessionService.ACTION_END))
        return builder.build()
    }

    fun update(session: WorkoutSession) {
        if (!hasPermission()) return
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, build(session))
    }

    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        context,
        action.hashCode(),
        Intent(context, WorkoutSessionService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE,
    )

    /** 알림을 누르면 앱을 연다. 마지막 화면 그대로다 — 어느 화면으로 갈지는 정하지 않는다. */
    private fun openAppIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** 안드로이드 13부터 알림에 권한이 필요하다. 그 아래는 늘 허용이다. */
    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val NOTIFICATION_ID = 1_001
        private const val CHANNEL_ID = "workout_session"
        private const val CHANNEL_NAME = "운동 세션"
    }
}
