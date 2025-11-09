package com.example.kickmatch.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kickmatch.databinding.ItemAmenityBinding
import com.example.kickmatch.model.Amenity

class AmenityAdapter(
    private val amenities: List<Amenity>,
    private val selectedAmenities: MutableList<String>
) : RecyclerView.Adapter<AmenityAdapter.AmenityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmenityViewHolder {
        val binding = ItemAmenityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AmenityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AmenityViewHolder, position: Int) {
        holder.bind(amenities[position])
    }

    override fun getItemCount(): Int = amenities.size

    inner class AmenityViewHolder(
        private val binding: ItemAmenityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(amenity: Amenity) {
            binding.apply {
                tvAmenityName.text = amenity.name

                val context = itemView.context
                val iconResId = context.resources.getIdentifier(
                    amenity.iconName,
                    "drawable",
                    context.packageName
                )

                if (iconResId != 0) {
                    ivAmenityIcon.setImageResource(iconResId)
                }

                cbAmenity.isChecked = selectedAmenities.contains(amenity.id)

                root.setOnClickListener {
                    cbAmenity.isChecked = !cbAmenity.isChecked
                }

                // Listener del checkbox
                cbAmenity.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!selectedAmenities.contains(amenity.id)) {
                            selectedAmenities.add(amenity.id)
                        }
                    } else {
                        selectedAmenities.remove(amenity.id)
                    }
                }
            }
        }
    }
}