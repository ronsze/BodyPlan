package kr.sdbk.bodyplan.core.ui.coordinator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun <E : Effect> CollectEffect(effect: Flow<E>, onEffect: (E) -> Unit) {
    LaunchedEffect(effect) {
        effect.collect(onEffect)
    }
}
