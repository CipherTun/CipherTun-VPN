package io.surprise.ciphertun.compat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch

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

/**
 * Thin pass-through wrapper around [LazyColumn]. Exists purely so call sites
 * don't reference `androidx.compose.foundation.lazy.LazyColumn` directly,
 * mirroring the pattern used by [verticalScrollCompat] /
 * [rememberOverscrollEffectCompat] elsewhere in this file.
 */
@Composable
fun LazyColumnCompat(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = null,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
        content = content,
    )
}

/**
 * Standalone placement-animation modifier for manually-implemented
 * drag-reorder lists that are NOT inside a `LazyItemScope` (so the real
 * `LazyItemScope.animateItem()` isn't available). Tracks this item's
 * position in its parent and animates any delta caused by reordering,
 * mirroring the call shape of the real `animateItem(placementSpec = ...)`.
 */
fun Modifier.animateItemCompat(
    placementSpec: FiniteAnimationSpec<IntOffset> = spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    ),
): Modifier = composed {
    val scope = rememberCoroutineScope()
    var lastPosition by remember { mutableStateOf<IntOffset?>(null) }
    val animatedOffset = remember { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }

    this
        .onGloballyPositioned { coordinates ->
            val newPosition = IntOffset(
                coordinates.positionInParent().x.toInt(),
                coordinates.positionInParent().y.toInt(),
            )
            val previous = lastPosition
            lastPosition = newPosition
            if (previous != null && previous != newPosition) {
                val delta = previous - newPosition
                scope.launch {
                    animatedOffset.snapTo(delta)
                    animatedOffset.animateTo(IntOffset.Zero, placementSpec)
                }
            }
        }
        .let { base ->
            base.then(
                Modifier.graphicsLayerOffsetCompat { animatedOffset.value },
            )
        }
}

private fun Modifier.graphicsLayerOffsetCompat(
    offset: () -> IntOffset,
): Modifier = this.then(
    androidx.compose.ui.layout.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val o = offset()
            placeable.place(o.x, o.y)
        }
    },
)
