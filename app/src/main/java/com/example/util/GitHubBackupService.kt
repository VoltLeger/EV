package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GistSyncResult(
    val gistId: String,
    val htmlUrl: String,
    val updatedAt: Long
)

object GitHubBackupService {

    /**
     * Uploads or updates a backup Gist on GitHub.
     * If existingGistId and token are provided, updates via PATCH.
     * Otherwise creates a new Gist via POST.
     */
    suspend fun uploadBackupToGist(
        backupJson: String,
        userEmail: String,
        existingGistId: String? = null,
        githubToken: String? = null
    ): Result<GistSyncResult> = withContext(Dispatchers.IO) {
        try {
            val hasToken = !githubToken.isNullOrBlank()
            val isPatch = !existingGistId.isNullOrBlank() && hasToken

            val urlString = if (isPatch) {
                "https://api.github.com/gists/$existingGistId"
            } else {
                "https://api.github.com/gists"
            }

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = if (isPatch) "PATCH" else "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "VoltLedger-Android")
                if (hasToken) {
                    setRequestProperty("Authorization", "Bearer ${githubToken!!.trim()}")
                }
            }

            val requestJson = JSONObject().apply {
                put("description", "VoltLedger Cloud Backup [$userEmail]")
                put("public", false)
                val files = JSONObject()
                val fileObj = JSONObject()
                fileObj.put("content", backupJson)
                files.put("voltledger_backup.json", fileObj)
                put("files", files)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(responseText)
                val gistId = respJson.getString("id")
                val htmlUrl = respJson.optString("html_url", "https://gist.github.com/$gistId")
                Result.success(
                    GistSyncResult(
                        gistId = gistId,
                        htmlUrl = htmlUrl,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                val errorStream = connection.errorStream
                val errorMsg = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                val parsedMsg = try {
                    JSONObject(errorMsg).optString("message", errorMsg)
                } catch (_: Exception) {
                    errorMsg
                }
                Result.failure(Exception("GitHub API ($responseCode): $parsedMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads backup JSON from GitHub Gist using its Gist ID.
     */
    suspend fun downloadBackupFromGist(
        gistId: String,
        githubToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanId = gistId.trim().substringAfterLast("/")
            val url = URL("https://api.github.com/gists/$cleanId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "VoltLedger-Android")
                if (!githubToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer ${githubToken.trim()}")
                }
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(responseText)
                val files = respJson.getJSONObject("files")

                // Find voltledger_backup.json or the first json file
                val fileKey = if (files.has("voltledger_backup.json")) {
                    "voltledger_backup.json"
                } else {
                    files.keys().asSequence().firstOrNull()
                }

                if (fileKey != null) {
                    val fileObj = files.getJSONObject(fileKey)
                    val content = fileObj.getString("content")
                    Result.success(content)
                } else {
                    Result.failure(Exception("Резервная копия не найдена в указанном Gist"))
                }
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                val parsedMsg = try {
                    JSONObject(errorMsg).optString("message", errorMsg)
                } catch (_: Exception) {
                    errorMsg
                }
                Result.failure(Exception("Ошибка GitHub ($responseCode): $parsedMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
