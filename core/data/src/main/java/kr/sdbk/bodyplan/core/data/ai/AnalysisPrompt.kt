package kr.sdbk.bodyplan.core.data.ai

import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.DietAnalysisRequest
import kr.sdbk.bodyplan.core.domain.model.DietSummaryRequest
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
        appendLine("식단 기록 ${request.entries.size}건입니다. 사진을 함께 봅니다.")
        request.entries.forEach { entry ->
            appendLine("- ${entry.date}: ${entry.memo?.takeIf { it.isNotBlank() } ?: "메모 없음"}")
        }
        appendLine()
        appendLine("아래 네 묶음을 이 제목과 순서로 채우세요.")
        appendLine("- 먹은 음식: 사진과 메모에서 알아낸 음식을 하나씩 적습니다.")
        appendLine("- 칼로리: 음식별 추정 칼로리와 합계를 적고, 신체 정보와 목적에 맞는 목표 칼로리와 견줍니다.")
        appendLine("- 영양 성분: 탄수화물·단백질·지방의 추정량과 균형을 적습니다.")
        appendLine("- 개선 방향: 목표에 견주어 무엇을 바꿀지 적습니다.")
    }

    fun dietSummary(request: DietSummaryRequest): String = buildString {
        appendLine("기간: ${request.periodLabel}")
        appendLine()
        appendLine(profileBlock(request.profile))
        appendLine()
        appendLine("아래는 이 기간에 날짜별로 이미 분석한 내용 ${request.dailyResults.size}일치입니다.")
        appendLine("사진은 첨부되지 않습니다. 이 내용만으로 종합하세요.")
        request.dailyResults.forEach { daily ->
            appendLine()
            appendLine("[${daily.date}]")
            appendLine(daily.content.summary)
            daily.content.sections.forEach { section ->
                appendLine("- ${section.title}: ${section.body}")
            }
        }
        appendLine()
        appendLine("아래 네 묶음을 이 제목과 순서로 채우세요.")
        appendLine("- 기간 흐름: 이 기간의 식사 습관이 어떻게 흘렀는지 적습니다.")
        appendLine("- 칼로리 흐름: 하루 평균 추정 칼로리와 오르내림을 목표 칼로리와 견줍니다.")
        appendLine("- 영양 균형: 기간 전체의 탄수화물·단백질·지방 균형을 적습니다.")
        appendLine("- 개선 방향: 다음 기간에 무엇을 바꿀지 적습니다.")
    }

    fun workout(request: WorkoutAnalysisRequest): String = buildString {
        appendLine("기간: ${request.periodLabel}")
        appendLine()
        appendLine(profileBlock(request.profile))
        appendLine()
        appendLine("운동한 날 ${request.workoutDayCount}일, 쉰 날 ${request.restDayCount}일입니다.")
        appendLine(
            "무게 종목 총 볼륨 ${request.totalWeightVolume}kg, " +
                "맨몸 종목 총 ${request.totalBodyweightReps}회입니다.",
        )
        appendLine("아래 숫자는 앱이 센 정확한 값이니 다시 계산하지 말고 그대로 쓰세요.")
        appendLine()
        appendLine("기록 ${request.entries.size}건입니다.")
        request.entries.forEach { entry -> appendLine("- ${entry.line}") }
        appendLine()
        appendLine("아래 네 묶음을 이 제목과 순서로 채우세요.")
        if (request.kind == AnalysisKind.WORKOUT_DAILY) {
            appendLine("- 수행한 운동: 부위별로 어떤 종목을 몇 세트 했는지 적습니다.")
            appendLine("- 볼륨: 합계는 위 값을 그대로 쓰고, 어느 종목에 몰렸는지와 목적에 맞는지를 적습니다.")
            appendLine("- 부위 균형: 이 날 자극한 부위와 빠진 부위를 적습니다.")
            appendLine("- 개선 방향: 목표에 견주어 무엇을 바꿀지 적습니다.")
        } else {
            appendLine("- 기간 흐름: 운동한 날과 쉰 날이 어떻게 갈렸는지, 빈도가 적절한지 적습니다.")
            appendLine("- 볼륨 흐름: 총 볼륨은 위 값을 그대로 쓰고, 날짜별 오르내림을 목적에 견줍니다.")
            appendLine("- 부위 균형: 기간 전체에서 부위별 분할이 치우치지 않았는지 적습니다.")
            appendLine("- 개선 방향: 다음 기간에 무엇을 바꿀지 적습니다.")
        }
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
        appendLine("사진이 인바디 결과지가 아니면 그 사실만 요약에 적고 묶음은 비우세요.")
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
    }
