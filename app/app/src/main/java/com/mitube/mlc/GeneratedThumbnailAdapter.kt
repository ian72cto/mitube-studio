package com.mitube.mlc

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class GeneratedThumbnailAdapter(
    private val images: List<Bitmap>,
    private val onImageSelected: (Int) -> Unit
) : RecyclerView.Adapter<GeneratedThumbnailAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheck)

        init {
            view.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    val oldSelectedPosition = selectedPosition
                    selectedPosition = currentPosition

                    if (oldSelectedPosition != -1) {
                        notifyItemChanged(oldSelectedPosition)
                    }
                    notifyItemChanged(selectedPosition)

                    onImageSelected(selectedPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_generated_thumbnail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ivThumbnail.setImageBitmap(images[position])
        holder.ivCheck.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = images.size
}
