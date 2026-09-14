package kr.sdbk.bodyplan.core.domain.model

/** 한 부위의 무게 볼륨. 무게 종목 기록이 있는 부위만 만들어진다 — 합이 0이어도 기록은 기록이다. */
data class BodyPartVolume(val bodyPart: BodyPart, val weightVolumeKg: Int)
