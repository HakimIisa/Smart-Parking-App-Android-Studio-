package com.smartparking

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class OwnerDashboardActivity : AppCompatActivity() {

    private lateinit var tvParkingName: TextView
    private lateinit var tvCurrentAvailability: TextView
    private lateinit var etNewAvailability: EditText
    private lateinit var btnUpdateAvailability: Button
    private lateinit var db: FirebaseFirestore
    private var parkingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_dashboard)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize Views
        tvParkingName = findViewById(R.id.tvParkingName)
        tvCurrentAvailability = findViewById(R.id.tvCurrentAvailability)
        etNewAvailability = findViewById(R.id.etNewAvailability)
        btnUpdateAvailability = findViewById(R.id.btnUpdateAvailability)

        // Get Parking ID from Intent
        parkingId = intent.getStringExtra("parkingId")

        if (parkingId != null) {
            fetchParkingData()
        } else {
            Toast.makeText(this, "Error: No parking ID found.", Toast.LENGTH_SHORT).show()
            finish() // Close activity if ID is missing
        }

        // Update Availability Button Click
        btnUpdateAvailability.setOnClickListener {
            updateParkingAvailability()
        }
    }

    private fun fetchParkingData() {
        db.collection("parking_spots").document(parkingId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val parkingName = document.getString("name") ?: "Unknown"
                    val availability = document.getLong("availability")?.toInt() ?: 0

                    tvParkingName.text = "Parking Spot Name: $parkingName"
                    tvCurrentAvailability.text = "Available Spots: $availability"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateParkingAvailability() {
        val newAvailability = etNewAvailability.text.toString().toIntOrNull()

        if (newAvailability != null && newAvailability >= 0) {
            db.collection("parking_spots").document(parkingId!!)
                .update("availability", newAvailability)
                .addOnSuccessListener {
                    tvCurrentAvailability.text = "Available Spots: $newAvailability"
                    Toast.makeText(this, "Availability Updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Enter a valid number!", Toast.LENGTH_SHORT).show()
        }
    }
}