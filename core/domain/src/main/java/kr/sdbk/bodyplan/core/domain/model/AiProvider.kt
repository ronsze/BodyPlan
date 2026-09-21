package kr.sdbk.bodyplan.core.domain.model

/** 분석에 쓸 AI 제공자. 한 번에 하나만 쓴다. */
enum class AiProvider {
    CLAUDE,
    GPT,
    GEMINI,

    /** 기기 안의 모델(Gemini Nano). 키가 없고, 지원 기기에서만 고를 수 있다. */
    ON_DEVICE,
}

/** 키를 넣어야 하는 제공자인지. 온디바이스는 키 대신 모델 다운로드로 연결된다. */
val AiProvider.requiresToken: Boolean
    get() = this != AiProvider.ON_DEVICE
