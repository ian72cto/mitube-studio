package com.mitube.mlc

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LiveCameraActivity : AppCompatActivity() {

    private lateinit var tvLiveIndicator: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvViewers: TextView
    private lateinit var tvLikes: TextView
    private lateinit var btnStopBroadcast: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_camera)

        tvLiveIndicator = findViewById(R.id.tvLiveIndicator)
        tvDuration = findViewById(R.id.tvDuration)
        tvViewers = findViewById(R.id.tvViewers)
        tvLikes = findViewById(R.id.tvLikes)
        btnStopBroadcast = findViewById(R.id.btnStopBroadcast)

        val rtmpUrl = intent.getStringExtra("EXTRA_RTMP_URL")
        val streamKey = intent.getStringExtra("EXTRA_STREAM_KEY")
        val broadcastId = intent.getStringExtra("EXTRA_BROADCAST_ID")

        if (rtmpUrl != null && streamKey != null) {
            Toast.makeText(this, "RTMP 송출 시작!\n$rtmpUrl", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "RTMP 정보가 없습니다. 카메라 목업 모드", Toast.LENGTH_SHORT).show()
        }

        btnStopBroadcast.setOnClickListener {
            // TODO: Call API to transition broadcast to 'complete' status
            Toast.makeText(this, "방송을 종료합니다...", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
