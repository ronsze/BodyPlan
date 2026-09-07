package kr.sdbk.bodyplan.core.domain.model

/** 세트 하나의 강도. */
sealed interface Intensity {
    /** 저장소에 그대로 들어가는 숫자. 타입과 짝지어야만 의미가 산다. */
    val value: Int

    data class Weight(val kilograms: Int) : Intensity {
        override val value: Int get() = kilograms
    }

    data class Angle(val degrees: Int) : Intensity {
        override val value: Int get() = degrees
    }

    companion object {
        fun of(type: IntensityType, value: Int): Intensity = when (type) {
            IntensityType.WEIGHT -> Weight(value)
            IntensityType.ANGLE -> Angle(value)
        }
    }
}
