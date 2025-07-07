package com.example.projectmobiles.presentation.signUp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.text.InputType
import android.view.MotionEvent
import android.widget.*
import com.example.projectmobiles.R
import com.example.projectmobiles.presentation.login.Login
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class Signup : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val emailField = findViewById<EditText>(R.id.email_name)
        val usernameField = findViewById<EditText>(R.id.username)
        val passwordField = findViewById<EditText>(R.id.pass)
        val confirmPasswordField = findViewById<EditText>(R.id.ConfPass)
        val phoneField = findViewById<EditText>(R.id.phone)

        var isPasswordVisible = false
        var isConfirmPasswordVisible = false

// Toggle untuk password utama
        passwordField.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= passwordField.right - passwordField.compoundDrawables[DRAWABLE_RIGHT]?.bounds?.width()!!) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        passwordField.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.baseline_pass_24, 0, R.drawable.ic_eye_on, 0
                        )
                    } else {
                        passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        passwordField.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.baseline_pass_24, 0, R.drawable.ic_eye_off, 0
                        )
                    }
                    passwordField.setSelection(passwordField.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

// Toggle untuk confirm password
        confirmPasswordField.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= confirmPasswordField.right - confirmPasswordField.compoundDrawables[DRAWABLE_RIGHT]?.bounds?.width()!!) {
                    isConfirmPasswordVisible = !isConfirmPasswordVisible
                    if (isConfirmPasswordVisible) {
                        confirmPasswordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        confirmPasswordField.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.baseline_pass_24, 0, R.drawable.ic_eye_on, 0
                        )
                    } else {
                        confirmPasswordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        confirmPasswordField.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.baseline_pass_24, 0, R.drawable.ic_eye_off, 0
                        )
                    }
                    confirmPasswordField.setSelection(confirmPasswordField.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        val signupButton = findViewById<Button>(R.id.signup_button)
        val loginRedirect = findViewById<TextView>(R.id.loginRedirectText)
        loginRedirect.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        signupButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()
            val phone           = phoneField.text.toString().trim()

            when {
                email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
                !isEmailValid(email) -> {
                    Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                }
                !isPasswordStrong(password) -> {
                    Toast.makeText(this, "Password must be at least 8 characters with upper, lower, and number", Toast.LENGTH_LONG).show()
                }
                password != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                phone.isEmpty() -> {
                    Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show()
                }
                phone.length > 13 -> {
                    Toast.makeText(this, "Phone number cannot exceed 13 digits", Toast.LENGTH_SHORT).show()
                }
                !phone.all { it.isDigit() } -> {
                    Toast.makeText(this, "Phone number must be numeric", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                val userMap = hashMapOf(
                                    "email" to email,
                                    "email_lower" to email.lowercase(),
                                    "username" to username,
                                    "phone"        to phone
                                )
                                userId?.let {
                                    db.collection("users").document(it)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, Login::class.java))
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Error saving user: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            } else {
                                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
        }
    }

    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return email.matches(emailPattern.toRegex())
    }

    private fun isPasswordStrong(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$")
        return passwordPattern.containsMatchIn(password)
    }
}