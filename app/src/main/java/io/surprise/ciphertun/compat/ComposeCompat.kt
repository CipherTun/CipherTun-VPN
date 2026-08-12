package io.surprise.ciphertun.compat

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass

/**
 * Compatibility wrappers kept in one place so the UI code is insulated from
 * Compose/Window API changes across AndroidX releases.
 */
object WindowSizeClassCompat {
    const val WIDTH_DP_MEDIUM_LOWER_BOUND =
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
}

fun WindowSizeClass.isWidthAtLeastBreakpointCompat(
    widthDpBreakpoint: Int,
): Boolean = isWidthAtLeastBreakpoint(widthDpBreakpoint)

@Composable
fun rememberOverscrollEffectCompat(): OverscrollEffect? = rememberOverscrollEffect()

fun Modifier.verticalScrollCompat(
    state: ScrollState,
    overscrollEffect: OverscrollEffect? = null,
): Modifier = verticalScroll(
    state = state,
    overscrollEffect = overscrollEffect,
)
