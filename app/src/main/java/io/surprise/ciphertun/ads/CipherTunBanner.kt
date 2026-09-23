package io.surprise.ciphertun.ads

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
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val adView = remember(adUnitId) {
        AdView(context).apply {
            adUnitId = this@CipherTunBanner.adUnitId

            val density = resources.displayMetrics.density
            val widthDp = (resources.displayMetrics.widthPixels / density).toInt().coerceAtLeast(320)

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

            adListener =
                object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("CipherTunAds", "Banner loaded: $adUnitId")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(
                            "CipherTunAds",
                            "Banner failed: $adUnitId - ${error.code}: ${error.message}",
                        )
                    }
                }

            loadAd(AdRequest.Builder().build())
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { adView },
    )

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }
}
