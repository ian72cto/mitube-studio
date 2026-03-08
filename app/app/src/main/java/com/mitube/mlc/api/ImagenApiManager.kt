package com.mitube.mlc.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * AI 이미지 생성 매니저
 *
 * Pollinations.ai 무료 서비스 사용 (API 키 불필요)
 * YouTube 썸네일 최적화: 1280×720 (16:9), Flux 모델
 *
 * URL: https://image.pollinations.ai/prompt/{prompt}?width=1280&height=720
 */
class ImagenApiManager(private val apiKey: String = "") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * YouTube 썸네일 이미지 생성
     *
     * @param prompt 이미지 설명 텍스트 (한글 가능)
     * @param sampleCount 생성할 이미지 수 (1~5)
     * @return 생성된 Bitmap 리스트
     */
    suspend fun generateImages(
        prompt: String,
        sampleCount: Int = 4
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val count = sampleCount.coerceIn(1, 5)
            val enhancedPrompt = buildYoutubeThumbnailPrompt(prompt)
            val encodedPrompt = URLEncoder.encode(enhancedPrompt, "UTF-8")
            val bitmaps = mutableListOf<Bitmap>()

            repeat(count) { index ->
                // seed를 다르게 해서 다양한 이미지 생성
                val seed = (System.currentTimeMillis() + index * 1000).toInt()
                val url = "https://image.pollinations.ai/prompt/$encodedPrompt" +
                        "?width=1280&height=720&model=flux&seed=$seed&nologo=true"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?.let { bitmaps.add(it) }
                    }
                }
            }

            if (bitmaps.isEmpty()) {
                Result.failure(Exception("이미지 생성에 실패했습니다.\n네트워크 연결을 확인해 주세요."))
            } else {
                Result.success(bitmaps)
            }
        } catch (e: Exception) {
            Result.failure(Exception("이미지 생성 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    /**
     * YouTube 썸네일에 최적화된 영문 프롬프트 생성
     */
    private fun buildYoutubeThumbnailPrompt(userPrompt: String): String {
        return "YouTube thumbnail, professional, eye-catching, vibrant colors, " +
                "16:9 ratio, dramatic lighting, bold design, high quality, " +
                "$userPrompt"
    }
}
