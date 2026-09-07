package kr.sdbk.bodyplan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 분석 워커가 Hilt 주입을 받으려면 앱이 워커 팩터리를 내줘야 한다.
 *
 * 매니페스트에서 WorkManager의 기본 초기화를 꺼 두었다. 그것이 먼저 돌면 이 설정이 쓰이지 않아
 * 워커가 만들어지지 못한다.
 */
@HiltAndroidApp
class BodyPlanApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
