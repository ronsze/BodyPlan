package kr.sdbk.bodyplan.feature.workoutlog.impl.exercisetrend

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kr.sdbk.bodyplan.core.designsystem.theme.PartCardio
import kr.sdbk.bodyplan.core.designsystem.theme.PartChest
import kr.sdbk.bodyplan.core.designsystem.theme.PartLeg
import kr.sdbk.bodyplan.core.designsystem.theme.PartShoulder
import kr.sdbk.bodyplan.core.domain.model.ExerciseTrendMetric

internal val ExerciseTrendMetric.label: String
    get() = when (this) {
        ExerciseTrendMetric.MAX_WEIGHT -> "최고중량"
        ExerciseTrendMetric.TOTAL_VOLUME -> "총볼륨"
        ExerciseTrendMetric.TOTAL_REPS -> "총 횟수"
        ExerciseTrendMetric.TOTAL_MINUTES -> "총 시간"
    }

internal val ExerciseTrendMetric.unit: String
    get() = when (this) {
        ExerciseTrendMetric.MAX_WEIGHT, ExerciseTrendMetric.TOTAL_VOLUME -> "kg"
        ExerciseTrendMetric.TOTAL_REPS -> "회"
        ExerciseTrendMetric.TOTAL_MINUTES -> "분"
    }

/** 지표마다 선 색을 고정한다 — 종목을 바꿔도 같은 지표가 같은 색이어야 눈이 따라간다. */
internal val ExerciseTrendMetric.color: Color
    get() = when (this) {
        ExerciseTrendMetric.MAX_WEIGHT -> PartChest
        ExerciseTrendMetric.TOTAL_VOLUME -> PartLeg
        ExerciseTrendMetric.TOTAL_REPS -> PartShoulder
        ExerciseTrendMetric.TOTAL_MINUTES -> PartCardio
    }

internal fun weekLabel(weekStart: LocalDate): String = WEEK_FORMAT.format(weekStart)

private val WEEK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.M.d")
