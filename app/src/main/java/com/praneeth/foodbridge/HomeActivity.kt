package com.praneeth.foodbridge

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class DonationItem(
    val id: String,
    val displayText: String,
    val category: String,
    val foodName: String,
    val location: String,
    val expiryTime: Long,
    val timestamp: Long,
    val donorName: String,
    val donorPhone: String
)

class HomeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var impactCounterText: TextView
    private lateinit var postDonationButton: Button
    private lateinit var searchInput: EditText
    private val allDonations = mutableListOf<DonationItem>()
    private var currentUserRole = "Donor"
    private var currentFilter = "All"
    private var currentSort = "Newest"
    private var searchQuery = ""
    private var donationsListener: ListenerRegistration? = null
    private val previouslyClaimedIds = mutableSetOf<String>()
    private var isFirstLoad = true

    companion object {
        const val CHANNEL_ID = "foodbridge_claims"
        const val NOTIFICATION_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        db = FirebaseFirestore.getInstance()
        listView = findViewById(R.id.donationListView)
        impactCounterText = findViewById(R.id.impactCounterText)
        postDonationButton = findViewById(R.id.postDonationButton)
        searchInput = findViewById(R.id.searchInput)

        createNotificationChannel()
        requestNotificationPermission()

        postDonationButton.setOnClickListener {
            startActivity(Intent(this, PostDonationActivity::class.java))
        }

        findViewById<Button>(R.id.navHomeButton).setOnClickListener {
            // already on Home, do nothing
        }

        findViewById<Button>(R.id.navProfileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim().lowercase()
                refreshListDisplay()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupFilterButtons()
        setupSortButtons()

        listView.setOnItemClickListener { _, _, position, _ ->
            val visibleList = getFilteredList()
            if (position >= visibleList.size) return@setOnItemClickListener

            if (currentUserRole == "Donor") {
                Toast.makeText(this, "Donors cannot claim donations. Only NGOs and Volunteers can claim.", Toast.LENGTH_LONG).show()
            } else {
                showClaimDialog(visibleList[position])
            }
        }

        checkUserRoleAndUpdateUI()
    }

    override fun onStart() {
        super.onStart()
        startListeningForDonations()
        startListeningForMyClaimedDonations()
    }

    override fun onStop() {
        super.onStop()
        donationsListener?.remove()
    }

    private fun setupSortButtons() {
        val sortNewestButton = findViewById<Button>(R.id.sortNewestButton)
        val sortExpiringButton = findViewById<Button>(R.id.sortExpiringButton)

        fun updateSortColors() {
            val selectedColor = resources.getColor(R.color.green_primary, theme)
            val unselectedColor = resources.getColor(R.color.green_light, theme)
            val selectedTextColor = resources.getColor(R.color.white, theme)
            val unselectedTextColor = resources.getColor(R.color.green_primary, theme)

            val isNewest = currentSort == "Newest"
            sortNewestButton.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isNewest) selectedColor else unselectedColor)
            sortNewestButton.setTextColor(if (isNewest) selectedTextColor else unselectedTextColor)
            sortExpiringButton.backgroundTintList = android.content.res.ColorStateList.valueOf(if (!isNewest) selectedColor else unselectedColor)
            sortExpiringButton.setTextColor(if (!isNewest) selectedTextColor else unselectedTextColor)
        }

        sortNewestButton.setOnClickListener {
            currentSort = "Newest"
            updateSortColors()
            refreshListDisplay()
        }
        sortExpiringButton.setOnClickListener {
            currentSort = "Expiring"
            updateSortColors()
            refreshListDisplay()
        }
        updateSortColors()
    }

    private fun setupFilterButtons() {
        val filterAllButton = findViewById<Button>(R.id.filterAllButton)
        val filterVegButton = findViewById<Button>(R.id.filterVegButton)
        val filterNonVegButton = findViewById<Button>(R.id.filterNonVegButton)
        val filterBakeryButton = findViewById<Button>(R.id.filterBakeryButton)
        val filterCookedButton = findViewById<Button>(R.id.filterCookedButton)

        val buttons = mapOf(
            "All" to filterAllButton,
            "Veg" to filterVegButton,
            "Non-Veg" to filterNonVegButton,
            "Bakery" to filterBakeryButton,
            "Cooked" to filterCookedButton
        )

        fun updateFilterColors() {
            val selectedColor = resources.getColor(R.color.green_primary, theme)
            val unselectedColor = resources.getColor(R.color.green_light, theme)
            val selectedTextColor = resources.getColor(R.color.white, theme)
            val unselectedTextColor = resources.getColor(R.color.green_primary, theme)

            for ((filterName, button) in buttons) {
                val isSelected = filterName == currentFilter
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isSelected) selectedColor else unselectedColor)
                button.setTextColor(if (isSelected) selectedTextColor else unselectedTextColor)
            }
        }

        for ((filterName, button) in buttons) {
            button.setOnClickListener {
                currentFilter = filterName
                updateFilterColors()
                refreshListDisplay()
            }
        }

        updateFilterColors()
    }

    private fun getFilteredList(): List<DonationItem> {
        var list = if (currentFilter == "All") allDonations else allDonations.filter { it.category == currentFilter }

        if (searchQuery.isNotEmpty()) {
            list = list.filter {
                it.foodName.lowercase().contains(searchQuery) || it.location.lowercase().contains(searchQuery)
            }
        }

        list = if (currentSort == "Newest") {
            list.sortedByDescending { it.timestamp }
        } else {
            list.sortedBy { it.expiryTime }
        }

        return list
    }

    private fun refreshListDisplay() {
        val visibleList = getFilteredList()
        val texts = if (visibleList.isEmpty()) listOf("No donations match your search/filter.") else visibleList.map { it.displayText }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, texts)

        val count = visibleList.size
        impactCounterText.text = "🌱 $count donation" + (if (count == 1) "" else "s") + " available right now"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Donation Claims", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifies you when someone claims your donation"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE)
            }
        }
    }

    private fun showClaimNotification(foodName: String, claimedBy: String, claimedByPhone: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your donation was claimed!")
            .setContentText("$foodName claimed by $claimedBy ($claimedByPhone)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun startListeningForMyClaimedDonations() {
        val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        db.collection("donations")
            .whereEqualTo("postedBy", myEmail)
            .whereEqualTo("claimed", true)
            .addSnapshotListener { result, error ->
                if (error != null || result == null) return@addSnapshotListener

                for (document in result) {
                    val id = document.id
                    if (id !in previouslyClaimedIds) {
                        previouslyClaimedIds.add(id)
                        if (!isFirstLoad) {
                            val foodName = document.getString("foodName") ?: "Your donation"
                            val claimedByName = document.getString("claimedByName") ?: "someone"
                            val claimedByPhone = document.getString("claimedByPhone") ?: ""
                            showClaimNotification(foodName, claimedByName, claimedByPhone)
                        }
                    }
                }
                isFirstLoad = false
            }
    }

    private fun checkUserRoleAndUpdateUI() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                currentUserRole = document.getString("role") ?: "Donor"
                postDonationButton.visibility = if (currentUserRole == "Donor") View.VISIBLE else View.GONE
            }
    }

    private fun showClaimDialog(item: DonationItem) {
        AlertDialog.Builder(this)
            .setTitle("Claim this donation?")
            .setMessage("${item.foodName}\nDonor: ${item.donorName}\nPhone: ${item.donorPhone}\n\nClaiming will mark it as taken.")
            .setPositiveButton("Claim") { _, _ ->
                claimDonation(item.id)
            }
            .setNeutralButton("View Map") { _, _ ->
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("location", item.location)
                intent.putExtra("foodName", item.foodName)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun claimDonation(donationId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val claimerName = userDoc.getString("name") ?: "Someone"
                val claimerPhone = userDoc.getString("phone") ?: "Not provided"

                db.collection("donations").document(donationId)
                    .update(
                        mapOf(
                            "claimed" to true,
                            "claimedBy" to currentUserEmail,
                            "claimedByName" to claimerName,
                            "claimedByPhone" to claimerPhone
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "Donation claimed!", Toast.LENGTH_SHORT).show()
                        showRatingDialog(donationId)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to claim: " + e.message, Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun showRatingDialog(donationId: String) {
        val stars = arrayOf("⭐ 1 - Poor", "⭐⭐ 2 - Fair", "⭐⭐⭐ 3 - Good", "⭐⭐⭐⭐ 4 - Very Good", "⭐⭐⭐⭐⭐ 5 - Excellent")
        AlertDialog.Builder(this)
            .setTitle("Rate this pickup experience")
            .setItems(stars) { _, which ->
                val rating = which + 1
                db.collection("donations").document(donationId).update("rating", rating)
            }
            .setNegativeButton("Skip", null)
            .show()
    }

    private fun formatTimeRemaining(expiryTime: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = expiryTime - now
        if (diffMillis <= 0) return "Expired"
        val hours = diffMillis / (60 * 60 * 1000)
        val minutes = (diffMillis % (60 * 60 * 1000)) / (60 * 1000)
        return if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
    }

    private fun startListeningForDonations() {
        donationsListener = db.collection("donations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { result, error ->
                if (error != null || result == null) return@addSnapshotListener

                allDonations.clear()
                val now = System.currentTimeMillis()

                for (document in result) {
                    val claimed = document.getBoolean("claimed") ?: false
                    if (claimed) continue

                    val expiryTime = document.getLong("expiryTime") ?: (now + 999999999L)
                    if (expiryTime <= now) continue

                    val foodName = document.getString("foodName") ?: "Unknown item"
                    val quantity = document.getString("quantity") ?: ""
                    val location = document.getString("location") ?: ""
                    val category = document.getString("category") ?: ""
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val donorName = document.getString("donorName") ?: "A donor"
                    val donorPhone = document.getString("donorPhone") ?: "Not provided"
                    val timeLeft = formatTimeRemaining(expiryTime)

                    val categoryTag = if (category.isNotEmpty()) "[$category] " else ""
                    val displayText = "🍽 $categoryTag$foodName ($quantity) - $location • ⏱ $timeLeft"

                    allDonations.add(
                        DonationItem(document.id, displayText, category, foodName, location, expiryTime, timestamp, donorName, donorPhone)
                    )
                }

                refreshListDisplay()
            }
    }
}