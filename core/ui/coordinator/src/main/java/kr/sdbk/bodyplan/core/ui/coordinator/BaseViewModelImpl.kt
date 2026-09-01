package kr.sdbk.bodyplan.core.ui.coordinator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 내부 상태 [S]와 화면이 그리는 상태 [U]를 다르게 두는 화면을 위한 베이스.
 * 둘이 같으면 [BaseViewModel]을 상속한다.
 */
abstract class BaseViewModelImpl<S : State, U : State, I : Intent, E : Effect>(
    initialState: S,
    private val enableReInitialization: Boolean = false,
) : ViewModel() {
    var isInitialized: Boolean = false
        private set

    protected val state: MutableStateFlow<S> = MutableStateFlow(initialState)
    abstract val uiState: StateFlow<U>

    private val _effect: Channel<E> = Channel(Channel.BUFFERED)
    open val effect: Flow<E> = _effect.receiveAsFlow()

    /** 화면이 [uiState]를 구독할 때 초기 로드를 태운다. 구현체는 이 함수로 [uiState]를 만든다. */
    protected fun Flow<U>.setup(initialUiState: U): StateFlow<U> = onStart {
        onStart()
        if (!isInitialized) {
            if (!enableReInitialization) isInitialized = true
            initializeData()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = initialUiState,
    )

    /** 구독이 시작될 때마다 수행한다. */
    protected open suspend fun onStart() = Unit

    /** 첫 구독에서 한 번만 수행하는 초기 로드. `enableReInitialization`이면 구독마다 수행한다. */
    protected open suspend fun initializeData() = Unit

    /** 화면은 이 함수로만 ViewModel에 입력을 전달한다. */
    abstract fun handleIntent(intent: I)

    protected open fun updateState(update: (S) -> S) = state.update(update)

    protected fun updateEffect(effect: E) = viewModelScope.launch { _effect.send(effect) }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
