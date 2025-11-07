package com.example.kickmatch

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.kickmatch.adapter.FieldAdapter
import com.example.kickmatch.api.GeoapifyService
import com.example.kickmatch.databinding.ActivitySearchFieldsBinding
import com.example.kickmatch.model.Field
import kotlinx.coroutines.launch

class SearchFieldsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchFieldsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geoapifyService: GeoapifyService
    private lateinit var fieldAdapter: FieldAdapter
    private val fields = mutableListOf<Field>()
    private val allFields = mutableListOf<Field>()
    private var currentLocation: Location? = null
    private val LOCATION_PERMISSION_REQUEST = 100
    private var isLoadingFromGeoapify = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchFieldsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geoapifyService = GeoapifyService()
        setupToolbar()
        setupRecyclerView()
        setupFilters()
        requestLocationPermission()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Buscar Canchas"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        fieldAdapter = FieldAdapter(
            fields = fields,
            onFieldClick = { field ->
                Toast.makeText(this, "Detalles de ${field.name}", Toast.LENGTH_SHORT).show()
            },
            onBookClick = { field ->
                Toast.makeText(this, "Reservar ${field.name}", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvFields.layoutManager = LinearLayoutManager(this)
        binding.rvFields.adapter = fieldAdapter
    }

    private fun setupFilters() {
        binding.btnSearch.setOnClickListener { filterFields() }
        binding.chipGroupType.setOnCheckedChangeListener { _, _ -> filterFields() }
        binding.chipGroupSurface.setOnCheckedChangeListener { _, _ -> filterFields() }
        binding.btnApplyFilters.setOnClickListener { filterFields() }
        binding.btnClearFilters.setOnClickListener { clearFilters() }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Permiso de ubicación denegado. Se usarán canchas de demostración.",
                    Toast.LENGTH_LONG
                ).show()
                loadDemoFields()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    fetchNearbyFields(location.latitude, location.longitude)
                } else {
                    fetchNearbyFields(3.4516, -76.5320)
                }
            }.addOnFailureListener {
                fetchNearbyFields(3.4516, -76.5320)
            }
        }
    }

    private fun fetchNearbyFields(latitude: Double, longitude: Double) {
        isLoadingFromGeoapify = true
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = geoapifyService.searchSoccerFields(latitude, longitude)
            binding.progressBar.visibility = View.GONE
            isLoadingFromGeoapify = false

            result.onSuccess { fetchedFields ->
                if (fetchedFields.isNotEmpty()) {
                    allFields.clear()
                    allFields.addAll(fetchedFields)
                    calculateDistances()
                    sortFieldsByDistance()

                    Toast.makeText(
                        this@SearchFieldsActivity,
                        "Canchas cargadas desde Geoapify (${fetchedFields.size})",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@SearchFieldsActivity,
                        "No se encontraron canchas cercanas. Mostrando demo.",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadDemoFields()
                }
            }.onFailure { e ->
                Toast.makeText(
                    this@SearchFieldsActivity,
                    "Error al obtener datos: ${e.message ?: "desconocido"}",
                    Toast.LENGTH_LONG
                ).show()
                loadDemoFields()
            }
        }
    }


    private fun calculateDistances() {
        currentLocation?.let { userLocation ->
            allFields.forEach { field ->
                val fieldLocation = Location("").apply {
                    latitude = field.latitude
                    longitude = field.longitude
                }
                field.distance = userLocation.distanceTo(fieldLocation) / 1000
            }
        }
    }

    private fun sortFieldsByDistance() {
        allFields.sortBy { it.distance }
        filterFields()
    }

    private fun loadDemoFields() {
        allFields.clear()
        allFields.addAll(
            listOf(
                Field(
                    id = "1",
                    name = "Cancha Sintética Los Campeones",
                    address = "Cra 5 #10-20, Valle del Cauca",
                    type = "Fútbol 5",
                    surface = "Sintética",
                    pricePerHour = 80000.0,
                    latitude = 3.4516,
                    longitude = -76.5320,
                    rating = 4.5f,
                    imageUrl = "",
                    hasParking = true,
                    hasLighting = true,
                    hasShowers = true
                ),
                Field(
                    id = "2",
                    name = "Complejo Deportivo El Diamante",
                    address = "Calle 25 #100-50, Valle del Cauca",
                    type = "Fútbol 7",
                    surface = "Sintética",
                    pricePerHour = 120000.0,
                    latitude = 3.4376,
                    longitude = -76.5225,
                    rating = 4.8f,
                    imageUrl = "",
                    hasParking = true,
                    hasLighting = true,
                    hasShowers = true
                )
            )
        )
        filterFields()
    }

    private fun filterFields() {
        fields.clear()
        val query = binding.etSearch.text.toString().trim().lowercase()
        val selectedType = when (binding.chipGroupType.checkedChipId) {
            R.id.chipFutbol5 -> "Fútbol 5"
            R.id.chipFutbol7 -> "Fútbol 7"
            R.id.chipFutbol11 -> "Fútbol 11"
            else -> null
        }
        val selectedSurface = when (binding.chipGroupSurface.checkedChipId) {
            R.id.chipSintetica -> "Sintética"
            R.id.chipNatural -> "Natural"
            R.id.chipCemento -> "Cemento"
            else -> null
        }
        val priceRange = binding.rangeSliderPrice.values
        val minPrice = priceRange[0].toDouble() * 1000
        val maxPrice = priceRange[1].toDouble() * 1000
        fields.addAll(allFields.filter { field ->
            val matchesQuery = query.isEmpty() ||
                    field.name.lowercase().contains(query) ||
                    field.address.lowercase().contains(query)
            val matchesType = selectedType == null || field.type == selectedType
            val matchesSurface = selectedSurface == null || field.surface == selectedSurface
            val matchesPrice = field.pricePerHour in minPrice..maxPrice
            matchesQuery && matchesType && matchesSurface && matchesPrice
        })
        fieldAdapter.notifyDataSetChanged()
    }

    private fun clearFilters() {
        binding.etSearch.text?.clear()
        binding.chipGroupType.check(R.id.chipAll)
        binding.chipGroupSurface.clearCheck()
        binding.rangeSliderPrice.values = listOf(0f, 300f)
        filterFields()
    }
}
