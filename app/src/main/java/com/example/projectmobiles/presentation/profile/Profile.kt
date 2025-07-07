package com.example.projectmobiles.presentation.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.projectmobiles.R
import com.example.projectmobiles.presentation.login.Login
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class profile : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var imgProfile    : ImageView
    private lateinit var txtName       : TextView   // up username
    private lateinit var txtEditImage  : TextView   // “Edit Profile Image”
    private lateinit var etUsername    : EditText
    private lateinit var etEmail       : EditText
    private lateinit var etPhone       : EditText
    private lateinit var etPassword    : EditText
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout     : Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isPasswordVisible = false
    private var isEditMode = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        imgProfile     = view.findViewById(R.id.imgProfile)
        txtName        = view.findViewById(R.id.txtName)
        txtEditImage   = view.findViewById(R.id.txtEditProfile)
        etUsername     = view.findViewById(R.id.etUsername)
        etEmail        = view.findViewById(R.id.etEmail)
        etPhone        = view.findViewById(R.id.etPhone)
        etPassword     = view.findViewById(R.id.etPassword)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout      = view.findViewById(R.id.btnLogout)
        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_off_eye, 0)

        var isPasswordVisible = false
        etPassword.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            if (isEditMode && event.action == MotionEvent.ACTION_UP) {
                // get the width of the right drawable
                val bounds = etPassword.compoundDrawables[DRAWABLE_RIGHT]?.bounds
                if (event.rawX >= etPassword.right - (bounds?.width() ?: 0)) {
                    // toggle flag
                    isPasswordVisible = !isPasswordVisible

                    // switch inputType and right icon
                    if (isPasswordVisible) {
                        etPassword.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_on_eye, 0
                        )
                    } else {
                        etPassword.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_off_eye, 0
                        )
                    }
                    // keep cursor at end
                    etPassword.setSelection(etPassword.text?.length ?: 0)
                    return@setOnTouchListener true
                }
            }
            false
        }
        // load user data into ALL fields
        loadUserData()

        txtEditImage = view.findViewById(R.id.txtEditProfile)
        txtEditImage.setOnClickListener {
            Toast.makeText(requireContext(), "Pick Image", Toast.LENGTH_SHORT).show()
        }

        // setup logout vs cancel
        btnLogout.setOnClickListener {
            if (isEditMode) {
                loadUserData(); setEditMode(false)
            } else {
                // sign out both
                googleSignInClient.signOut().addOnCompleteListener {
                    auth.signOut()
                    startActivity(Intent(requireActivity(), Login::class.java))
                    requireActivity().finish()
                }
            }
        }

        // setup edit vs save
        btnEditProfile.setOnClickListener {
            if (isEditMode) showSaveDialog() else setEditMode(true)
        }

        return view
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return

        // 1) Big Name at top
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username") ?: ""
                txtName.text       = username
                etUsername.setText(username)

                etPhone.setText(doc.getString("phone") ?: "")
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal ambil profile", Toast.LENGTH_SHORT).show()
            }

        // 2) Email (always from Auth)
        etEmail.setText(user.email)

        // 3) Mask password
        etPassword.setText("******")

        // 4) Profile image (static “Edit Profile Image” label stays unchanged)
        user.photoUrl?.let { uri ->
            Glide.with(this).load(uri).circleCrop().into(imgProfile)
        }
    }

    private fun setEditMode(enable: Boolean) {
        isEditMode = enable

        // enable/disable fields
        etUsername.isEnabled = enable
        etEmail   .isEnabled = enable
        etPhone   .isEnabled = enable
        etPassword.isEnabled = enable

        if (enable) {
            btnEditProfile.text = "Save"
            btnEditProfile.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.green_500)

            btnLogout.text = "Cancel"
            btnLogout.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.red_500)
        } else {
            btnEditProfile.text = "Edit Profil"
            btnEditProfile.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.ungu_lain)

            btnLogout.text = "Logout"
            btnLogout.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.red_500)
        }
    }

    private fun showSaveDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Simpan Perubahan?")
            .setMessage("Apakah Anda ingin menyimpan perubahan?")
            .setPositiveButton("Simpan") { _, _ ->
                val newName  = etUsername.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()
                val newPhone = etPhone.text.toString().trim()
                val newPass  = etPassword.text.toString().trim()

                // 1) Required fields
                when {
                    newName.isEmpty() -> {
                        etUsername.error = "Username required"
                        etUsername.requestFocus()
                    }
                    newEmail.isEmpty() -> {
                        etEmail.error = "Email required"
                        etEmail.requestFocus()
                    }
                    !isEmailValid(newEmail) -> {
                        etEmail.error = "Invalid email format"
                        etEmail.requestFocus()
                    }
                    newPhone.isEmpty() -> {
                        etPhone.error = "Phone required"
                        etPhone.requestFocus()
                    }
                    newPhone.length > 13 -> {
                        etPhone.error = "Max 13 digits"
                        etPhone.requestFocus()
                    }
                    !newPhone.all { it.isDigit() } -> {
                        etPhone.error = "Must be numeric"
                        etPhone.requestFocus()
                    }
                    newPass != "******" && !isPasswordStrong(newPass) -> {
                        etPassword.error = "Min 8 chars, upper, lower & digit"
                        etPassword.requestFocus()
                    }
                    else -> {
                        // all good → actually save
                        saveUserData()
                    }
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                setEditMode(false)
            }
            .show()
    }

    private fun saveUserData() {
        val user = auth.currentUser ?: return

        val newName  = etUsername.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()
        val newPhone = etPhone.text.toString().trim()
        val newPass  = etPassword.text.toString().trim()

        // update Firestore fields
        db.collection("users").document(user.uid)
            .set(mapOf(
                "username" to newName,
                "phone"    to newPhone,
                "email"    to newEmail
            ), com.google.firebase.firestore.SetOptions.merge())

        // update Auth displayName
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()
        user.updateProfile(profileUpdates)

        // update email & password if changed
        if (newEmail.isNotEmpty() && newEmail != user.email) user.updateEmail(newEmail)
        if (newPass.isNotEmpty() && newPass!="******")       user.updatePassword(newPass)

        Toast.makeText(context, "Profile berhasil disimpan", Toast.LENGTH_SHORT).show()
        setEditMode(false)
    }

    private fun isEmailValid(email: String): Boolean {
        val pattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return email.matches(pattern.toRegex())
    }

    private fun isPasswordStrong(password: String): Boolean {
        val regex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$")
        return regex.containsMatchIn(password)
    }
}