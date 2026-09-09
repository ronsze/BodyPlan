package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.domain.model.ProgressHeadline
import kr.sdbk.bodyplan.core.domain.model.ProgressMetric
import kr.sdbk.bodyplan.core.domain.model.ProgressMetricKey
import kr.sdbk.bodyplan.core.domain.model.ProgressSummary
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.model.WeightTrend
import kr.sdbk.bodyplan.core.domain.model.WorkoutEntry
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.domain.repository.UserProfileRepository
import kr.sdbk.bodyplan.core.domain.repository.WeightLogRepository
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/**
 * 지금 나아지고 있는지를 지표로 낸다.
 *
 * 좋고 나쁨의 기준은 축마다 다르다. 골격근·체지방·운동량은 목표와 무관하게 방향이 같지만,
 * 체중만은 감량인지 증량인지에 따라 반대가 되므로 [UserProfile.goals]를 본다. 목표가 없으면
 * 체중은 값만 내고 판정하지 않는다 — 고정 기준으로는 어느 쪽이 좋은지 정할 근거가 없다.
 */
class GetProgressSummaryUseCase
@Inject
constructor(
    private val weightLogRepository: WeightLogRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val userProfileRepository: UserProfileRepository,
    private val analysisResultRepository: AnalysisResultRepository,
    private val getWeightTrend: GetWeightTrendUseCase,
    private val summarizeWorkoutVolume: SummarizeWorkoutVolumeUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<ProgressSummary> {
        // 구독 중 자정을 넘겨도 구간이 흔들리지 않도록 여기서 한 번만 읽고, 모든 판정이 이 값을 쓴다.
        val today = LocalDate.now(clock)
        val previousFrom = today.minusDays(COMPARE_DAYS * 2 - 1)
        val recentFrom = today.minusDays(COMPARE_DAYS - 1)

        return combine(
            weightLogRepository.observeRecordsInRange(previousFrom, today),
            workoutLogRepository.observeEntriesInRange(previousFrom, today),
            userProfileRepository.observeProfile(),
            analysisResultRepository.observeHistory(AnalysisKind.INBODY),
        ) { records, entriesByDate, profile, inbodyHistory ->
            val recent = listOf(
                weightMetric(records, today, profile),
                volumeMetric(entriesByDate, recentFrom, previousFrom, today),
                daysMetric(entriesByDate, recentFrom, previousFrom, today),
            )
            val bodyComposition = bodyCompositionMetrics(inbodyHistory)
            ProgressSummary(
                headline = headlineOf(recent + bodyComposition),
                recentMetrics = recent,
                bodyCompositionMetrics = bodyComposition,
            )
        }
    }

    /**
     * 최근 7일 평균에서 그 앞 7일 평균을 뺀 값. 셈은 [GetWeightTrendUseCase]가 이미 하므로 다시 쓰지 않는다.
     *
     * 넘기는 기록이 14일치라 월간 값은 나오지 않는다. 여기서는 주간 값만 쓴다.
     */
    private fun weightMetric(records: List<WeightRecord>, today: LocalDate, profile: UserProfile): ProgressMetric {
        val trend = getWeightTrend(records, today)
        return ProgressMetric(
            key = ProgressMetricKey.WEIGHT,
            changeValue = trend.weeklyAverageChangeKg,
            direction = weightDirection(trend, profile),
        )
    }

    private fun weightDirection(trend: WeightTrend, profile: UserProfile): ProgressDirection {
        val change = trend.weeklyAverageChangeKg ?: return ProgressDirection.UNKNOWN
        if (abs(change) < WEIGHT_THRESHOLD_KG) return ProgressDirection.STEADY

        val targetWeight = profile.targetWeightKg?.toDouble()
        val recentAverage = trend.recentWeeklyAverageKg
        val previousAverage = trend.previousWeeklyAverageKg
        return when {
            // 목표 체중이 있으면 늘었는지 줄었는지가 아니라 목표에 가까워졌는지를 본다.
            Goal.TARGET_WEIGHT in profile.goals &&
                targetWeight != null &&
                recentAverage != null &&
                previousAverage != null ->
                improvingIf(abs(recentAverage - targetWeight) < abs(previousAverage - targetWeight))

            Goal.DIET in profile.goals -> improvingIf(change < 0)

            Goal.MUSCLE_GAIN in profile.goals -> improvingIf(change > 0)

            // 목표가 없으면 어느 쪽이 좋은지 정할 근거가 없다. 값만 보이고 판정하지 않는다.
            else -> ProgressDirection.STEADY
        }
    }

    private fun volumeMetric(
        entriesByDate: Map<LocalDate, List<WorkoutEntry>>,
        recentFrom: LocalDate,
        previousFrom: LocalDate,
        today: LocalDate,
    ): ProgressMetric {
        val recentEntries = entriesByDate.between(recentFrom, today)
        val previousEntries = entriesByDate.between(previousFrom, recentFrom.minusDays(1))
        val recent = summarizeWorkoutVolume(recentEntries).weightVolumeKg
        val previous = summarizeWorkoutVolume(previousEntries).weightVolumeKg
        // 볼륨 합이 아니라 기록의 유무로 가른다 — 맨몸으로 남긴 0kg 기록도 기록이다.
        val hasAny = recentEntries.hasWeightEntry() || previousEntries.hasWeightEntry()
        return metric(ProgressMetricKey.WORKOUT_VOLUME, if (hasAny) (recent - previous).toDouble() else null)
    }

    private fun daysMetric(
        entriesByDate: Map<LocalDate, List<WorkoutEntry>>,
        recentFrom: LocalDate,
        previousFrom: LocalDate,
        today: LocalDate,
    ): ProgressMetric {
        val recent = summarizeWorkoutVolume(entriesByDate.between(recentFrom, today)).workoutDays
        val previous = summarizeWorkoutVolume(
            entriesByDate.between(previousFrom, recentFrom.minusDays(1)),
        ).workoutDays
        val hasAny = recent > 0 || previous > 0
        return metric(ProgressMetricKey.WORKOUT_DAYS, if (hasAny) (recent - previous).toDouble() else null)
    }

    /**
     * 인바디 최근 두 건의 차이. 기간으로 묶지 않는 것은 인바디를 매일 찍지 않기 때문이다.
     *
     * 결과지가 항목을 늘 싣지는 않아 값이 있는 것만 골라 센다 — 축마다 견줄 두 건이 다를 수 있다.
     */
    private fun bodyCompositionMetrics(history: List<AnalysisResult>): List<ProgressMetric> {
        val measurements = history.mapNotNull { it.measurement }
        return listOf(
            metric(ProgressMetricKey.SKELETAL_MUSCLE, latestChange(measurements.mapNotNull { it.skeletalMuscleKg })),
            metric(ProgressMetricKey.BODY_FAT, latestChange(measurements.mapNotNull { it.bodyFatKg })),
        )
    }

    /** [values]는 최신이 앞이다([AnalysisResultRepository.observeHistory]가 그렇게 낸다). */
    private fun latestChange(values: List<Double>): Double? = if (values.size < 2) null else values[0] - values[1]

    private fun metric(key: ProgressMetricKey, change: Double?): ProgressMetric = ProgressMetric(
        key = key,
        changeValue = change,
        direction = when {
            change == null -> ProgressDirection.UNKNOWN
            abs(change) < thresholdOf(key) -> ProgressDirection.STEADY
            key == ProgressMetricKey.BODY_FAT -> improvingIf(change < 0)
            else -> improvingIf(change > 0)
        },
    )

    private fun thresholdOf(key: ProgressMetricKey): Double = when (key) {
        ProgressMetricKey.WORKOUT_VOLUME, ProgressMetricKey.WORKOUT_DAYS -> COUNT_THRESHOLD
        else -> WEIGHT_THRESHOLD_KG
    }

    private fun headlineOf(metrics: List<ProgressMetric>): ProgressHeadline {
        val improving = metrics.count { it.direction == ProgressDirection.IMPROVING }
        val worsening = metrics.count { it.direction == ProgressDirection.WORSENING }
        return when {
            metrics.all { it.direction == ProgressDirection.UNKNOWN } -> ProgressHeadline.NOT_ENOUGH_DATA
            improving > worsening -> ProgressHeadline.IMPROVING
            worsening > improving -> ProgressHeadline.WORSENING
            else -> ProgressHeadline.STEADY
        }
    }

    private fun improvingIf(condition: Boolean): ProgressDirection =
        if (condition) ProgressDirection.IMPROVING else ProgressDirection.WORSENING

    private fun Map<LocalDate, List<WorkoutEntry>>.hasWeightEntry(): Boolean =
        values.any { entries -> entries.any { it.intensityType == IntensityType.WEIGHT } }

    private fun Map<LocalDate, List<WorkoutEntry>>.between(
        from: LocalDate,
        to: LocalDate,
    ): Map<LocalDate, List<WorkoutEntry>> = filterKeys { !it.isBefore(from) && !it.isAfter(to) }
}

/** 견주는 구간의 길이. 최근 이만큼과 그 앞 같은 길이를 견준다. */
private const val COMPARE_DAYS = 7L

/** 저울이 0.1kg 단위로 찍으므로 그보다 작은 차이는 변화로 보지 않는다. */
private const val WEIGHT_THRESHOLD_KG = 0.1

/** 볼륨·일수는 정수라 1보다 작은 차이가 없다. */
private const val COUNT_THRESHOLD = 1.0
