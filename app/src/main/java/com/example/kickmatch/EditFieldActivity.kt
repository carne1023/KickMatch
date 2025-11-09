package com.example.kickmatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kickmatch.adapter.AmenityAdapter
import com.example.kickmatch.adapter.PhotoAdapter
import com.example.kickmatch.databinding.ActivityAddFieldBinding
import com.example.kickmatch.model.Amenity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditFieldActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFieldBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var amenityAdapter: AmenityAdapter
    private lateinit var photoAdapter: PhotoAdapter

    private val selectedPhotos = mutableListOf<Uri>()
    private val existingPhotoUrls = mutableListOf<String>()
    private val uploadedPhotoUrls = mutableListOf<String>()
    private val selectedAmenities = mutableListOf<String>()

    private var fieldId: String = ""

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val totalPhotos = selectedPhotos.size + existingPhotoUrls.size
                    if (totalPhotos < 5) {
                        selectedPhotos.add(clipData.getItemAt(i).uri)
                    }
                }
            } ?: result.data?.data?.let { uri ->
                val totalPhotos = selectedPhotos.size + existingPhotoUrls.size
                if (totalPhotos < 5) {
                    selectedPhotos.add(uri)
                }
            }
            photoAdapter.notifyDataSetChanged()
            updatePhotoCount()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFieldBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        fieldId = intent.getStringExtra("fieldId") ?: ""

        if (fieldId.isEmpty()) {
            Toast.makeText(this, "Error: ID de cancha inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupAmenities()
        setupPhotos()
        setupListeners()
        loadFieldData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editar Cancha"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupAmenities() {
        amenityAdapter = AmenityAdapter(
            amenities = Amenity.getCommonAmenities(),
            selectedAmenities = selectedAmenities
        )

        binding.rvAmenities.apply {
            layoutManager = GridLayoutManager(this@EditFieldActivity, 2)
            adapter = amenityAdapter
        }
    }

    private fun setupPhotos() {
        photoAdapter = PhotoAdapter(
            photos = selectedPhotos,
            onRemoveClick = { position ->
                selectedPhotos.removeAt(position)
                photoAdapter.notifyItemRemoved(position)
                updatePhotoCount()
            }
        )

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(this@EditFieldActivity, 3)
            adapter = photoAdapter
        }
    }

    private fun setupListeners() {
        binding.btnAddPhotos.setOnClickListener {
            val totalPhotos = selectedPhotos.size + existingPhotoUrls.size
            if (totalPhotos >= 5) {
                Toast.makeText(this, "Máximo 5 fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            pickImages.launch(intent)
        }

        binding.btnSave.text = "Actualizar Cancha"
        binding.btnSave.setOnClickListener {
            validateAndUpdateField()
        }
    }

    private fun loadFieldData() {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("fields").document(fieldId)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE

                if (document.exists()) {
                    binding.etFieldName.setText(document.getString("name") ?: "")
                    binding.etAddress.setText(document.getString("address") ?: "")
                    binding.etDescription.setText(document.getString("description") ?: "")

                    val price = document.getDouble("pricePerHour") ?: 0.0
                    binding.etPrice.setText(price.toInt().toString())

                    val amenities = document.get("amenities") as? List<String> ?: emptyList()
                    selectedAmenities.clear()
                    selectedAmenities.addAll(amenities)
                    amenityAdapter.notifyDataSetChanged()

                    val photos = document.get("photos") as? List<String> ?: emptyList()
                    existingPhotoUrls.clear()
                    existingPhotoUrls.addAll(photos)
                    updatePhotoCount()

                } else {
                    Toast.makeText(this, "Cancha no encontrada", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updatePhotoCount() {
        val totalPhotos = selectedPhotos.size + existingPhotoUrls.size
        binding.tvPhotosCount.text = "$totalPhotos/5 fotos"
    }

    private fun validateAndUpdateField() {
        val name = binding.etFieldName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilFieldName.error = "El nombre es requerido"
            return
        }
        binding.tilFieldName.error = null

        if (address.isEmpty()) {
            binding.tilAddress.error = "La dirección es requerida"
            return
        }
        binding.tilAddress.error = null

        if (priceStr.isEmpty()) {
            binding.tilPrice.error = "El precio es requerido"
            return
        }
        binding.tilPrice.error = null

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.tilPrice.error = "Precio inválido"
            return
        }
        binding.tilPrice.error = null

        val totalPhotos = selectedPhotos.size + existingPhotoUrls.size
        if (totalPhotos == 0) {
            Toast.makeText(this, "Agrega al menos una foto", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        if (selectedPhotos.isNotEmpty()) {
            uploadPhotos { newPhotoUrls ->
                val allPhotoUrls = existingPhotoUrls + newPhotoUrls
                updateFieldInFirestore(name, address, description, price, allPhotoUrls)
            }
        } else {
            updateFieldInFirestore(name, address, description, price, existingPhotoUrls)
        }
    }

    private fun uploadPhotos(onComplete: (List<String>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        uploadedPhotoUrls.clear()

        var uploadCount = 0
        val totalPhotos = selectedPhotos.size

        selectedPhotos.forEachIndexed { index, uri ->
            val photoRef = storage.reference.child(
                "fields/$userId/${UUID.randomUUID()}.jpg"
            )

            photoRef.putFile(uri)
                .addOnSuccessListener {
                    photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        uploadedPhotoUrls.add(downloadUri.toString())
                        uploadCount++

                        if (uploadCount == totalPhotos) {
                            onComplete(uploadedPhotoUrls)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(
                        this,
                        "Error al subir foto ${index + 1}: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun updateFieldInFirestore(
        name: String,
        address: String,
        description: String,
        price: Double,
        photoUrls: List<String>
    ) {
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "address" to address,
            "description" to description,
            "pricePerHour" to price,
            "amenities" to selectedAmenities,
            "photos" to photoUrls,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("fields").document(fieldId)
            .update(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Cancha actualizada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}