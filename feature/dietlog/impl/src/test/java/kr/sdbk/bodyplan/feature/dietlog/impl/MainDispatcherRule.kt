package kr.sdbk.bodyplan.feature.dietlog.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope`는 `Dispatchers.Main`에서 돈다. 테스트가 이 규칙으로 그 자리를 대신 채운다.
 *
 * 즉시 실행 디스패처를 쓴다. 상태 변경과 Effect 전달이 여러 코루틴을 건너뛰며 이어지는데,
 * 큐에 쌓는 디스패처로는 그 사슬이 한 번의 진행으로 끝까지 흐르지 않아 검증 시점이 어긋난다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
