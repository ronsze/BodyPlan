package kr.sdbk.bodyplan.core.data.ai

import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisSummaryRequest
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.InbodyAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisEntry
import kr.sdbk.bodyplan.core.domain.model.WorkoutAnalysisRequest

/**
 * AI에게 보내는 지시문.
 *
 * 답을 JSON으로 받는 것은 요약과 묶음을 갈라 저장해야 하기 때문이다.
 * 글로 받아 나누면 구분자를 정하는 규칙이 또 생긴다.
 */
internal object AnalysisPrompt {
    const val SYSTEM = """당신은 한국어로 답하는 운동·식단 코치입니다.
사용자의 신체 정보와 목적을 바탕으로 기록을 분석하고 무엇을 고칠지 알려 줍니다.
반드시 아래 JSON 하나만 출력하고 다른 말은 붙이지 마세요.
{"summary": "한 문단 요약", "sections": [{"title": "묶음 제목", "body": "묶음 내용"}]}
묶음 제목은 요청에서 지정한 것을 그 순서대로 씁니다.
사진으로 추정한 값은 추정임을 밝히고, 신체 정보가 비어 있으면 일반적인 기준으로 답하세요."""

    fun diet(request: DietAnalysisRequest): String = buildString {
        appendLine("기간: ${request.periodLabel}")
        appendLine()
        appendLine(profileBlock(request.profile))
        appendLine()
        appendLine("이 날 식단 기록 ${request.entries.size}건의 사진과 메모입니다.")
        request.entries.forEach { entry ->
            appendLine("- ${entry.memo?.takeIf { it.isNotBlank() } ?: "메모 없음"}")
        }
        appendLine()
        appendLine("아래 네 묶음을 이 제목과 순서로 채우세요.")
        appendLine("- 먹은 음식: 사진과 메모에서 알아낸 음식을 하나씩 적습니다.")
        appendLine("- 칼로리: 음식별 추정 칼로리와 하루 합계를 적고, 신체 정보와 목적에 맞는 목표와 견줍니다.")
        appendLine("- 영양 성분: 탄수화물·단백질·지방의 추정량과 균형을 적습니다.")
        appendLine("- 개선 방향: 목표에 견주어 무엇을 바꿀지 적습니다.")
    }

    fun workout(request: WorkoutAnalysisRequest): String = buildString {
        appendLine("기간: ${request.periodLabel}")
        appendLine()
        appendLine(profileBlock(request.profile))
        appendLine()
        appendLine("아래 다섯 숫자는 앱이 센 정확한 값입니다. 다시 계산하지 말고 그대로 쓰세요.")
        appendLine("- 운동한 날: ${request.workoutDayCount}일")
        appendLine("- 쉰 날: ${request.restDayCount}일")
        appendLine("- 무게 종목 총 볼륨: ${request.totalWeightVolume}kg")
        appendLine("- 각도 종목 총 횟수: ${request.totalBodyweightReps}회")
        appendLine("- 유산소 총 시간: ${request.totalCardioMinutes}분")
        appendLine()
        appendLine("기록 ${request.entries.size}건입니다.")
        request.entries.forEach { entry -> appendLine("- ${entry.line}") }
        appendLine()
        appendLine("아래 네 묶음을 이 제목과 순서로 채우세요.")
        appendLine("- 수행한 운동: 부위별로 어떤 종목을 몇 세트 했는지 적습니다.")
        appendLine(
            "- 볼륨: 합계는 위 값을 그대로 쓰고, 어느 종목에 몰렸는지와 목적에 맞는지를 적습니다. " +
                "유산소는 볼륨이 아니라 시간으로 적습니다.",
        )
        appendLine(
            "- 부위 균형: 이 날 자극한 부위와 빠진 부위를 적습니다. " +
                "유산소는 부위가 아니므로 빠진 부위로 세지 말고 따로 언급합니다.",
        )
        appendLine("- 개선 방향: 목표에 견주어 무엇을 바꿀지 적습니다.")
    }

    /**
     * 아래 계층의 분석을 모아 위 계층을 만드는 지시문.
     *
     * 끼니→하루→주→월이 같은 얼개라 하나로 쓰고 묶음 제목만 종류로 고른다.
     */
    fun summary(request: AnalysisSummaryRequest): String = buildString {
        appendLine("기간: ${request.periodLabel}")
        appendLine()
        appendLine(profileBlock(request.profile))
        appendLine()
        appendLine("아래는 이 기간에 이미 분석해 둔 ${request.children.size}건입니다.")
        appendLine("사진이나 기록 원문은 첨부되지 않습니다. 이 내용만으로 종합하세요.")
        request.children.forEach { child ->
            appendLine()
            appendLine("[${child.label}]")
            appendLine(child.content.summary)
            child.content.sections.forEach { section ->
                appendLine("- ${section.title}: ${section.body}")
            }
        }
        appendLine()
        appendLine("아래 네 묶음을 이 제목과 순서로 채우세요.")
        summarySections(request.kind).forEach { appendLine("- $it") }
    }

