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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.kickmatch.adapter.FieldAdapter
import com.example.kickmatch.databinding.ActivityFieldAdminProfileBinding
import com.example.kickmatch.model.Field
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class FieldAdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFieldAdminProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var fieldAdapter: FieldAdapter
    private val fields = mutableListOf<Field>()

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
        binding = ActivityFieldAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadUserProfile()
        loadFields()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mi Perfil - Administrador"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        fieldAdapter = FieldAdapter(
            fields = fields,
            onFieldClick = { field ->
                val intent = Intent(this, EditFieldActivity::class.java)
                intent.putExtra("fieldId", field.id)
                startActivity(intent)
            },
            onBookClick = { },
            onEditClick = { field ->
                val intent = Intent(this, EditFieldActivity::class.java)
                intent.putExtra("fieldId", field.id)
                startActivity(intent)
            },
            onDeleteClick = { field ->
                showDeleteFieldDialog(field)
            },
            onToggleActiveClick = { field ->
                toggleFieldActive(field)
            },
            showAdminControls = true
        )

        binding.rvFields.apply {
            layoutManager = LinearLayoutManager(this@FieldAdminProfileActivity)
            adapter = fieldAdapter
        }
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

        binding.fabAddField.setOnClickListener {
            startActivity(Intent(this, AddFieldActivity::class.java))
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
                    val photoUrl = document.getString("photoUrl") ?: ""
                    val createdAt = document.getLong("createdAt") ?: 0L

                    binding.etName.setText(name)
                    binding.tvEmail.text = email
                    binding.etPhone.setText(phone)
                    binding.etBio.setText(bio)

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

    private fun loadFields() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("fields")
            .whereEqualTo("adminId", userId)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                fields.clear()

                for (document in documents) {
                    val field = Field(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        address = document.getString("address") ?: "",
                        adminId = document.getString("adminId") ?: "",
                        description = document.getString("description") ?: "",
                        amenities = document.get("amenities") as? List<String> ?: emptyList(),
                        photos = (document.get("photos") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        pricePerHour = document.getDouble("pricePerHour") ?: 0.0,
                        rating = (document.getDouble("rating") ?: 0.0).toFloat(),
                        totalRatings = document.getLong("totalRatings")?.toInt() ?: 0,
                        isActive = document.getBoolean("isActive") ?: true,
                        createdAt = document.getLong("createdAt") ?: 0L,
                        updatedAt = document.getLong("updatedAt") ?: 0L
                    )
                    fields.add(field)
                }

                fieldAdapter.notifyDataSetChanged()
                updateFieldsCount()

                if (fields.isEmpty()) {
                    binding.tvNoFields.visibility = View.VISIBLE
                    binding.rvFields.visibility = View.GONE
                } else {
                    binding.tvNoFields.visibility = View.GONE
                    binding.rvFields.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar canchas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFieldsCount() {
        binding.tvFieldsCount.text = "${fields.size} cancha(s) registrada(s)"
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

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
                updateProfileData(name, phone, bio, photoUrl)
            }
        } else {
            updateProfileData(name, phone, bio, null)
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
        photoUrl: String?
    ) {
        val userId = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "bio" to bio,
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

    private fun toggleFieldActive(field: Field) {
        firestore.collection("fields").document(field.id)
            .update("isActive", !field.isActive)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    if (field.isActive) "Cancha desactivada" else "Cancha activada",
                    Toast.LENGTH_SHORT
                ).show()
                loadFields()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteFieldDialog(field: Field) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar cancha")
            .setMessage("¿Estás seguro de eliminar '${field.name}'? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteField(field)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteField(field: Field) {
        firestore.collection("fields").document(field.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Cancha eliminada", Toast.LENGTH_SHORT).show()
                loadFields()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        loadFields()
    }
}
