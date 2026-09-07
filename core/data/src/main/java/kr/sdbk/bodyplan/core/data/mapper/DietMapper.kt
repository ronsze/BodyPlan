package kr.sdbk.bodyplan.core.data.mapper

import java.time.LocalDate
import kr.sdbk.bodyplan.core.data.image.LocalImageStore
import kr.sdbk.bodyplan.core.domain.model.DietEntry
import kr.sdbk.bodyplan.core.local.entity.DateImage
import kr.sdbk.bodyplan.core.local.entity.DietEntryEntity

internal fun DietEntryEntity.toDomain(imageStore: LocalImageStore): DietEntry = DietEntry(
    id = id,
    imagePath = imageStore.pathOf(imageFileName),
    memo = memo,
)

internal fun List<DateImage>.toImagePathsByDate(imageStore: LocalImageStore): Map<LocalDate, String> =
    associate { row ->
        LocalDate.ofEpochDay(row.dateEpochDay) to imageStore.pathOf(row.imageFileName)
    }
