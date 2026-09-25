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
    private const val RETRY_BASE_MS = 10_000L
    private const val RETRY_MAX_MS = 60_000L
    private const val APP_OPEN_MAX_AGE_MS = 4L * 60L * 60L * 1000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var mobileAdsInitialized = false
    private var consentReady = false
    private var consentRequestInProgress = false

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialRetryAttempt = 0
    private var interstitialRetryRunnable: Runnable? = null
    private var lastInterstitialAt = 0L

    private var appOpen: AppOpenAd? = null
    private var appOpenLoading = false
    private var appOpenRetryAttempt = 0
    private var appOpenRetryRunnable: Runnable? = null
    private var appOpenLoadTime = 0L
    private var lastAppOpenAt = 0L
    private var firstForegroundSeen = false

    private var currentActivity: Activity? = null
    private var fullscreenAdShowing = false

    fun initialize(activity: Activity) {
        runOnMain {
            currentActivity = activity

            val context = activity.applicationContext
            val consentInformation =
                UserMessagingPlatform.getConsentInformation(context)

            if (consentRequestInProgress) {
                if (consentInformation.canRequestAds()) {
                    consentReady = true
                    initializeMobileAds(context)
                }
                return@runOnMain
            }

            consentRequestInProgress = true

            val params = ConsentRequestParameters.Builder().build()

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        activity,
                    ) {
                        consentRequestInProgress = false
                        consentReady = consentInformation.canRequestAds()

                        if (consentReady) {
                            initializeMobileAds(context)
                        } else {
                            Log.w(TAG, "Ads unavailable: consent not granted")
                        }
                    }
                },
                { error ->
                    consentRequestInProgress = false

                    Log.w(
                        TAG,
                        "Consent update failed: ${error.message}",
                    )

                    /*
                     * Google recommends checking canRequestAds() even
                     * when consent update itself fails because a valid
                     * previous-session consent state may still exist.
                     */
                    consentReady = consentInformation.canRequestAds()

                    if (consentReady) {
                        initializeMobileAds(context)
                    }
                },
            )

            /*
             * A previous valid consent decision can allow ads immediately.
             */
            if (consentInformation.canRequestAds()) {
                consentReady = true
                initializeMobileAds(context)
            }
        }
    }

    fun setCurrentActivity(activity: Activity?) {
        runOnMain {
            currentActivity = activity
        }
    }

    private fun initializeMobileAds(context: Context) {
        if (!consentReady) return

        if (mobileAdsInitialized) {
            loadInterstitial(context)
            loadAppOpen(context)
            return
        }

        mobileAdsInitialized = true

        MobileAds.initialize(context) {
            runOnMain {
                Log.d(TAG, "Mobile Ads initialized")

                loadInterstitial(context)
                loadAppOpen(context)
            }
        }
    }

    fun showPrivacyOptions(activity: Activity) {
        runOnMain {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) {
                    Log.w(
                        TAG,
                        "Privacy options failed: ${error.message}",
                    )
                }
            }
        }
    }

    fun showInterstitial(activity: Activity): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                showInterstitial(activity)
            }
            return false
        }

        currentActivity = activity

        if (!consentReady) {
            Log.d(TAG, "Interstitial skipped: consent not ready")
            return false
        }

        if (AdsConfig.INTERSTITIAL.isBlank()) {
            return false
        }

        if (fullscreenAdShowing) {
            return false
        }

        if (activity.isFinishing || activity.isDestroyed) {
            return false
        }

        val now = System.currentTimeMillis()

        if (now - lastInterstitialAt < INTERSTITIAL_COOLDOWN_MS) {
            return false
        }

        val ad = interstitial

        if (ad == null) {
            /*
             * Never wait for the load callback and then unexpectedly
             * interrupt the user's current navigation. Just preload it
             * for the next valid transition.
             */
            loadInterstitial(activity.applicationContext)
            return false
        }

        interstitial = null
        fullscreenAdShowing = true
        lastInterstitialAt = now

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial shown")
                }

                override fun onAdDismissedFullScreenContent() {
                    fullscreenAdShowing = false

                    Log.d(TAG, "Interstitial dismissed")

                    interstitialRetryAttempt = 0
                    loadInterstitial(
                        activity.applicationContext,
                    )
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError,
                ) {
                    fullscreenAdShowing = false
                    lastInterstitialAt = 0L

                    Log.w(
                        TAG,
                        "Interstitial failed to show: " +
                            "${adError.code}: ${adError.message}",
                    )

                    loadInterstitial(
                        activity.applicationContext,
                    )
                }
            }

        try {
            ad.show(activity)
            return true
        } catch (t: Throwable) {
            fullscreenAdShowing = false
            lastInterstitialAt = 0L

            Log.e(
                TAG,
                "Interstitial show threw",
                t,
            )

            loadInterstitial(
                activity.applicationContext,
            )

            return false
        }
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

        interstitialRetryRunnable?.let {
            mainHandler.removeCallbacks(it)
        }
        interstitialRetryRunnable = null

        interstitialLoading = true

        InterstitialAd.load(
            context,
            AdsConfig.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitial = ad
                    interstitialRetryAttempt = 0

                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError,
                ) {
                    interstitialLoading = false
                    interstitial = null

                    Log.w(
                        TAG,
                        "Interstitial failed to load: " +
                            "${error.code}: ${error.message}",
                    )

                    scheduleInterstitialRetry(context)
                }
            },
        )
    }

    private fun scheduleInterstitialRetry(context: Context) {
        if (!consentReady) return

        interstitialRetryRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        val delay =
            retryDelay(interstitialRetryAttempt)

        interstitialRetryAttempt =
            (interstitialRetryAttempt + 1).coerceAtMost(6)

        val retry =
            Runnable {
                interstitialRetryRunnable = null
                loadInterstitial(context)
            }

        interstitialRetryRunnable = retry

        Log.d(
            TAG,
            "Retrying interstitial in ${delay}ms",
        )

        mainHandler.postDelayed(
            retry,
            delay,
        )
    }

    fun onAppForeground(activity: Activity) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                onAppForeground(activity)
            }
            return
        }

        currentActivity = activity

        if (!consentReady) return
        if (fullscreenAdShowing) return

        /*
         * First foreground only preloads. This prevents an app-open
         * ad from unexpectedly covering the initial screen.
         */
        if (!firstForegroundSeen) {
            firstForegroundSeen = true
            loadAppOpen(activity.applicationContext)
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val now = System.currentTimeMillis()

        if (now - lastAppOpenAt < APP_OPEN_COOLDOWN_MS) {
            return
        }

        if (!isAppOpenFresh()) {
            loadAppOpen(activity.applicationContext)
            return
        }

        val ad = appOpen ?: run {
            loadAppOpen(activity.applicationContext)
            return
        }

        appOpen = null
        appOpenLoadTime = 0L
        lastAppOpenAt = now
        fullscreenAdShowing = true

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App-open shown")
                }

                override fun onAdDismissedFullScreenContent() {
                    fullscreenAdShowing = false

                    Log.d(TAG, "App-open dismissed")

                    appOpenRetryAttempt = 0
                    loadAppOpen(
                        activity.applicationContext,
                    )
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError,
                ) {
                    fullscreenAdShowing = false
                    lastAppOpenAt = 0L

                    Log.w(
                        TAG,
                        "App-open failed to show: " +
                            "${adError.code}: ${adError.message}",
                    )

                    loadAppOpen(
                        activity.applicationContext,
                    )
                }
            }

        try {
            ad.show(activity)
        } catch (t: Throwable) {
            fullscreenAdShowing = false
            lastAppOpenAt = 0L

            Log.e(
                TAG,
                "App-open show threw",
                t,
            )

            loadAppOpen(
                activity.applicationContext,
            )
        }
    }

    private fun isAppOpenFresh(): Boolean =
        appOpen != null &&
            appOpenLoadTime > 0L &&
            Date().time - appOpenLoadTime <
            APP_OPEN_MAX_AGE_MS

    private fun loadAppOpen(context: Context) {
        if (
            !consentReady ||
            AdsConfig.APP_OPEN.isBlank() ||
            appOpenLoading ||
            isAppOpenFresh()
        ) {
            return
        }

        appOpenRetryRunnable?.let {
            mainHandler.removeCallbacks(it)
        }
        appOpenRetryRunnable = null

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
                    appOpenRetryAttempt = 0

                    Log.d(TAG, "App-open loaded")
                }

                override fun onAdFailedToLoad(
                    error: LoadAdError,
                ) {
                    appOpenLoading = false
                    appOpen = null
                    appOpenLoadTime = 0L

                    Log.w(
                        TAG,
                        "App-open failed to load: " +
                            "${error.code}: ${error.message}",
                    )

                    scheduleAppOpenRetry(context)
                }
            },
        )
    }

    private fun scheduleAppOpenRetry(context: Context) {
        if (!consentReady) return

        appOpenRetryRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        val delay =
            retryDelay(appOpenRetryAttempt)

        appOpenRetryAttempt =
            (appOpenRetryAttempt + 1).coerceAtMost(6)

        val retry =
            Runnable {
                appOpenRetryRunnable = null
                loadAppOpen(context)
            }

        appOpenRetryRunnable = retry

        Log.d(
            TAG,
            "Retrying app-open in ${delay}ms",
        )

        mainHandler.postDelayed(
            retry,
            delay,
        )
    }

    private fun retryDelay(attempt: Int): Long {
        val multiplier =
            1L shl attempt.coerceIn(0, 3)

        return (RETRY_BASE_MS * multiplier)
            .coerceAtMost(RETRY_MAX_MS)
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}
