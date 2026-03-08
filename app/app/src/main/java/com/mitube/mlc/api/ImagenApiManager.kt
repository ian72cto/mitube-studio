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
 * Google Imagen 3 API 매니저
 *
 * 같은 Google AI Studio API Key(gemini와 동일)를 사용하여
 * 텍스트 프롬프트 → 고품질 이미지 생성.
 *
 * 모델: imagen-3.0-generate-001
 * API 문서: https://ai.google.dev/api/generate-images
 * 무료 Tier: 분당 10 QPM (2025년 기준)
 */
class ImagenApiManager(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-001:predict"

    /**
     * 썸네일 이미지 생성
     *
     * @param prompt 이미지 설명 텍스트
     * @param sampleCount 생성할 이미지 수 (1~4, 기본값 4)
     * @return 생성된 Bitmap 리스트 (실패 시 빈 리스트)
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

        try {
            // YouTube 썸네일에 최적화된 프롬프트로 강화
            val enhancedPrompt = buildYoutubeThumbnailPrompt(prompt)

            val requestBody = JSONObject().apply {
                put("instances", JSONArray().apply {
                    put(JSONObject().apply {
                        put("prompt", enhancedPrompt)
                    })
                })
                put("parameters", JSONObject().apply {
                    put("sampleCount", sampleCount.coerceIn(1, 4))
                    put("aspectRatio", "16:9")           // YouTube 썸네일 비율
                    put("safetyFilterLevel", "block_few") // 적절한 안전 수준
                    put("personGeneration", "allow_adult") // 성인 인물 허용
                })
            }.toString()

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMessage = parseApiError(responseBody, response.code)
                return@withContext Result.failure(Exception(errorMessage))
            }

            val bitmaps = parseImageResponse(responseBody)
            Result.success(bitmaps)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * YouTube 썸네일에 최적화된 프롬프트 생성
     */
    private fun buildYoutubeThumbnailPrompt(userPrompt: String): String {
        return """YouTube thumbnail image, high quality, eye-catching, vibrant colors, 
            |professional design, 16:9 aspect ratio, 
            |$userPrompt, 
            |bold typography space, dramatic lighting, high contrast, 
            |photorealistic, 4K quality""".trimMargin()
    }

    /**
     * API 응답에서 Bitmap 리스트 추출
     */
    private fun parseImageResponse(responseJson: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val json = JSONObject(responseJson)
            val predictions = json.optJSONArray("predictions") ?: return emptyList()

            for (i in 0 until predictions.length()) {
                val prediction = predictions.getJSONObject(i)
                val base64Data = prediction.optString("bytesBase64Encoded", "")
                if (base64Data.isNotEmpty()) {
                    val bitmap = decodeBase64ToBitmap(base64Data)
                    if (bitmap != null) bitmaps.add(bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmaps
    }

    /**
     * API 에러 응답 파싱 → 사람이 읽기 쉬운 메시지로 변환
     */
    private fun parseApiError(responseJson: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseJson)
            val error = json.optJSONObject("error")
            val message = error?.optString("message", "") ?: ""
            when (statusCode) {
                400 -> "요청 오류: $message"
                401, 403 -> "API Key 인증 실패. Google AI Studio에서 유효한 키인지 확인해 주세요."
                429 -> "요청 한도 초과 (Rate Limit). 잠시 후 다시 시도해 주세요."
                500, 503 -> "Google 서버 오류. 잠시 후 다시 시도해 주세요."
                else -> "오류 ($statusCode): $message"
            }
        } catch (e: Exception) {
            "알 수 없는 오류 (HTTP $statusCode)"
        }
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
