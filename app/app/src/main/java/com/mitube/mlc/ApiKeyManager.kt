package com.mitube.mlc

import android.content.Context
import androidx.core.content.edit

/**
 * Gemini/Imagen API Key를 SharedPreferences에 안전하게 저장/읽기
 *
 * 개발: local.properties → BuildConfigUtil (메타데이터)
 * 배포: 앱 내 설정 화면 → ApiKeyManager 저장
 *
 * 우선순위: SharedPreferences > meta-data (local.properties)
 */
object ApiKeyManager {

    private const val PREF_NAME = "mitube_secure_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_GEMINI_API_KEY, key.trim())
        }
    }

    fun getApiKey(context: Context): String {
        val saved = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GEMINI_API_KEY, "") ?: ""

        // SharedPreferences에 있으면 우선 사용
        if (saved.isNotEmpty()) return saved

        // 없으면 local.properties에서 읽기 (개발용)
        return BuildConfigUtil.getGeminiApiKey(context)
    }

    fun hasApiKey(context: Context): Boolean {
        val key = getApiKey(context)
        return key.isNotEmpty() && key != "YOUR_GEMINI_API_KEY_HERE"
    }

    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_GEMINI_API_KEY)
        }
    }
}
