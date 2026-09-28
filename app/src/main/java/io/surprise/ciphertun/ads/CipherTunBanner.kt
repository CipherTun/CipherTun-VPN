package io.surprise.ciphertun.ads

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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

@Composable
fun CipherTunBanner(
    bannerAdUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

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
                retryAttempt[0] = 0
                retryHandler.removeCallbacks(retryRunnable)
            }

            override fun onAdFailedToLoad(
                error: LoadAdError,
            ) {
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeightDp),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(adHeightDp),
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
