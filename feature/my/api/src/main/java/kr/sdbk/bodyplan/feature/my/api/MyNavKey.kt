package kr.sdbk.bodyplan.feature.my.api

import kotlinx.serialization.Serializable
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavKey
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator

@Serializable
data object MyNavKey : BodyPlanNavKey()

@Serializable
data object AiTokenNavKey : BodyPlanNavKey()

@Serializable
data object OnboardingNavKey : BodyPlanNavKey()

@Serializable
data object ProfileNavKey : BodyPlanNavKey()

@Serializable
data object InbodyNavKey : BodyPlanNavKey()

@Serializable
data object InbodyAddNavKey : BodyPlanNavKey()

@Serializable
data class InbodyDetailNavKey(val recordId: Long) : BodyPlanNavKey()

@Serializable
data object WeightNavKey : BodyPlanNavKey()

@Serializable
data object BackupNavKey : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToMy() = navigate(MyNavKey)

fun BodyPlanNavigator.navigateToAiToken() = navigate(AiTokenNavKey)

fun BodyPlanNavigator.navigateToOnboarding() = navigate(OnboardingNavKey)

fun BodyPlanNavigator.navigateToProfile() = navigate(ProfileNavKey)

fun BodyPlanNavigator.navigateToInbody() = navigate(InbodyNavKey)

fun BodyPlanNavigator.navigateToInbodyAdd() = navigate(InbodyAddNavKey)

fun BodyPlanNavigator.navigateToInbodyDetail(recordId: Long) = navigate(InbodyDetailNavKey(recordId))

fun BodyPlanNavigator.navigateToWeight() = navigate(WeightNavKey)

fun BodyPlanNavigator.navigateToBackup() = navigate(BackupNavKey)
