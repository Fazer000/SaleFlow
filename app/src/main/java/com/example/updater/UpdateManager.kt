package com.example.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class UpdateCheckResult {
    object Idle : UpdateCheckResult()
    object Checking : UpdateCheckResult()
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseNotes: String,
        val releaseUrl: String,
        val apkDownloadUrl: String?
    ) : UpdateCheckResult()
    data class Downloading(val progressPercent: Int) : UpdateCheckResult()
    data class ReadyToInstall(val apkFile: File) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class UpdateManager {
    private val service: GitHubUpdateService
    private val okHttpClient: OkHttpClient

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        service = retrofit.create(GitHubUpdateService::class.java)
    }

    suspend fun checkForUpdates(
        repoSlug: String, // e.g. "Fazer000/SaleFlow"
        currentVersion: String
    ): UpdateCheckResult {
        return try {
            val parts = repoSlug.trim().split("/")
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return UpdateCheckResult.Error("Неверный формат репозитория GitHub (ожидается owner/repo)")
            }
            val owner = parts[0]
            val repo = parts[1]

            val release = service.getLatestRelease(owner, repo)
            val latestVersion = release.tagName.trim().removePrefix("v")
            val cleanCurrent = currentVersion.trim().removePrefix("v")

            if (isVersionNewer(latestVersion, cleanCurrent)) {
                val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = release.tagName,
                    releaseNotes = release.body ?: "Описание изменения отсутствует.",
                    releaseUrl = release.htmlUrl,
                    apkDownloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl
                )
            } else {
                UpdateCheckResult.UpToDate(cleanCurrent)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Ошибка проверки обновлений: ${e.localizedMessage ?: "Неизвестная ошибка"}")
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(downloadUrl).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Ошибка скачивания: HTTP ${response.code}")
        val body = response.body ?: throw IOException("Пустое тело ответа сервера")

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val outputFile = File(downloadsDir, "saleflow-update.apk")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val totalBytes = body.contentLength()
        var downloadedBytes = 0L

        body.byteStream().use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                        onProgress(progress)
                    }
                }
            }
        }
        outputFile
    }

    fun installApk(context: Context, apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest == current) return false
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
