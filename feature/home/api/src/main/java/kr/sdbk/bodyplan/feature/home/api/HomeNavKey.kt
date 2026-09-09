package kr.sdbk.bodyplan.feature.home.api

import kotlinx.serialization.Serializable
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavKey
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator

@Serializable
data object HomeNavKey : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToHome() = navigate(HomeNavKey)
