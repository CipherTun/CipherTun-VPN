package io.surprise.ciphertun.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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

    private const val INTERSTITIAL_COOLDOWN_MS = 30_000L
    private const val APP_OPEN_COOLDOWN_MS = 60_000L
    private const val AD_RETRY_DELAY_MS = 10_000L
    private const val APP_OPEN_MAX_AGE_MS = 4L * 60L * 60L * 1000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var mobileAdsInitialized = false
    private var consentReady = false

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var lastInterstitialAt = 0L

    private var appOpen: AppOpenAd? = null
    private var appOpenLoading = false
    private var appOpenLoadTime = 0L
    private var lastAppOpenAt = 0L

    private var currentActivity: Activity? = null
    private var isFullscreenAdShowing = false

    fun initialize(activity: Activity) {
        currentActivity = activity

        val context = activity.applicationContext
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    consentReady = consentInformation.canRequestAds()
                    if (consentReady) {
                        initializeMobileAds(context)
                    }
                }
            },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.message}")
                consentReady = consentInformation.canRequestAds()
                if (consentReady) {
                    initializeMobileAds(context)
                }
            },
        )

        if (consentInformation.canRequestAds()) {
            consentReady = true
            initializeMobileAds(context)
        }
    }

    fun setCurrentActivity(activity: Activity?) {
        currentActivity = activity
    }

    private fun initializeMobileAds(context: Context) {
        if (mobileAdsInitialized) {
            loadInterstitial(context)
            loadAppOpen(context)
            return
        }

        mobileAdsInitialized = true

        MobileAds.initialize(context) {
            Log.d(TAG, "Mobile Ads initialized")
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
        currentActivity = activity

        if (!consentReady || AdsConfig.INTERSTITIAL.isBlank()) {
            return false
        }

        if (isFullscreenAdShowing) {
            return false
        }

        val now = System.currentTimeMillis()

        if (now - lastInterstitialAt < INTERSTITIAL_COOLDOWN_MS) {
            return false
        }

        val ad = interstitial

        if (ad == null) {
            loadInterstitial(activity.applicationContext)
            return false
        }

        interstitial = null
        isFullscreenAdShowing = true

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    lastInterstitialAt = System.currentTimeMillis()
                    Log.d(TAG, "Interstitial shown")
                }

                override fun onAdDismissedFullScreenContent() {
                    isFullscreenAdShowing = false
                    Log.d(TAG, "Interstitial dismissed")
                    loadInterstitial(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isFullscreenAdShowing = false
                    lastInterstitialAt = 0L
                    Log.w(
                        TAG,
                        "Interstitial failed to show: ${adError.code}: ${adError.message}",
                    )
                    loadInterstitial(activity.applicationContext)
                }
            }

        ad.show(activity)
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
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitial = null

                    Log.w(
                        TAG,
                        "Interstitial failed to load: ${error.code}: ${error.message}",
                    )

                    scheduleInterstitialRetry(context)
                }
            },
        )
    }

    private fun scheduleInterstitialRetry(context: Context) {
        mainHandler.removeCallbacksAndMessages("interstitial_retry")
        mainHandler.postDelayed(
            {
                loadInterstitial(context)
            },
            AD_RETRY_DELAY_MS,
        )
    }

    fun onAppForeground(activity: Activity) {
        currentActivity = activity

        if (!consentReady || isFullscreenAdShowing) {
            return
        }

        val now = System.currentTimeMillis()

        if (now - lastAppOpenAt < APP_OPEN_COOLDOWN_MS) {
            return
        }

        val ad = appOpen

        if (ad == null || !isAppOpenFresh()) {
            loadAppOpen(activity.applicationContext)
            return
        }

        appOpen = null
        appOpenLoadTime = 0L
        lastAppOpenAt = now
        isFullscreenAdShowing = true

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App-open shown")
                }

                override fun onAdDismissedFullScreenContent() {
                    isFullscreenAdShowing = false
                    Log.d(TAG, "App-open dismissed")
                    loadAppOpen(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isFullscreenAdShowing = false
                    lastAppOpenAt = 0L

                    Log.w(
                        TAG,
                        "App-open failed to show: ${adError.code}: ${adError.message}",
                    )

                    loadAppOpen(activity.applicationContext)
                }
            }

        ad.show(activity)
    }

    private fun isAppOpenFresh(): Boolean =
        appOpen != null &&
            appOpenLoadTime > 0L &&
            Date().time - appOpenLoadTime < APP_OPEN_MAX_AGE_MS

    private fun loadAppOpen(context: Context) {
        if (
            !consentReady ||
            AdsConfig.APP_OPEN.isBlank() ||
            appOpenLoading ||
            isAppOpenFresh()
        ) {
            return
        }

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
                    Log.d(TAG, "App-open loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenLoading = false
                    appOpen = null
                    appOpenLoadTime = 0L

                    Log.w(
                        TAG,
                        "App-open failed to load: ${error.code}: ${error.message}",
                    )

                    mainHandler.removeCallbacksAndMessages("appopen_retry")
                    mainHandler.postDelayed(
                        {
                            loadAppOpen(context)
                        },
                        AD_RETRY_DELAY_MS,
                    )
                }
            },
        )
    }
}
