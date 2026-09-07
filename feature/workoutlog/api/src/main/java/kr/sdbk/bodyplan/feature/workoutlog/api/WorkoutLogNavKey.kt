package kr.sdbk.bodyplan.feature.workoutlog.api

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavKey
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator

/**
 * 날짜는 epochDay로 담는다. `LocalDate`는 kotlinx.serialization이 다루지 못한다.
 * 변환은 아래 navigate 확장 함수가 맡아, 호출부가 epochDay를 직접 계산하지 않게 한다.
 */
@Serializable
data object WorkoutCalendarNavKey : BodyPlanNavKey()

@Serializable
data class WorkoutLogNavKey(val dateEpochDay: Long) : BodyPlanNavKey()

@Serializable
data class WorkoutEntryEditNavKey(val dateEpochDay: Long, val entryId: Long? = null) : BodyPlanNavKey()

@Serializable
data object ExerciseManageNavKey : BodyPlanNavKey()

/** 분석 화면 하나가 셋을 겸한다. 기간만 다르고 화면이 하는 일은 같다. */
enum class WorkoutAnalysisPeriod { DAILY, WEEKLY, MONTHLY }

/** [dateEpochDay]는 기간에 든 아무 날이다. 기간의 시작과 끝은 화면이 되짚는다. */
@Serializable
data class WorkoutAnalysisNavKey(val period: WorkoutAnalysisPeriod, val dateEpochDay: Long) : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToWorkoutCalendar() = navigate(WorkoutCalendarNavKey)

fun BodyPlanNavigator.navigateToWorkoutLog(date: LocalDate) = navigate(WorkoutLogNavKey(date.toEpochDay()))

fun BodyPlanNavigator.navigateToWorkoutEntryEdit(date: LocalDate, entryId: Long? = null) =
    navigate(WorkoutEntryEditNavKey(date.toEpochDay(), entryId))

fun BodyPlanNavigator.navigateToExerciseManage() = navigate(ExerciseManageNavKey)

fun BodyPlanNavigator.navigateToWorkoutAnalysis(period: WorkoutAnalysisPeriod, date: LocalDate) =
    navigate(WorkoutAnalysisNavKey(period, date.toEpochDay()))
