package kr.sdbk.bodyplan.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 화면 목적지. 각 feature의 `api` 모듈이 이것을 상속해 선언한다.
 * 인자가 있으면 data class로, 없으면 data object로 선언한다.
 */
@Serializable
abstract class BodyPlanNavKey : NavKey
