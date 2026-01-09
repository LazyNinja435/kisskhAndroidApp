package com.example.kisskh.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val downloadUrl: String?,
    val releaseNotes: String?
)

object UpdateRepository {
    private const val GITHUB_REPO_OWNER = "LazyNinja435"
    private const val GITHUB_REPO_NAME = "kisskhAndroidApp"
    private const val GITHUB_API_BASE = "https://api.github.com"
    
    private val client = OkHttpClient.Builder()
        .build()
    
    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.longVersionCode.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }
    
    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            e.printStackTrace()
            "1.0"
        }
    }
    
    suspend fun checkForUpdates(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val currentVersionCode = getCurrentVersionCode(context)
            
            // Fetch latest release from GitHub
            val url = "$GITHUB_API_BASE/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext UpdateInfo(
                    isUpdateAvailable = false,
                    latestVersionCode = currentVersionCode,
                    latestVersionName = getCurrentVersionName(context),
                    downloadUrl = null,
                    releaseNotes = null
                )
            }
            
            val responseBody = response.body?.string() ?: return@withContext UpdateInfo(
                isUpdateAvailable = false,
                latestVersionCode = currentVersionCode,
                latestVersionName = getCurrentVersionName(context),
                downloadUrl = null,
                releaseNotes = null
            )
            
            val json = JSONObject(responseBody)
            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "")
            val assets = json.optJSONArray("assets")
            
            // Parse version from tag (e.g., "v1.1" or "1.1")
            val versionCode = parseVersionCodeFromTag(tagName)
            val versionName = tagName.removePrefix("v")
            
            // Find APK asset
            var downloadUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }
            
            val isUpdateAvailable = versionCode > currentVersionCode
            
            UpdateInfo(
                isUpdateAvailable = isUpdateAvailable,
                latestVersionCode = versionCode,
                latestVersionName = versionName,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes
            )
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(
                isUpdateAvailable = false,
                latestVersionCode = getCurrentVersionCode(context),
                latestVersionName = getCurrentVersionName(context),
                downloadUrl = null,
                releaseNotes = null
            )
        }
    }
    
    private fun parseVersionCodeFromTag(tag: String): Int {
        // Try to parse version from tag (e.g., "v1.1" -> 11, "v1.0.1" -> 101)
        // Simple approach: remove 'v' prefix and parse as version
        val cleanTag = tag.removePrefix("v").removePrefix("V")
        val parts = cleanTag.split(".")
        
        return try {
            when (parts.size) {
                1 -> parts[0].toInt() * 10
                2 -> parts[0].toInt() * 10 + parts[1].toInt()
                3 -> parts[0].toInt() * 100 + parts[1].toInt() * 10 + parts[2].toInt()
                else -> {
                    // Fallback: try to extract first number
                    val firstNumber = cleanTag.filter { it.isDigit() }.take(3).toIntOrNull() ?: 1
                    firstNumber * 10
                }
            }
        } catch (e: Exception) {
            // If parsing fails, return a high version code to indicate update needed
            // This is a fallback - ideally tags should follow version format
            999
        }
    }
    
    suspend fun downloadApk(context: Context, downloadUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection()
            connection.connect()
            
            val inputStream = connection.getInputStream()
            val file = File(context.getExternalFilesDir(null), "update.apk")
            
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
