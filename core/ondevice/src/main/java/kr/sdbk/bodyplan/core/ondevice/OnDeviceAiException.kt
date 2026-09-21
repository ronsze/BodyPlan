package kr.sdbk.bodyplan.core.ondevice

import com.google.mlkit.genai.common.GenAiException

/**
 * 온디바이스 모델이 요청을 끝내지 못한 경우. [code]는 ML Kit의 `GenAiException.ErrorCode`다.
 *
 * [reason]은 화면에 그대로 보이는 한국어 문구다. 코드마다 사용자가 할 수 있는 일이 달라
 * (기다리기, 저장 공간 비우기, 앱을 앞에 두기) 여기서 갈라 둔다.
 */
class OnDeviceAiException(val code: Int, cause: Throwable? = null) : Exception(reasonOf(code), cause) {
    val reason: String get() = message.orEmpty()
}

internal fun GenAiException.toOnDeviceFailure(): OnDeviceAiException = OnDeviceAiException(errorCode, this)

private fun reasonOf(code: Int): String = when (code) {
    GenAiException.ErrorCode.NOT_AVAILABLE -> "온디바이스 모델이 준비되지 않았어요"
    GenAiException.ErrorCode.BUSY -> "온디바이스 모델이 다른 작업 중이에요"
    GenAiException.ErrorCode.REQUEST_TOO_LARGE -> "기록이 길어 온디바이스 모델 한도를 넘었어요"
    GenAiException.ErrorCode.NOT_SUPPORTED -> "이 기기의 온디바이스 모델이 지원하지 않는 요청이에요"
    GenAiException.ErrorCode.BACKGROUND_USE_BLOCKED -> "온디바이스 분석은 앱이 화면에 있을 때만 돼요"
    GenAiException.ErrorCode.NOT_ENOUGH_DISK_SPACE -> "저장 공간이 부족해요"
    GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE -> "시스템 업데이트가 필요해요"
    GenAiException.ErrorCode.INVALID_INPUT_IMAGE -> "온디바이스 모델이 읽을 수 없는 사진이에요"
    else -> "온디바이스 오류 $code"
}
