package com.praneeth.foodbridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isSignUpMode = false
    private var selectedRole = "Donor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpText = findViewById<TextView>(R.id.signUpText)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)
        val roleSelectorLayout = findViewById<LinearLayout>(R.id.roleSelectorLayout)
        val roleDonorButton = findViewById<Button>(R.id.roleDonorButton)
        val roleNgoButton = findViewById<Button>(R.id.roleNgoButton)
        val roleVolunteerButton = findViewById<Button>(R.id.roleVolunteerButton)

        fun updateRoleButtonColors() {
            val selectedColor = resources.getColor(R.color.green_primary, theme)
            val unselectedColor = resources.getColor(R.color.green_light, theme)
            val selectedTextColor = resources.getColor(R.color.white, theme)
            val unselectedTextColor = resources.getColor(R.color.green_primary, theme)

            roleDonorButton.backgroundTintList = android.content.res.ColorStateList.valueOf(if (selectedRole == "Donor") selectedColor else unselectedColor)
            roleDonorButton.setTextColor(if (selectedRole == "Donor") selectedTextColor else unselectedTextColor)

            roleNgoButton.backgroundTintList = android.content.res.ColorStateList.valueOf(if (selectedRole == "NGO") selectedColor else unselectedColor)
            roleNgoButton.setTextColor(if (selectedRole == "NGO") selectedTextColor else unselectedTextColor)

            roleVolunteerButton.backgroundTintList = android.content.res.ColorStateList.valueOf(if (selectedRole == "Volunteer") selectedColor else unselectedColor)
            roleVolunteerButton.setTextColor(if (selectedRole == "Volunteer") selectedTextColor else unselectedTextColor)
        }

        roleDonorButton.setOnClickListener { selectedRole = "Donor"; updateRoleButtonColors() }
        roleNgoButton.setOnClickListener { selectedRole = "NGO"; updateRoleButtonColors() }
        roleVolunteerButton.setOnClickListener { selectedRole = "Volunteer"; updateRoleButtonColors() }

        fun setLoading(isLoading: Boolean) {
            loginButton.isEnabled = !isLoading
            loginButton.alpha = if (isLoading) 0.6f else 1.0f
        }

        forgotPasswordText.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter your email above first, then tap 'Forgot password?'", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed: " + e.message, Toast.LENGTH_LONG).show()
                }
        }

        loginButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSignUpMode) {
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (phone.isEmpty()) {
                    Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                setLoading(true)
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val userId = authResult.user?.uid ?: ""
                        val userData = hashMapOf(
                            "name" to name,
                            "phone" to phone,
                            "email" to email,
                            "role" to selectedRole
                        )
                        db.collection("users").document(userId).set(userData)

                        setLoading(false)
                        Toast.makeText(this, "Account created as $selectedRole! You can log in now.", Toast.LENGTH_SHORT).show()
                        isSignUpMode = false
                        loginButton.text = "Log In"
                        signUpText.text = "New here? Tap to Sign Up"
                        roleSelectorLayout.visibility = View.GONE
                        nameInput.visibility = View.GONE
                        phoneInput.visibility = View.GONE
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "Sign up failed: " + e.message, Toast.LENGTH_LONG).show()
                    }
            } else {
                setLoading(true)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        setLoading(false)
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "Login failed: " + e.message, Toast.LENGTH_LONG).show()
                    }
            }
        }

        signUpText.setOnClickListener {
            isSignUpMode = !isSignUpMode
            if (isSignUpMode) {
                loginButton.text = "Sign Up"
                signUpText.text = "Already have an account? Tap to Log In"
                roleSelectorLayout.visibility = View.VISIBLE
                nameInput.visibility = View.VISIBLE
                phoneInput.visibility = View.VISIBLE
                updateRoleButtonColors()
            } else {
                loginButton.text = "Log In"
                signUpText.text = "New here? Tap to Sign Up"
                roleSelectorLayout.visibility = View.GONE
                nameInput.visibility = View.GONE
                phoneInput.visibility = View.GONE
            }
        }
    }
}