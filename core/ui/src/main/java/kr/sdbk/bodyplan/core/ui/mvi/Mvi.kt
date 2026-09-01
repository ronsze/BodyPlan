package kr.sdbk.bodyplan.core.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 화면이 그리는 값의 전부. 데이터 클래스로 구현하고 기본값을 초기 상태로 쓴다. */
interface UiState

/** 화면에서 올라오는 사용자 입력. */
interface UiIntent

/** 한 번만 소비되는 화면 밖 동작 — 이동, 스낵바, 시스템 호출. */
interface UiEffect

abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initialState: S) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    /** 화면은 이 함수로만 ViewModel에 입력을 전달한다. */
    abstract fun onIntent(intent: I)

    protected fun setState(reduce: S.() -> S) {
        _state.update(reduce)
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effect.send(effect) }
    }
}

@Composable
fun <E : UiEffect> CollectEffect(effect: Flow<E>, onEffect: (E) -> Unit) {
    LaunchedEffect(effect) {
        effect.collect(onEffect)
    }
}
