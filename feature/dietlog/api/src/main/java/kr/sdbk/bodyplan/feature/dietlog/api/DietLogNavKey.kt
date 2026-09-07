package kr.sdbk.bodyplan.feature.dietlog.api

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavKey
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator

/**
 * 날짜는 epochDay로 담는다. `LocalDate`는 kotlinx.serialization이 다루지 못한다.
 * 변환은 아래 navigate 확장 함수가 맡아, 호출부가 epochDay를 직접 계산하지 않게 한다.
 */
@Serializable
data object DietCalendarNavKey : BodyPlanNavKey()

@Serializable
data class DietLogNavKey(val dateEpochDay: Long) : BodyPlanNavKey()

@Serializable
data class DietEntryEditNavKey(val dateEpochDay: Long, val entryId: Long? = null) : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToDietCalendar() = navigate(DietCalendarNavKey)

fun BodyPlanNavigator.navigateToDietLog(date: LocalDate) = navigate(DietLogNavKey(date.toEpochDay()))

fun BodyPlanNavigator.navigateToDietEntryEdit(date: LocalDate, entryId: Long? = null) =
    navigate(DietEntryEditNavKey(date.toEpochDay(), entryId))
