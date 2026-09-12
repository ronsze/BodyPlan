package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 저장된 분석 결과.
 *
 * 같은 대상을 여러 번 분석하면 행이 쌓이고, 화면은 마지막 것만 본다.
 * 지난 결과를 남겨 두는 것은 인바디처럼 이력을 보여줄 화면이 뒤에 오기 때문이다.
 *
 * [sections]는 묶음 목록을 담은 JSON 배열이다. 묶음만 따로 조회하거나 정렬할 일이 없어
 * 표를 나누지 않는다.
 */
@Serializable
@Entity(
    tableName = "analysis_result",
    indices = [Index(value = ["kind", "scopeKey"])],
)
data class AnalysisResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val kind: String,
    val scopeKey: String,
    val summary: String,
    val sections: String,
    /** 인바디가 분석한 사진의 파일명. 다른 분석은 null이다. */
    val imageFileName: String? = null,
    /** 결과지에서 읽어 낸 값. 인바디만 채우고, 항목마다 없을 수 있다. */
    val weightKg: Double? = null,
    val skeletalMuscleKg: Double? = null,
    val bodyFatKg: Double? = null,
    val heightCm: Double? = null,
    val createdAtMillis: Long,
)
