package kr.sdbk.bodyplan.core.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind

/**
 * 분석이 끝난 것을 알린다.
 *
 * 화면 밖에서 도는 일이라 알림이 없으면 사용자가 끝난 줄 모른다.
 * 권한이 없으면 아무 것도 하지 않는다 — 알림은 곁다리이고 분석 자체는 끝까지 돈다.
 */
@Singleton
internal class AnalysisNotifier
@Inject
constructor(@ApplicationContext private val context: Context) {
    fun notifySucceeded(kind: AnalysisKind, periodLabel: String, fromEpochDay: Long) {
        notify(
            kind = kind,
            fromEpochDay = fromEpochDay,
            title = "분석이 끝났어요",
            body = listOf(periodLabel, kind.subject).filter { it.isNotBlank() }.joinToString(" "),
        )
    }

    fun notifyFailed(kind: AnalysisKind, reason: String?, fromEpochDay: Long) {
        notify(
            kind = kind,
            fromEpochDay = fromEpochDay,
            title = "분석하지 못했어요",
            body = reason ?: "다시 시도해 주세요",
        )
    }

    private fun notify(kind: AnalysisKind, fromEpochDay: Long, title: String, body: String) {
        if (!hasPermission()) return
        createChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(openIntent(kind, fromEpochDay))
            .build()

        // 종류마다 하나만 남긴다. 같은 대상을 다시 분석하면 앞의 알림을 갈아 끼운다.
        NotificationManagerCompat.from(context).notify(kind.ordinal, notification)
    }

    /**
     * 알림을 누르면 그 분석 화면을 연다.
     *
     * 앱의 시작 화면을 그대로 쓰고 무엇을 열지는 여분 값으로 넘긴다 — `core:data`가
     * 액티비티나 NavKey를 알지 않아도 된다.
     */
    private fun openIntent(kind: AnalysisKind, fromEpochDay: Long): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.putExtra(EXTRA_KIND, kind.name)
            ?.putExtra(EXTRA_DATE, fromEpochDay)
            ?: return null
        return PendingIntent.getActivity(
            context,
            kind.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** 안드로이드 13부터 알림에 권한이 필요하다. 그 아래는 늘 허용이다. */
    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "analysis_result"
        const val CHANNEL_NAME = "분석 결과"
    }
}

/** 알림이 어느 화면을 열지 담는 여분 값. `:app`이 같은 이름으로 읽는다. */
const val EXTRA_KIND = "analysisKind"
const val EXTRA_DATE = "analysisDateEpochDay"

private val AnalysisKind.subject: String
    get() = when (this) {
        AnalysisKind.DIET_DAILY, AnalysisKind.DIET_WEEKLY, AnalysisKind.DIET_MONTHLY -> "식단 분석"
        AnalysisKind.WORKOUT_DAILY, AnalysisKind.WORKOUT_WEEKLY, AnalysisKind.WORKOUT_MONTHLY -> "운동 분석"
        AnalysisKind.INBODY -> "인바디 분석"
    }
