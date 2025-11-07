package com.example.kickmatch

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.example.kickmatch.adapter.PostAdapter
import com.example.kickmatch.databinding.ActivityHomeBinding
import com.example.kickmatch.model.Post

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        loadDemoPosts()
        setupFab()
        setupBottomNavigation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            posts = posts,
            onLikeClick = { post ->
                val index = posts.indexOf(post)
                if (index != -1) {
                    post.isLiked = !post.isLiked
                    post.likes += if (post.isLiked) 1 else -1
                    postAdapter.notifyItemChanged(index)
                }
            },
            onCommentClick = { post ->
                Toast.makeText(this, "Comentarios: Demo", Toast.LENGTH_SHORT).show()
            },
            onMenuClick = { post ->
                Toast.makeText(this, "Opciones: Demo", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = postAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            Toast.makeText(this, "Feed actualizado (Demo)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabLayout() {
        binding.chipSeguidos.setOnClickListener {
            binding.chipSeguidos.isChecked = true
            binding.chipParaTi.isChecked = false
            binding.chipFavoritos.isChecked = false
            Toast.makeText(this, "Vista: Seguidos (Demo)", Toast.LENGTH_SHORT).show()
        }

        binding.chipParaTi.setOnClickListener {
            binding.chipSeguidos.isChecked = false
            binding.chipParaTi.isChecked = true
            binding.chipFavoritos.isChecked = false
            Toast.makeText(this, "Vista: Para ti (Demo)", Toast.LENGTH_SHORT).show()
        }

        binding.chipFavoritos.setOnClickListener {
            binding.chipSeguidos.isChecked = false
            binding.chipParaTi.isChecked = false
            binding.chipFavoritos.isChecked = true
            Toast.makeText(this, "Vista: Favoritos (Demo)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFab() {
        binding.fabNewPost.setOnClickListener {
            Toast.makeText(this, "Crear post (Demo)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchFieldsActivity::class.java))
                    true
                }
                R.id.nav_add -> {
                    Toast.makeText(this, "Crear (Demo)", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_notifications -> {
                    Toast.makeText(this, "Notificaciones (Demo)", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    // Ir al perfil
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadDemoPosts() {
        posts.clear()

        posts.add(
            Post(
                id = "1",
                userId = "user1",
                userName = "Alejandra Rivera",
                userPhoto = "",
                groupName = "Nombre del grupo",
                content = "No olviden que tenemos las mejores canchas sintéticas, ven y disfrútalas!",
                imageUrl = "soccer",
                timestamp = System.currentTimeMillis() - 180000,
                likes = 21,
                comments = 4,
                isLiked = false
            )
        )

        posts.add(
            Post(
                id = "2",
                userId = "user2",
                userName = "Daniel",
                userPhoto = "",
                groupName = "",
                content = "@JuanJose @SamuelGelpu01, ¡muchachos ya tenemos sintetica!",
                imageUrl = "",
                timestamp = System.currentTimeMillis() - 7200000,
                likes = 6,
                comments = 18,
                isLiked = false
            )
        )

        posts.add(
            Post(
                id = "3",
                userId = "user3",
                userName = "Oscar",
                userPhoto = "",
                groupName = "Nombre del grupo",
                content = "¡Gran partido de hoy! El equipo azul demostró un excelente trabajo en equipo.",
                imageUrl = "player",
                timestamp = System.currentTimeMillis() - 3600000,
                likes = 15,
                comments = 7,
                isLiked = false
            )
        )

        posts.add(
            Post(
                id = "4",
                userId = "user4",
                userName = "María González",
                userPhoto = "",
                groupName = "Fútbol Femenino",
                content = "Invitación a todas las chicas interesadas en unirse a nuestro equipo. ¡Entrenamientos todos los sábados!",
                imageUrl = "",
                timestamp = System.currentTimeMillis() - 86400000,
                likes = 32,
                comments = 12,
                isLiked = true
            )
        )

        posts.add(
            Post(
                id = "5",
                userId = "user5",
                userName = "Carlos Méndez",
                userPhoto = "",
                groupName = "Liga Local",
                content = "Resultados del torneo de este fin de semana. ¡Felicitaciones a todos los equipos participantes!",
                imageUrl = "",
                timestamp = System.currentTimeMillis() - 172800000,
                likes = 45,
                comments = 23,
                isLiked = false
            )
        )

        postAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                // Ir al perfil
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_notifications -> {
                Toast.makeText(this, "Notificaciones (Demo)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}