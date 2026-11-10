package com.myiphone.store.admin

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.myiphone.store.R
import com.myiphone.store.databinding.ActivityLoginAdminBinding

/** Login del administrador: solo deja pasar cuentas con tipoUsuario = "administrador". */
class LoginAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginAdminBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var progreso: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progreso = ProgressDialog(this)
        progreso.setMessage(getString(R.string.auth_espere))
        progreso.setCanceledOnTouchOutside(false)

        binding.btnLoginAdmin.setOnClickListener { validar() }
        binding.tvVolver.setOnClickListener { finish() }
    }

    private fun validar() {
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = getString(R.string.auth_email_invalido); binding.etEmail.requestFocus()
        } else if (pass.isEmpty()) {
            binding.etPassword.error = getString(R.string.auth_ingrese_password); binding.etPassword.requestFocus()
        } else {
            progreso.show()
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { verificarRol() }
                .addOnFailureListener { e ->
                    progreso.dismiss()
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun verificarRol() {
        val uid = auth.uid ?: return
        FirebaseDatabase.getInstance().getReference("Usuarios").child(uid).child("tipoUsuario")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(sn: DataSnapshot) {
                    progreso.dismiss()
                    if (sn.getValue(String::class.java) == Roles.ADMIN) {
                        startActivity(Intent(this@LoginAdminActivity, MainActivityAdmin::class.java))
                        finishAffinity()
                    } else {
                        auth.signOut()
                        Toast.makeText(this@LoginAdminActivity, R.string.adm_no_es_admin, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onCancelled(e: DatabaseError) {
                    progreso.dismiss()
                    auth.signOut()
                }
            })
    }
}
