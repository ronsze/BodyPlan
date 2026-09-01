package kr.sdbk.bodyplan.core.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey

/**
 * 백스택 조작 창구. feature의 `api` 모듈이 이 클래스의 확장 함수로 자기 화면으로의 이동을 공개한다.
 * feature가 백스택을 직접 만지지 않게 해, 이동 경로가 항상 NavKey를 거치도록 한다.
 */
@Stable
class BodyPlanNavigator(private val backStack: MutableList<NavKey>) {
    fun navigate(key: BodyPlanNavKey) {
        backStack.add(key)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }
}
