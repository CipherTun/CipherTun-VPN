package io.surprise.ciphertun.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlin.math.roundToInt
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup

@Composable
fun CipherTunBanner(
    bannerAdUnitId: String,
    modifier: Modifier = Modifier,
    respectTopSystemInset: Boolean = false,
) {
    val context = LocalContext.current
    var isLoaded by remember(bannerAdUnitId) { mutableStateOf(false) }

    /*
     * Calculate the exact anchored-adaptive size once for this
     * banner/unit. The Compose slot is then constrained to exactly
     * that height instead of allowing extra vertical space.
     */
    val adSize = remember(bannerAdUnitId) {
        val density = context.resources.displayMetrics.density
        val widthDp =
            (context.resources.displayMetrics.widthPixels / density)
                .toInt()
                .coerceAtLeast(1)

        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            context,
            widthDp,
        )
    }

    val adHeightDp = adSize.height.coerceAtLeast(1).dp

    val adView = remember(bannerAdUnitId, adSize) {
        val density = context.resources.displayMetrics.density

        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = bannerAdUnitId

            /*
             * Match the actual adaptive banner height in pixels.
             * This prevents the AndroidView from creating a larger
             * vertical slot than the ad itself requires.
             */
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (adSize.height * density)
                    .roundToInt()
                    .coerceAtLeast(1),
            )
        }
    }

    val retryHandler = remember(adView) {
        Handler(Looper.getMainLooper())
    }

    /*
     * Retry state is independent for each banner instance.
     */
    val retryAttempt = remember(adView) {
        intArrayOf(0)
    }

    val retryRunnable = remember(adView) {
        object : Runnable {
            override fun run() {
                adView.loadAd(
                    AdRequest.Builder().build(),
                )
            }
        }
    }

    DisposableEffect(adView, bannerAdUnitId) {
        adView.adListener = object : AdListener() {

            override fun onAdLoaded() {
                isLoaded = true
                retryAttempt[0] = 0
                retryHandler.removeCallbacks(retryRunnable)
            }

            override fun onAdFailedToLoad(
                error: LoadAdError,
            ) {
                // Collapse the slot when no ad is available; do not leave
                // a blank banner-sized obstruction over app content.
                isLoaded = false
                retryHandler.removeCallbacks(retryRunnable)

                /*
                 * Aggressive but bounded retry:
                 * 5s -> 10s -> 20s -> 30s -> 30s...
                 *
                 * This only retries failures. It does NOT manually
                 * refresh successful banners every 20 seconds.
                 */
                val attempt =
                    retryAttempt[0]
                        .coerceIn(0, 3)

                retryAttempt[0] =
                    (attempt + 1)
                        .coerceAtMost(4)

                val delay =
                    (5_000L * (1L shl attempt))
                        .coerceAtMost(30_000L)

                retryHandler.postDelayed(
                    retryRunnable,
                    delay,
                )
            }
        }

        /*
         * Listener MUST be installed before loadAd().
         */
        adView.loadAd(
            AdRequest.Builder().build(),
        )

        onDispose {
            retryHandler.removeCallbacks(retryRunnable)
            adView.destroy()
        }
    }

    /*
     * The Compose slot is EXACTLY the adaptive banner height.
     */
    val visibleHeight = if (isLoaded) adHeightDp else 0.dp
    val insetModifier = if (respectTopSystemInset && isLoaded) {
        Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(insetModifier)
            .height(visibleHeight),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(visibleHeight),
            factory = {
                adView
            },
            update = { view ->
                if (view.adUnitId != bannerAdUnitId) {
                    view.adUnitId = bannerAdUnitId
                    retryAttempt[0] = 0
                    retryHandler.removeCallbacks(retryRunnable)
                    view.loadAd(
                        AdRequest.Builder().build(),
                    )
                }
            },
        )
    }
}
