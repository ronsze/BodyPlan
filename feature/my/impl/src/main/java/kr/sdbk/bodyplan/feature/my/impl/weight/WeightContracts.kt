package kr.sdbk.bodyplan.feature.my.impl.weight

import java.time.LocalDate
import kr.sdbk.bodyplan.core.domain.model.WeightRecord
import kr.sdbk.bodyplan.core.domain.model.WeightTrend
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class WeightState(
    /** 화면에 들어온 시점의 오늘. 자정을 넘겨도 화면이 그리는 기준은 흔들리지 않는다. */
    val today: LocalDate? = null,
    val selectedDate: LocalDate? = null,
    /** 입력 중인 체중. 소수를 받으므로 문자열로 들고 있다 — 지우는 도중의 빈 칸과 0을 구분해야 한다. */
    val input: String = "",
    /** 최근 60일 기록. 오래된 날짜부터다. */
    val records: List<WeightRecord> = emptyList(),
    val trend: WeightTrend = WeightTrend(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) : State {
    /** 쓰거나 고칠 수 있는 날짜. 오늘과 어제뿐이다. */
    val editableDates: List<LocalDate>
        get() = today?.let { listOf(it, it.minusDays(1)) } ?: emptyList()

    /** 사람 체중으로 볼 수 없는 값은 막는다. 손이 미끄러져 자릿수가 틀린 값이 이력에 남지 않게 한다. */
    val canSave: Boolean
        get() = !isSaving && input.toDoubleOrNull()?.let { it in MIN_WEIGHT_KG..MAX_WEIGHT_KG } == true

    /**
     * 목록에 보이는 기록. 최근 것이 위다 — 그래프 없이 숫자만 보므로 최신이 먼저여야 한다.
     *
     * [records]가 이미 오래된 날짜부터 정렬돼 있어 되집기만 한다. 다시 정렬하면 입력 한 글자마다
     * 60건을 정렬하게 된다.
     */
    val recentRecords: List<WeightRecord>
        get() = records.asReversed().take(HISTORY_ROWS)

    /** 그 날짜에 남아 있는 값. 없으면 빈 칸이다. */
    fun inputTextOn(date: LocalDate?): String =
        records.firstOrNull { it.date == date }?.weightKg?.let(::weightText).orEmpty()
}

internal sealed interface WeightIntent : Intent {
    data class SelectDate(val date: LocalDate) : WeightIntent

    data class ChangeInput(val text: String) : WeightIntent

    data object ClickSave : WeightIntent

    data object ClickBack : WeightIntent
}

internal sealed interface WeightEffect : Effect {
    data object GoBack : WeightEffect

    data class ShowMessage(val message: String) : WeightEffect
}

private const val MIN_WEIGHT_KG = 20.0
private const val MAX_WEIGHT_KG = 300.0
private const val HISTORY_ROWS = 14
