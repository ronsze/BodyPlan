package kr.sdbk.bodyplan.core.data.mapper

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.local.entity.WeightRecordEntity

internal fun WeightRecordEntity.toDomain(): WeightRecord =
    WeightRecord(
        date = LocalDate.ofEpochDay(dateEpochDay),
        weightKg = weightKg,
    )

internal fun WeightRecord.toEntity(updatedAtMillis: Long): WeightRecordEntity =
    WeightRecordEntity(
        dateEpochDay = date.toEpochDay(),
        weightKg = weightKg,
        updatedAtMillis = updatedAtMillis,
    )
