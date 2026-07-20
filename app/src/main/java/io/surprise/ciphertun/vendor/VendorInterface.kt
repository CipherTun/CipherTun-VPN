package io.surprise.ciphertun.vendor

import android.app.Activity
import androidx.camera.core.ImageAnalysis
import io.surprise.ciphertun.compose.screen.qrscan.QRCodeCropArea
import io.surprise.ciphertun.update.UpdateInfo
import io.surprise.ciphertun.update.UpdateSource

interface VendorInterface {
    fun checkUpdate(activity: Activity, byUser: Boolean)

    fun createQRCodeAnalyzer(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onCropArea: ((QRCodeCropArea?) -> Unit)? = null,
    ): ImageAnalysis.Analyzer?

    fun isPerAppProxyAvailable(): Boolean = true

    val hasCustomUpdate: Boolean get() = false

    val updateSources: List<UpdateSource> get() = listOf(UpdateSource.GITHUB)

    fun checkUpdateAsync(): UpdateInfo? = null

    fun scheduleAutoUpdate() {}

    suspend fun verifySilentInstallMethod(method: String): Boolean = false

    suspend fun downloadAndInstall(context: android.content.Context, downloadUrl: String): Unit = throw UnsupportedOperationException("Not supported in this flavor")
}
