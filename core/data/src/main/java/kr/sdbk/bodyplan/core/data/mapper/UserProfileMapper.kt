package kr.sdbk.bodyplan.core.data.mapper

import kr.sdbk.bodyplan.core.domain.model.Gender
import kr.sdbk.bodyplan.core.domain.model.Goal
import kr.sdbk.bodyplan.core.domain.model.UserProfile
import kr.sdbk.bodyplan.core.local.entity.UserProfileEntity

private const val GOAL_SEPARATOR = ","

internal fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    ageYears = ageYears,
    heightCm = heightCm,
    weightKg = weightKg,
    // 저장된 이름이 낯설면 지금은 없는 값이므로 버린다.
    gender = gender?.let { name -> Gender.entries.firstOrNull { it.name == name } },
    goals = goals.split(GOAL_SEPARATOR)
        .mapNotNull { name -> Goal.entries.firstOrNull { it.name == name } }
        .toSet(),
    targetWeightKg = targetWeightKg,
    targetNote = targetNote,
)

internal fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    ageYears = ageYears,
    heightCm = heightCm,
    weightKg = weightKg,
    gender = gender?.name,
    goals = goals.joinToString(GOAL_SEPARATOR) { it.name },
    targetWeightKg = targetWeightKg,
    targetNote = targetNote?.trim()?.ifBlank { null },
)
