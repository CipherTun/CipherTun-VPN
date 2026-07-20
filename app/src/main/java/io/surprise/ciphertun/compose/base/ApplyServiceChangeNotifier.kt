package io.surprise.ciphertun.compose.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.surprise.ciphertun.constant.Status

@Composable
fun rememberApplyServiceChangeNotifier(
    serviceStatus: Status,
): (UiEvent.ApplyServiceChange.Mode) -> Unit = remember(serviceStatus) {
    { mode ->
        if (serviceStatus == Status.Started) {
            GlobalEventBus.tryEmit(UiEvent.ApplyServiceChange(mode))
        }
    }
}
