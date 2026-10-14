package kr.sdbk.bodyplan.feature.my.impl.inbodyadd

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kr.sdbk.bodyplan.core.domain.repository.AiCredentialRepository
import kr.sdbk.bodyplan.core.domain.usecase.AddInbodyRecordUseCase
import kr.sdbk.bodyplan.core.domain.usecase.RequestInbodyAnalysisUseCase
import kr.sdbk.bodyplan.core.ui.coordinator.BaseViewModel

@HiltViewModel
internal class InbodyAddViewModel
@Inject
constructor(
    private val addInbodyRecord: AddInbodyRecordUseCase,
    private val requestInbodyAnalysis: RequestInbodyAnalysisUseCase,
    private val aiCredentialRepository: AiCredentialRepository,
    private val clock: Clock,
) : BaseViewModel<InbodyAddState, InbodyAddIntent, InbodyAddEffect>(
    initialState = InbodyAddState(measuredOn = LocalDate.now(clock)),
) {
    override suspend fun initializeData() {
        viewModelScope.launch {
            aiCredentialRepository.observeCredential().collect { credential ->
                updateState { it.copy(hasCredential = credential != null) }
            }
        }
    }

    override fun handleIntent(intent: InbodyAddIntent) {
        when (intent) {
            is InbodyAddIntent.PickImage ->
                updateState { it.copy(pickedImageUri = intent.uri, errorMessage = null) }

            is InbodyAddIntent.ClickPickDate -> updateState {
                it.copy(isDatePickerVisible = true, pickerYearMonth = YearMonth.from(it.measuredOn))
            }

            is InbodyAddIntent.SelectDate -> selectDate(intent.date)

            is InbodyAddIntent.ChangePickerMonth -> updateState { it.copy(pickerYearMonth = intent.yearMonth) }

            is InbodyAddIntent.DismissDatePicker -> updateState { it.copy(isDatePickerVisible = false) }

            is InbodyAddIntent.ClickAddOnly -> submit(analyze = false)

            is InbodyAddIntent.ClickAddAndAnalyze -> submit(analyze = true)

            is InbodyAddIntent.ConfirmTokenDialog -> {
                updateState { it.copy(isTokenDialogVisible = false) }
                updateEffect(InbodyAddEffect.NavigateToAiToken)
            }

            is InbodyAddIntent.DismissTokenDialog -> updateState { it.copy(isTokenDialogVisible = false) }
        }
    }

    /** 아직 오지 않은 날은 잰 날이 될 수 없다. */
    private fun selectDate(date: LocalDate) {
        if (date.isAfter(LocalDate.now(clock))) return
        updateState { it.copy(measuredOn = date, isDatePickerVisible = false) }
    }

    /**
     * 기록을 만들고, 원하면 이어서 분석을 요청한다.
     *
     * 연결이 없을 때 기록부터 만들면 분석 없이 사진만 쌓여 사용자가 의도한 것과 달라진다.
     * 그래서 요청이 필요한 경로는 만들기 전에 막는다.
     */
    private fun submit(analyze: Boolean) {
        val current = state.value
        if (!current.canSubmit) return
        val uri = current.pickedImageUri ?: return
        if (analyze && !current.hasCredential) {
            updateState { it.copy(isTokenDialogVisible = true) }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSubmitting = true, errorMessage = null) }
            val recordId = try {
                addInbodyRecord(uri, current.measuredOn)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                updateState { it.copy(isSubmitting = false, errorMessage = ADD_FAILED) }
                return@launch
            }

            if (analyze) {
                try {
                    requestInbodyAnalysis(recordId)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    // 기록은 이미 남았다. 화면은 닫고, 닫히기 전에 무엇이 잘못됐는지만 알린다.
                    updateEffect(InbodyAddEffect.ShowMessage(ANALYZE_FAILED))
                }
            }
            updateEffect(InbodyAddEffect.Close)
        }
    }
}

private const val ADD_FAILED = "사진을 저장하지 못했습니다"
private const val ANALYZE_FAILED = "사진은 추가했지만 분석을 요청하지 못했습니다"
