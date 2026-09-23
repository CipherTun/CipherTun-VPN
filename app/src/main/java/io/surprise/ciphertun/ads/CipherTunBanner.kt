package io.surprise.ciphertun.ads

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun CipherTunBanner(
    bannerAdUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val adView = remember(bannerAdUnitId) {
        AdView(context).apply {
            setAdUnitId(bannerAdUnitId)

            val density = resources.displayMetrics.density
            val widthDp =
                (resources.displayMetrics.widthPixels / density)
                    .toInt()
                    .coerceAtLeast(320)

            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    widthDp,
                ),
            )

            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )

            val handler = Handler(Looper.getMainLooper())
            var retryRunnable: Runnable? = null
            var destroyed = false

            adListener =
                object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(
                            "CipherTunAds",
                            "Banner loaded: $bannerAdUnitId",
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(
                            "CipherTunAds",
                            "Banner failed: $bannerAdUnitId - " +
                                "${error.code}: ${error.message}",
                        )

                        retryRunnable?.let(handler::removeCallbacks)

                        val retry =
                            Runnable {
                                if (!destroyed && !isDestroyed && !isLoading) {
                                    loadAd(
                                        AdRequest.Builder().build(),
                                    )
                                }
                            }

                        retryRunnable = retry
                        handler.postDelayed(retry, 10_000L)
                    }
                }

            loadAd(AdRequest.Builder().build())

            tag = BannerLifecycle(
                handler = handler,
                destroy = {
                    destroyed = true
                    retryRunnable?.let(handler::removeCallbacks)
                    retryRunnable = null
                    destroy()
                },
            )
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { adView },
    )

    DisposableEffect(adView) {
        onDispose {
            val lifecycle = adView.tag as? BannerLifecycle
            if (lifecycle != null) {
                lifecycle.destroy()
            } else {
                adView.destroy()
            }
        }
    }
}

private class BannerLifecycle(
    private val handler: Handler,
    private val destroy: () -> Unit,
) {
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        destroy.invoke()
    }
}
