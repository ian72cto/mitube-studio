package com.mitube.mlc

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mitube.mlc.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appAuthManager: YouTubeAppAuthManager
    private val channelList = mutableListOf<YouTubeChannel>()
    private lateinit var channelAdapter: ChannelAdapter

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            binding.tvLoginStatus.text = "권한 승인 됨. 토큰 발급 중..."
            appAuthManager.handleAuthorizationResponse(result.data!!) { token ->
                if (token != null) {
                    binding.tvLoginStatus.text = "토큰 발급 완료. 채널 정보 요청 중..."
                    fetchYouTubeChannels(token)
                } else {
                    binding.tvLoginStatus.text = "오류: 액세스 토큰을 받아오지 못했습니다."
                    Log.e("YouTubeAuth", "Failed to get access token from AppAuth")
                }
            }
        } else {
            binding.tvLoginStatus.text = "로그인 취소됨"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appAuthManager = YouTubeAppAuthManager(this)

        // 어댑터 설정 (채널 클릭 / X 버튼 로그아웃)
        channelAdapter = ChannelAdapter(
            channelList,
            onChannelClick = { selectedChannel ->
                binding.tvLoginStatus.text = "선택된 채널: ${selectedChannel.title}\n(${selectedChannel.id})"
                val intent = android.content.Intent(this@MainActivity, LiveReservationActivity::class.java).apply {
                    putExtra("channel_token", selectedChannel.token)
                    putExtra("channel_id", selectedChannel.id)
                }
                startActivity(intent)
            },
            onRemoveClick = { targetChannel ->
                try {
                    ChannelRepository.removeChannel(this, targetChannel.id)
                    val remaining = ChannelRepository.getSavedChannels(this)
                    channelList.clear()
                    channelList.addAll(remaining)
                    channelAdapter.notifyDataSetChanged()

                    if (remaining.isEmpty()) {
                        binding.tvSelectChannel.visibility = android.view.View.GONE
                        binding.tvLoginStatus.text = "로그인 대기 중..."
                    } else {
                        binding.tvLoginStatus.text = "${targetChannel.title} 채널 연동 해제됨"
                    }
                    Log.d("YouTubeAPI", "Channel removed: ${targetChannel.id}. Remaining: ${remaining.size}")
                } catch (e: Exception) {
                    binding.tvLoginStatus.text = "채널 해제 중 에러: ${e.message}"
                    Log.e("MainActivity", "Error during channel removal", e)
                }
            }
        )
        binding.rvChannels.adapter = channelAdapter

        // 저장된 채널 리스트 불러오기 (멀티 로그인)
        val savedChannels = ChannelRepository.getSavedChannels(this)
        if (savedChannels.isNotEmpty()) {
            channelList.addAll(savedChannels)
            channelAdapter.notifyDataSetChanged()
            binding.tvSelectChannel.visibility = android.view.View.VISIBLE
        }

        binding.btnYoutubeLogin.setOnClickListener {
            signInLauncher.launch(appAuthManager.getAuthIntent())
        }

        // C++ NDK 엔진 연결 테스트 (기존)
        // val engineReadyText = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'mlc' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'mlc' library on application startup.
        init {
            System.loadLibrary("mlc")
        }
    }

    private fun fetchYouTubeChannels(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                var totalSaved = 0

                // 1. 선택된 계정/브랜드 채널 조회 (mine=true)
                // prompt=select_account를 통해 선택한 단일 채널이 반환됩니다.
                totalSaved += fetchAndSaveChannels(
                    client, token,
                    "https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true",
                    "mine"
                )

                // 2. UI 업데이트 (기기에 저장된 "모든" 채널 불러와 어댑터 갱신)
                withContext(Dispatchers.Main) {
                    val allSavedChannels = ChannelRepository.getSavedChannels(this@MainActivity)
                    Log.d("YouTubeAPI", "Total saved channels in storage: ${allSavedChannels.size}")

                    if (allSavedChannels.isEmpty()) {
                        binding.tvLoginStatus.text = "인증 성공.\n(하지만 이 계정에 연결된 유튜브 채널이 없습니다)"
                    } else {
                        binding.tvLoginStatus.text = "계정 연동 완료! 총 ${allSavedChannels.size}개 채널 목록에 추가되었습니다."
                        binding.tvSelectChannel.visibility = android.view.View.VISIBLE
                        channelList.clear()
                        channelList.addAll(allSavedChannels)
                        channelAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.tvLoginStatus.text = "채널 조회 중 에러 발생: ${e.message}"
                    Log.e("YouTubeAPI", "Exception: ${e.message}")
                }
            }
        }
    }

    /**
     * 주어진 URL로 YouTube 채널 목록을 조회하고 저장소에 저장합니다.
     * @return 새로 저장된 채널 수
     */
    private fun fetchAndSaveChannels(client: OkHttpClient, token: String, url: String, tag: String): Int {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e("YouTubeAPI", "[$tag] API 호출 실패: $responseBody")
                return 0
            }

            val items = JSONObject(responseBody).optJSONArray("items")
            var savedCount = 0
            if (items != null) {
                for (i in 0 until items.length()) {
                    val channelObj = items.getJSONObject(i)
                    val id = channelObj.getString("id")
                    val snippet = channelObj.getJSONObject("snippet")
                    val title = snippet.getString("title")
                    val defaultThumb = snippet
                        .getJSONObject("thumbnails")
                        .getJSONObject("default")
                        .getString("url")

                    Log.d("YouTubeAPI", "[$tag] 채널 발견 → id=$id, title=$title")
                    ChannelRepository.saveChannel(this@MainActivity, YouTubeChannel(id, title, defaultThumb, token))
                    savedCount++
                }
            }
            savedCount
        } catch (e: Exception) {
            Log.e("YouTubeAPI", "[$tag] 예외: ${e.message}")
            0
        }
    }
}