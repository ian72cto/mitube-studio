package com.mitube.mlc.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class YouTubeLiveApiManager(private val token: String) {
    private val client = OkHttpClient()
    private val TAG = "YouTubeLiveApi"

    suspend fun createLiveBroadcast(
        title: String,
        description: String,
        privacyStatus: String,
        isMadeForKids: Boolean = false,
        scheduledStartTime: String? = null, // ISO 8601 format (e.g. 2026-03-09T03:55:00Z)
        enableDvr: Boolean = true,
        is360: Boolean = false,
        latencyPreference: String = "normal", // normal, low, ultraLow
        enableChat: Boolean = true,
        enableChatReplay: Boolean = true,
        enableEmbed: Boolean = true,
        tags: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/youtube/v3/liveBroadcasts?part=snippet,status,contentDetails"

            val snippet = JSONObject().apply {
                put("title", title)
                put("description", description)
                if (scheduledStartTime != null) {
                    put("scheduledStartTime", scheduledStartTime)
                } else {
                    // Start immediately if not scheduled
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                    format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    put("scheduledStartTime", format.format(java.util.Date()))
                }
            }

            val status = JSONObject().apply {
                put("privacyStatus", privacyStatus)
                put("selfDeclaredMadeForKids", isMadeForKids)
            }

            val contentDetails = JSONObject().apply {
                put("enableDvr", enableDvr)
                put("enableEmbed", enableEmbed)
                put("recordFromStart", true)
                put("startWithSlate", false)
                put("latencyPreference", latencyPreference)
                put("enableAutoStart", false)
                put("enableAutoStop", false)
                put("closedCaptionsType", "closedCaptionsDisabled")
                put("is360", is360)
            }

            val requestBodyJson = JSONObject().apply {
                put("snippet", snippet)
                put("status", status)
                put("contentDetails", contentDetails)
            }

            Log.d(TAG, "Creating Broadcast with payload: $requestBodyJson")

            val body = requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body?.string()

            if (response.isSuccessful && responseData != null) {
                val json = JSONObject(responseData)
                val broadcastId = json.getString("id")
                Log.d(TAG, "Broadcast created successfully. ID: $broadcastId")
                
                // Note: Chat settings are technically part of the LiveChat API, but we can only manipulate them 
                // after the broadcast is created and liveChatId is known, or via YouTube studio UI.
                
                return@withContext broadcastId
            } else {
                Log.e(TAG, "Failed to create broadcast: $responseData")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating broadcast: ${e.message}")
            return@withContext null
        }
    }

    suspend fun createLiveStream(
        title: String,
        resolution: String = "1080p", // Options: 1080p, 720p, 480p, etc.
        frameRate: String = "30fps" // Options: 30fps, 60fps
    ): LiveStreamInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/youtube/v3/liveStreams?part=snippet,cdn,contentDetails"

            val snippet = JSONObject().apply {
                put("title", title)
            }

            val cdn = JSONObject().apply {
                put("resolution", resolution)
                put("frameRate", frameRate)
                put("ingestionType", "rtmp")
            }

            val requestBodyJson = JSONObject().apply {
                put("snippet", snippet)
                put("cdn", cdn)
            }

            val body = requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body?.string()

            if (response.isSuccessful && responseData != null) {
                val json = JSONObject(responseData)
                val streamId = json.getString("id")
                val ingestionInfo = json.getJSONObject("cdn").getJSONObject("ingestionInfo")
                val rtmpUrl = ingestionInfo.getString("ingestionAddress")
                val streamName = ingestionInfo.getString("streamName") // This is the stream key
                
                Log.d(TAG, "Stream created successfully. ID: $streamId")
                return@withContext LiveStreamInfo(streamId, rtmpUrl, streamName)
            } else {
                Log.e(TAG, "Failed to create stream: $responseData")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating stream: ${e.message}")
            return@withContext null
        }
    }

    suspend fun bindBroadcastToStream(broadcastId: String, streamId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/youtube/v3/liveBroadcasts/bind?id=$broadcastId&part=id,contentDetails&streamId=$streamId"
            
            // Empty post body
            val body = ByteArray(0).toRequestBody(null, 0, 0)
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body?.string()

            if (response.isSuccessful) {
                Log.d(TAG, "Successfully bound broadcast $broadcastId to stream $streamId")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to bind broadcast: $responseData")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception binding broadcast: ${e.message}")
            return@withContext false
        }
    }
    
    suspend fun transitionBroadcast(broadcastId: String, broadcastStatus: String): Boolean = withContext(Dispatchers.IO) {
        // broadcastStatus: "testing", "live", "complete"
        try {
            val url = "https://www.googleapis.com/youtube/v3/liveBroadcasts/transition?broadcastStatus=$broadcastStatus&id=$broadcastId&part=id,status"
            
            val body = ByteArray(0).toRequestBody(null, 0, 0)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body?.string()

            if (response.isSuccessful) {
                Log.d(TAG, "Successfully transitioned broadcast $broadcastId to $broadcastStatus")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to transition broadcast: $responseData")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception transitioning broadcast: ${e.message}")
            return@withContext false
        }
    }

    /**
     * 라이브 중 채팅 설정 변경을 위한 scaffolding 메서드.
     * 주의: 현재 YouTube Data API v3 에서는 '저속 모드(Slow Mode)' 나 '구독자 전용(Subscriber Only)'을
     * 실시간으로 변경하는 공식 API 엔드포인트가 제공되지 않으며, YouTube Studio 엡/웹에서만 가능합니다.
     * 따라서 이 메서드는 향후 API 추가 시 연동을 위한 자리 표시자(Placeholder)로 작동합니다.
     */
    suspend fun updateLiveChatSettings(
        broadcastId: String,
        participantMode: String,
        allowReactions: Boolean,
        slowModeSeconds: Int
    ): Boolean = withContext(Dispatchers.IO) {
        Log.w(TAG, "updateLiveChatSettings: YouTube API v3 does not natively support mid-stream slow-mode/subscriber-only updates yet.")
        Log.d(TAG, "Requested Updates - Broadcast: $broadcastId, Mode: $participantMode, Reactions: $allowReactions, SlowMode: $slowModeSeconds")
        
        // TODO: 공식 Data API가 이를 지원하게 되면 여기에 PUT/PATCH 요청을 구현.
        // 현재는 앱 내 UX 상 무조건 성공(true)으로 반환하여 UI 흐름을 막지 않음.
        return@withContext true
    }

    suspend fun listBroadcasts(): List<BroadcastItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<BroadcastItem>()
        try {
            val url = "https://www.googleapis.com/youtube/v3/liveBroadcasts?part=snippet,status&broadcastStatus=upcoming"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body?.string()

            if (response.isSuccessful && responseData != null) {
                val json = JSONObject(responseData)
                val items = json.optJSONArray("items") ?: return@withContext emptyList()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val id = item.getString("id")
                    val snippet = item.getJSONObject("snippet")
                    val title = snippet.getString("title")
                    val scheduledStartTime = snippet.optString("scheduledStartTime", "")
                    
                    val thumbnails = snippet.optJSONObject("thumbnails")
                    val defaultThumb = thumbnails?.optJSONObject("default")?.optString("url", "") ?: ""

                    result.add(BroadcastItem(id, title, scheduledStartTime, defaultThumb))
                }
            } else {
                Log.e(TAG, "Failed to list broadcasts: $responseData")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception listing broadcasts: ${e.message}")
        }
        return@withContext result
    }
}

data class LiveStreamInfo(
    val id: String,
    val rtmpUrl: String,
    val streamKey: String
)

data class BroadcastItem(
    val id: String,
    val title: String,
    val scheduledStartTime: String,
    val thumbnailUrl: String
)
