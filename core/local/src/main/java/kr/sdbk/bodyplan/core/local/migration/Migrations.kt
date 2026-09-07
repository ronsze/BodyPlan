package kr.sdbk.bodyplan.core.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
