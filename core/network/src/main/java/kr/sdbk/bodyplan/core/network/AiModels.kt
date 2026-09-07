package kr.sdbk.bodyplan.core.network

/**
 * 제공자별로 부를 모델 이름.
 *
 * 제공자가 모델을 갈아치우면 여기만 고친다.
 * 클로드 외의 둘은 이 프로젝트가 확인한 값이 아니라 흔히 쓰이는 이름을 적은 것이다.
 */
object AiModels {
    const val CLAUDE = "claude-sonnet-5"
    const val GPT = "gpt-4o"
    const val GEMINI = "gemini-2.0-flash"
}
