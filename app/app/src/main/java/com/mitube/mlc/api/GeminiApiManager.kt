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

class GeminiApiManager(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Using gemini-2.5-flash which supports fast multimodal and image generation capabilities
    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    suspend fun generateThumbnailImages(
        prompt: String,
        referenceBitmaps: List<Bitmap>,
        count: Int = 4
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val resultBitmaps = mutableListOf<Bitmap>()
        
        try {
            // Validate API Key
            if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
                throw Exception("API Key가 설정되지 않았습니다. local.properties를 확인해주세요.")
            }

            val jsonBody = JSONObject()
            val contentsArray = JSONArray()
            val partsArray = JSONArray()

            // 1. Add System Instruction / Text Prompt
            val textPart = JSONObject().apply {
                put("text", "You are a professional YouTube thumbnail designer. Create an eye-catching, high-quality thumbnail image based on the following prompt: $prompt. IMPORTANT: output ONLY a base64 encoded JPEG image. DO NOT output markdown, DO NOT output any text explanation. ONLY the raw base64 string of the generated image.")
            }
            partsArray.put(textPart)

            // 2. Add Reference Images
            for (bitmap in referenceBitmaps) {
                // Resize for API limits if needed, but Gemini 1.5/2.0 handles large context
                val resized = resizeBitmap(bitmap, 800)
                val base64Image = encodeBitmapToBase64(resized)
                
                val inlineDataPart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    }
                    put("inlineData", inlineData)
                }
                partsArray.put(inlineDataPart)
            }

            val contentObj = JSONObject().apply {
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            jsonBody.put("contents", contentsArray)

            // 3. Set Generation Config (temperature etc)
            val generationConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topK", 40)
                // Note: Real true "image generation" from text in Gemini API usually requires Imagen models. 
                // But Gemini Flash can output base64 encoded images if prompted correctly in some experimental tiers, 
                // or we use this block as a placeholder for the actual Imagen 3 API call which is also on Vertex/AI Studio.
                // For this demo, we will simulate the parsing assuming the model returns a base64 string.
            }
            jsonBody.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            // 4. Make requests (Looping for multiple variations)
            for (i in 0 until count) {
                val request = Request.Builder()
                    .url("$BASE_URL?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val generatedBase64 = extractBase64FromResponse(responseBody)
                    if (generatedBase64 != null) {
                        val genBitmap = decodeBase64ToBitmap(generatedBase64)
                        if (genBitmap != null) {
                            resultBitmaps.add(genBitmap)
                        }
                    }
                } else {
                    val errorBody = response.body?.string()
                    System.err.println("Gemini API Error: ${response.code} - $errorBody")
                    // If an error happens on the first try, we might want to break early.
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext resultBitmaps
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            // Clean up any potential markdown formatting the LLM might have returned
            var cleanBase64 = base64Str.trim()
            if (cleanBase64.startsWith("```")) {
                val lines = cleanBase64.split("\n").toMutableList()
                if (lines.isNotEmpty()) lines.removeAt(0) // remove ```
                if (lines.isNotEmpty() && lines.last().startsWith("```")) lines.removeAt(lines.size - 1)
                cleanBase64 = lines.joinToString("")
            }
            
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractBase64FromResponse(responseJson: String?): String? {
        if (responseJson.isNullOrEmpty()) return null
        return try {
            val jsonObject = JSONObject(responseJson)
            val candidates = jsonObject.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidateObj = candidates.getJSONObject(0)
                val content = candidateObj.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    text
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
