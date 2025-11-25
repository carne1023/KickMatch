package com.example.kickmatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kickmatch.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null
    private var isEditMode = false

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.ivProfilePhoto)

                if (!isEditMode) toggleEditMode()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupToolbar()
        setupPositionSelector()
        setupListeners()
        loadUserProfile()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mi Perfil"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupPositionSelector() {
        val positions = listOf(
            "Portero",
            "Defensa central",
            "Lateral derecho",
            "Lateral izquierdo",
            "Mediocentro defensivo",
            "Mediocentro ofensivo",
            "Extremo derecho",
            "Extremo izquierdo",
            "Delantero centro"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, positions)
        binding.actvPosition.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.ivProfilePhoto.setOnClickListener { if (isEditMode) selectImage() }
        binding.btnChangePhoto.setOnClickListener { selectImage() }

        binding.btnEdit.setOnClickListener {
            if (isEditMode) saveProfile() else toggleEditMode()
        }

        binding.btnCancel.setOnClickListener {
            if (isEditMode) {
                toggleEditMode()
                loadUserProfile()
            }
        }

        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        val enabled = isEditMode

        binding.tilName.isEnabled = enabled
        binding.tilPhone.isEnabled = enabled
        binding.tilBio.isEnabled = enabled
        binding.tilPosition.isEnabled = enabled

        binding.btnEdit.text = if (enabled) "Guardar" else "Editar Perfil"
        binding.btnEdit.setIconResource(if (enabled) R.drawable.ic_save else R.drawable.ic_edit)
        binding.btnCancel.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnChangePhoto.visibility = if (enabled) View.VISIBLE else View.GONE

        if (enabled) binding.etName.requestFocus() else selectedImageUri = null
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE

                if (document.exists()) {
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val bio = document.getString("bio") ?: ""
                    val position = document.getString("position") ?: ""
                    val photoUrl = document.getString("photoUrl") ?: ""
                    val createdAt = document.getLong("createdAt") ?: 0L

                    binding.etName.setText(name)
                    binding.tvEmail.text = email
                    binding.etPhone.setText(phone)
                    binding.etBio.setText(bio)
                    binding.actvPosition.setText(position, false)

                    val registrationDate = java.text.SimpleDateFormat(
                        "dd MMM yyyy", java.util.Locale("es", "ES")
                    ).format(java.util.Date(createdAt))
                    binding.tvMemberSince.text = "Miembro desde $registrationDate"

                    if (photoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person_large)
                            .error(R.drawable.ic_person_large)
                            .circleCrop()
                            .into(binding.ivProfilePhoto)
                    } else {
                        binding.ivProfilePhoto.setImageResource(R.drawable.ic_person_large)
                    }

                } else {
                    Toast.makeText(this, "No se encontró el perfil", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        val position = binding.actvPosition.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "El nombre es requerido"
            return
        }

        if (name.length < 3) {
            binding.tilName.error = "Mínimo 3 caracteres"
            return
        }

        binding.tilName.error = null
        binding.progressBar.visibility = View.VISIBLE
        binding.btnEdit.isEnabled = false

        if (selectedImageUri != null) {
            uploadProfileImage { photoUrl ->
                updateProfileData(name, phone, bio, position, photoUrl)
            }
        } else {
            updateProfileData(name, phone, bio, position, null)
        }
    }

    private fun uploadProfileImage(onSuccess: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val imageRef = storage.reference.child("profiles/$userId/profile.jpg")

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnEdit.isEnabled = true
                    Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateProfileData(
        name: String,
        phone: String,
        bio: String,
        position: String,
        photoUrl: String?
    ) {
        val userId = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "bio" to bio,
            "position" to position,
            "updatedAt" to System.currentTimeMillis()
        )

        photoUrl?.let { updates["photoUrl"] = it }

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .apply { photoUrl?.let { setPhotoUri(Uri.parse(it)) } }
                    .build()

                auth.currentUser?.updateProfile(profileUpdates)

                binding.progressBar.visibility = View.GONE
                binding.btnEdit.isEnabled = true
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                toggleEditMode()
                loadUserProfile()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnEdit.isEnabled = true
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}