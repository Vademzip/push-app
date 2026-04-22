package com.pushapp.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import com.pushapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object AppUpdater {

    /**
     * URL до файла version.json на GitHub (raw).
     *
     * Формат version.json:
     * {
     *   "versionCode": 2,
     *   "versionName": "0.0.6",
     *   "downloadUrl": "https://github.com/ВАШ_НИК/pushapp/releases/download/v0.0.6/app-release.apk",
     *   "releaseNotes": "Что нового"
     * }
     */
    private const val VERSION_URL =
        "https://raw.githubusercontent.com/Vademzip/push-app/master/version.json"

    /** Возвращает UpdateInfo если доступна новая версия, иначе null. */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val text = URL(VERSION_URL).readText()
            val json = JSONObject(text)
            val remoteCode = json.getInt("versionCode")
            if (remoteCode > BuildConfig.VERSION_CODE) {
                UpdateInfo(
                    versionCode  = remoteCode,
                    versionName  = json.getString("versionName"),
                    downloadUrl  = json.getString("downloadUrl"),
                    releaseNotes = json.optString("releaseNotes", "")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Запускает скачивание APK. Возвращает downloadId для отслеживания прогресса.
     * По завершении автоматически открывает установщик.
     */
    fun downloadAndInstall(context: Context, info: UpdateInfo): Long {
        val fileName = "pushapp-${info.versionName}.apk"

        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("PushApp ${info.versionName}")
            .setDescription("Скачивание обновления…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(DownloadManager::class.java)
        val downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                ctx.unregisterReceiver(this)

                val uri = dm.getUriForDownloadedFile(downloadId) ?: return
                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(install)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        return downloadId
    }

    /** Опрашивает DownloadManager и возвращает прогресс от 0f до 1f, или null если завершено/ошибка. */
    fun getDownloadProgress(context: Context, downloadId: Long): Float? {
        val dm = context.getSystemService(DownloadManager::class.java)
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = dm.query(query) ?: return null
        return try {
            if (!cursor.moveToFirst()) return null
            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(statusCol)
            if (status == DownloadManager.STATUS_SUCCESSFUL) return null
            if (status == DownloadManager.STATUS_FAILED) return null

            val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val downloadedCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val total = cursor.getLong(totalCol)
            val downloaded = cursor.getLong(downloadedCol)
            if (total <= 0) 0f else (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } finally {
            cursor.close()
        }
    }
}
