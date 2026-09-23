package io.surprise.ciphertun.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.surprise.ciphertun.compose.MainActivity

class AdsLifecycle :
    Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {
    private var currentActivity: Activity? = null

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let(AdsManager::onAppForeground)
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (activity is MainActivity) AdsManager.initialize(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
