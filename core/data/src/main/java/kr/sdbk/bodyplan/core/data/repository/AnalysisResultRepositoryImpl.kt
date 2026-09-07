package kr.sdbk.bodyplan.core.data.repository

import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.data.image.InbodyImages
import kr.sdbk.bodyplan.core.data.image.LocalImageStore
import kr.sdbk.bodyplan.core.data.mapper.encodeSections
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.domain.model.AnalysisContent
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.AnalysisResult
import kr.sdbk.bodyplan.core.domain.model.InbodyMeasurement
import kr.sdbk.bodyplan.core.domain.repository.AnalysisResultRepository
import kr.sdbk.bodyplan.core.local.dao.AnalysisResultDao
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity

internal class AnalysisResultRepositoryImpl
@Inject
constructor(
    private val analysisResultDao: AnalysisResultDao,
    @InbodyImages private val inbodyImageStore: LocalImageStore,
    private val json: Json,
    private val clock: Clock,
) : AnalysisResultRepository {
    override fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?> =
        analysisResultDao.observeLatest(kind.name, scopeKey).map { it?.toDomain(json, ::pathOf) }

    override fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>> =
        analysisResultDao.observeByKind(kind.name).map { rows -> rows.mapNotNull { it.toDomain(json, ::pathOf) } }

    // 쿼리가 최신순이므로 열쇠마다 처음 만난 행이 마지막 결과다.
    override suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult> {
        if (scopeKeys.isEmpty()) return emptyMap()
        return analysisResultDao.getByScopeKeys(kind.name, scopeKeys)
            .mapNotNull { it.toDomain(json, ::pathOf) }
            .groupBy { it.scopeKey }
            .mapValues { (_, results) -> results.first() }
    }

    // 지난 결과를 지우지 않고 쌓는다. 화면은 마지막 것만 보고, 이력은 뒤 화면이 쓴다.
    override suspend fun save(
        kind: AnalysisKind,
        scopeKey: String,
        content: AnalysisContent,
        imageFileName: String?,
        measurement: InbodyMeasurement?,
    ) {
        analysisResultDao.insert(
            AnalysisResultEntity(
                kind = kind.name,
                scopeKey = scopeKey,
                summary = content.summary,
                sections = encodeSections(json, content.sections),
                imageFileName = imageFileName,
                weightKg = measurement?.weightKg,
                skeletalMuscleKg = measurement?.skeletalMuscleKg,
                bodyFatKg = measurement?.bodyFatKg,
                heightCm = measurement?.heightCm,
                createdAtMillis = clock.millis(),
            ),
        )
    }

    private fun pathOf(fileName: String): String = inbodyImageStore.pathOf(fileName)
}
