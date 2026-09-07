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

    /**
     * 답 한 번의 최대 길이.
     *
     * 한국어는 글자당 토큰이 많이 들어, 묶음 넷을 항목까지 적으면 2048로는 답이 중간에 끊긴다.
     * 끊긴 JSON은 읽히지 않아 받은 글이 통째로 요약에 들어가고, 화면에서는 글이 잘린 것처럼 보인다.
     */
    const val MAX_OUTPUT_TOKENS = 8192
}
