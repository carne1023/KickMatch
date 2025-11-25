package com.example.kickmatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kickmatch.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedUserType = "player" // player o field_admin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupToolbar()
        setupListeners()
        setupPositionSelector()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        binding.chipPlayer.setOnClickListener {
            selectedUserType = "player"
            binding.chipPlayer.isChecked = true
            binding.chipFieldAdmin.isChecked = false

            // Mostrar/ocultar campos según tipo
            binding.tilPosition.visibility = View.VISIBLE
            binding.tilFieldName.visibility = View.GONE
            binding.tilFieldAddress.visibility = View.GONE
        }

        binding.chipFieldAdmin.setOnClickListener {
            selectedUserType = "field_admin"
            binding.chipPlayer.isChecked = false
            binding.chipFieldAdmin.isChecked = true

            // Mostrar/ocultar campos según tipo
            binding.tilPosition.visibility = View.GONE
            binding.tilFieldName.visibility = View.VISIBLE
            binding.tilFieldAddress.visibility = View.VISIBLE
        }

        // Checkbox términos
        binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.btnRegister.isEnabled = isChecked
        }

        // Botón registrar
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (validateFields(name, email, password, confirmPassword, phone)) {
                registerUser(name, email, password, phone)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }

        // Demo de botones de redes sociales
        binding.btnGoogle.setOnClickListener {
            Toast.makeText(this, "Registro con Google (Demo)", Toast.LENGTH_SHORT).show()
        }

        binding.btnFacebook.setOnClickListener {
            Toast.makeText(this, "Registro con Facebook (Demo)", Toast.LENGTH_SHORT).show()
        }
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

        // CORRECCIÓN: Usar android.R.layout.simple_dropdown_item_1line para el selector
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, positions)
        binding.actvPosition.setAdapter(adapter)

        // Hacer que se muestre el dropdown al hacer clic
        binding.actvPosition.setOnClickListener {
            binding.actvPosition.showDropDown()
        }
    }

    private fun validateFields(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        phone: String
    ): Boolean {
        if (name.isEmpty()) {
            binding.tilName.error = "El nombre es requerido"
            return false
        }

        if (name.length < 3) {
            binding.tilName.error = "Mínimo 3 caracteres"
            return false
        }

        binding.tilName.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "El email es requerido"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email inválido"
            return false
        }

        binding.tilEmail.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = "La contraseña es requerida"
            return false
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Mínimo 6 caracteres"
            return false
        }

        binding.tilPassword.error = null

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Las contraseñas no coinciden"
            return false
        }

        binding.tilConfirmPassword.error = null

        // Validaciones específicas para admin de canchas
        if (selectedUserType == "field_admin") {
            val fieldName = binding.etFieldName.text.toString().trim()
            val fieldAddress = binding.etFieldAddress.text.toString().trim()

            if (fieldName.isEmpty()) {
                binding.tilFieldName.error = "El nombre de la cancha es requerido"
                return false
            }

            if (fieldAddress.isEmpty()) {
                binding.tilFieldAddress.error = "La dirección es requerida"
                return false
            }

            binding.tilFieldName.error = null
            binding.tilFieldAddress.error = null
        }

        if (!binding.cbTerms.isChecked) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerUser(name: String, email: String, password: String, phone: String) {
        binding.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    if (selectedUserType == "player") {
                        createPlayerProfile(userId, name, email, phone)
                    } else {
                        createFieldAdminProfile(userId, name, email, phone)
                    }
                } else {
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(
                        this,
                        "Error: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun createPlayerProfile(userId: String?, name: String, email: String, phone: String) {
        if (userId == null) {
            binding.btnRegister.isEnabled = true
            Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener la posición seleccionada (puede estar vacía)
        val position = binding.actvPosition.text.toString().trim()

        val userData = hashMapOf(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "bio" to "",
            "photoUrl" to "",
            "userType" to "player",
            "position" to position,  // Agregar la posición
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                goToHome()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
            }
    }

    private fun createFieldAdminProfile(userId: String?, name: String, email: String, phone: String) {
        if (userId == null) {
            binding.btnRegister.isEnabled = true
            Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show()
            return
        }

        val fieldName = binding.etFieldName.text.toString().trim()
        val fieldAddress = binding.etFieldAddress.text.toString().trim()

        val userData = hashMapOf(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "bio" to "",
            "photoUrl" to "",
            "userType" to "field_admin",
            "fieldName" to fieldName,
            "fieldAddress" to fieldAddress,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                createFieldDocument(userId, fieldName, fieldAddress)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
            }
    }

    private fun createFieldDocument(adminId: String, fieldName: String, fieldAddress: String) {
        val fieldData = hashMapOf(
            "name" to fieldName,
            "address" to fieldAddress,
            "adminId" to adminId,
            "description" to "",
            "amenities" to listOf<String>(),
            "photos" to listOf<String>(),
            "pricePerHour" to 0.0,
            "rating" to 0.0,
            "totalRatings" to 0,
            "isActive" to true,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("fields")
            .add(fieldData)
            .addOnSuccessListener {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                goToHome()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear cancha: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
            }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finishAffinity()
    }
}