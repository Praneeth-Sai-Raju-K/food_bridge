package com.praneeth.foodbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var myDonationsContainer: LinearLayout
    private lateinit var statsText: TextView
    private var currentUserEmail = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val userNameText = findViewById<TextView>(R.id.userNameText)
        val userEmailText = findViewById<TextView>(R.id.userEmailText)
        val userRoleBadge = findViewById<TextView>(R.id.userRoleBadge)
        statsText = findViewById(R.id.statsText)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val aboutButton = findViewById<Button>(R.id.aboutButton)
        val donationsListTitle = findViewById<TextView>(R.id.donationsListTitle)
        myDonationsContainer = findViewById(R.id.myDonationsContainer)

        findViewById<Button>(R.id.navHomeButton).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.navProfileButton).setOnClickListener {
            // already here
        }

        val currentUser = auth.currentUser
        currentUserEmail = currentUser?.email ?: "Unknown"
        val userId = currentUser?.uid ?: ""

        userEmailText.text = currentUserEmail

        aboutButton.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        logoutButton.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("You'll need to log in again to access your account.")
                .setPositiveButton("Log Out") { _, _ ->
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc.getString("name") ?: "Food Bridge User"
                val role = userDoc.getString("role") ?: "Donor"

                userNameText.text = name
                userRoleBadge.text = role

                if (role == "Donor") {
                    donationsListTitle.text = "My Donations"
                    loadMyPostedDonations()
                } else {
                    donationsListTitle.text = "My Claimed Pickups"
                    loadMyClaimedDonations()
                }
            }
    }

    private fun addRow(text: String, showDeleteFor: String? = null, showMapFor: String? = null) {
        val rowLayout = LinearLayout(this)
        rowLayout.orientation = LinearLayout.VERTICAL
        val rowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        rowParams.bottomMargin = 8
        rowLayout.layoutParams = rowParams
        rowLayout.setBackgroundResource(R.drawable.rounded_input_bg)
        rowLayout.setPadding(16, 16, 16, 16)

        val textView = TextView(this)
        textView.text = text
        textView.textSize = 14f
        rowLayout.addView(textView)

        if (showDeleteFor != null || showMapFor != null) {
            val buttonRow = LinearLayout(this)
            buttonRow.orientation = LinearLayout.HORIZONTAL
            val buttonRowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            buttonRowParams.topMargin = 8
            buttonRow.layoutParams = buttonRowParams

            if (showMapFor != null) {
                val mapButton = Button(this)
                mapButton.text = "View Map"
                mapButton.textSize = 11f
                mapButton.setBackgroundResource(R.drawable.rounded_button_bg)
                mapButton.setTextColor(resources.getColor(R.color.white, theme))
                mapButton.setOnClickListener {
                    val intent = Intent(this, MapActivity::class.java)
                    intent.putExtra("location", showMapFor)
                    startActivity(intent)
                }
                val mapParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                mapParams.marginEnd = 8
                mapButton.layoutParams = mapParams
                buttonRow.addView(mapButton)
            }

            if (showDeleteFor != null) {
                val deleteButton = Button(this)
                deleteButton.text = "Delete"
                deleteButton.textSize = 11f
                deleteButton.setBackgroundResource(R.drawable.rounded_button_bg)
                deleteButton.setTextColor(resources.getColor(R.color.white, theme))
                deleteButton.setOnClickListener {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Delete this donation?")
                        .setMessage("This can't be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            db.collection("donations").document(showDeleteFor).delete()
                                .addOnSuccessListener { loadMyPostedDonations() }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                val deleteParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                deleteButton.layoutParams = deleteParams
                buttonRow.addView(deleteButton)
            }

            rowLayout.addView(buttonRow)
        }

        myDonationsContainer.addView(rowLayout)
    }

    private fun loadMyPostedDonations() {
        db.collection("donations")
            .whereEqualTo("postedBy", currentUserEmail)
            .get()
            .addOnSuccessListener { result ->
                myDonationsContainer.removeAllViews()
                var claimedCount = 0
                var totalCount = 0
                var ratingSum = 0
                var ratingCount = 0

                for (document in result) {
                    totalCount++
                    val foodName = document.getString("foodName") ?: "Unknown item"
                    val claimed = document.getBoolean("claimed") ?: false
                    val location = document.getString("location") ?: ""
                    if (claimed) claimedCount++

                    val rating = document.getLong("rating")
                    if (rating != null) {
                        ratingSum += rating.toInt()
                        ratingCount++
                    }

                    val status = if (claimed) "✅ Claimed" else "🕓 Available"
                    val ratingText = if (rating != null) " ⭐$rating" else ""

                    if (!claimed) {
                        addRow("$foodName - $status$ratingText", showDeleteFor = document.id, showMapFor = location)
                    } else {
                        val claimedByName = document.getString("claimedByName") ?: ""
                        val claimedByPhone = document.getString("claimedByPhone") ?: ""
                        addRow("$foodName - $status$ratingText\nClaimed by $claimedByName ($claimedByPhone)", showMapFor = location)
                    }
                }

                if (totalCount == 0) {
                    addRow("You haven't posted any donations yet.")
                }

                val avgRating = if (ratingCount > 0) String.format("%.1f", ratingSum.toDouble() / ratingCount) else "N/A"
                statsText.text = "🌱 $totalCount donation" + (if (totalCount == 1) "" else "s") + " posted • $claimedCount claimed • ⭐ $avgRating avg rating"
            }
    }

    private fun loadMyClaimedDonations() {
        db.collection("donations")
            .whereEqualTo("claimedBy", currentUserEmail)
            .get()
            .addOnSuccessListener { result ->
                myDonationsContainer.removeAllViews()
                var count = 0

                for (document in result) {
                    count++
                    val foodName = document.getString("foodName") ?: "Unknown item"
                    val location = document.getString("location") ?: ""
                    val donorName = document.getString("donorName") ?: ""
                    val donorPhone = document.getString("donorPhone") ?: ""
                    addRow("$foodName - picked up from $location\nDonor: $donorName ($donorPhone)", showMapFor = location)
                }

                if (count == 0) {
                    addRow("You haven't claimed any donations yet.")
                }

                statsText.text = "🚴 $count pickup" + (if (count == 1) "" else "s") + " completed"
            }
    }
}