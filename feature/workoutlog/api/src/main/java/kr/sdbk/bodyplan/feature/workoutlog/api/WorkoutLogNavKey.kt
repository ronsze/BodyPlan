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

fun BodyPlanNavigator.navigateToWorkoutCalendar() = navigate(WorkoutCalendarNavKey)

fun BodyPlanNavigator.navigateToWorkoutLog(date: LocalDate) = navigate(WorkoutLogNavKey(date.toEpochDay()))

fun BodyPlanNavigator.navigateToWorkoutEntryEdit(date: LocalDate, entryId: Long? = null) =
    navigate(WorkoutEntryEditNavKey(date.toEpochDay(), entryId))

fun BodyPlanNavigator.navigateToExerciseManage() = navigate(ExerciseManageNavKey)
