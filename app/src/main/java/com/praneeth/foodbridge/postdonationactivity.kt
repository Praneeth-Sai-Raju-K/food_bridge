package com.praneeth.foodbridge

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class PostDonationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var selectedCategory = "Veg"
    private var capturedLat: Double? = null
    private var capturedLng: Double? = null

    companion object {
        const val LOCATION_PERMISSION_CODE = 201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_donation)

        db = FirebaseFirestore.getInstance()

        val foodNameInput = findViewById<EditText>(R.id.foodNameInput)
        val quantityInput = findViewById<EditText>(R.id.quantityInput)
        val locationInput = findViewById<EditText>(R.id.locationInput)
        val expiryHoursInput = findViewById<EditText>(R.id.expiryHoursInput)
        val foodNameError = findViewById<TextView>(R.id.foodNameError)
        val quantityError = findViewById<TextView>(R.id.quantityError)
        val locationError = findViewById<TextView>(R.id.locationError)
        val postButton = findViewById<Button>(R.id.postButton)
        val postProgressBar = findViewById<ProgressBar>(R.id.postProgressBar)
        val useCurrentLocationButton = findViewById<Button>(R.id.useCurrentLocationButton)

        val catVegButton = findViewById<Button>(R.id.catVegButton)
        val catNonVegButton = findViewById<Button>(R.id.catNonVegButton)
        val catBakeryButton = findViewById<Button>(R.id.catBakeryButton)
        val catCookedButton = findViewById<Button>(R.id.catCookedButton)

        fun updateCategoryColors() {
            val selectedColor = resources.getColor(R.color.green_primary, theme)
            val unselectedColor = resources.getColor(R.color.green_light, theme)
            val selectedTextColor = resources.getColor(R.color.white, theme)
            val unselectedTextColor = resources.getColor(R.color.green_primary, theme)

            val buttons = mapOf(
                "Veg" to catVegButton,
                "Non-Veg" to catNonVegButton,
                "Bakery" to catBakeryButton,
                "Cooked" to catCookedButton
            )

            for ((category, button) in buttons) {
                val isSelected = category == selectedCategory
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isSelected) selectedColor else unselectedColor)
                button.setTextColor(if (isSelected) selectedTextColor else unselectedTextColor)
            }
        }

        catVegButton.setOnClickListener { selectedCategory = "Veg"; updateCategoryColors() }
        catNonVegButton.setOnClickListener { selectedCategory = "Non-Veg"; updateCategoryColors() }
        catBakeryButton.setOnClickListener { selectedCategory = "Bakery"; updateCategoryColors() }
        catCookedButton.setOnClickListener { selectedCategory = "Cooked"; updateCategoryColors() }

        useCurrentLocationButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_CODE)
                return@setOnClickListener
            }
            fetchCurrentLocation(locationInput, useCurrentLocationButton)
        }

        postButton.setOnClickListener {
            val foodName = foodNameInput.text.toString().trim()
            val quantity = quantityInput.text.toString().trim()
            val location = locationInput.text.toString().trim()
            val expiryHoursText = expiryHoursInput.text.toString().trim()

            var hasError = false

            if (foodName.isEmpty()) {
                foodNameError.text = "Please enter a food item"
                foodNameError.visibility = View.VISIBLE
                hasError = true
            } else foodNameError.visibility = View.GONE

            if (quantity.isEmpty()) {
                quantityError.text = "Please enter a quantity"
                quantityError.visibility = View.VISIBLE
                hasError = true
            } else quantityError.visibility = View.GONE

            if (location.isEmpty()) {
                locationError.text = "Please enter a pickup location"
                locationError.visibility = View.VISIBLE
                hasError = true
            } else locationError.visibility = View.GONE

            if (hasError) return@setOnClickListener

            val expiryHours = expiryHoursText.toIntOrNull() ?: 4
            val postTime = System.currentTimeMillis()
            val expiryTime = postTime + (expiryHours * 60 * 60 * 1000L)

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val postedByEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"

            postButton.isEnabled = false
            postButton.text = ""
            postProgressBar.visibility = View.VISIBLE

            db.collection("users").document(userId).get()
                .addOnSuccessListener { userDoc ->
                    val donorName = userDoc.getString("name") ?: "A donor"
                    val donorPhone = userDoc.getString("phone") ?: "Not provided"

                    val donation = hashMapOf(
                        "foodName" to foodName,
                        "quantity" to quantity,
                        "location" to location,
                        "category" to selectedCategory,
                        "timestamp" to postTime,
                        "expiryTime" to expiryTime,
                        "claimed" to false,
                        "postedBy" to postedByEmail,
                        "donorName" to donorName,
                        "donorPhone" to donorPhone,
                        "lat" to capturedLat,
                        "lng" to capturedLng
                    )

                    db.collection("donations")
                        .add(donation)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Donation posted successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            postButton.isEnabled = true
                            postButton.text = "Post Donation"
                            postProgressBar.visibility = View.GONE
                            Toast.makeText(this, "Failed to post: " + e.message, Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    postButton.isEnabled = true
                    postButton.text = "Post Donation"
                    postProgressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to load your info: " + e.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val locationInput = findViewById<EditText>(R.id.locationInput)
            val useCurrentLocationButton = findViewById<Button>(R.id.useCurrentLocationButton)
            fetchCurrentLocation(locationInput, useCurrentLocationButton)
        } else {
            Toast.makeText(this, "Location permission denied. Please type your location manually.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("MissingPermission")
    private fun fetchCurrentLocation(locationInput: EditText, button: Button) {
        button.text = "Getting location..."
        button.isEnabled = false

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        Thread {
            try {
                var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }

                runOnUiThread {
                    if (location != null) {
                        capturedLat = location.latitude
                        capturedLng = location.longitude

                        try {
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (!results.isNullOrEmpty()) {
                                val address = results[0]
                                val addressLine = address.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
                                locationInput.setText(addressLine)
                            } else {
                                locationInput.setText("${location.latitude}, ${location.longitude}")
                            }
                        } catch (e: Exception) {
                            locationInput.setText("${location.latitude}, ${location.longitude}")
                        }

                        Toast.makeText(this, "Location captured!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Couldn't get location. Make sure GPS is on, or type manually.", Toast.LENGTH_LONG).show()
                    }
                    button.text = "📍 Use My Current Location"
                    button.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Location error: " + e.message, Toast.LENGTH_LONG).show()
                    button.text = "📍 Use My Current Location"
                    button.isEnabled = true
                }
            }
        }.start()
    }
}