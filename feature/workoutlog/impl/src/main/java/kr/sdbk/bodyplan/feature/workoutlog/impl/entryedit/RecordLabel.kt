package kr.sdbk.bodyplan.feature.workoutlog.impl.entryedit

import kr.sdbk.bodyplan.core.domain.model.IntensityType

/** PR 알림에 쓰는 축 이름. 각도 종목은 각도가 아니라 횟수가 기록이라 "최고 횟수"다. */
internal val IntensityType.recordLabel: String
    get() = when (this) {
        IntensityType.WEIGHT -> "최고 무게"
        IntensityType.ANGLE -> "최고 횟수"
        IntensityType.DURATION -> "최장 시간"
    }
