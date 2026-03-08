package com.mitube.mlc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mitube.mlc.api.BroadcastItem
import com.mitube.mlc.api.YouTubeLiveApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReservationListActivity : AppCompatActivity() {

    private lateinit var rvReservations: RecyclerView
    private lateinit var adapter: ReservationAdapter
    private lateinit var apiManager: YouTubeLiveApiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_list)

        val token = intent.getStringExtra("channel_token") ?: ""
        if (token.isEmpty()) {
            Toast.makeText(this, "채널 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        apiManager = YouTubeLiveApiManager(token)
        rvReservations = findViewById(R.id.rvReservations)
        rvReservations.layoutManager = LinearLayoutManager(this)

        adapter = ReservationAdapter(emptyList()) { broadcast ->
            startBroadcast(broadcast)
        }
        rvReservations.adapter = adapter

        loadReservations()
    }

    private fun loadReservations() {
        lifecycleScope.launch {
            val list = apiManager.listBroadcasts()
            if (list.isEmpty()) {
                Toast.makeText(this@ReservationListActivity, "예약된 방송이 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                adapter.updateData(list)
            }
        }
    }

    private fun startBroadcast(broadcast: BroadcastItem) {
        Toast.makeText(this, "스트림 바인딩 중...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            // 1. Create stream
            val streamInfo = apiManager.createLiveStream(broadcast.title)
            if (streamInfo == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReservationListActivity, "스트림 발급 실패!", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            
            // 2. Bind broadcast
            val bound = apiManager.bindBroadcastToStream(broadcast.id, streamInfo.id)
            withContext(Dispatchers.Main) {
                if (bound) {
                    Toast.makeText(this@ReservationListActivity, "바인딩 완료! 카메라로 이동합니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ReservationListActivity, LiveCameraActivity::class.java).apply {
                        putExtra("EXTRA_RTMP_URL", streamInfo.rtmpUrl)
                        putExtra("EXTRA_STREAM_KEY", streamInfo.streamKey)
                        putExtra("EXTRA_BROADCAST_ID", broadcast.id)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ReservationListActivity, "바인딩 실패!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class ReservationAdapter(
    private var items: List<BroadcastItem>,
    private val onStartClick: (BroadcastItem) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvScheduledTime: TextView = view.findViewById(R.id.tvScheduledTime)
        val btnStart: Button = view.findViewById(R.id.btnStartFromList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        
        val displayTime = if (item.scheduledStartTime.isNotEmpty()) {
            item.scheduledStartTime.replace("T", " ").replace("Z", "")
        } else {
            "예약 시간 없음"
        }
        holder.tvScheduledTime.text = displayTime

        if (item.thumbnailUrl.isNotEmpty()) {
            Glide.with(holder.ivThumbnail.context)
                .load(item.thumbnailUrl)
                .into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_camera)
        }

        holder.btnStart.setOnClickListener {
            onStartClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<BroadcastItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
