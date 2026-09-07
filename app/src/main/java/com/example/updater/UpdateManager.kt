package com.example.updater

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class UpdateManager {
    private val service: GitHubUpdateService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
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
        repoSlug: String, // e.g. "myorg/posterminal" or "user/repo"
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
