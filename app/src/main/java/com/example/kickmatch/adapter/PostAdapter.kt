package com.example.kickmatch.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kickmatch.R
import com.example.kickmatch.databinding.ItemPostBinding
import com.example.kickmatch.model.Post
import java.util.concurrent.TimeUnit

class PostAdapter(
    private val posts: List<Post>,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onMenuClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {

            binding.tvUserName.text = post.userName

            if (post.groupName.isNotEmpty()) {
                binding.tvGroupName.visibility = View.VISIBLE
                binding.tvGroupName.text = "en ${post.groupName}"
            } else {
                binding.tvGroupName.visibility = View.GONE
            }


            binding.tvTimestamp.text = getRelativeTime(post.timestamp)

            binding.ivUserPhoto.setImageResource(R.drawable.ic_person)


            binding.tvContent.text = post.content


            if (post.imageUrl.isNotEmpty()) {
                binding.ivPostImage.visibility = View.VISIBLE

                // Usar imágenes de demo según el marcador
                when (post.imageUrl) {
                    "soccer" -> {
                        // Si tienes el logo de soccer, úsalo, si no, usa un placeholder
                        binding.ivPostImage.setImageResource(R.drawable.ic_soccer)
                    }
                    "player" -> {
                        binding.ivPostImage.setImageResource(R.drawable.ic_player)
                    }
                    else -> {
                        binding.ivPostImage.setImageResource(R.drawable.placeholder_image)
                    }
                }
            } else {
                binding.ivPostImage.visibility = View.GONE
            }


            updateLikeButton(post.isLiked)
            binding.tvLikes.text = "${post.likes} me gusta"


            binding.tvComments.text = "${post.comments} comentarios"

            binding.llLike.setOnClickListener {
                onLikeClick(post)
                updateLikeButton(post.isLiked)
                binding.tvLikes.text = "${post.likes} me gusta"
            }

            binding.llComments.setOnClickListener {
                onCommentClick(post)
            }

            binding.btnMenu.setOnClickListener {
                onMenuClick(post)
            }
        }

        private fun updateLikeButton(isLiked: Boolean) {
            if (isLiked) {
                binding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                binding.ivLike.setColorFilter(
                    ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                )
                binding.tvLikes.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                )
            } else {
                binding.ivLike.setImageResource(R.drawable.ic_heart_outline)
                binding.ivLike.setColorFilter(
                    ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                )
                binding.tvLikes.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                )
            }
        }

        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Justo ahora"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "Hace $minutes ${if (minutes == 1L) "minuto" else "minutos"}"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "Hace $hours ${if (hours == 1L) "hora" else "horas"}"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "Hace $days ${if (days == 1L) "día" else "días"}"
                }
                diff < TimeUnit.DAYS.toMillis(30) -> {
                    val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
                    "Hace $weeks ${if (weeks == 1L) "semana" else "semanas"}"
                }
                else -> "Hace más de un mes"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size
}