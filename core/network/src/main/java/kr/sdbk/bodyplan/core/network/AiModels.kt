package kr.sdbk.bodyplan.core.network

/**
 * 제공자별로 부를 모델 이름.
 *
 * 제공자가 모델을 갈아치우면 여기만 고친다.
 * 클로드와 GPT의 이름은 아직 실제 호출로 확인되지 않았다.
 */
object AiModels {
    const val CLAUDE = "claude-sonnet-5"
    const val GPT = "gpt-4o"

    /** 2.0-flash가 내려가면서 제미나이가 응답으로 알려 준 이름이다. 주소가 `models/`를 붙이므로 여기엔 이름만 둔다. */
    const val GEMINI = "gemini-3.6-flash"

    /** 답 한 번의 최대 길이. 요약과 개선 방향이 잘리지 않을 만큼만 준다. */
    const val MAX_OUTPUT_TOKENS = 2048
}
