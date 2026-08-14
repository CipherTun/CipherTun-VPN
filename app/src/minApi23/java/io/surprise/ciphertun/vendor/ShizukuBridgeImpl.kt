package io.surprise.ciphertun.vendor

import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

internal fun createShizukuBridge(): ShizukuBridge = RealShizukuBridge

private object RealShizukuBridge : ShizukuBridge {
    override fun isSupported(): Boolean = true

    override fun pingBinder(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    override fun isPreV11(): Boolean = try {
        Shizuku.isPreV11()
    } catch (_: Throwable) {
        true
    }

    override fun checkSelfPermissionGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    override fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (_: Throwable) {
        }
    }

    override fun wrapBinder(raw: IBinder): IBinder = ShizukuBinderWrapper(raw)

    private var binderReceivedListener: Shizuku.OnBinderReceivedListener? = null
    private var binderDeadListener: Shizuku.OnBinderDeadListener? = null
    private var permissionResultListener: Shizuku.OnRequestPermissionResultListener? = null

    override fun addListeners(
        onBinderReceived: () -> Unit,
        onBinderDead: () -> Unit,
        onPermissionResult: (requestCode: Int, granted: Boolean) -> Unit,
    ) {
        removeListeners()
        val received = Shizuku.OnBinderReceivedListener { onBinderReceived() }
        val dead = Shizuku.OnBinderDeadListener { onBinderDead() }
        val permission = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            onPermissionResult(requestCode, grantResult == PackageManager.PERMISSION_GRANTED)
        }
        binderReceivedListener = received
        binderDeadListener = dead
        permissionResultListener = permission
        try {
            Shizuku.addBinderReceivedListenerSticky(received)
            Shizuku.addBinderDeadListener(dead)
            Shizuku.addRequestPermissionResultListener(permission)
        } catch (_: Throwable) {
        }
    }

    override fun removeListeners() {
        try {
            binderReceivedListener?.let { Shizuku.removeBinderReceivedListener(it) }
            binderDeadListener?.let { Shizuku.removeBinderDeadListener(it) }
            permissionResultListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
        } catch (_: Throwable) {
        }
        binderReceivedListener = null
        binderDeadListener = null
        permissionResultListener = null
    }
}
