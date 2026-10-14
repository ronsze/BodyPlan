package kr.sdbk.bodyplan.feature.my.impl.inbodyadd

import java.time.LocalDate
import java.time.YearMonth
import kr.sdbk.bodyplan.core.ui.coordinator.Effect
import kr.sdbk.bodyplan.core.ui.coordinator.Intent
import kr.sdbk.bodyplan.core.ui.coordinator.State

internal data class InbodyAddState(
    val pickedImageUri: String? = null,
    /** 처음 값은 오늘이다. 화면이 열릴 때의 시계로 정한다. */
    val measuredOn: LocalDate,
    val isDatePickerVisible: Boolean = false,
    val pickerYearMonth: YearMonth = YearMonth.from(measuredOn),
    val isSubmitting: Boolean = false,
    val hasCredential: Boolean = false,
    val isTokenDialogVisible: Boolean = false,
    val errorMessage: String? = null,
) : State {
    /** 사진을 고르기 전에는 추가할 것이 없다. */
    val canSubmit: Boolean get() = pickedImageUri != null && !isSubmitting
}

internal sealed interface InbodyAddIntent : Intent {
    data class PickImage(val uri: String) : InbodyAddIntent

    data object ClickPickDate : InbodyAddIntent

    data class SelectDate(val date: LocalDate) : InbodyAddIntent

    data class ChangePickerMonth(val yearMonth: YearMonth) : InbodyAddIntent

    data object DismissDatePicker : InbodyAddIntent

    data object ClickAddOnly : InbodyAddIntent

    data object ClickAddAndAnalyze : InbodyAddIntent

    data object ConfirmTokenDialog : InbodyAddIntent

    data object DismissTokenDialog : InbodyAddIntent
}

internal sealed interface InbodyAddEffect : Effect {
    data object Close : InbodyAddEffect

    data class ShowMessage(val message: String) : InbodyAddEffect

    data object NavigateToAiToken : InbodyAddEffect
}
