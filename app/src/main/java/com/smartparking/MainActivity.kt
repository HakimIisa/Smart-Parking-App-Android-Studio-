package com.smartparking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firestore and FusedLocationProviderClient
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        val findParkingButton: Button = findViewById(R.id.findParkingButton)
        findParkingButton.setOnClickListener {
            Log.d("MainActivity", "Find Parking button clicked")
            getUserLocation { userLocation ->
                fetchParkingSpots { parkingSpots ->
                    val nearestSpot = getNearestParkingSpot(userLocation, parkingSpots)
                    if (nearestSpot != null) {
                        drawRoute(userLocation, nearestSpot)
                    } else {
                        Toast.makeText(this, "No available parking spots found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val loginButton: Button = findViewById(R.id.btnGoToLogin)
        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val addParkingButton: Button = findViewById(R.id.addParkingButton)
        addParkingButton.setOnClickListener {
            Log.d("MainActivity", "Add Parking button clicked")
            val intent = Intent(this, AddParkingActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getUserLocation(onLocationFetched: (LatLng) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    onLocationFetched(userLatLng)
                } else {
                    Toast.makeText(this, "Unable to fetch your location.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun fetchParkingSpots(onParkingFetched: (List<Pair<LatLng, Int>>) -> Unit) {
        db.collection("parking_spots")
            .whereGreaterThan("availability", 0) // Only fetch spots with availability
            .get()
            .addOnSuccessListener { result ->
                val parkingLocations = mutableListOf<Pair<LatLng, Int>>() // Store location + availability
                for (document in result) {
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0
                    val availability = document.getLong("availability")?.toInt() ?: 0
                    parkingLocations.add(Pair(LatLng(latitude, longitude), availability))
                }
                onParkingFetched(parkingLocations)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch parking spots: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getNearestParkingSpot(userLocation: LatLng, parkingSpots: List<Pair<LatLng, Int>>): LatLng? {
        return parkingSpots
            .filter { it.second > 0 } // Ensure spot has availability
            .minByOrNull { spot ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    spot.first.latitude, spot.first.longitude, results
                )
                results[0] // Sort by distance
            }?.first // Return the closest available parking
    }

    private fun drawRoute(userLocation: LatLng, parkingLocation: LatLng) {
        // Clear existing routes and markers
        mMap.clear()

        // Add markers for user location and parking location
        mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
        mMap.addMarker(MarkerOptions().position(parkingLocation).title("Parking Spot"))

        // Draw a line connecting user and parking spot
        mMap.addPolyline(
            PolylineOptions()
                .add(userLocation, parkingLocation)
                .color(ContextCompat.getColor(this, R.color.purple_500))
                .width(8f)
        )

        // Adjust camera view to show both locations
        val bounds = LatLngBounds.Builder()
            .include(userLocation)
            .include(parkingLocation)
            .build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}