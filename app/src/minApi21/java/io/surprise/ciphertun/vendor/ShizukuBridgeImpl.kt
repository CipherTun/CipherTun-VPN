package io.surprise.ciphertun.vendor

import android.os.IBinder

internal fun createShizukuBridge(): ShizukuBridge = UnsupportedShizukuBridge

/**
 * `otherLegacy` (API 21+) intentionally has no `dev.rikka.shizuku` dependency
 * (see `app/build.gradle.kts`). Package queries on this flavor fall back to
 * root (via [RootClient]) or the normal PackageManager instead.
 */
private object UnsupportedShizukuBridge : ShizukuBridge {
    override fun isSupported(): Boolean = false
    override fun pingBinder(): Boolean = false
    override fun isPreV11(): Boolean = true
    override fun checkSelfPermissionGranted(): Boolean = false
    override fun requestPermission(requestCode: Int) {}
    override fun wrapBinder(raw: IBinder): IBinder = raw

    override fun addListeners(
        onBinderReceived: () -> Unit,
        onBinderDead: () -> Unit,
        onPermissionResult: (requestCode: Int, granted: Boolean) -> Unit,
    ) {
    }

    override fun removeListeners() {}
}
