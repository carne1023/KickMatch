package com.example.kickmatch.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kickmatch.R
import com.example.kickmatch.databinding.ItemPhotoBinding

class PhotoAdapter(
    private val photos: List<Any>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], position)
    }

    override fun getItemCount(): Int = photos.size

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Any, position: Int) {
            binding.apply {
                when (photo) {
                    is Uri -> {
                        Glide.with(itemView.context)
                            .load(photo)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.ic_person)
                            .centerCrop()
                            .into(ivPhoto)
                    }
                    is String -> {
                        Glide.with(itemView.context)
                            .load(photo)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.ic_person)
                            .centerCrop()
                            .into(ivPhoto)
                    }
                }

                btnRemove.setOnClickListener {
                    onRemoveClick(position)
                }

                tvPhotoNumber.text = "${position + 1}"
            }
        }
    }
}