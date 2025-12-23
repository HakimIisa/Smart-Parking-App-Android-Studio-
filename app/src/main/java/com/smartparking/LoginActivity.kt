package com.smartparking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var parkingNameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = FirebaseFirestore.getInstance()

        // Initialize views
        parkingNameInput = findViewById(R.id.etParkingName)
        passwordInput = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        loginButton.setOnClickListener {
            val parkingName = parkingNameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (parkingName.isNotEmpty() && password.isNotEmpty()) {
                authenticateParkingOwner(parkingName, password)
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun authenticateParkingOwner(parkingName: String, password: String) {
        progressBar.visibility = View.VISIBLE
        loginButton.isEnabled = false

        db.collection("parking_spots")
            .whereEqualTo("name", parkingName)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    val storedPassword = document.getString("password") // Fixed syntax error

                    if (storedPassword != null && storedPassword == password) { // Handle null case
                        // Login successful, navigate to owner dashboard
                        val intent = Intent(this, OwnerDashboardActivity::class.java)
                        intent.putExtra("parkingId", document.id) // Pass parking ID
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Incorrect password.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Parking name not found.", Toast.LENGTH_SHORT).show()
                }
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true
            }
    }
}