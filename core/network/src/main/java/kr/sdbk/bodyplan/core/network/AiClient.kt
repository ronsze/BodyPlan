package kr.sdbk.bodyplan.core.network

/**
 * AI 제공자 하나를 부르는 창구. 제공자마다 주소와 인증 방식, 요청 모양이 달라 구현이 따로다.
 *
 * 이 모듈은 어느 제공자를 쓸지 고르지 않는다. 고르는 일은 `core:data`가 한다.
 */
interface AiClient {
    /** 인증 정보가 실제로 쓸 수 있는지 확인한다. 실패하면 던진다. */
    suspend fun verify(token: String)
}
