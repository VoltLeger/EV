package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Information about a GitHub Release for VoltLedger
 */
data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val downloadUrl: String,
    val fileName: String,
    val fileSize: Long,
    val htmlUrl: String,
    val isNewer: Boolean
)

/**
 * State of the update process
 */
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpToDate(val currentVersion: String, val checkedTime: Long = System.currentTimeMillis()) : UpdateStatus()
    data class Available(val info: UpdateInfo) : UpdateStatus()
    data class Downloading(
        val info: UpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateStatus()
    data class ReadyToInstall(val info: UpdateInfo, val apkFile: File) : UpdateStatus()
    data class Error(val message: String, val canRetry: Boolean = true) : UpdateStatus()
}

/**
 * Manager responsible for checking GitHub releases at https://github.com/VoltLeger/EV,
 * downloading APK assets, and triggering in-app package installation.
 */
object AppUpdateManager {
    const val GITHUB_REPO_URL = "https://github.com/VoltLeger/EV"
    const val GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/VoltLeger/EV/releases/latest"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val _latestAvailableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val latestAvailableUpdate: StateFlow<UpdateInfo?> = _latestAvailableUpdate.asStateFlow()

    fun resetStatus() {
        _status.value = UpdateStatus.Idle
    }

    /**
     * Checks https://api.github.com/repos/VoltLeger/EV/releases/latest for newer versions
     */
    suspend fun checkForUpdate(
        context: Context,
        onComplete: ((UpdateInfo?) -> Unit)? = null
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        _status.value = UpdateStatus.Checking
        try {
            val request = Request.Builder()
                .url(GITHUB_API_LATEST_RELEASE)
                .addHeader("User-Agent", "VoltLedger-EV-Android/${BuildConfig.VERSION_NAME}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                val err = if (code == 404) "Релизов на GitHub пока не найдено" else "Ошибка связи с GitHub ($code)"
                _status.value = UpdateStatus.Error(err)
                onComplete?.invoke(null)
                return@withContext null
            }

            val responseBody = response.body?.string().orEmpty()
            if (responseBody.isBlank()) {
                _status.value = UpdateStatus.Error("Пустой ответ от GitHub")
                onComplete?.invoke(null)
                return@withContext null
            }

            val json = JSONObject(responseBody)
            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName).ifBlank { tagName }
            val releaseNotes = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")
            val htmlUrl = json.optString("html_url", GITHUB_REPO_URL)

            val remoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val localVersion = BuildConfig.VERSION_NAME.trim()

            // Find APK asset
            var apkDownloadUrl = ""
            var apkFileName = "VoltLedger-$remoteVersion.apk"
            var apkFileSize = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        apkFileName = name
                        apkFileSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            // Fallback direct release URL if no asset attached
            if (apkDownloadUrl.isBlank()) {
                apkDownloadUrl = "$GITHUB_REPO_URL/releases/download/$tagName/VoltLedger-$tagName.apk"
            }

            val isNewer = isRemoteNewer(remoteVersion, localVersion)

            val updateInfo = UpdateInfo(
                tagName = tagName,
                versionName = remoteVersion,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                publishedAt = publishedAt,
                downloadUrl = apkDownloadUrl,
                fileName = apkFileName,
                fileSize = apkFileSize,
                htmlUrl = htmlUrl,
                isNewer = isNewer
            )

            if (isNewer) {
                _latestAvailableUpdate.value = updateInfo
                _status.value = UpdateStatus.Available(updateInfo)
            } else {
                _latestAvailableUpdate.value = null
                _status.value = UpdateStatus.UpToDate(localVersion)
            }

            onComplete?.invoke(if (isNewer) updateInfo else null)
            return@withContext updateInfo
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Сетевая ошибка при проверке обновлений"
            _status.value = UpdateStatus.Error(msg)
            onComplete?.invoke(null)
            return@withContext null
        }
    }

    /**
     * Downloads APK from GitHub and prompts user to install
     */
    suspend fun downloadAndInstall(
        context: Context,
        updateInfo: UpdateInfo
    ) = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, updateInfo.fileName)

            // If file already exists and matches size, skip download
            if (apkFile.exists() && updateInfo.fileSize > 0 && apkFile.length() == updateInfo.fileSize) {
                _status.value = UpdateStatus.ReadyToInstall(updateInfo, apkFile)
                withContext(Dispatchers.Main) {
                    installApk(context, apkFile)
                }
                return@withContext
            }

            _status.value = UpdateStatus.Downloading(
                info = updateInfo,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = updateInfo.fileSize
            )

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .addHeader("User-Agent", "VoltLedger-EV-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _status.value = UpdateStatus.Error("Не удалось скачать APK с GitHub (HTTP ${response.code})")
                return@withContext
            }

            val body = response.body ?: throw IllegalStateException("Пустое тело ответа")
            val totalBytes = if (updateInfo.fileSize > 0) updateInfo.fileSize else body.contentLength()

            val tempFile = File(updatesDir, "${updateInfo.fileName}.tmp")
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReportTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 200 || totalRead == totalBytes) {
                            lastReportTime = now
                            val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes.toFloat() else 0f
                            _status.value = UpdateStatus.Downloading(
                                info = updateInfo,
                                progress = progress.coerceIn(0f, 1f),
                                downloadedBytes = totalRead,
                                totalBytes = totalBytes
                            )
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.exists()) {
                if (apkFile.exists()) apkFile.delete()
                tempFile.renameTo(apkFile)
            }

            _status.value = UpdateStatus.ReadyToInstall(updateInfo, apkFile)

            // Trigger installation
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            _status.value = UpdateStatus.Error("Ошибка загрузки обновления: ${e.message}")
        }
    }

    /**
     * Launches standard Android package installer via FileProvider
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return false
            }

            // On Android 8.0+ (Oreo), verify if app can request package installs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: open in browser
            openInBrowser(context, GITHUB_REPO_URL)
            false
        }
    }

    /**
     * Opens GitHub project or release page in browser
     */
    fun openInBrowser(context: Context, url: String = GITHUB_REPO_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Compares remote version with local version using SemVer rules (major.minor.patch)
     * Returns true if remote is strictly newer than local
     */
    fun isRemoteNewer(remote: String, local: String): Boolean {
        try {
            val remoteParts = remote.split("-")[0].split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val localParts = local.split("-")[0].split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        } catch (_: Exception) {
            return remote != local && remote.isNotBlank()
        }
    }
}
