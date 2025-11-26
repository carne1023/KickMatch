package com.example.kickmatch.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kickmatch.R
import com.example.kickmatch.databinding.ItemTimeSlotBinding

class TimeSlotAdapter(
    private val timeSlots: List<String>,
    private val onTimeSlotClick: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private val availableSlots = timeSlots.toMutableList()
    private val bookedSlots = mutableSetOf<String>()
    private var selectedSlot: String? = null

    fun updateAvailability(booked: List<String>) {
        bookedSlots.clear()
        bookedSlots.addAll(booked)
        selectedSlot = null
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val binding = ItemTimeSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        holder.bind(timeSlots[position])
    }

    override fun getItemCount(): Int = timeSlots.size

    inner class TimeSlotViewHolder(
        private val binding: ItemTimeSlotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(time: String) {
            binding.apply {
                tvTime.text = time

                val isBooked = bookedSlots.contains(time)
                val isSelected = selectedSlot == time

                when {
                    isBooked -> {
                        // Slot ocupado
                        cardSlot.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.gray_300)
                        )
                        tvTime.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.gray_600)
                        )
                        cardSlot.isEnabled = false
                        cardSlot.alpha = 0.5f
                    }
                    isSelected -> {
                        // Slot seleccionado
                        cardSlot.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.primary)
                        )
                        tvTime.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.white)
                        )
                        cardSlot.isEnabled = true
                        cardSlot.alpha = 1f
                    }
                    else -> {
                        // Slot disponible
                        cardSlot.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.white)
                        )
                        tvTime.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.gray_900)
                        )
                        cardSlot.isEnabled = true
                        cardSlot.alpha = 1f
                    }
                }

                cardSlot.setOnClickListener {
                    if (!isBooked) {
                        selectedSlot = if (isSelected) null else time
                        notifyDataSetChanged()
                        if (selectedSlot != null) {
                            onTimeSlotClick(time)
                        }
                    }
                }
            }
        }
    }
}