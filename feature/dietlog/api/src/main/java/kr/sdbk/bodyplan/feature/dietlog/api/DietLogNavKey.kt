package kr.sdbk.bodyplan.feature.dietlog.api

import kotlinx.serialization.Serializable
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavKey
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator

@Serializable
data object DietLogNavKey : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToDietLog() = navigate(DietLogNavKey)
