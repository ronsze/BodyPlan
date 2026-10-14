package kr.sdbk.bodyplan.core.data.repository

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import kr.sdbk.bodyplan.core.data.image.InbodyImages
import kr.sdbk.bodyplan.core.data.image.LocalImageStore
import kr.sdbk.bodyplan.core.data.mapper.newInbodyRecordEntity
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.domain.model.AnalysisKind
import kr.sdbk.bodyplan.core.domain.model.InbodyRecord
import kr.sdbk.bodyplan.core.domain.model.InbodyRecordWithResult
import kr.sdbk.bodyplan.core.domain.repository.InbodyRecordRepository
import kr.sdbk.bodyplan.core.local.dao.AnalysisResultDao
import kr.sdbk.bodyplan.core.local.dao.InbodyRecordDao
import kr.sdbk.bodyplan.core.local.entity.AnalysisResultEntity
import kr.sdbk.bodyplan.core.local.entity.InbodyRecordEntity

internal class InbodyRecordRepositoryImpl
@Inject
constructor(
    private val inbodyRecordDao: InbodyRecordDao,
    private val analysisResultDao: AnalysisResultDao,
    @InbodyImages private val inbodyImageStore: LocalImageStore,
    private val json: Json,
    private val clock: Clock,
) : InbodyRecordRepository {
    override fun observeRecords(): Flow<List<InbodyRecordWithResult>> = combine(
        inbodyRecordDao.observeAll(),
        analysisResultDao.observeByKind(AnalysisKind.INBODY.name),
    ) { records, results -> join(records, results) }

    override suspend fun getRecords(): List<InbodyRecordWithResult> = join(
        inbodyRecordDao.getAll(),
        analysisResultDao.getAll().filter { it.kind == AnalysisKind.INBODY.name },
    )

    override suspend fun getRecord(id: Long): InbodyRecord? = inbodyRecordDao.getById(id)?.toDomain(::pathOf)

    override suspend fun insert(imageFileName: String, measuredOn: LocalDate): Long =
        inbodyRecordDao.insert(newInbodyRecordEntity(imageFileName, measuredOn, clock.millis()))

    override suspend fun setFailureReason(id: Long, reason: String?) {
        inbodyRecordDao.updateFailureReason(id, reason)
    }

    /**
     * 결과는 기록 번호를 열쇠로 붙는다.
     *
     * 결과 교체가 열쇠당 하나만 남기므로 정상에서는 둘이 겹치지 않는다. 겹쳐 있더라도 가장 나중 것을 쓴다.
     */
    private fun join(
        records: List<InbodyRecordEntity>,
        results: List<AnalysisResultEntity>,
    ): List<InbodyRecordWithResult> {
        val resultByKey = results
            .sortedWith(compareByDescending<AnalysisResultEntity> { it.createdAtMillis }.thenByDescending { it.id })
            .distinctBy { it.scopeKey }
            .associateBy { it.scopeKey }
        return records.map { record ->
            InbodyRecordWithResult(
                record = record.toDomain(::pathOf),
                result = resultByKey[record.id.toString()]?.toDomain(json, ::pathOf),
            )
        }
    }

    private fun pathOf(fileName: String): String = inbodyImageStore.pathOf(fileName)
}
