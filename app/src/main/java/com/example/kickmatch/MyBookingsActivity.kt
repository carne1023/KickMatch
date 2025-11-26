package com.example.kickmatch

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kickmatch.adapter.BookingAdapter
import com.example.kickmatch.databinding.ActivityMyBookingsBinding
import com.example.kickmatch.model.Booking
import com.example.kickmatch.model.BookingStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBookingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var bookingAdapter: BookingAdapter
    private val bookings = mutableListOf<Booking>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        loadBookings("all")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mis Reservas"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(
            bookings = bookings,
            onCancelClick = { booking ->
                showCancelDialog(booking)
            },
            onDetailsClick = { booking ->
                showBookingDetails(booking)
            }
        )

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(this@MyBookingsActivity)
            adapter = bookingAdapter
        }
    }

    private fun setupTabs() {
        binding.chipAll.setOnClickListener {
            selectTab("all")
        }

        binding.chipUpcoming.setOnClickListener {
            selectTab("upcoming")
        }

        binding.chipPast.setOnClickListener {
            selectTab("past")
        }
    }

    private fun selectTab(tab: String) {
        binding.chipAll.isChecked = tab == "all"
        binding.chipUpcoming.isChecked = tab == "upcoming"
        binding.chipPast.isChecked = tab == "past"
        loadBookings(tab)
    }

    private fun loadBookings(filter: String) {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        var query: Query = firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)

        query.get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                bookings.clear()

                val now = System.currentTimeMillis()

                for (document in documents) {
                    val booking = Booking(
                        id = document.id,
                        fieldId = document.getString("fieldId") ?: "",
                        fieldName = document.getString("fieldName") ?: "",
                        userId = document.getString("userId") ?: "",
                        userName = document.getString("userName") ?: "",
                        userEmail = document.getString("userEmail") ?: "",
                        userPhone = document.getString("userPhone") ?: "",
                        date = document.getLong("date") ?: 0L,
                        startTime = document.getString("startTime") ?: "",
                        endTime = document.getString("endTime") ?: "",
                        duration = document.getLong("duration")?.toInt() ?: 1,
                        totalPrice = document.getDouble("totalPrice") ?: 0.0,
                        status = try {
                            BookingStatus.valueOf(document.getString("status") ?: "PENDING")
                        } catch (e: Exception) {
                            BookingStatus.PENDING
                        },
                        notes = document.getString("notes") ?: "",
                        createdAt = document.getLong("createdAt") ?: 0L,
                        updatedAt = document.getLong("updatedAt") ?: 0L
                    )

                    // Filtrar según tab seleccionado
                    when (filter) {
                        "upcoming" -> {
                            if (booking.date >= now && booking.status != BookingStatus.CANCELLED) {
                                bookings.add(booking)
                            }
                        }
                        "past" -> {
                            if (booking.date < now || booking.status == BookingStatus.CANCELLED) {
                                bookings.add(booking)
                            }
                        }
                        else -> bookings.add(booking)
                    }
                }

                bookingAdapter.notifyDataSetChanged()

                if (bookings.isEmpty()) {
                    binding.tvNoBookings.visibility = View.VISIBLE
                    binding.rvBookings.visibility = View.GONE
                } else {
                    binding.tvNoBookings.visibility = View.GONE
                    binding.rvBookings.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar reservas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCancelDialog(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle("Cancelar Reserva")
            .setMessage("¿Estás seguro de que quieres cancelar esta reserva?")
            .setPositiveButton("Cancelar Reserva") { _, _ ->
                cancelBooking(booking)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking(booking: Booking) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("bookings").document(booking.id)
            .update(
                mapOf(
                    "status" to BookingStatus.CANCELLED.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Reserva cancelada", Toast.LENGTH_SHORT).show()
                loadBookings("all")
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cancelar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showBookingDetails(booking: Booking) {
        val message = """
            Cancha: ${booking.fieldName}
            Fecha: ${com.example.kickmatch.model.TimeSlotHelper.formatDate(booking.date)}
            Hora: ${booking.startTime} - ${booking.endTime}
            Duración: ${booking.duration} hora(s)
            Precio: $${booking.totalPrice.toInt()}
            Estado: ${getStatusText(booking.status)}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Detalles de la Reserva")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun getStatusText(status: BookingStatus): String {
        return when (status) {
            BookingStatus.PENDING -> "Pendiente"
            BookingStatus.CONFIRMED -> "Confirmada"
            BookingStatus.CANCELLED -> "Cancelada"
            BookingStatus.COMPLETED -> "Completada"
        }
    }
}