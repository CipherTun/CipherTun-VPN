package io.surprise.ciphertun.compat

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Compatibility wrappers kept in one place so the UI code is insulated from
 * Compose API differences between the two Compose BOMs this project targets
 * across flavors (2026.02.00 for play/other, 2025.01.00 for otherLegacy).
 */

/** The Material Design "medium" width breakpoint (dp), used to decide
 * tablet-vs-phone layout. Hardcoded rather than read from
 * `androidx.window.core.layout.WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND`
 * because that constant/breakpoint API isn't present in the older,
 * otherLegacy-paired Compose BOM. */
object WindowSizeClassCompat {
    const val WIDTH_DP_MEDIUM_LOWER_BOUND = 600
}

/**
 * Version-agnostic replacement for
 * `windowSizeClass.isWidthAtLeastBreakpoint(dp)`, which isn't available on
 * the older Compose BOM. Reads the raw configuration width instead — this
 * has been stable Compose API for years, so it works identically on both
 * BOMs targeted by this project.
 */
@Composable
fun isWidthAtLeastBreakpointCompat(widthDpBreakpoint: Int): Boolean =
    LocalConfiguration.current.screenWidthDp >= widthDpBreakpoint

/**
 * `rememberOverscrollEffect()` is `internal` in this project's Compose
 * Foundation version, so app code can't call it directly. Always returns
 * `null`, which every call site here already treats as "use platform
 * default overscroll" — same visual result, just not customizable.
 */
@Composable
fun rememberOverscrollEffectCompat(): OverscrollEffect? = null

/**
 * `Modifier.verticalScroll(state, overscrollEffect)` — the 2-arg overload
 * with `overscrollEffect` — doesn't exist on the older, otherLegacy-paired
 * Compose BOM. [overscrollEffect] is accepted here purely so call sites
 * don't need editing, but is not forwarded; scrolling still gets the
 * platform's default overscroll either way.
 */
fun Modifier.verticalScrollCompat(
    state: ScrollState,
    @Suppress("UNUSED_PARAMETER") overscrollEffect: OverscrollEffect? = null,
): Modifier = verticalScroll(state = state)

/**
 * Thin pass-through wrapper around [LazyColumn]. Exists purely so call sites
 * don't reference `androidx.compose.foundation.lazy.LazyColumn` directly.
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
 * The call site (`DashboardSettingsBottomSheet.kt`, inside an
 * `itemsIndexed { }` block) is inside `LazyItemScope`, so this is a
 * `LazyItemScope` extension — not a standalone `Modifier` extension as
 * originally assumed — delegating straight to the real, built-in
 * `LazyItemScope.animateItem()`.
 */
fun LazyItemScope.animateItemCompat(
    placementSpec: FiniteAnimationSpec<IntOffset> = spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    ),
): Modifier = Modifier.animateItem(placementSpec = placementSpec)

/**
 * `ExposedDropdownMenuAnchorType` doesn't exist on the older,
 * otherLegacy-paired Material3 version — only the newer BOM has it. Falls
 * back to the older, deprecated-but-still-functional no-arg `menuAnchor()`
 * overload, which every Material3 version supports.
 */
@Suppress("DEPRECATION")
fun androidx.compose.material3.ExposedDropdownMenuBoxScope.menuAnchorCompat(): Modifier =
    Modifier.menuAnchor()
