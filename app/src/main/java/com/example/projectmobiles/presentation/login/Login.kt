@file:Suppress("DEPRECATION")

package com.example.projectmobiles.presentation.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectmobiles.R
import com.example.projectmobiles.presentation.signUp.Signup
import com.example.projectmobiles.presentation.explore.landing
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private lateinit var progressBar: ProgressBar

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        progressBar = findViewById(R.id.progressBar)

        val emailField = findViewById<EditText>(R.id.email_name)
        val passwordField = findViewById<EditText>(R.id.pass)
        val loginBtn = findViewById<Button>(R.id.signup_button)
        val signupRedirectText = findViewById<TextView>(R.id.loginRedirectText)

        val noSpaceFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ")) "" else null
        }
        emailField.filters = arrayOf(noSpaceFilter)
        passwordField.filters = arrayOf(noSpaceFilter)

        signupRedirectText.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
            finish()
        }

        val forgotPassword = findViewById<TextView>(R.id.forgot)
        forgotPassword.setOnClickListener {
            startActivity(Intent(this, ResetPassword::class.java))
        }

        loginBtn.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty()) {
                emailField.error = "Email harus diisi"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.error = "Email tidak valid"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordField.error = "Password harus diisi"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener { document ->
                                    val storedEmail = document.getString("email")
                                    if (storedEmail == email) {
                                        Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, landing::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Email tidak sesuai dengan kapitalisasi saat pendaftaran", Toast.LENGTH_LONG).show()
                                        auth.signOut()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Gagal ambil data pengguna: ${e.message}", Toast.LENGTH_LONG).show()
                                    auth.signOut()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        var isPasswordVisible = false
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

        val googleBtn = findViewById<Button>(R.id.btnGoogle)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.client_id)) // pakai client_id dari strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleBtn.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GoogleSignIn", "Google sign-in successful: ${account.displayName}")

                // Log idToken untuk memastikan token yang diterima valid
                Log.d("GoogleSignIn", "Account ID Token: ${account.idToken}")

                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { authTask ->
                        progressBar.visibility = View.VISIBLE
                        if (authTask.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            val userId = firebaseUser?.uid

                            Log.d("FirebaseAuth", "Login berhasil dengan user ID: $userId")

                            if (firebaseUser != null && userId != null) {
                                val email = firebaseUser.email ?: "No Email"
                                val username = firebaseUser.displayName ?: "User Google"

                                // Log email dan username untuk memastikan data yang diterima
                                Log.d("FirebaseAuth", "Email: $email, Username: $username")
                                val photoUrl = account.photoUrl?.toString()
                                val userDocRef = db.collection("users").document(userId)

                                userDocRef.get().addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        Toast.makeText(this, "Login Google berhasil", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, landing::class.java))
                                        finish()
                                    } else {
                                        val userData = hashMapOf(
                                            "email" to email,
                                            "username" to username,
                                            "photoUrl" to photoUrl
                                        )
                                        userDocRef.set(userData).addOnSuccessListener {
                                            Toast.makeText(this, "Sign-up Google berhasil", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, landing::class.java))
                                            finish()
                                        }.addOnFailureListener { e ->
                                            Toast.makeText(this, "Gagal menyimpan data pengguna: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }

                            Toast.makeText(this, "Login Google berhasil", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, landing::class.java))
                            finish()
                        } else {
                            Log.e("FirebaseAuth", "Login gagal: ${authTask.exception?.message}")
                            Toast.makeText(this, "Login Google gagal: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: ApiException) {
                // Jika terjadi error saat login Google
                Log.e("GoogleSignIn", "Gagal login Google: ${e.message}", e)
                Toast.makeText(this, "Gagal login Google: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

