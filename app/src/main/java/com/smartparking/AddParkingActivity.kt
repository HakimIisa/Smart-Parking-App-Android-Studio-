package com.smartparking

import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddParkingActivity : AppCompatActivity() {

    private lateinit var parkingName: EditText
    private lateinit var parkingLocation: EditText
    private lateinit var parkingCapacity: EditText
    private lateinit var parkingPassword: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_parking)

        // Initialize views
        parkingName = findViewById(R.id.etParkingName)
        parkingLocation = findViewById(R.id.etLocation)
        parkingCapacity = findViewById(R.id.etCapacity)
        parkingPassword = findViewById(R.id.etPassword) // New password field
        submitButton = findViewById(R.id.btn_submit_parking)
        progressBar = findViewById(R.id.progress_bar)
        db = FirebaseFirestore.getInstance()

        submitButton.setOnClickListener {
            val name = parkingName.text.toString().trim()
            val location = parkingLocation.text.toString().trim()
            val capacity = parkingCapacity.text.toString().toIntOrNull() ?: 0
            val password = parkingPassword.text.toString().trim() // Get the password field value

            if (name.isNotEmpty() && location.isNotEmpty() && capacity > 0 && password.isNotEmpty()) {
                checkParkingNameUniqueness(name, location, capacity, password)
            } else {
                Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Check if the parking name is unique
    private fun checkParkingNameUniqueness(name: String, location: String, capacity: Int, password: String) {
        progressBar.visibility = View.VISIBLE
        submitButton.isEnabled = false

        db.collection("parking_spots")
            .whereEqualTo("name", name)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    // If the parking name is unique, save it
                    getCoordinatesFromLocationNameAndSave(name, location, capacity, password)
                } else {
                    Toast.makeText(this, "The parking name is already taken.", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    submitButton.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                submitButton.isEnabled = true
            }
    }

    // Get coordinates and save parking data
    private fun getCoordinatesFromLocationNameAndSave(name: String, location: String, capacity: Int, password: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(location, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val lat = address.latitude
                val lng = address.longitude

                val parkingData = hashMapOf(
                    "name" to name,
                    "location" to location,
                    "capacity" to capacity,
                    "latitude" to lat,
                    "longitude" to lng,
                    "password" to password, // Include password in the parking data
                    "availability" to capacity // Initialize availability with capacity
                )

                db.collection("parking_spots")
                    .add(parkingData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Parking Spot Added!", Toast.LENGTH_SHORT).show()
                        parkingName.text.clear()
                        parkingLocation.text.clear()
                        parkingCapacity.text.clear()
                        parkingPassword.text.clear() // Clear password field
                        progressBar.visibility = View.GONE
                        submitButton.isEnabled = true
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        submitButton.isEnabled = true
                    }
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                submitButton.isEnabled = true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            submitButton.isEnabled = true
        }
    }
}