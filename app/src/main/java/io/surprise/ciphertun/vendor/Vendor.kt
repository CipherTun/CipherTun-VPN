package io.surprise.ciphertun.vendor

import android.app.Activity
import android.content.Context
import androidx.camera.core.ImageAnalysis
import io.surprise.ciphertun.compose.screen.qrscan.QRCodeCropArea
import io.surprise.ciphertun.update.UpdateInfo
import io.surprise.ciphertun.update.UpdateSource

/**
 * Default CipherTun vendor implementation.
 *
 * Vendor-specific builds can replace/extend this implementation later, while
 * the common application remains buildable without a proprietary vendor layer.
 */
object Vendor : VendorInterface {
    override fun checkUpdate(activity: Activity, byUser: Boolean) {
        // No vendor-specific update UI is required by the common build.
    }

    override fun createQRCodeAnalyzer(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onCropArea: ((QRCodeCropArea?) -> Unit)?,
    ): ImageAnalysis.Analyzer? = null

    override fun isPerAppProxyAvailable(): Boolean = true

    override val hasCustomUpdate: Boolean
        get() = false

    override val updateSources: List<UpdateSource>
        get() = listOf(UpdateSource.GITHUB)

    override fun checkUpdateAsync(): UpdateInfo? = null

    override fun scheduleAutoUpdate() = Unit

    override suspend fun verifySilentInstallMethod(method: String): Boolean = false

    override suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
    ) {
        throw UnsupportedOperationException("Vendor update installation is not configured")
    }
}
