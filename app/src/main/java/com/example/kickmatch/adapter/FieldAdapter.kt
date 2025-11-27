package com.example.kickmatch.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kickmatch.R
import com.example.kickmatch.databinding.ItemFieldBinding
import com.example.kickmatch.model.Field
import com.example.kickmatch.utils.AmenitiesIcons

class FieldAdapter(
    private val fields: List<Field>,
    private val onFieldClick: ((Field) -> Unit)? = null,
    private val onBookClick: ((Field) -> Unit)? = null,
    private val onEditClick: ((Field) -> Unit)? = null,
    private val onDeleteClick: ((Field) -> Unit)? = null,
    private val onToggleActiveClick: ((Field) -> Unit)? = null,
    private val showAdminControls: Boolean = false
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

                if (field.photos.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(field.photos[0])
                        .placeholder(R.drawable.ic_stadium)
                        .error(R.drawable.ic_stadium)
                        .centerCrop()
                        .into(ivFieldPhoto)
                } else {
                    ivFieldPhoto.setImageResource(R.drawable.ic_stadium)
                }

                containerAmenities.removeAllViews()

                if (field.amenities.isNullOrEmpty()) {

                    val tv = TextView(itemView.context).apply {
                        text = "Sin amenidades"
                        textSize = 12f
                    }
                    containerAmenities.addView(tv)

                } else {
                    field.amenities.forEach { amenityId ->
                        val iconView = ImageView(itemView.context)

                        iconView.setImageResource(
                            AmenitiesIcons.getIconForAmenity(amenityId)
                        )

                        val size = itemView.resources
                            .getDimensionPixelSize(R.dimen.amenity_icon_size)

                        val params = LinearLayout.LayoutParams(size, size)
                        params.setMargins(12, 0, 12, 0)

                        iconView.layoutParams = params

                        containerAmenities.addView(iconView)
                    }
                }

                root.setOnClickListener {
                    onFieldClick?.invoke(field)
                }

                if (showAdminControls) {

                    btnEdit.visibility = View.VISIBLE
                    btnDelete.visibility = View.VISIBLE
                    switchActive.visibility = View.VISIBLE
                    btnBook?.visibility = View.GONE

                    btnEdit.setOnClickListener { onEditClick?.invoke(field) }
                    btnDelete.setOnClickListener { onDeleteClick?.invoke(field) }

                    switchActive.isChecked = field.isActive
                    switchActive.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != field.isActive) {
                            onToggleActiveClick?.invoke(field)
                        }
                    }

                } else {

                    btnEdit.visibility = View.GONE
                    btnDelete.visibility = View.GONE
                    switchActive.visibility = View.GONE
                    btnBook?.visibility = View.VISIBLE

                    btnBook?.setOnClickListener { onBookClick?.invoke(field) }
                }
            }
        }
    }
}
