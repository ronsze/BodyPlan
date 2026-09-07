package kr.sdbk.bodyplan.core.domain.model

/** 인증 정보가 제공자에게 거절된 경우. 키를 다시 받아야 한다. */
class AiUnauthorizedException : Exception("인증 정보가 올바르지 않습니다")

/** 인증 말고 다른 이유로 호출이 끝나지 못한 경우. 네트워크 단절과 서버 오류를 함께 담는다. */
class AiRequestFailedException(cause: Throwable?) : Exception("AI를 부르지 못했습니다", cause)
