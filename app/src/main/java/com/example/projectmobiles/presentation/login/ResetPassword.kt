package com.example.projectmobiles.presentation.login

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectmobiles.R
import com.google.firebase.auth.FirebaseAuth

class ResetPassword : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()
        val emailField = findViewById<EditText>(R.id.resetEmail)
        val resetBtn = findViewById<Button>(R.id.btnResetPassword)

        resetBtn.setOnClickListener {
            val email = emailField.text.toString().trim()

            if (email.isEmpty()) {
                emailField.error = "Email harus diisi"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.error = "Format email tidak valid"
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Link reset telah dikirim ke email", Toast.LENGTH_LONG).show()
                        finish() // optional: balik ke login
                    } else {
                        Toast.makeText(this, "Gagal mengirim reset: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
