package kr.sdbk.bodyplan.core.data.repository

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.data.mapper.toEntity
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.repository.WeightLogRepository
import kr.sdbk.bodyplan.core.local.dao.WeightRecordDao

internal class WeightLogRepositoryImpl
@Inject
constructor(
    private val weightRecordDao: WeightRecordDao,
    private val clock: Clock,
) : WeightLogRepository {
    override fun observeRecordsInRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>> =
        weightRecordDao.observeInRange(from.toEpochDay(), to.toEpochDay())
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(date: LocalDate, weightKg: Double) {
        weightRecordDao.upsert(WeightRecord(date, weightKg).toEntity(clock.millis()))
    }
}
