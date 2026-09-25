package io.surprise.ciphertun.ads

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
            val density = resources.displayMetrics.density
            val widthDp =
                (resources.displayMetrics.widthPixels / density)
                    .toInt()
                    .coerceAtLeast(1)

            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    widthDp,
                ),
            )

            adUnitId = bannerAdUnitId

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    val retryHandler = remember(adView) {
        Handler(Looper.getMainLooper())
    }

    val retryRunnable = remember(adView) {
        object : Runnable {
            override fun run() {
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    }

    DisposableEffect(adView, bannerAdUnitId) {
        adView.adListener = object : AdListener() {

            override fun onAdLoaded() {
                retryHandler.removeCallbacks(retryRunnable)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                retryHandler.removeCallbacks(retryRunnable)
                retryHandler.postDelayed(
                    retryRunnable,
                    10_000L,
                )
            }
        }

        // Listener MUST be installed before loadAd().
        adView.loadAd(AdRequest.Builder().build())

        onDispose {
            retryHandler.removeCallbacks(retryRunnable)
            adView.destroy()
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { adView },
            update = { view ->
                if (view.adUnitId != bannerAdUnitId) {
                    view.adUnitId = bannerAdUnitId
                    retryHandler.removeCallbacks(retryRunnable)
                    view.loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}
