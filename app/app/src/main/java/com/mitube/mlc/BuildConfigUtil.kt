package com.mitube.mlc

import android.content.Context

object BuildConfigUtil {
    fun getGeminiApiKey(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
            appInfo.metaData?.getString("GEMINI_API_KEY") ?: "YOUR_GEMINI_API_KEY_HERE"
        } catch (e: Exception) {
            "YOUR_GEMINI_API_KEY_HERE"
        }
    }
}
