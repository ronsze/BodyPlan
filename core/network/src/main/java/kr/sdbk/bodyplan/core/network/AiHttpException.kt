package kr.sdbk.bodyplan.core.network

/** 제공자가 2xx가 아닌 응답을 준 경우. [code]로 인증 실패와 그 밖의 실패를 가른다. */
class AiHttpException(val code: Int, val body: String) : Exception("AI 호출 실패: $code")
