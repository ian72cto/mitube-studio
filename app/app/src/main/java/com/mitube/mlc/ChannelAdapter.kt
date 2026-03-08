package com.mitube.mlc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class YouTubeChannel(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val token: String = ""
)

class ChannelAdapter(
    private val channelList: List<YouTubeChannel>,
    private val onChannelClick: (YouTubeChannel) -> Unit,
    private val onRemoveClick: (YouTubeChannel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.iv_channel_profile)
        val tvName: TextView = itemView.findViewById(R.id.tv_channel_name)
        val btnRemove: TextView = itemView.findViewById(R.id.btn_remove_channel)

        fun bind(channel: YouTubeChannel) {
            tvName.text = channel.title

            Glide.with(itemView.context)
                .load(channel.thumbnailUrl)
                .into(ivProfile)

            itemView.setOnClickListener {
                onChannelClick(channel)
            }

            btnRemove.setOnClickListener {
                onRemoveClick(channel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(channelList[position])
    }

    override fun getItemCount(): Int = channelList.size
}
