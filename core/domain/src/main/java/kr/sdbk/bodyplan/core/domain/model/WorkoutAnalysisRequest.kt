package kr.sdbk.bodyplan.core.domain.model

import java.time.LocalDate

/** 세트 하나를 분석에 넘길 모양으로 편 것. 강도를 값과 축으로 갈라 담는다. */
data class WorkoutAnalysisSet(val repeatCount: Int, val intensityValue: Int, val intensityType: IntensityType)

/** 기록 한 건. 종목 이름은 작성 시점의 스냅샷이라 지금 종목 목록과 다를 수 있다. */
data class WorkoutAnalysisEntry(
    val date: LocalDate,
    val bodyPart: BodyPart,
    val exerciseName: String,
    val sets: List<WorkoutAnalysisSet>,
)

/**
 * 운동 분석 한 번에 필요한 것 전부.
 *
 * 날짜별과 기간이 같은 재료(기록 원문)를 쓰고 [kind]로만 갈린다. 식단처럼 사진이 없어
 * 한 달치를 그대로 보내도 요청이 가볍다.
 *
 * 볼륨과 날 수를 함께 넘기는 것은 AI가 세지 않게 하기 위해서다. 정확한 산술이라 추정할
 * 이유가 없고, 시키면 틀린 숫자가 결과에 남는다.
 */
data class WorkoutAnalysisRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    val kind: AnalysisKind,
    val periodLabel: String,
    val entries: List<WorkoutAnalysisEntry>,
    /** 기간 안에 기록이 있는 날 수. */
    val workoutDayCount: Int,
    /** 기간 안에 기록이 없는 날 수. 오늘 이후의 날은 세지 않는다. */
    val restDayCount: Int,
    /** 무게 종목의 총 볼륨(kg). `무게 × 횟수`의 합이다. */
    val totalWeightVolume: Int,
    /** 각도 종목의 총 횟수. 무게가 없어 볼륨에 섞지 않는다. */
    val totalBodyweightReps: Int,
    /** 유산소의 총 시간(분). 무게도 횟수도 없어 따로 센다. */
    val totalCardioMinutes: Int,
)
