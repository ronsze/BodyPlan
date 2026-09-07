package kr.sdbk.bodyplan.core.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kr.sdbk.bodyplan.core.local.DefaultExercises

/**
 * 식단 기록 테이블을 더한다. 기존 운동 기록은 건드리지 않는다.
 *
 * SQL은 `core/local/schemas`의 2.json이 적어 둔 것을 옮긴 것이다.
 * 스키마가 바뀌면 이 함수를 고치지 말고 새 마이그레이션을 더한다.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `diet_entry` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dateEpochDay` INTEGER NOT NULL, " +
                "`imageFileName` TEXT NOT NULL, " +
                "`memo` TEXT, " +
                "`createdAtMillis` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_diet_entry_dateEpochDay` " +
                "ON `diet_entry` (`dateEpochDay`)",
        )
    }
}

/** 사용자 정보 테이블을 더한다. 기존 기록은 건드리지 않는다. */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                "`id` INTEGER NOT NULL, " +
                "`ageYears` INTEGER, " +
                "`heightCm` INTEGER, " +
                "`weightKg` INTEGER, " +
                "`gender` TEXT, " +
                "`goals` TEXT NOT NULL, " +
                "`targetWeightKg` INTEGER, " +
                "`targetNote` TEXT, " +
                "PRIMARY KEY(`id`))",
        )
    }
}

/** 분석 결과 테이블을 더한다. 기존 기록은 건드리지 않는다. */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `analysis_result` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`kind` TEXT NOT NULL, " +
                "`scopeKey` TEXT NOT NULL, " +
                "`summary` TEXT NOT NULL, " +
                "`sections` TEXT NOT NULL, " +
                "`createdAtMillis` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_analysis_result_kind_scopeKey` " +
                "ON `analysis_result` (`kind`, `scopeKey`)",
        )
    }
}

/** 인바디 결과가 분석한 사진을 가리킨다. 다른 분석은 비어 있다. */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `analysis_result` ADD COLUMN `imageFileName` TEXT")
    }
}

/**
 * 유산소 기본 종목을 심는다. 표 모양은 바뀌지 않는다.
 *
 * 기본 종목은 데이터베이스를 처음 만들 때만 심어, 이미 쓰던 사용자에게는 여기서 넣어야 한다.
 *
 * 같은 부위에 같은 이름이 살아 있으면 넣지 않는다 — 사용자가 손수 만든 것을 겹쳐 쌓지 않는다.
 * 부위까지 보는 것은 다른 부위의 동명 종목이 삽입을 막지 않게 하기 위해서다.
 * 사용자가 지운(`isDeleted`) 동명 종목은 막지 않고 새로 심는다 — 지운 것은 그 종목이지
 * 유산소를 안 쓰겠다는 뜻이 아니고, 막으면 되살릴 길이 손수 재등록뿐이다.
 */
internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        DefaultExercises.cardio().forEach { exercise ->
            db.execSQL(
                "INSERT INTO `exercise` (`bodyPart`, `name`, `intensityType`, `isDeleted`) " +
                    "SELECT ?, ?, ?, 0 WHERE NOT EXISTS " +
                    "(SELECT 1 FROM `exercise` " +
                    "WHERE `name` = ? AND `bodyPart` = ? AND `isDeleted` = 0)",
                arrayOf(
                    exercise.bodyPart,
                    exercise.name,
                    exercise.intensityType,
                    exercise.name,
                    exercise.bodyPart,
                ),
            )
        }
    }
}
