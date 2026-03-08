package com.mitube.mlc.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Google AI 이미지 생성 매니저
 *
 * 1차: Imagen 4.0 (imagen-4.0-generate-001)
 * 폴백: Gemini 2.0 Flash 이미지 생성 (gemini-2.0-flash-exp-image-generation)
 *
 * 같은 Google AI Studio API Key 사용 (무료 Tier 가능)
 */
class ImagenApiManager(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * 썸네일 이미지 생성
     * Imagen 4.0 → 실패 시 Gemini Flash 이미지 생성 폴백
     */
    suspend fun generateImages(
        prompt: String,
        sampleCount: Int = 4
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            return@withContext Result.failure(
                Exception("API Key가 설정되지 않았습니다.\nGoogle AI Studio(aistudio.google.com)에서 무료 키를 발급 받아\n설정 화면에 입력해 주세요.")
            )
        }

        val enhancedPrompt = buildYoutubeThumbnailPrompt(prompt)

        // 1차 시도: Imagen 4.0
        val imagenResult = tryImagen4(enhancedPrompt, sampleCount)
        if (imagenResult.isSuccess && imagenResult.getOrNull()?.isNotEmpty() == true) {
            return@withContext imagenResult
        }

        // 폴백: Gemini 2.0 Flash 이미지 생성
        return@withContext tryGeminiFlashImage(enhancedPrompt, sampleCount)
    }

    /**
     * Imagen 4.0 이미지 생성
     * 엔드포인트: imagen-4.0-generate-001:predict
     */
    private suspend fun tryImagen4(
        prompt: String,
        sampleCount: Int
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/imagen-4.0-generate-001:predict?key=$apiKey"

            val requestBody = JSONObject().apply {
                put("instances", JSONArray().apply {
                    put(JSONObject().apply {
                        put("prompt", prompt)
                    })
                })
                put("parameters", JSONObject().apply {
                    put("sampleCount", sampleCount.coerceIn(1, 4))
                })
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Imagen4 ${response.code}: $responseBody"))
            }

            val bitmaps = parseImagenResponse(responseBody)
            if (bitmaps.isEmpty()) Result.failure(Exception("Imagen4: empty result"))
            else Result.success(bitmaps)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gemini 2.0 Flash 이미지 생성 (폴백)
     * 엔드포인트: gemini-2.0-flash-exp-image-generation:generateContent
     */
    private suspend fun tryGeminiFlashImage(
        prompt: String,
        sampleCount: Int
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val bitmaps = mutableListOf<Bitmap>()
            val count = sampleCount.coerceIn(1, 4)
            var lastError = ""

            repeat(count) {
                val url = "$BASE_URL/gemini-2.0-flash-exp-image-generation:generateContent?key=$apiKey"

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("TEXT")
                            put("IMAGE")
                        })
                    })
                }.toString()

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    parseGeminiImageResponse(responseBody)?.let { bitmaps.add(it) }
                } else {
                    lastError = "Gemini ${response.code}: $responseBody"
                }
            }

            if (bitmaps.isEmpty()) {
                Result.failure(Exception("이미지 생성 실패\n$lastError"))
            } else {
                Result.success(bitmaps)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** YouTube 썸네일 최적화 프롬프트 */
    private fun buildYoutubeThumbnailPrompt(userPrompt: String): String =
        "YouTube thumbnail, professional, high quality, vibrant colors, eye-catching, 16:9 ratio, " +
                "$userPrompt, dramatic lighting, bold design"

    /** Imagen predict 응답 파싱 */
    private fun parseImagenResponse(responseJson: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val json = JSONObject(responseJson)
            val predictions = json.optJSONArray("predictions") ?: return emptyList()
            for (i in 0 until predictions.length()) {
                val b64 = predictions.getJSONObject(i).optString("bytesBase64Encoded", "")
                if (b64.isNotEmpty()) decodeBase64ToBitmap(b64)?.let { bitmaps.add(it) }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return bitmaps
    }

    /** Gemini generateContent 응답에서 이미지 추출 */
    private fun parseGeminiImageResponse(responseJson: String): Bitmap? {
        return try {
            val json = JSONObject(responseJson)
            val parts = json
                .optJSONArray("candidates")
                ?.getJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts") ?: return null

            for (i in 0 until parts.length()) {
                val b64 = parts.getJSONObject(i)
                    .optJSONObject("inlineData")
                    ?.optString("data", "") ?: continue
                if (b64.isNotEmpty()) return decodeBase64ToBitmap(b64)
            }
            null
        } catch (e: Exception) { null }
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }
}
