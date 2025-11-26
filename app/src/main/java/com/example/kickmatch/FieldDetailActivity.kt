package com.example.kickmatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.kickmatch.adapter.AmenityAdapter
import com.example.kickmatch.databinding.ActivityFieldDetailBinding
import com.example.kickmatch.model.Amenity
import com.example.kickmatch.model.Field
import com.google.firebase.firestore.FirebaseFirestore

class FieldDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFieldDetailBinding
    private lateinit var firestore: FirebaseFirestore
    private var field: Field? = null
    private var isFromGeoapify = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFieldDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        setupToolbar()

        if (intent.hasExtra("field_data")) {
            isFromGeoapify = true
            field = intent.getSerializableExtra("field_data") as? Field
            field?.let { displayFieldData(it) }
        } else {
            // Cargar desde Firebase usando fieldId
            val fieldId = intent.getStringExtra("fieldId")
            if (!fieldId.isNullOrEmpty()) {
                loadFieldFromFirebase(fieldId)
            } else {
                Toast.makeText(this, "Error: ID de cancha inválido", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnReserve.setOnClickListener {
            field?.let { currentField ->
                if (isFromGeoapify) {
                    Toast.makeText(
                        this,
                        "Esta es una cancha externa. Por favor, contacta directamente usando:\n${currentField.phoneNumber}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Para canchas de Firebase, proceder con la reserva normal
                    val intent = Intent(this, BookFieldActivity::class.java)
                    intent.putExtra("fieldId", currentField.id)
                    intent.putExtra("fieldName", currentField.name)
                    intent.putExtra("pricePerHour", currentField.pricePerHour)
                    startActivity(intent)
                }
            }
        }
    }

    private fun loadFieldFromFirebase(fieldId: String) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("fields").document(fieldId)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE

                if (document.exists()) {
                    val loadedField = Field(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        address = document.getString("address") ?: "",
                        description = document.getString("description") ?: "",
                        pricePerHour = document.getDouble("pricePerHour") ?: 0.0,
                        rating = (document.getDouble("rating") ?: 0.0).toFloat(),
                        totalRatings = document.getLong("totalRatings")?.toInt() ?: 0,
                        photos = (document.get("photos") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        amenities = document.get("amenities") as? List<String> ?: emptyList(),
                        phoneNumber = document.getString("phoneNumber") ?: "",
                        openingHours = document.getString("openingHours") ?: "Consultar horarios",
                        isActive = document.getBoolean("isActive") ?: true
                    )
                    field = loadedField
                    displayFieldData(loadedField)
                } else {
                    Toast.makeText(this, "Cancha no encontrada", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayFieldData(field: Field) {
        binding.apply {
            supportActionBar?.title = field.name
            tvFieldName.text = field.name
            tvFieldAddress.text = field.address
            tvFieldPrice.text = "$${field.pricePerHour.toInt()}/hora"

            tvFieldRating.text = String.format("%.1f", field.rating)
            tvRatingCount.text = "(${field.totalRatings} reseñas)"

            if (field.description.isNotEmpty()) {
                tvDescription.text = field.description
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            tvOpeningHours.text = if (field.openingHours.isNotEmpty()) {
                field.openingHours
            } else {
                "Horarios no disponibles"
            }

            // Teléfono
            if (field.phoneNumber.isNotEmpty()) {
                tvPhoneNumber.text = field.phoneNumber
                tvPhoneNumber.visibility = View.VISIBLE
            } else {
                tvPhoneNumber.visibility = View.GONE
            }

            // Fotos
            if (field.photos.isNotEmpty()) {
                Glide.with(this@FieldDetailActivity)
                    .load(field.photos[0])
                    .placeholder(R.drawable.ic_stadium)
                    .error(R.drawable.ic_stadium)
                    .centerCrop()
                    .into(ivFieldPhoto)
            } else {
                ivFieldPhoto.setImageResource(R.drawable.ic_stadium)
            }

            if (field.amenities.isNotEmpty()) {
                setupAmenities(field.amenities)
                tvAmenitiesTitle.visibility = View.VISIBLE
                rvAmenities.visibility = View.VISIBLE
            } else {
                tvAmenitiesTitle.visibility = View.GONE
                rvAmenities.visibility = View.GONE
            }

            btnReserve.text = if (isFromGeoapify) {
                "Ver Información de Contacto"
            } else {
                "Reservar Cancha"
            }

            if (isFromGeoapify) {
                (tvExternalFieldInfo.parent as? View)?.visibility = View.VISIBLE
            } else {
                (tvExternalFieldInfo.parent as? View)?.visibility = View.GONE
            }
        }
    }

    private fun setupAmenities(amenityIds: List<String>) {
        val allAmenities = Amenity.getCommonAmenities()
        val availableAmenities = allAmenities.filter { amenity ->
            amenityIds.contains(amenity.id)
        }

        val adapter = AmenityAdapter(
            amenities = availableAmenities,
            selectedAmenities = mutableListOf()
        )

        binding.rvAmenities.apply {
            layoutManager = GridLayoutManager(this@FieldDetailActivity, 2)
            this.adapter = adapter
            isNestedScrollingEnabled = false
        }
    }
}