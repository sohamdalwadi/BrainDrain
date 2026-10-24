package com.dalwadi.braindrain.core.ota

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

object ApkDownloader {
    private var activeDownloadId: Long? = null

    fun downloadAndInstall(context: Context, url: String, fileName: String) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return

        activeDownloadId?.let { id ->
            if (isDownloadRunning(manager, id)) {
                Toast.makeText(context, "Download already in progress...", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(destinationDir, fileName)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle("BrainDrain Update")
            setDescription("Downloading latest APK")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(file))
        }

        val downloadId = manager.enqueue(request)
        activeDownloadId = downloadId
        Toast.makeText(context, "Downloading update in background...", Toast.LENGTH_SHORT).show()

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    activeDownloadId = null
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {
                    }
                    installApk(context, file)
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun isDownloadRunning(manager: DownloadManager, downloadId: Long): Boolean {
        val query = DownloadManager.Query().setFilterById(downloadId)
        try {
            manager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex != -1) {
                        val status = cursor.getInt(statusIndex)
                        return status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING
                    }
                }
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun installApk(context: Context, file: File) {
        try {
            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
