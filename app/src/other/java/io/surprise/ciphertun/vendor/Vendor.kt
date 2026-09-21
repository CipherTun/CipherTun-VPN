package io.surprise.ciphertun.vendor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageAnalysis
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.surprise.ciphertun.Application
import io.surprise.ciphertun.R
import io.surprise.ciphertun.bg.RootClient
import io.surprise.ciphertun.compose.screen.qrscan.QRCodeCropArea
import io.surprise.ciphertun.database.Settings
import io.surprise.ciphertun.update.UpdateCheckException
import io.surprise.ciphertun.update.UpdateInfo
import io.surprise.ciphertun.update.UpdateSource
import io.surprise.ciphertun.update.UpdateState
import io.surprise.ciphertun.update.UpdateTrack
import io.surprise.ciphertun.update.checkFDroidUpdate

object Vendor : VendorInterface {
    private const val TAG = "Vendor"

    override fun checkUpdate(activity: Activity, byUser: Boolean) {
        try {
            val updateInfo = checkUpdateAsync()
            if (updateInfo != null) {
                activity.runOnUiThread {
                    showUpdateDialog(activity, updateInfo)
                }
            } else if (byUser) {
                activity.runOnUiThread {
                    showNoUpdatesDialog(activity)
                }
            }
        } catch (e: UpdateCheckException.TrackNotSupported) {
            Log.d(TAG, "checkUpdate: track not supported")
            if (byUser) {
                activity.runOnUiThread {
                    showTrackNotSupportedDialog(activity)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkUpdate: ", e)
            if (byUser) {
                activity.runOnUiThread {
                    showNoUpdatesDialog(activity)
                }
            }
        }
    }

    private fun showUpdateDialog(activity: Activity, updateInfo: UpdateInfo) {
        val message = buildString {
            append(activity.getString(R.string.new_version_available, updateInfo.versionName))
            if (!updateInfo.releaseNotes.isNullOrBlank()) {
                append("\n\n")
                append(updateInfo.releaseNotes.take(500))
                if (updateInfo.releaseNotes.length > 500) {
                    append("...")
                }
            }
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.check_update)
            .setMessage(message)
            .setPositiveButton(R.string.update) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.releaseUrl))
                activity.startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showNoUpdatesDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.check_update)
            .setMessage(R.string.no_updates_available)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showTrackNotSupportedDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.check_update)
            .setMessage(R.string.update_track_not_supported)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun createQRCodeAnalyzer(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onCropArea: ((QRCodeCropArea?) -> Unit)?,
    ): ImageAnalysis.Analyzer? = null

    override val hasCustomUpdate = true

    override val updateSources = listOf(UpdateSource.GITHUB, UpdateSource.FDROID)

    override fun checkUpdateAsync(): UpdateInfo? = when (UpdateSource.fromString(Settings.updateSource)) {
        UpdateSource.FDROID -> checkFDroidUpdate(Application.application)
        UpdateSource.GITHUB -> {
            val track = UpdateTrack.fromString(Settings.updateTrack)
            GitHubUpdateChecker().use { checker ->
                checker.checkUpdate(track, Settings.githubToken)
            }
        }
    }

    override fun scheduleAutoUpdate() {
        UpdateWorker.schedule(io.surprise.ciphertun.Application.application)
    }

    override suspend fun verifySilentInstallMethod(method: String): Boolean {
        return when (method) {
            "PACKAGE_INSTALLER" -> {
                ApkInstaller.canSystemSilentInstall()
            }
            "SHIZUKU" -> {
                if (!ShizukuInstaller.isAvailable()) {
                    return false
                }
                if (!ShizukuInstaller.checkPermission()) {
                    ShizukuInstaller.requestPermission()
                    return false
                }
                true
            }
            "ROOT" -> RootClient.checkRootAvailable()
            else -> false
        }
    }

    override suspend fun downloadAndInstall(context: android.content.Context, downloadUrl: String) {
        val cachedApk = UpdateState.cachedApkFile.value
        val apkFile = if (cachedApk != null && cachedApk.exists() && cachedApk.length() > 0) {
            cachedApk
        } else {
            ApkDownloader().use { it.download(downloadUrl) }
        }
        ApkInstaller.install(context, apkFile)
    }
}
