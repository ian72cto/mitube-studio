package com.mitube.mlc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ChannelRepository {
    private const val PREFS_NAME = "MLC_Channel_Prefs"
    private const val KEY_CHANNELS = "saved_channels"

    fun saveChannel(context: Context, newChannel: YouTubeChannel) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val channelsJsonString = prefs.getString(KEY_CHANNELS, "[]")
        
        val jsonArray = try { JSONArray(channelsJsonString) } catch (e: Exception) { JSONArray() }
        
        // 중복 채널이 있다면 제거 (업데이트 효과)
        val updatedArray = JSONArray()
        try {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") != newChannel.id) {
                    updatedArray.put(obj)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        // 새 채널 정보 추가
        val newObj = JSONObject().apply {
            put("id", newChannel.id)
            put("title", newChannel.title)
            put("thumbnailUrl", newChannel.thumbnailUrl)
            put("token", newChannel.token)
        }
        updatedArray.put(newObj)
        
        prefs.edit().putString(KEY_CHANNELS, updatedArray.toString()).apply()
    }

    fun removeChannel(context: Context, channelId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val channelsJsonString = prefs.getString(KEY_CHANNELS, "[]")
        val jsonArray = try { JSONArray(channelsJsonString) } catch (e: Exception) { JSONArray() }

        val updatedArray = JSONArray()
        try {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") != channelId) {
                    updatedArray.put(obj)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        prefs.edit().putString(KEY_CHANNELS, updatedArray.toString()).apply()
    }

    fun getSavedChannels(context: Context): List<YouTubeChannel> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val channelsJsonString = prefs.getString(KEY_CHANNELS, "[]")
        
        val list = mutableListOf<YouTubeChannel>()
        try {
            val jsonArray = JSONArray(channelsJsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    YouTubeChannel(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        thumbnailUrl = obj.getString("thumbnailUrl"),
                        token = obj.optString("token", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
