package kr.sdbk.bodyplan.core.data.mapper

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.local.entity.InbodyRecordEntity

/** 사진 파일 이름은 보관소가 풀어 준 경로로 바꾼다. 도메인은 파일 이름을 모른다. */
internal fun InbodyRecordEntity.toDomain(pathOf: (String) -> String): InbodyRecord = InbodyRecord(
    id = id,
    imagePath = pathOf(imageFileName),
    measuredOn = LocalDate.ofEpochDay(measuredOnEpochDay),
    createdAtMillis = createdAtMillis,
    failureReason = failureReason,
)

internal fun newInbodyRecordEntity(
    imageFileName: String,
    measuredOn: LocalDate,
    createdAtMillis: Long,
): InbodyRecordEntity = InbodyRecordEntity(
    imageFileName = imageFileName,
    measuredOnEpochDay = measuredOn.toEpochDay(),
    createdAtMillis = createdAtMillis,
)
