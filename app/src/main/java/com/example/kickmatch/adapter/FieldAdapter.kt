package com.example.kickmatch.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kickmatch.R
import com.example.kickmatch.databinding.ItemFieldBinding
import com.example.kickmatch.model.Field
import java.text.NumberFormat
import java.util.Locale

class FieldAdapter(
    private val fields: List<Field>,
    private val onFieldClick: (Field) -> Unit,
    private val onBookClick: (Field) -> Unit
) : RecyclerView.Adapter<FieldAdapter.FieldViewHolder>() {

    inner class FieldViewHolder(private val binding: ItemFieldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(field: Field) = with(binding) {
            tvFieldName.text = field.name
            tvFieldType.text = "${field.type} • ${field.surface}"
            tvFieldAddress.text = field.address

            val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            tvFieldPrice.text = "${format.format(field.pricePerHour)}/hora"

            if (field.distance > 0) {
                tvFieldDistance.visibility = View.VISIBLE
                tvFieldDistance.text = "%.1f km".format(field.distance)
            } else {
                tvFieldDistance.visibility = View.GONE
            }

            tvFieldRating.text = "⭐ %.1f".format(field.rating)

            ivParking.visibility = if (field.hasParking) View.VISIBLE else View.GONE
            ivLighting.visibility = if (field.hasLighting) View.VISIBLE else View.GONE
            ivShowers.visibility = if (field.hasShowers) View.VISIBLE else View.GONE

            when (field.surface) {
                "Sintética" -> ivFieldImage.setImageResource(R.drawable.field_synthetic)
                "Natural" -> ivFieldImage.setImageResource(R.drawable.field_natural)
                "Cemento" -> ivFieldImage.setImageResource(R.drawable.field_concrete)
                else -> ivFieldImage.setImageResource(R.drawable.field_placeholder)
            }

            cardField.setOnClickListener { onFieldClick(field) }
            btnBook.setOnClickListener { onBookClick(field) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val binding = ItemFieldBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FieldViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        holder.bind(fields[position])
    }

    override fun getItemCount(): Int = fields.size
}
