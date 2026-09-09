package kr.sdbk.bodyplan.feature.my.impl.weight

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.WeightLogRepository
import kr.sdbk.bodyplan.core.domain.usecase.GetWeightTrendUseCase
import kr.sdbk.bodyplan.core.domain.usecase.IsEditableDateUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class WeightViewModel
@Inject
constructor(
    private val weightLogRepository: WeightLogRepository,
    private val getWeightTrend: GetWeightTrendUseCase,
    private val isEditableDate: IsEditableDateUseCase,
    private val clock: Clock,
) : BaseViewModel<WeightState, WeightIntent, WeightEffect>(initialState = WeightState()) {
    override suspend fun initializeData() {
        // 화면이 그리는 기준일은 여기서 한 번만 읽는다. 저장할 때만 지금 시각으로 다시 판정한다.
        val today = LocalDate.now(clock)
        updateState { it.copy(today = today, selectedDate = today, isLoading = true) }
        observeRecords(today)
    }

    override fun handleIntent(intent: WeightIntent) {
        when (intent) {
            is WeightIntent.SelectDate -> selectDate(intent.date)

            is WeightIntent.ChangeInput ->
                if (isAcceptableWeightInput(intent.text)) updateState { it.copy(input = intent.text) }

            is WeightIntent.ClickSave -> save()

            is WeightIntent.ClickBack -> updateEffect(WeightEffect.GoBack)
        }
    }

    /**
     * 기록을 구독한다. 목록과 변동이 같은 흐름에서 나온다 — 구독을 둘로 나누면 저장할 때마다
     * 같은 구간을 두 번 읽고, 조회가 실패할 때 같은 문구가 두 번 뜬다.
     *
     * 입력칸은 첫 방출에서만 채운다. 저장이 흘려보내는 뒤이은 방출까지 채우면 사용자가 이어
     * 고치던 글자를 덮는다.
     */
    private fun observeRecords(today: LocalDate) {
        viewModelScope.launch {
            weightLogRepository.observeRecordsInRange(today.minusDays(HISTORY_DAYS - 1), today)
                .catch {
                    updateState { it.copy(isLoading = false) }
                    updateEffect(WeightEffect.ShowMessage(RECORDS_LOAD_FAILED))
                }
                .collect { records ->
                    updateState { current ->
                        val loaded = current.copy(
                            isLoading = false,
                            records = records,
                            trend = getWeightTrend(records, today),
                        )
                        if (current.isLoading) loaded.copy(input = loaded.inputTextOn(loaded.selectedDate)) else loaded
                    }
                }
        }
    }

    private fun selectDate(date: LocalDate) {
        if (date !in state.value.editableDates) return
        updateState { it.copy(selectedDate = date, input = it.inputTextOn(date)) }
    }

    /**
     * 고른 날짜에 값을 남긴다.
     *
     * 편집 가능 여부를 여기서 다시 본다. 화면을 열어 둔 채 자정을 넘기면 어제였던 날짜가
     * 그저께가 되는데, 칩은 화면에 들어온 시점의 기준으로 그려져 있어 눌리기 때문이다.
     */
    private fun save() {
        val current = state.value
        val date = current.selectedDate ?: return
        if (!current.canSave) return
        val weightKg = current.input.toDoubleOrNull() ?: return
        if (!isEditableDate(date)) {
            updateEffect(WeightEffect.ShowMessage(NOT_EDITABLE))
            return
        }

        // 코루틴을 띄우기 전에 세운다. 안에서 세우면 연타한 두 번째 누름이 같은 검사를 통과한다.
        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { weightLogRepository.save(date, weightKg) }
                .onFailure { updateEffect(WeightEffect.ShowMessage(SAVE_FAILED)) }
            updateState { it.copy(isSaving = false) }
        }
    }
}

/** 목록에 보이는 것은 14줄이지만, 월간 변동이 최근 30일과 그 앞 30일을 견주므로 60일을 읽는다. */
private const val HISTORY_DAYS = 60L

private const val RECORDS_LOAD_FAILED = "체중 기록을 불러오지 못했습니다"
private const val SAVE_FAILED = "체중을 저장하지 못했습니다"
private const val NOT_EDITABLE = "오늘과 어제만 기록할 수 있어요"
