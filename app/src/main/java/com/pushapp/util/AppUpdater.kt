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

    /** Запускает скачивание APK через системный DownloadManager и устанавливает по завершении. */
    fun downloadAndInstall(context: Context, info: UpdateInfo) {
        val fileName = "pushapp-${info.versionName}.apk"

        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("PushApp ${info.versionName}")
            .setDescription("Скачивание обновления…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(DownloadManager::class.java)
        val downloadId = dm.enqueue(request)

        // Слушаем завершение скачивания
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
    }
}
