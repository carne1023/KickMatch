package com.example.kickmatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
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
                    val message = if (currentField.phoneNumber.isNotEmpty()) {
                        "Esta es una cancha externa. Por favor, contacta directamente usando:\n${currentField.phoneNumber}"
                    } else {
                        "Esta es una cancha externa. Contacta directamente para hacer tu reserva."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
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

            if (field.pricePerHour > 0) {
                tvFieldPrice.text = "$${field.pricePerHour.toInt()}/hora"
            } else {
                tvFieldPrice.text = "Consultar precio"
            }

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
                tvPhoneNumber.text = "📞 ${field.phoneNumber}"
                tvPhoneNumber.visibility = View.VISIBLE
            } else {
                tvPhoneNumber.visibility = View.GONE
            }

            loadFieldPhoto(field)

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

    private fun loadFieldPhoto(field: Field) {
        try {
            // Intentamos obtener la foto (sea de la lista o el campo imageUrl)
            val photoData = field.photos.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: field.imageUrl

            if (!photoData.isNullOrBlank()) {
                // VERIFICAMOS: ¿Es una URL normal o es nuestro código Base64?
                if (photoData.startsWith("http") || photoData.startsWith("https")) {
                    // Es una URL normal (Geoapify o internet), usar Glide como siempre
                    Glide.with(this)
                        .load(photoData)
                        .placeholder(R.drawable.ic_stadium)
                        .error(R.drawable.ic_stadium)
                        .centerCrop()
                        .into(binding.ivFieldPhoto)
                } else {
                    // El truco de base64: decodificar y mostrar
                    try {
                        val decodedString = Base64.decode(photoData, Base64.DEFAULT)
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        binding.ivFieldPhoto.setImageBitmap(decodedByte)
                    } catch (e: Exception) {
                        // Si falla la conversión, ponemos el placeholder
                        binding.ivFieldPhoto.setImageResource(R.drawable.ic_stadium)
                    }
                }
            } else {
                // No hay datos de foto
                binding.ivFieldPhoto.setImageResource(R.drawable.ic_stadium)
            }
        } catch (e: Exception) {
            binding.ivFieldPhoto.setImageResource(R.drawable.ic_stadium)
        }
    }

    //Este es viejo (por ahora)
   /** private fun loadFieldPhoto(field: Field) {
        try {
            val photoUrl = field.photos.firstOrNull()?.takeIf { it.isNotBlank() }

            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_stadium)
                    .error(R.drawable.ic_stadium)
                    .centerCrop()
                    .into(binding.ivFieldPhoto)
            } else if (field.imageUrl.isNotBlank()) {

                Glide.with(this)
                    .load(field.imageUrl)
                    .placeholder(R.drawable.ic_stadium)
                    .error(R.drawable.ic_stadium)
                    .centerCrop()
                    .into(binding.ivFieldPhoto)
            } else {
                // Si no hay fotos, usar placeholder
                binding.ivFieldPhoto.setImageResource(R.drawable.ic_stadium)
            }
        } catch (e: Exception) {
            binding.ivFieldPhoto.setImageResource(R.drawable.ic_stadium)
        }
    } **/

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