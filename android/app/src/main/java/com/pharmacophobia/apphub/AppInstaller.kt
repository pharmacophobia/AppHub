package com.pharmacophobia.apphub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppInstaller(private val context: Context) {

    suspend fun downloadApk(
        app: AppItem,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var currentUrl = app.apkUrl
        val destination = File(context.cacheDir, "${app.id}.apk")
        if (destination.exists()) destination.delete()

        try {
            var redirects = 0
            while (redirects < 5) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "AppHub-Client/1.0")
                conn.connectTimeout = 12000
                conn.readTimeout = 30000
                conn.connect()

                val code = conn.responseCode
                if (code in 300..399) {
                    val newLoc = conn.getHeaderField("Location")
                    if (newLoc != null) {
                        currentUrl = newLoc
                        redirects++
                        continue
                    }
                }

                if (code in 200..299) {
                    val fileLength = conn.contentLength
                    conn.inputStream.use { input ->
                        FileOutputStream(destination).use { output ->
                            val buffer = ByteArray(8192)
                            var total: Long = 0
                            var count: Int
                            while (input.read(buffer).also { count = it } != -1) {
                                total += count
                                output.write(buffer, 0, count)
                                if (fileLength > 0) {
                                    val pct = ((total * 100) / fileLength).toInt()
                                    withContext(Dispatchers.Main) {
                                        onProgress(pct)
                                    }
                                }
                            }
                            output.flush()
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onProgress(100)
                    }
                    return@withContext destination
                } else {
                    return@withContext null
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun installApk(file: File) {
        if (!file.exists()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permIntent)
                    Toast.makeText(context, "Please allow AppHub to install apps, then tap Install again.", Toast.LENGTH_LONG).show()
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                Toast.makeText(context, "Could not launch $packageName", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening app: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openBrowserLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {}
    }
}
