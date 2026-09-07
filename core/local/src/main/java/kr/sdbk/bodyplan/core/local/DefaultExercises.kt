package kr.sdbk.bodyplan.core.local

import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity

/** 첫 실행 때 심는 기본 종목. 사용자가 이후 더하거나 고칠 수 있다. */
internal object DefaultExercises {
    val all: List<ExerciseEntity> =
        chest() + back() + shoulder() + leg() + biceps() + triceps()

    private fun chest() = listOf(
        weight(BodyPart.CHEST, "인클라인 벤치프레스 머신"),
        weight(BodyPart.CHEST, "플랫 벤치프레스 머신"),
        weight(BodyPart.CHEST, "팩덱 플라이 머신"),
        weight(BodyPart.CHEST, "체스트프레스 머신"),
        angle(BodyPart.CHEST, "푸쉬업"),
    )

    private fun back() = listOf(
        weight(BodyPart.BACK, "랫풀다운"),
        weight(BodyPart.BACK, "티바로우 하부"),
        weight(BodyPart.BACK, "티바로우 상부"),
        weight(BodyPart.BACK, "케이블로우"),
        weight(BodyPart.BACK, "바벨로우"),
        weight(BodyPart.BACK, "원암로우 머신"),
    )

    private fun shoulder() = listOf(
        weight(BodyPart.SHOULDER, "사이드 레터럴 레이즈 머신"),
        weight(BodyPart.SHOULDER, "리버스 펙덱 플라이"),
        weight(BodyPart.SHOULDER, "숄더프레스 머신"),
        weight(BodyPart.SHOULDER, "OHP"),
    )

    private fun leg() = listOf(
        weight(BodyPart.LEG, "레그 익스텐션"),
        weight(BodyPart.LEG, "레그컬"),
        weight(BodyPart.LEG, "레그프레스"),
        weight(BodyPart.LEG, "스쿼트"),
    )

    private fun biceps() = listOf(
        weight(BodyPart.BICEPS, "해머컬"),
        weight(BodyPart.BICEPS, "덤벨컬"),
        weight(BodyPart.BICEPS, "피처컬 머신"),
    )

    private fun triceps() = listOf(
        weight(BodyPart.TRICEPS, "케이블 푸쉬다운"),
        weight(BodyPart.TRICEPS, "라잉 트라이셉스 익스텐션"),
        weight(BodyPart.TRICEPS, "케이블 오버헤드 익스텐션"),
    )

    private fun weight(bodyPart: BodyPart, name: String) = ExerciseEntity(
        bodyPart = bodyPart.name,
        name = name,
        intensityType = IntensityType.WEIGHT.name,
    )

    private fun angle(bodyPart: BodyPart, name: String) = ExerciseEntity(
        bodyPart = bodyPart.name,
        name = name,
        intensityType = IntensityType.ANGLE.name,
    )
}
