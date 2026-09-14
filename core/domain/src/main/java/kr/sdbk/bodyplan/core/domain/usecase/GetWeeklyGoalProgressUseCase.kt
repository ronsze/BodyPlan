package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kr.sdbk.bodyplan.core.domain.model.AnalysisScopeKey
import kr.sdbk.bodyplan.core.domain.model.WeeklyGoalProgress
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/**
 * 이번 주 목표 달성과 연속 달성 주 수를 낸다.
 *
 * 이번 주는 달성했으면 세고 아직이면 깨지 않는다 — 주 초에 "연속이 끊겼다"고 보이면 억울하다.
 * 운동한 날은 기록이 하나라도 있으면 하루다. 세트까지 읽지 않으려고 부위 조회를 쓴다.
 */
class GetWeeklyGoalProgressUseCase
@Inject
constructor(
    private val userProfileRepository: UserProfileRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<WeeklyGoalProgress?> {
        // 구독 중 자정을 넘겨도 구간이 흔들리지 않도록 여기서 한 번만 읽는다.
        val today = LocalDate.now(clock)
        val thisWeekStart = AnalysisScopeKey.weekStart(today)
        val from = thisWeekStart.minusWeeks(STREAK_MAX_WEEKS.toLong())

        return combine(
            userProfileRepository.observeProfile(),
            workoutLogRepository.observeBodyPartsInRange(from, today),
        ) { profile, recorded ->
            val goal = profile.weeklyWorkoutGoal ?: return@combine null
            val daysByWeek = recorded.keys.groupingBy { AnalysisScopeKey.weekStart(it) }.eachCount()
            val doneDays = daysByWeek[thisWeekStart] ?: 0
            WeeklyGoalProgress(
                goalDays = goal,
                doneDays = doneDays,
                streakWeeks = streakOf(daysByWeek, goal, thisWeekStart, doneDays >= goal),
            )
        }
    }

    private fun streakOf(
        daysByWeek: Map<LocalDate, Int>,
        goal: Int,
        thisWeekStart: LocalDate,
        thisWeekAchieved: Boolean,
    ): Int {
        var streak = if (thisWeekAchieved) 1 else 0
        var week = thisWeekStart.minusWeeks(1)
        while (streak < STREAK_MAX_WEEKS && (daysByWeek[week] ?: 0) >= goal) {
            streak++
            week = week.minusWeeks(1)
        }
        return streak
    }
}

/** 이보다 오래된 주는 읽지도 세지도 않는다. 1년이면 화면에 적기에 충분히 긴 연속이다. */
private const val STREAK_MAX_WEEKS = 52
