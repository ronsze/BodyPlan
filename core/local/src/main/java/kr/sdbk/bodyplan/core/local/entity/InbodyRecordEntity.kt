package kr.sdbk.bodyplan.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 인바디 사진 한 장과 사용자가 고른 측정일.
 *
 * 분석 결과는 여기 붙지 않고 `analysis_result`가 기록 번호를 열쇠로 따로 든다. 사진만 저장한
 * 기록이 있어야 해서 결과가 있을 때만 행이 생기는 구조를 쓸 수 없다.
 *
 * [failureReason]은 마지막 시도가 실패했을 때의 사유다. 작업 시스템의 기록은 정리되면 사라져
 * 실패 표시가 남지 않으므로 기록에 직접 둔다.
 */
@Serializable
@Entity(tableName = "inbody_record")
data class InbodyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val imageFileName: String,
    val measuredOnEpochDay: Long,
    val createdAtMillis: Long,
    val failureReason: String? = null,
)
