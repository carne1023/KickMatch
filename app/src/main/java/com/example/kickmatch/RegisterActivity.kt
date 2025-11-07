package com.example.kickmatch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kickmatch.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupToolbar()
        setupListeners()
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

            if (validateFields(name, email, password, confirmPassword)) {
                registerUser(name, email, password)
            }
        }

        // Ir a login
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

    private fun validateFields(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
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

        if (!binding.cbTerms.isChecked) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerUser(name: String, email: String, password: String) {
        binding.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Guardar datos de perfil en Firestore
                    val userId = auth.currentUser?.uid
                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to "",
                        "bio" to "",
                        "photoUrl" to "",
                        "createdAt" to System.currentTimeMillis()
                    )

                    userId?.let {
                        firestore.collection("users").document(it)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Registro exitoso",
                                    Toast.LENGTH_SHORT
                                ).show()
                                goToHome()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Error al guardar datos: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.btnRegister.isEnabled = true
                            }
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

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finishAffinity()
    }
}