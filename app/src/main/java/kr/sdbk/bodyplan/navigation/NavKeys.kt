package kr.sdbk.bodyplan.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 화면 목적지. 화면을 추가할 때 여기에 NavKey를 하나 더한다.
 * 인자가 있으면 data class로, 없으면 data object로 선언한다.
 */
@Serializable
data object Home : NavKey
