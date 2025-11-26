package com.example.kickmatch.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kickmatch.R
import com.example.kickmatch.databinding.ItemBookingBinding
import com.example.kickmatch.model.Booking
import com.example.kickmatch.model.BookingStatus
import com.example.kickmatch.model.TimeSlotHelper

class BookingAdapter(
    private val bookings: List<Booking>,
    private val onCancelClick: (Booking) -> Unit,
    private val onDetailsClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.apply {
                tvFieldName.text = booking.fieldName
                tvBookingDate.text = TimeSlotHelper.formatDate(booking.date)
                tvBookingTime.text = "${booking.startTime} - ${booking.endTime}"
                tvBookingPrice.text = "$${booking.totalPrice.toInt()}"

                when (booking.status) {
                    BookingStatus.PENDING -> {
                        chipStatus.text = "Pendiente"
                        chipStatus.setChipBackgroundColorResource(R.color.orange)
                    }
                    BookingStatus.CONFIRMED -> {
                        chipStatus.text = "Confirmada"
                        chipStatus.setChipBackgroundColorResource(R.color.green)
                    }
                    BookingStatus.CANCELLED -> {
                        chipStatus.text = "Cancelada"
                        chipStatus.setChipBackgroundColorResource(R.color.red)
                    }
                    BookingStatus.COMPLETED -> {
                        chipStatus.text = "Completada"
                        chipStatus.setChipBackgroundColorResource(R.color.blue)
                    }
                }

                val now = System.currentTimeMillis()
                val canCancel = (booking.status == BookingStatus.PENDING ||
                        booking.status == BookingStatus.CONFIRMED) &&
                        booking.date >= now

                btnCancel.visibility = if (canCancel) View.VISIBLE else View.GONE

                btnCancel.setOnClickListener {
                    onCancelClick(booking)
                }

                root.setOnClickListener {
                    onDetailsClick(booking)
                }
            }
        }
    }
}