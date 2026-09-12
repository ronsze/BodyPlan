package kr.sdbk.bodyplan.core.local.snapshot

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kr.sdbk.bodyplan.core.domain.model.UnsupportedBackupVersionException
import kr.sdbk.bodyplan.core.local.BodyPlanDatabase

/**
 * DB 전체를 [BodyPlanSnapshot]으로 뜨고, 스냅샷으로 통째로 되돌린다.
 *
 * 여러 DAO에 걸치므로 트랜잭션은 DAO가 아니라 여기서 묶는다. 읽기도 트랜잭션 안에서 해
 * 표 사이에 다른 쓰기가 끼어들어 반쪽 스냅샷이 되지 않게 한다.
 */
@Singleton
class SnapshotStore
@Inject
constructor(private val database: BodyPlanDatabase) {
    suspend fun export(exportedAtMillis: Long): BodyPlanSnapshot = database.withTransaction {
        BodyPlanSnapshot(
            formatVersion = BodyPlanSnapshot.FORMAT_VERSION,
            dbVersion = currentDbVersion(),
            exportedAtMillis = exportedAtMillis,
            exercises = database.exerciseDao().getAll(),
            workoutEntries = database.workoutEntryDao().getAll(),
            workoutSets = database.workoutEntryDao().getAllSets(),
            dietEntries = database.dietEntryDao().getAll(),
            userProfiles = database.userProfileDao().getAll(),
            analysisResults = database.analysisResultDao().getAll(),
            weightRecords = database.weightRecordDao().getAll(),
            workoutMemos = database.workoutMemoDao().getAll(),
            routines = database.routineDao().getAll(),
            routineEntries = database.routineDao().getAllEntries(),
            routineSets = database.routineDao().getAllSets(),
        )
    }

    /**
     * 표를 전부 비우고 스냅샷의 행을 원래 id 그대로 넣는다. 하나라도 실패하면 아무것도 바뀌지 않는다.
     * 스키마 버전이 다르면 [UnsupportedBackupVersionException] — 다른 버전의 행은 컬럼이 맞지 않는다.
     */
    suspend fun import(snapshot: BodyPlanSnapshot) {
        if (snapshot.dbVersion != currentDbVersion()) {
            throw UnsupportedBackupVersionException(snapshot.dbVersion)
        }
        database.withTransaction {
            // 부모만 지운다. 자식(세트·루틴 항목)은 외래 키 CASCADE가 따라 지운다.
            database.exerciseDao().deleteAll()
            database.workoutEntryDao().deleteAll()
            database.dietEntryDao().deleteAll()
            database.userProfileDao().deleteAll()
            database.analysisResultDao().deleteAll()
            database.weightRecordDao().deleteAll()
            database.workoutMemoDao().deleteAll()
            database.routineDao().deleteAll()

            // 자식은 부모 뒤에 넣는다. 외래 키가 부모 행을 요구한다.
            database.exerciseDao().insertAll(snapshot.exercises)
            database.workoutEntryDao().insertAll(snapshot.workoutEntries)
            database.workoutEntryDao().insertSets(snapshot.workoutSets)
            database.dietEntryDao().insertAll(snapshot.dietEntries)
            database.userProfileDao().insertAll(snapshot.userProfiles)
            database.analysisResultDao().insertAll(snapshot.analysisResults)
            database.weightRecordDao().insertAll(snapshot.weightRecords)
            database.workoutMemoDao().insertAll(snapshot.workoutMemos)
            database.routineDao().insertAll(snapshot.routines)
            database.routineDao().insertEntries(snapshot.routineEntries)
            database.routineDao().insertSets(snapshot.routineSets)
        }
    }

    private fun currentDbVersion(): Int = database.openHelper.readableDatabase.version
}
