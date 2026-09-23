package io.surprise.ciphertun.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.Date

object AdsManager {
    private const val TAG = "CipherTunAds"
    private const val INTERSTITIAL_COOLDOWN_MS = 90_000L
    private const val APP_OPEN_COOLDOWN_MS = 60_000L

    private var mobileAdsInitialized = false
    private var consentReady = false

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var lastInterstitialAt = 0L

    private var appOpen: AppOpenAd? = null
    private var appOpenLoading = false
    private var appOpenLoadTime = 0L
    private var lastAppOpenAt = 0L
    private var firstForegroundSeen = false

    fun initialize(activity: Activity) {
        val context = activity.applicationContext
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    consentReady = consentInformation.canRequestAds()
                    if (consentReady) initializeMobileAds(context)
                }
            },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.message}")
                consentReady = consentInformation.canRequestAds()
                if (consentReady) initializeMobileAds(context)
            },
        )

        if (consentInformation.canRequestAds()) {
            consentReady = true
            initializeMobileAds(context)
        }
    }

    private fun initializeMobileAds(context: Context) {
        if (mobileAdsInitialized) return
        mobileAdsInitialized = true
        MobileAds.initialize(context) {
            loadInterstitial(context)
            loadAppOpen(context)
        }
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.w(TAG, "Privacy options failed: ${error.message}")
            }
        }
    }

    fun showInterstitial(activity: Activity): Boolean {
        if (!consentReady || AdsConfig.INTERSTITIAL.isBlank()) return false

        val now = System.currentTimeMillis()
        if (now - lastInterstitialAt < INTERSTITIAL_COOLDOWN_MS) return false

        val ad = interstitial ?: run {
            loadInterstitial(activity.applicationContext)
            return false
        }

        interstitial = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadInterstitial(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    lastInterstitialAt = 0L
                    loadInterstitial(activity.applicationContext)
                }
            }

        ad.show(activity)
        lastInterstitialAt = now
        return true
    }

    private fun loadInterstitial(context: Context) {
        if (
            !consentReady ||
            AdsConfig.INTERSTITIAL.isBlank() ||
            interstitialLoading ||
            interstitial != null
        ) {
            return
        }

        interstitialLoading = true
        InterstitialAd.load(
            context,
            AdsConfig.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitial = null
                    Log.d(TAG, "Interstitial load failed: ${error.message}")
                }
            },
        )
    }

    fun onAppForeground(activity: Activity) {
        if (!consentReady) return

        if (!firstForegroundSeen) {
            firstForegroundSeen = true
            loadAppOpen(activity.applicationContext)
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastAppOpenAt < APP_OPEN_COOLDOWN_MS) return

        if (!isAppOpenFresh()) {
            loadAppOpen(activity.applicationContext)
            return
        }

        val ad = appOpen ?: return
        appOpen = null
        lastAppOpenAt = now

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadAppOpen(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    loadAppOpen(activity.applicationContext)
                }
            }

        ad.show(activity)
    }

    private fun isAppOpenFresh(): Boolean =
        appOpen != null &&
            appOpenLoadTime > 0L &&
            Date().time - appOpenLoadTime < 4L * 60L * 60L * 1000L

    private fun loadAppOpen(context: Context) {
        if (!consentReady || appOpenLoading || isAppOpenFresh()) return

        appOpenLoading = true
        AppOpenAd.load(
            context,
            AdsConfig.APP_OPEN,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenLoading = false
                    appOpen = ad
                    appOpenLoadTime = Date().time
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenLoading = false
                    appOpen = null
                    Log.d(TAG, "App open load failed: ${error.message}")
                }
            },
        )
    }
}
