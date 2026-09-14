package kr.sdbk.bodyplan.feature.home.impl.home

import java.util.Locale
import kr.sdbk.bodyplan.core.domain.model.BodyPartVolumeTrend
import kr.sdbk.bodyplan.core.domain.model.ProgressDirection
import kr.sdbk.bodyplan.core.ui.components.label

/** 늘었는지 줄었는지가 먼저 읽히게 부호를 붙인다. 같으면 `+0kg`이 아니라 말로 적는다 — 0은 "변화 없음"이라 읽히지 않는다. */
internal fun trendChangeText(trend: BodyPartVolumeTrend): String =
    if (trend.changeKg == 0) "변화 없음" else String.format(Locale.US, "%+,dkg", trend.changeKg)

/**
 * 늘어난 부위와 줄어든 부위를 한 줄로 묶는다. 부위마다 한 줄씩 적으면 일곱 줄이 되어 카드가 길어진다.
 *
 * 부위 이름 뒤에 조사를 붙이지 않는다 — 받침 유무가 부위마다 달라 은/는이 틀린다.
 *
 * 빈 목록은 받지 않는다 — 카드가 그 경우 메시지 대신 빈 문구를 보인다.
 */
internal fun volumeTrendMessage(trends: List<BodyPartVolumeTrend>): String {
    val improving = trends.filter {
        it.direction == ProgressDirection.IMPROVING
    }.joinToString("·") { it.bodyPart.label }
    val worsening = trends.filter {
        it.direction == ProgressDirection.WORSENING
    }.joinToString("·") { it.bodyPart.label }
    return when {
        improving.isNotEmpty() && worsening.isNotEmpty() -> "$improving 볼륨이 늘고 $worsening 볼륨이 줄었어요"
        improving.isNotEmpty() -> "$improving 볼륨이 늘었어요"
        worsening.isNotEmpty() -> "$worsening 볼륨이 줄었어요. 이번 주에 챙겨보세요"
        else -> "모든 부위가 지난주와 비슷해요"
    }
}
