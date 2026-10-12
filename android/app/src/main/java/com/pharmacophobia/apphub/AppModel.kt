package com.pharmacophobia.apphub

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppItem(
    val id: String,
    val name: String,
    val packageName: String,
    val version: String,
    val category: String,
    val description: String,
    val icon: String,
    val colorHex: String,
    val githubRepo: String,
    val apkUrl: String,
    val releaseUrl: String,
    val tags: List<String> = emptyList(),
    val installedVersion: String? = null,
    val isInstalled: Boolean = installedVersion != null,
    val hasUpdate: Boolean = isInstalled && isVersionNewer(version, installedVersion ?: "")
)

fun isVersionNewer(latest: String, current: String): Boolean {
    val l = latest.removePrefix("v").trim()
    val c = current.removePrefix("v").trim()
    if (l.isEmpty() || c.isEmpty()) return false
    val lParts = l.split(".").mapNotNull { it.toIntOrNull() }
    val cParts = c.split(".").mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(lParts.size, cParts.size)) {
        val lp = lParts.getOrElse(i) { 0 }
        val cp = cParts.getOrElse(i) { 0 }
        if (lp > cp) return true
        if (lp < cp) return false
    }
    return false
}

object AppRepository {

    private val FALLBACK_APPS = listOf(
        AppItem(
            id = "airbridge",
            name = "AirBridge",
            packageName = "com.airbridge.app",
            version = "1.0.0",
            category = "Audio & Productivity",
            description = "Seamless Phone-to-PC Companion: Low-latency virtual microphone, instant image vault with auto-clipboard sync, and GPU AI transcription.",
            icon = "⚡",
            colorHex = "#06b6d4",
            githubRepo = "pharmacophobia/AirBridge",
            apkUrl = "https://github.com/pharmacophobia/AirBridge/raw/main/AirBridge.apk",
            releaseUrl = "https://github.com/pharmacophobia/AirBridge/releases/latest",
            tags = listOf("Virtual Mic", "Clipboard", "Images", "Whisper AI", "Companion")
        ),
        AppItem(
            id = "omnivault",
            name = "OmniVault",
            packageName = "com.omnivault",
            version = "1.1.0",
            category = "Security",
            description = "Zero-Knowledge AI Password Manager with Raspberry Pi Pico USB Typer, local neural OCR, and biometric unlock.",
            icon = "🔐",
            colorHex = "#89b4fa",
            githubRepo = "pharmacophobia/OmniVault",
            apkUrl = "https://github.com/pharmacophobia/OmniVault/raw/main/OmniVault.apk",
            releaseUrl = "https://github.com/pharmacophobia/OmniVault/releases/latest",
            tags = listOf("Passwords", "Pico Typer", "Security", "Biometrics")
        ),
        AppItem(
            id = "privacyguard",
            name = "PrivacyGuard",
            packageName = "com.privacyguard",
            version = "1.0.0",
            category = "Privacy",
            description = "Open-source Android system auditor, permission inspector, tracker blocker, and network privacy dashboard.",
            icon = "🛡️",
            colorHex = "#a6e3a1",
            githubRepo = "pharmacophobia/PrivacyGuard",
            apkUrl = "https://github.com/pharmacophobia/PrivacyGuard/raw/main/PrivacyGuard.apk",
            releaseUrl = "https://github.com/pharmacophobia/PrivacyGuard/releases/latest",
            tags = listOf("Permissions", "Firewall", "Audit", "Privacy")
        ),
        AppItem(
            id = "voxmind",
            name = "VoxMind",
            packageName = "com.voxmind.app",
            version = "1.0.0",
            category = "AI",
            description = "Completely offline, local on-device AI voice assistant and agent executor powered by neural speech models.",
            icon = "🧠",
            colorHex = "#cba6f7",
            githubRepo = "pharmacophobia/VoxMind",
            apkUrl = "https://github.com/pharmacophobia/VoxMind/raw/main/VoxMind.apk",
            releaseUrl = "https://github.com/pharmacophobia/VoxMind/releases/latest",
            tags = listOf("AI", "Voice", "Offline", "LLM")
        ),
        AppItem(
            id = "guitartuner",
            name = "Guitar Tuner Pro",
            packageName = "com.guitartuner.app",
            version = "1.0.0",
            category = "Audio",
            description = "High-precision strobe audio guitar & instrument tuner with real-time FFT pitch analysis and custom alternate tunings.",
            icon = "🎸",
            colorHex = "#fab387",
            githubRepo = "pharmacophobia/GuitarTuner",
            apkUrl = "https://github.com/pharmacophobia/GuitarTuner/raw/main/GuitarTuner.apk",
            releaseUrl = "https://github.com/pharmacophobia/GuitarTuner/releases/latest",
            tags = listOf("Music", "Tuner", "Audio", "DSP")
        ),
        AppItem(
            id = "featherfling",
            name = "FeatherFling",
            packageName = "com.stealthsms.app",
            version = "1.0.0",
            category = "Communication",
            description = "Carrier-proof lightweight stealth communication client and local P2P encrypted messaging channel.",
            icon = "🪶",
            colorHex = "#f9e2af",
            githubRepo = "pharmacophobia/FeatherFeed",
            apkUrl = "https://github.com/pharmacophobia/FeatherFeed/raw/main/FeatherFling.apk",
            releaseUrl = "https://github.com/pharmacophobia/FeatherFeed/releases/latest",
            tags = listOf("SMS", "Stealth", "Feed", "P2P")
        ),
        AppItem(
            id = "moonlightvr",
            name = "Moonlight VR",
            packageName = "com.pharmacophobia.moonlightvr",
            version = "1.0.0",
            category = "Gaming & VR",
            description = "Ultra-low-latency PC game and desktop streaming client built for Android VR headsets and mobile screens.",
            icon = "🥽",
            colorHex = "#74c7ec",
            githubRepo = "pharmacophobia/MoonlightVR-Unity",
            apkUrl = "https://github.com/pharmacophobia/MoonlightVR-Unity/raw/main/MoonlightVR-Unity.apk",
            releaseUrl = "https://github.com/pharmacophobia/MoonlightVR-Unity/releases/latest",
            tags = listOf("VR", "Streaming", "Moonlight", "Gaming")
        ),
        AppItem(
            id = "seedcipher",
            name = "SeedCipher",
            packageName = "com.seedcipher.app",
            version = "1.0.0",
            category = "Security",
            description = "Encrypted SMS and steganography encoder for sensitive crypto seeds, mnemonic phrases, and private keys.",
            icon = "🌱",
            colorHex = "#94e2d5",
            githubRepo = "pharmacophobia/SeedCipher",
            apkUrl = "https://github.com/pharmacophobia/SeedCipher/raw/main/SeedCipher.apk",
            releaseUrl = "https://github.com/pharmacophobia/SeedCipher/releases/latest",
            tags = listOf("Crypto", "Seed", "Steganography", "SMS")
        )
    )

