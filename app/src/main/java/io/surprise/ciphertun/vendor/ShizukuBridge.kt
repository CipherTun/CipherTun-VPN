package io.surprise.ciphertun.vendor

import android.os.IBinder

/**
 * Abstraction over the Shizuku API so shared code (`src/main`) never
 * references `rikka.shizuku.*` types directly. `dev.rikka.shizuku:api` is
 * only a dependency of the `play` and `other` flavors (API 23+) — see the
 * comment in `app/build.gradle.kts` — so `otherLegacy` has no Shizuku
 * dependency on its classpath at all.
 *
 * The real implementation lives in `src/minApi23/java` (merged into both
 * `play` and `other` per the existing sourceSets config); a no-op stub
 * lives in `src/minApi21/java` (merged into `otherLegacy`). Both provide
 * [createShizukuBridge], matching this project's existing pattern for
 * min-API-gated code.
 */
internal interface ShizukuBridge {
    fun isSupported(): Boolean
    fun pingBinder(): Boolean
    fun isPreV11(): Boolean
    fun checkSelfPermissionGranted(): Boolean
    fun requestPermission(requestCode: Int)
    fun wrapBinder(raw: IBinder): IBinder

    /** Registers listeners; callbacks fire on Shizuku's own binder thread. */
    fun addListeners(
        onBinderReceived: () -> Unit,
        onBinderDead: () -> Unit,
        onPermissionResult: (requestCode: Int, granted: Boolean) -> Unit,
    )

    fun removeListeners()
}
