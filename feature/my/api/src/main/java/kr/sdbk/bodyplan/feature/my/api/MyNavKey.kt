package kr.sdbk.bodyplan.feature.my.api

import kotlinx.serialization.Serializable
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavKey
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator

@Serializable
data object MyNavKey : BodyPlanNavKey()

@Serializable
data object AiTokenNavKey : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToMy() = navigate(MyNavKey)

fun BodyPlanNavigator.navigateToAiToken() = navigate(AiTokenNavKey)
