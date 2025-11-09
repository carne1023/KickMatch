package com.example.kickmatch.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kickmatch.R
import com.example.kickmatch.databinding.ItemFieldBinding
import com.example.kickmatch.model.Field

class FieldAdapter(
    private val fields: List<Field>,
    private val onFieldClick: (Field) -> Unit,
    private val onBookClick: (Field) -> Unit,
    private val onEditClick: (Field) -> Unit,
    private val onDeleteClick: (Field) -> Unit,
    private val onToggleActiveClick: (Field) -> Unit,
    private val showAdminControls: Boolean
) : RecyclerView.Adapter<FieldAdapter.FieldViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val binding = ItemFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FieldViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        holder.bind(fields[position])
    }

    override fun getItemCount(): Int = fields.size

    inner class FieldViewHolder(
        private val binding: ItemFieldBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(field: Field) {
            binding.apply {
                tvFieldName.text = field.name
                tvFieldAddress.text = field.address
                tvFieldPrice.text = "$${field.pricePerHour}/hora"

                tvFieldRating.text = String.format("%.1f", field.rating)
                tvRatingCount.text = "(${field.totalRatings})"

                chipActive.text = if (field.isActive) "Activa" else "Inactiva"
                chipActive.setChipBackgroundColorResource(
                    if (field.isActive) R.color.green else R.color.red
                )

                if (showAdminControls) {
                    btnEdit.visibility = View.VISIBLE
                    btnDelete.visibility = View.VISIBLE
                    switchActive.visibility = View.VISIBLE

                    btnEdit.setOnClickListener { onEditClick(field) }
                    btnDelete.setOnClickListener { onDeleteClick(field) }
                    switchActive.setOnCheckedChangeListener(null)
                    switchActive.isChecked = field.isActive
                    switchActive.setOnCheckedChangeListener { _, _ ->
                        onToggleActiveClick(field)
                    }
                } else {
                    btnEdit.visibility = View.GONE
                    btnDelete.visibility = View.GONE
                    switchActive.visibility = View.GONE

                    val bookBtn = binding.root.findViewById<View>(R.id.btnBook)
                    bookBtn?.setOnClickListener { onBookClick(field) }
                }

                if (field.photos.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(field.photos[0])
                        .placeholder(R.drawable.ic_stadium)
                        .error(R.drawable.ic_stadium)
                        .centerCrop()
                        .into(ivFieldPhoto)
                } else {
                    ivFieldPhoto.setImageResource(R.drawable.svg_stadium)
                }

                val amenitiesText = if (field.amenities.isNotEmpty()) {
                    field.amenities.take(3).joinToString(", ")
                } else {
                    "Sin amenidades"
                }
                tvAmenities.text = amenitiesText
            }

            itemView.setOnClickListener { onFieldClick(field) }
        }
    }
}
