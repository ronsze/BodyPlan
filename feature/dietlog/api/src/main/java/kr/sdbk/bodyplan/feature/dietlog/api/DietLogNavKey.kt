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

/** 분석 화면 하나가 셋을 겸한다. 기간만 다르고 화면이 하는 일은 같다. */
enum class DietAnalysisPeriod { DAILY, WEEKLY, MONTHLY }

/** [dateEpochDay]는 기간에 든 아무 날이다. 기간의 시작과 끝은 화면이 정한다. */
@Serializable
data class DietAnalysisNavKey(val period: DietAnalysisPeriod, val dateEpochDay: Long) : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToDietCalendar() = navigate(DietCalendarNavKey)

fun BodyPlanNavigator.navigateToDietLog(date: LocalDate) = navigate(DietLogNavKey(date.toEpochDay()))

fun BodyPlanNavigator.navigateToDietEntryEdit(date: LocalDate, entryId: Long? = null) =
    navigate(DietEntryEditNavKey(date.toEpochDay(), entryId))

fun BodyPlanNavigator.navigateToDietAnalysis(period: DietAnalysisPeriod, date: LocalDate) =
    navigate(DietAnalysisNavKey(period, date.toEpochDay()))
