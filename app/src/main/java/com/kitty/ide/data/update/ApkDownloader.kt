package com.kitty.ide.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * APK 静默下载 + 启动安装。
 */
object ApkDownloader {

    /** 下载目录：context.cacheDir/downloads/ */
    private fun downloadDir(context: Context): File {
        val dir = File(context.cacheDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 从 url 下载 APK 到缓存目录。
     * @param onProgress (0..100)，会在 IO 线程回调
     * @return 下载完成的 File，失败返回 null
     */
    suspend fun download(
        context: Context,
        url: String,
        fileName: String = "kitty-update.apk",
        onProgress: (Int) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        try {
            val target = File(downloadDir(context), fileName)
            if (target.exists()) target.delete()

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "KittyIDE-Android")
            }

            // GitHub 的下载地址会 302 跳转，需要手动处理一下
            var realConn = conn
            var redirectCount = 0
            while (realConn.responseCode in 300..399 && redirectCount < 5) {
                val location = realConn.getHeaderField("Location") ?: break
                realConn.disconnect()
                realConn = (URL(location).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "KittyIDE-Android")
                }
                redirectCount++
            }

            if (realConn.responseCode != 200) {
                realConn.disconnect()
                return@withContext null
            }

            val total = realConn.contentLengthLong
            realConn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buf = ByteArray(8192)
                    var read: Int
                    var sum = 0L
                    while (input.read(buf).also { read = it } > 0) {
                        output.write(buf, 0, read)
                        sum += read
                        if (total > 0) {
                            val pct = ((sum * 100) / total).toInt().coerceIn(0, 100)
                            onProgress(pct)
                        }
                    }
                    output.flush()
                }
            }
            realConn.disconnect()

            target
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 启动系统安装器。
     * - Android 8.0+ 需要"安装未知应用"权限，没有就跳设置页
     */
    fun installApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}