    private fun summarySections(kind: AnalysisKind): List<String> = when (kind) {
        AnalysisKind.DIET_WEEKLY, AnalysisKind.DIET_MONTHLY -> listOf(
            "기간 흐름: 이 기간의 식사 습관이 어떻게 흘렀는지 적습니다.",
            "칼로리 흐름: 하루 평균 추정 칼로리와 오르내림을 목표와 견줍니다.",
            "영양 균형: 기간 전체의 탄수화물·단백질·지방 균형을 적습니다.",
            "개선 방향: 다음 기간에 무엇을 바꿀지 적습니다.",
        )

        AnalysisKind.WORKOUT_WEEKLY, AnalysisKind.WORKOUT_MONTHLY -> listOf(
            "기간 흐름: 운동한 날과 쉰 날이 어떻게 갈렸는지, 빈도가 적절한지 적습니다.",
            "볼륨 흐름: 기간의 볼륨 오르내림을 목적에 견줍니다. 유산소는 시간으로 적습니다.",
            "부위 균형: 기간 전체에서 부위별 분할이 치우치지 않았는지 적습니다. 유산소는 비중만 봅니다.",
            "개선 방향: 다음 기간에 무엇을 바꿀지 적습니다.",
        )

        AnalysisKind.DIET_DAILY, AnalysisKind.WORKOUT_DAILY, AnalysisKind.INBODY ->
            error("종합하는 종류가 아닙니다: $kind")
    }

    fun inbody(request: InbodyAnalysisRequest): String = buildString {
        appendLine(profileBlock(request.profile))
        appendLine()
        appendLine("첨부한 인바디 측정 결과지 사진을 읽고 아래 네 묶음을 이 제목과 순서로 채우세요.")
        appendLine("- 측정값: 사진에서 읽어 낸 항목과 값을 그대로 옮깁니다. 읽지 못한 항목은 그렇다고 적습니다.")
        appendLine("- 체성분 평가: 골격근량·체지방률을 중심으로 지금 상태를 신체 정보와 목적에 견줍니다.")
        appendLine("- 식단 개선: 무엇을 어떻게 먹을지 적습니다.")
        appendLine("- 운동 개선: 어떤 운동을 어떤 강도로 할지 적습니다.")
        appendLine()
        appendLine("그리고 JSON에 measurement 칸을 함께 넣으세요. 읽어 내지 못한 항목은 null로 둡니다.")
        appendLine(MEASUREMENT_EXAMPLE)
        appendLine("단위는 kg과 cm이고 숫자만 넣습니다. 결과지에 없는 항목은 지어내지 말고 null로 둡니다.")
        appendLine()
        appendLine("사진이 인바디 결과지가 아니면 그 사실만 요약에 적고 묶음과 measurement를 비우세요.")
    }

    private fun profileBlock(profile: UserProfile): String {
        if (profile.isEmpty) return "신체 정보: 입력되지 않음"
        return buildString {
            appendLine("신체 정보")
            profile.gender?.let { appendLine("- 성별: ${it.text}") }
            profile.ageYears?.let { appendLine("- 나이: ${it}세") }
            profile.heightCm?.let { appendLine("- 키: ${it}cm") }
            profile.weightKg?.let { appendLine("- 몸무게: ${it}kg") }
            if (profile.goals.isNotEmpty()) {
                appendLine("- 목적: ${profile.goals.joinToString(", ") { it.text }}")
            }
            profile.targetWeightKg?.let { appendLine("- 목표 체중: ${it}kg") }
            profile.targetNote?.takeIf { it.isNotBlank() }?.let { appendLine("- 그 밖의 목표: $it") }
        }.trimEnd()
    }
}

private val Gender.text: String
    get() = when (this) {
        Gender.MALE -> "남성"
        Gender.FEMALE -> "여성"
    }

private val Goal.text: String
    get() = when (this) {
        Goal.DIET -> "다이어트"
        Goal.MUSCLE_GAIN -> "근성장"
        Goal.TARGET_WEIGHT -> "목표 체중 달성"
        Goal.TARGET_STRENGTH -> "목표 근력 달성"
    }

/** `2026-09-08 가슴 · 벤치프레스: 60kg×10회, 60kg×8회` 꼴로 편다. */
private val WorkoutAnalysisEntry.line: String
    get() {
        val sets = sets.joinToString(", ") { set ->
            when (set.intensityType) {
                IntensityType.WEIGHT -> "${set.intensityValue}kg×${set.repeatCount}회"

                IntensityType.ANGLE -> "각도 ${set.intensityValue}도×${set.repeatCount}회"

                // 시간으로 재는 종목은 한 세트가 한 회차라 횟수를 적지 않는다.
                IntensityType.DURATION -> "${set.intensityValue}분"
            }
        }
        return "$date ${bodyPart.text} · $exerciseName: $sets"
    }

private val BodyPart.text: String
    get() = when (this) {
        BodyPart.CHEST -> "가슴"
        BodyPart.BACK -> "등"
        BodyPart.SHOULDER -> "어깨"
        BodyPart.LEG -> "하체"
        BodyPart.BICEPS -> "이두"
        BodyPart.TRICEPS -> "삼두"
        BodyPart.CARDIO -> "유산소"
    }

/** 지시문에 그대로 싣는 예시. 따옴표가 많아 문자열 안에 두면 읽기 어렵다. */
private val MEASUREMENT_EXAMPLE = """{"measurement": {"weightKg": 72.4, "skeletalMuscleKg": 33.1, """ +
    """"bodyFatKg": 15.4, "heightCm": 175}}"""
