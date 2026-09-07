package kr.sdbk.bodyplan.navigation

import android.content.Intent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisNavKey
import kr.sdbk.bodyplan.feature.dietlog.api.DietAnalysisPeriod
import kr.sdbk.bodyplan.feature.my.api.InbodyNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisNavKey
import kr.sdbk.bodyplan.feature.workoutlog.api.WorkoutAnalysisPeriod

/**
 * 알림이 넘긴 여분 값을 열 화면으로 옮긴다.
 *
 * 알림에 NavKey를 직렬화해 담지 않는 것은, NavKey 모양이 바뀌면 이미 떠 있는 알림이
 * 깨지기 때문이다. 종류와 날짜만 담고 화면을 정하는 일은 여기서 한다 —
 * 세 feature를 동시에 아는 곳이 `:app`뿐이다.
 */
internal fun Intent.analysisNavKey(): BodyPlanNavKey? {
    val kind = getStringExtra(EXTRA_KIND)
        ?.let { name -> AnalysisKind.entries.firstOrNull { it.name == name } }
        ?: return null
    val epochDay = getLongExtra(EXTRA_DATE, NO_DATE)

    return when (kind) {
        AnalysisKind.INBODY -> InbodyNavKey
        AnalysisKind.DIET_DAILY -> dietKey(DietAnalysisPeriod.DAILY, epochDay)
        AnalysisKind.DIET_WEEKLY -> dietKey(DietAnalysisPeriod.WEEKLY, epochDay)
        AnalysisKind.DIET_MONTHLY -> dietKey(DietAnalysisPeriod.MONTHLY, epochDay)
        AnalysisKind.WORKOUT_DAILY -> workoutKey(WorkoutAnalysisPeriod.DAILY, epochDay)
        AnalysisKind.WORKOUT_WEEKLY -> workoutKey(WorkoutAnalysisPeriod.WEEKLY, epochDay)
        AnalysisKind.WORKOUT_MONTHLY -> workoutKey(WorkoutAnalysisPeriod.MONTHLY, epochDay)
    }
}

// 날짜가 없으면 그 화면을 열 수 없다. 알림만 지우고 앱은 첫 탭으로 연다.
private fun dietKey(period: DietAnalysisPeriod, epochDay: Long): BodyPlanNavKey? =
    epochDay.takeIf { it != NO_DATE }?.let { DietAnalysisNavKey(period, it) }

private fun workoutKey(period: WorkoutAnalysisPeriod, epochDay: Long): BodyPlanNavKey? =
    epochDay.takeIf { it != NO_DATE }?.let { WorkoutAnalysisNavKey(period, it) }

private const val EXTRA_KIND = "analysisKind"
private const val EXTRA_DATE = "analysisDateEpochDay"
private val NO_DATE = Long.MIN_VALUE
