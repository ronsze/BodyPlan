package kr.sdbk.bodyplan.core.local.migration

import java.time.Instant
import java.time.ZoneId
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity
import kr.sdbk.bodyplan.core.local.entity.InbodyRecordEntity
import kr.sdbk.bodyplan.core.local.snapshot.BodyPlanSnapshot

/**
 * 기록 구조가 생기기 전의 인바디 분석 결과를 기록으로 옮긴다.
 *
 * DB 이전과 옛 백업 복원이 같은 규칙을 쓰도록 한 곳에 둔다. 규칙이 둘이면 같은 이력이
 * 이전 경로에 따라 다른 측정일을 갖게 된다.
 */
internal object LegacyInbodyConverter {
    /** [records]는 새로 만든 기록, [rewritten]은 열쇠와 사진 파일 이름이 바뀐 결과(바뀐 것만)다. */
    data class Converted(val records: List<InbodyRecordEntity>, val rewritten: List<AnalysisResultEntity>)

    fun convert(results: List<AnalysisResultEntity>, zoneId: ZoneId): Converted {
        // 사진이 없는 결과는 기록으로 만들 수 없다. 기록은 사진 한 장이 있어야 성립한다.
        val targets = results
            .filter { it.kind == INBODY_KIND && it.scopeKey.isEmpty() && it.imageFileName != null }
            .sortedWith(compareBy({ it.createdAtMillis }, { it.id }))

        val records = targets.mapIndexed { index, result ->
            InbodyRecordEntity(
                id = index + 1L,
                imageFileName = checkNotNull(result.imageFileName),
                measuredOnEpochDay = Instant.ofEpochMilli(
                    result.createdAtMillis,
                ).atZone(zoneId).toLocalDate().toEpochDay(),
                createdAtMillis = result.createdAtMillis,
            )
        }
        val rewritten = targets.mapIndexed { index, result ->
            result.copy(scopeKey = (index + 1L).toString(), imageFileName = null)
        }
        return Converted(records, rewritten)
    }

    /** 14 이후 스냅샷은 이미 기록을 담고 있어 그대로 둔다. */
    fun convert(snapshot: BodyPlanSnapshot, zoneId: ZoneId): BodyPlanSnapshot {
        if (snapshot.dbVersion >= RECORD_DB_VERSION) return snapshot

        val converted = convert(snapshot.analysisResults, zoneId)
        val rewrittenById = converted.rewritten.associateBy { it.id }
        return snapshot.copy(
            inbodyRecords = converted.records,
            analysisResults = snapshot.analysisResults.map { rewrittenById[it.id] ?: it },
        )
    }
}

private const val INBODY_KIND = "INBODY"

/** 기록 테이블이 생긴 DB 버전. */
private const val RECORD_DB_VERSION = 14
