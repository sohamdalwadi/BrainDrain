package com.dalwadi.braindrain.core.ota

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class OtaRepository(private val context: Context) {

    private val githubOwner = "sohamdalwadi"
    private val githubRepo = "BrainDrain"

    suspend fun checkUpdate(channel: OtaChannel): OtaUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://raw.githubusercontent.com/$githubOwner/$githubRepo/${channel.branch}/${channel.manifestFile}"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != 200) {
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            val json = JSONObject(response)
            val remoteVersionCode = json.getInt("versionCode")
            val remoteVersionName = json.getString("versionName")
            val releaseNotes = json.optString("releaseNotes", "Performance enhancements and updates.")
            val apkUrl = json.getString("apkUrl")

            val currentVersionCode = getCurrentVersionCode()

            if (remoteVersionCode > currentVersionCode) {
                return@withContext OtaUpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = remoteVersionName,
                    releaseNotes = releaseNotes,
                    apkUrl = apkUrl,
                    channel = channel.displayName
                )
            }
        } catch (_: Exception) {
        }
        return@withContext null
    }

    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}
