package kr.sdbk.bodyplan.core.domain.model

/** 분석에 쓸 AI 제공자. 한 번에 하나만 쓴다. */
enum class AiProvider {
    CLAUDE,
    GPT,
    GEMINI,
}
