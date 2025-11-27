package com.example.kickmatch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
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

class AddFieldActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFieldBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var amenityAdapter: AmenityAdapter
    private lateinit var photoAdapter: PhotoAdapter

    private val selectedPhotos = mutableListOf<Uri>()
    private val uploadedPhotoUrls = mutableListOf<String>()
    private val selectedAmenities = mutableListOf<String>()

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    if (selectedPhotos.size < 5) {
                        selectedPhotos.add(clipData.getItemAt(i).uri)
                    }
                }
            } ?: result.data?.data?.let { uri ->
                if (selectedPhotos.size < 5) {
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

        setupToolbar()
        setupAmenities()
        setupPhotos()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agregar Cancha"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupAmenities() {
        amenityAdapter = AmenityAdapter(
            amenities = Amenity.getCommonAmenities(),
            selectedAmenities = selectedAmenities
        )

        binding.rvAmenities.apply {
            layoutManager = GridLayoutManager(this@AddFieldActivity, 2)
            adapter = amenityAdapter
        }
    }

    private fun setupPhotos() {
        photoAdapter = PhotoAdapter(
            photos = selectedPhotos,
            onRemoveClick = { position ->
                if (position in 0 until selectedPhotos.size) {
                    selectedPhotos.removeAt(position)
                    photoAdapter.notifyItemRemoved(position)
                    updatePhotoCount()
                }
            }
        )

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(this@AddFieldActivity, 3)
            adapter = photoAdapter
        }
    }

    private fun setupListeners() {
        binding.btnAddPhotos.setOnClickListener {
            if (selectedPhotos.size >= 5) {
                Toast.makeText(this, "Máximo 5 fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            pickImages.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            validateAndSaveField()
        }
    }

    private fun updatePhotoCount() {
        binding.tvPhotosCount.text = "${selectedPhotos.size}/5 fotos"
    }

    private fun validateAndSaveField() {
        val name = binding.etFieldName.text?.toString()?.trim().orEmpty()
        val address = binding.etAddress.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val priceStr = binding.etPrice.text?.toString()?.trim().orEmpty()

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

        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, "Agrega al menos una foto", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSave.isEnabled = false

        val firstPhotoUri = selectedPhotos[0]
        val base64Image = encodeImageToBase64(firstPhotoUri)

        if (base64Image != null) {
            // Simulacion de subir foto pero con base64
            saveFieldToFirestore(name, address, description, price, listOf(base64Image))
        } else {
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSave.isEnabled = true
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun encodeImageToBase64(imageUri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Se comprime durisimo el bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 800, 800, true)

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 40, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // Retorna el string en Base64
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

   /** private fun uploadPhotos(onComplete: (List<String>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSave.isEnabled = true
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        uploadedPhotoUrls.clear()

        var uploadCount = 0
        val totalPhotos = selectedPhotos.size
        if (totalPhotos == 0) {
            onComplete(emptyList())
            return
        }

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
                            onComplete(uploadedPhotoUrls.toList())
                        }
                    }.addOnFailureListener { e ->
                        // fallo al obtener url
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(this, "Error al obtener URL de la foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(
                        this,
                        "Error al subir foto ${index + 1}: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    } **/

    private fun saveFieldToFirestore(
        name: String,
        address: String,
        description: String,
        price: Double,
        photoUrls: List<String>
    ) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSave.isEnabled = true
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val fieldData = hashMapOf(
            "name" to name,
            "address" to address,
            "description" to description,
            "pricePerHour" to price,
            "adminId" to userId,
            "amenities" to selectedAmenities,
            "photos" to photoUrls,
            "rating" to 0.0,
            "totalRatings" to 0,
            "isActive" to true,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("fields")
            .add(fieldData)
            .addOnSuccessListener { documentReference ->
                val fieldId = documentReference.id
                updateOwnerProfile(fieldId, userId)

                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Cancha creada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateOwnerProfile(fieldId: String, userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val existing = document.get("ownerProfile.fieldsOwned")
                val fieldsOwned = when (existing) {
                    is List<*> -> existing.mapNotNull { it?.toString() }.toMutableList()
                    else -> mutableListOf()
                }
                fieldsOwned.add(fieldId)

                firestore.collection("users").document(userId)
                    .update("ownerProfile.fieldsOwned", fieldsOwned)
                    .addOnFailureListener {

                    }
            }
            .addOnFailureListener {
            }
    }
}
