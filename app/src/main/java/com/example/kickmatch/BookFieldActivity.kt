package com.example.kickmatch

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kickmatch.databinding.ActivityBookFieldBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookFieldActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookFieldBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var fieldId: String = ""
    private var fieldName: String = ""
    private var pricePerHour: Double = 0.0

    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedStartTime: Calendar = Calendar.getInstance()
    private var selectedEndTime: Calendar = Calendar.getInstance()
    private var durationHours: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookFieldBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        fieldId = intent.getStringExtra("fieldId") ?: ""
        fieldName = intent.getStringExtra("fieldName") ?: ""
        pricePerHour = intent.getDoubleExtra("pricePerHour", 0.0)

        if (fieldId.isEmpty()) {
            Toast.makeText(this, "Error: Datos de cancha inválidos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupViews()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Reservar Cancha"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        binding.tvFieldName.text = fieldName
        updatePriceDisplay()
    }

    private fun setupListeners() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSelectStartTime.setOnClickListener {
            showStartTimePicker()
        }

        binding.btnSelectEndTime.setOnClickListener {
            showEndTimePicker()
        }

        binding.btnConfirmBooking.setOnClickListener {
            confirmBooking()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun showStartTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedStartTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedStartTime.set(Calendar.MINUTE, minute)

                selectedEndTime.timeInMillis = selectedStartTime.timeInMillis
                selectedEndTime.add(Calendar.HOUR_OF_DAY, 1)

                updateTimeDisplay()
                calculateDuration()
            },
            8,
            0,
            true
        ).show()
    }

    private fun showEndTimePicker() {
        if (selectedStartTime.get(Calendar.HOUR_OF_DAY) == 0) {
            Toast.makeText(this, "Selecciona la hora de inicio primero", Toast.LENGTH_SHORT).show()
            return
        }

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedEndTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedEndTime.set(Calendar.MINUTE, minute)

                if (selectedEndTime.timeInMillis <= selectedStartTime.timeInMillis) {
                    Toast.makeText(this, "La hora de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                updateTimeDisplay()
                calculateDuration()
            },
            selectedStartTime.get(Calendar.HOUR_OF_DAY) + 1,
            0,
            true
        ).show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        binding.tvSelectedDate.text = dateFormat.format(selectedDate.time)
    }

    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale("es", "ES"))
        binding.tvSelectedStartTime.text = timeFormat.format(selectedStartTime.time)
        binding.tvSelectedEndTime.text = timeFormat.format(selectedEndTime.time)
    }

    private fun calculateDuration() {
        val diffMillis = selectedEndTime.timeInMillis - selectedStartTime.timeInMillis
        durationHours = (diffMillis / (1000 * 60 * 60)).toInt()

        if (durationHours < 1) {
            durationHours = 1
        }

        binding.tvDuration.text = "$durationHours hora(s)"
        updatePriceDisplay()
    }

    private fun updatePriceDisplay() {
        val totalPrice = pricePerHour * durationHours
        binding.tvTotalPrice.text = "Total: $${totalPrice.toInt()}"
    }

    private fun confirmBooking() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Debes iniciar sesión para reservar", Toast.LENGTH_SHORT).show()
            return
        }

        // Validaciones adicionales
        if (selectedStartTime.get(Calendar.HOUR_OF_DAY) == 0) {
            Toast.makeText(this, "Selecciona la fecha y horarios", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnConfirmBooking.isEnabled = false

        val bookingData = hashMapOf(
            "fieldId" to fieldId,
            "fieldName" to fieldName,
            "userId" to userId,
            "date" to selectedDate.timeInMillis,
            "startTime" to selectedStartTime.timeInMillis,
            "endTime" to selectedEndTime.timeInMillis,
            "durationHours" to durationHours,
            "pricePerHour" to pricePerHour,
            "totalPrice" to (pricePerHour * durationHours),
            "status" to "pending", // pending, confirmed, cancelled
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "¡Reserva realizada con éxito!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnConfirmBooking.isEnabled = true
                Toast.makeText(this, "Error al crear reserva: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}