    fun getInstalledVersion(context: Context, packageName: String): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadApps(context: Context): List<AppItem> = withContext(Dispatchers.IO) {
        val baseApps = mutableListOf<AppItem>()

        // 1. Try remote GitHub feed
        try {
            val url = URL("https://raw.githubusercontent.com/pharmacophobia/AppHub/main/apps.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 6000
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val jsonStr = reader.use { it.readText() }
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tagsArr = obj.optJSONArray("tags")
                    val tagsList = mutableListOf<String>()
                    if (tagsArr != null) {
                        for (j in 0 until tagsArr.length()) {
                            tagsList.add(tagsArr.getString(j))
                        }
                    }
                    baseApps.add(
                        AppItem(
                            id = obj.optString("id", "app_$i"),
                            name = obj.optString("name", "App"),
                            packageName = obj.optString("packageName", ""),
                            version = obj.optString("version", "1.0.0"),
                            category = obj.optString("category", "Tools"),
                            description = obj.optString("description", ""),
                            icon = obj.optString("icon", "📱"),
                            colorHex = obj.optString("color", "#89b4fa"),
                            githubRepo = obj.optString("githubRepo", ""),
                            apkUrl = obj.optString("apkUrl", ""),
                            releaseUrl = obj.optString("releaseUrl", ""),
                            tags = tagsList
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback to built-in list
        }

        val sourceList = if (baseApps.isNotEmpty()) baseApps else FALLBACK_APPS

        // Map with live installed version detection from Android system
        sourceList.map { app ->
            val installedVer = getInstalledVersion(context, app.packageName)
            app.copy(
                installedVersion = installedVer,
                isInstalled = installedVer != null,
                hasUpdate = installedVer != null && isVersionNewer(app.version, installedVer)
            )
        }
    }
}
