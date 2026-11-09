package com.myiphone.store

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.myiphone.store.databinding.ActivityRecuperarPasswordBinding

class RecuperarPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecuperarPasswordBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecuperarPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnEnviarEnlace.setOnClickListener { validarYEnviar() }
        binding.tvVolverLogin.setOnClickListener { finish() }
    }

    private fun validarYEnviar() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.etEmail.error = getString(R.string.auth_ingrese_email)
            binding.etEmail.requestFocus()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = getString(R.string.auth_email_invalido)
            binding.etEmail.requestFocus()
        } else {
            binding.btnEnviarEnlace.isEnabled = false
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, R.string.auth_enlace_enviado, Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnEnviarEnlace.isEnabled = true
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
        }
    }
}
