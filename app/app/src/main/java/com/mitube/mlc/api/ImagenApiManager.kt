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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Google AI 이미지 생성 매니저
 *
 * AI Studio API Key(gemini와 동일)를 사용하여
 * 텍스트 프롬프트 → 고품질 이미지 생성.
 *
 * 1차 시도: Imagen 3 (imagen-3.0-generate-001)
 * 폴백: Gemini 2.0 Flash 이미지 생성
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
     * Imagen 3 → 실패 시 Gemini Flash 이미지 생성 폴백
     *
     * @param prompt 이미지 설명 텍스트
     * @param sampleCount 생성할 이미지 수 (1~4, 기본값 4)
     * @return 생성된 Bitmap 리스트
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

        // 1차 시도: Imagen 3
        val imagenResult = tryImagen3(enhancedPrompt, sampleCount)
        if (imagenResult.isSuccess && imagenResult.getOrNull()?.isNotEmpty() == true) {
            return@withContext imagenResult
        }

        // 폴백: Gemini Flash 이미지 생성
        return@withContext tryGeminiImageGeneration(enhancedPrompt, sampleCount)
    }

    /**
     * Imagen 3 시도 (단순화된 파라미터)
     */
    private suspend fun tryImagen3(
        prompt: String,
        sampleCount: Int
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/imagen-3.0-generate-001:predict?key=$apiKey"

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
                return@withContext Result.failure(Exception("Imagen 404: $responseBody"))
            }

            val bitmaps = parseImagenResponse(responseBody)
            Result.success(bitmaps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gemini 2.0 Flash 이미지 생성 (폴백)
     * generateContent 엔드포인트 사용 — AI Studio 키로 확실히 동작
     */
    private suspend fun tryGeminiImageGeneration(
        prompt: String,
        sampleCount: Int
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val bitmaps = mutableListOf<Bitmap>()
            val count = sampleCount.coerceIn(1, 4)

            // Gemini는 1번에 1장 생성 → count 만큼 반복
            repeat(count) {
                val url = "$BASE_URL/gemini-2.0-flash-preview-image-generation:generateContent?key=$apiKey"

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("IMAGE")
                            put("TEXT")
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
                    val bitmap = parseGeminiImageResponse(responseBody)
                    if (bitmap != null) bitmaps.add(bitmap)
                }
            }

            if (bitmaps.isEmpty()) {
                Result.failure(Exception("이미지 생성에 실패했습니다.\nAPI Key가 올바른지 확인하거나 잠시 후 다시 시도해 주세요."))
            } else {
                Result.success(bitmaps)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * YouTube 썸네일에 최적화된 프롬프트 생성
     */
    private fun buildYoutubeThumbnailPrompt(userPrompt: String): String {
        return "YouTube thumbnail image, high quality, eye-catching, vibrant colors, " +
                "professional design, 16:9 aspect ratio, $userPrompt, " +
                "bold typography space, dramatic lighting, high contrast, photorealistic"
    }

    /**
     * Imagen API 응답 파싱
     */
    private fun parseImagenResponse(responseJson: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val json = JSONObject(responseJson)
            val predictions = json.optJSONArray("predictions") ?: return emptyList()
            for (i in 0 until predictions.length()) {
                val prediction = predictions.getJSONObject(i)
                val base64Data = prediction.optString("bytesBase64Encoded", "")
                if (base64Data.isNotEmpty()) {
                    decodeBase64ToBitmap(base64Data)?.let { bitmaps.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmaps
    }

    /**
     * Gemini generateContent 응답에서 이미지 추출
     */
    private fun parseGeminiImageResponse(responseJson: String): Bitmap? {
        return try {
            val json = JSONObject(responseJson)
            val candidates = json.optJSONArray("candidates") ?: return null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData") ?: continue
                val base64Data = inlineData.optString("data", "")
                if (base64Data.isNotEmpty()) {
                    return decodeBase64ToBitmap(base64Data)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
