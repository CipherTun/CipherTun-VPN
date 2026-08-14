package io.surprise.ciphertun.vendor

import android.content.pm.PackageInfo
import android.os.Build
import android.os.IBinder
import io.surprise.ciphertun.bg.RootClient
import io.surprise.ciphertun.database.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Dispatches "list installed packages" requests (used by the per-app proxy
 * picker and privilege settings screens) to whichever privileged backend the
 * user has selected in [Settings.perAppProxyPackageQueryMode]: Shizuku or
 * root. Falls back to the normal (unprivileged, visibility-filtered)
 * PackageManager query if neither is available.
 *
 * Root queries are proxied through the existing [RootClient] root service.
 * Shizuku queries go through [ShizukuBridge] (flavor-specific — see that
 * file) and the same `IPackageManager` reflection technique as
 * [PrivilegedServiceUtils], transacted over a Shizuku-wrapped binder.
 */
object PackageQueryManager {

    private val bridge: ShizukuBridge by lazy { createShizukuBridge() }

    /** Package query mode selection is only meaningful once package
     * visibility filtering exists to work around (Android 11 / API 30+). */
    val showModeSelector: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private val _shizukuInstalled = MutableStateFlow(false)
    val shizukuInstalled: StateFlow<Boolean> = _shizukuInstalled

    private val _shizukuBinderReady = MutableStateFlow(false)
    val shizukuBinderReady: StateFlow<Boolean> = _shizukuBinderReady

    private val _shizukuPermissionGranted = MutableStateFlow(false)
    val shizukuPermissionGranted: StateFlow<Boolean> = _shizukuPermissionGranted

    private const val SHIZUKU_REQUEST_CODE = 24_231
    private var listenersRegistered = false

    /** Must be called from a lifecycle-aware scope (e.g. `DisposableEffect`)
     * before reading Shizuku state; pair with [unregisterListeners]. */
    fun registerListeners() {
        if (!bridge.isSupported() || listenersRegistered) return
        listenersRegistered = true
        bridge.addListeners(
            onBinderReceived = {
                _shizukuBinderReady.value = true
                refreshShizukuPermissionState()
            },
            onBinderDead = {
                _shizukuBinderReady.value = false
                _shizukuPermissionGranted.value = false
            },
            onPermissionResult = { requestCode, granted ->
                if (requestCode == SHIZUKU_REQUEST_CODE) {
                    _shizukuPermissionGranted.value = granted
                }
            },
        )
    }

    fun unregisterListeners() {
        if (!listenersRegistered) return
        listenersRegistered = false
        bridge.removeListeners()
    }

    /** Re-derives current Shizuku install/binder/permission state on demand
     * (e.g. after returning from the Shizuku manager app). */
    fun refreshShizukuState() {
        _shizukuInstalled.value = isShizukuAppInstalled()
        _shizukuBinderReady.value = bridge.pingBinder()
        refreshShizukuPermissionState()
    }

    fun isShizukuAvailable(): Boolean =
        bridge.isSupported() && bridge.pingBinder() && !bridge.isPreV11()

    fun requestShizukuPermission() {
        if (!isShizukuAvailable()) return
        if (bridge.checkSelfPermissionGranted()) {
            _shizukuPermissionGranted.value = true
            return
        }
        bridge.requestPermission(SHIZUKU_REQUEST_CODE)
    }

    suspend fun checkRootAvailable(): Boolean = RootClient.checkRootAvailable()

    fun setQueryMode(mode: String) {
        Settings.perAppProxyPackageQueryMode = mode
    }

    /**
     * Returns installed packages using the currently configured privileged
     * backend, first trying [flags], and retrying with [retryFlags] if the
     * privileged path is unavailable or throws. Falls back to the normal
     * (filtered) PackageManager query as a last resort.
     */
    suspend fun getInstalledPackages(flags: Int, retryFlags: Int): List<PackageInfo> {
        val mode = Settings.perAppProxyPackageQueryMode
        val privileged = try {
            when (mode) {
                Settings.PACKAGE_QUERY_MODE_ROOT -> queryViaRoot(flags) ?: queryViaRoot(retryFlags)
                Settings.PACKAGE_QUERY_MODE_SHIZUKU -> queryViaShizuku(flags) ?: queryViaShizuku(retryFlags)
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
        if (privileged != null) return privileged

        return try {
            queryViaUnprivileged(flags)
        } catch (_: Throwable) {
            queryViaUnprivileged(retryFlags)
        }
    }

    private suspend fun queryViaRoot(flags: Int): List<PackageInfo>? = try {
        if (RootClient.checkRootAvailable()) RootClient.getInstalledPackages(flags) else null
    } catch (_: Throwable) {
        null
    }

    private fun queryViaShizuku(flags: Int): List<PackageInfo>? {
        if (!isShizukuAvailable()) return null
        if (!bridge.checkSelfPermissionGranted()) return null
        return try {
            val rawBinder = SystemServiceHelperCompat.getSystemService("package") ?: return null
            val wrapped: IBinder = bridge.wrapBinder(rawBinder)
            ShizukuIPackageManagerReflection.getInstalledPackages(wrapped, flags)
        } catch (_: Throwable) {
            null
        }
    }

    private fun queryViaUnprivileged(flags: Int): List<PackageInfo> {
        val context = io.surprise.ciphertun.Application.application
        @Suppress("DEPRECATION")
        return context.packageManager.getInstalledPackages(flags)
    }

    private fun isShizukuAppInstalled(): Boolean = try {
        val context = io.surprise.ciphertun.Application.application
        context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        true
    } catch (_: Exception) {
        false
    }

    private fun refreshShizukuPermissionState() {
        _shizukuPermissionGranted.value =
            bridge.pingBinder() && bridge.checkSelfPermissionGranted()
    }
}

/**
 * Minimal, self-contained `IPackageManager` reflection for a
 * Shizuku-wrapped binder. Kept separate from [PrivilegedServiceUtils]
 * because that object always fetches its own (unwrapped) binder via
 * [SystemServiceHelperCompat], which only works when the calling process
 * itself is already privileged (e.g. inside the root service). Here the
 * binder is supplied externally, already wrapped for Shizuku transacting.
 */
private object ShizukuIPackageManagerReflection {
    private val stubClass by lazy { Class.forName("android.content.pm.IPackageManager\$Stub") }
    private val asInterfaceMethod by lazy { stubClass.getMethod("asInterface", IBinder::class.java) }
    private val iPackageManagerClass by lazy { Class.forName("android.content.pm.IPackageManager") }

    private val getInstalledPackagesLong by lazy {
        iPackageManagerClass.getMethod(
            "getInstalledPackages",
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
    }
    private val getInstalledPackagesInt by lazy {
        iPackageManagerClass.getMethod(
            "getInstalledPackages",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
    }
    private val getListMethod by lazy {
        Class.forName("android.content.pm.ParceledListSlice").getMethod("getList")
    }

    fun getInstalledPackages(binder: IBinder, flags: Int): List<PackageInfo> {
        val iPackageManager = asInterfaceMethod.invoke(null, binder)
            ?: throw IllegalStateException("IPackageManager is null")
        val userId = android.os.Process.myUserHandle().hashCode()
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getInstalledPackagesLong.invoke(iPackageManager, flags.toLong(), userId)
        } else {
            getInstalledPackagesInt.invoke(iPackageManager, flags, userId)
        }
        @Suppress("UNCHECKED_CAST")
        val list = getListMethod.invoke(result) as? List<*>
        return list?.filterIsInstance<PackageInfo>() ?: emptyList()
    }
}
