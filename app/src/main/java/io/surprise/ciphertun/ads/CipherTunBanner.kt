package io.surprise.ciphertun.ads

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

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
                )
            )

            adUnitId = bannerAdUnitId

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    var retryKey by remember(bannerAdUnitId) {
        mutableStateOf(0)
    }

    var retryJob by remember(bannerAdUnitId) {
        mutableStateOf<Job?>(null)
    }

    val scope = rememberCoroutineScope()

    fun loadBanner() {
        retryJob?.cancel()

        adView.loadAd(
            AdRequest.Builder().build()
        )
    }

    LaunchedEffect(bannerAdUnitId, retryKey) {
        loadBanner()
    }

    DisposableEffect(adView, bannerAdUnitId) {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                retryJob?.cancel()
                retryJob = null
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                retryJob?.cancel()
                retryJob = scope.launch {
                    delay(10_000L)
                    retryKey++
                }
            }
        }

        onDispose {
            retryJob?.cancel()
            retryJob = null
            adView.adListener = null
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            adView
        },
        update = { view ->
            if (view.adUnitId != bannerAdUnitId) {
                view.adUnitId = bannerAdUnitId
                retryKey++
            }
        },
    )
}
