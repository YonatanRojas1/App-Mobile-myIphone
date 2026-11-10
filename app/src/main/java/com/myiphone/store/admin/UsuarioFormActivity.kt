package com.myiphone.store.admin

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.myiphone.store.Constantes
import com.myiphone.store.R
import com.myiphone.store.databinding.ActivityUsuarioFormBinding

/**
 * Crear o editar un usuario. Si llegan extras "uid" es edición (nombre y rol).
 * Para crear se usa una segunda instancia de FirebaseApp, así la sesión del administrador no se cierra.
 */
class UsuarioFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuarioFormBinding
    private var uidEditar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuarioFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        uidEditar = intent.getStringExtra("uid")
        if (uidEditar != null) {
            supportActionBar?.title = getString(R.string.adm_editar_usuario)
            binding.etNombre.setText(intent.getStringExtra("nombres"))
            binding.etEmail.setText(intent.getStringExtra("email"))
            binding.etEmail.isEnabled = false
            binding.layoutPassword.visibility = View.GONE
            when (intent.getStringExtra("rol")) {
                Roles.VENDEDOR -> binding.rbVendedor.isChecked = true
                Roles.ADMIN -> binding.rbAdmin.isChecked = true
                else -> binding.rbComprador.isChecked = true
            }
        } else {
            supportActionBar?.title = getString(R.string.adm_nuevo_usuario)
        }
        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun rolElegido() = when (binding.rgRol.checkedRadioButtonId) {
        R.id.rbVendedor -> Roles.VENDEDOR
        R.id.rbAdmin -> Roles.ADMIN
        else -> Roles.COMPRADOR
    }

    private fun guardar() {
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        if (nombre.isEmpty()) { binding.etNombre.error = getString(R.string.auth_ingrese_nombre); binding.etNombre.requestFocus(); return }

        val uid = uidEditar
        if (uid != null) {
            binding.btnGuardar.isEnabled = false
            val ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
            ref.updateChildren(mapOf("nombres" to nombre, "tipoUsuario" to rolElegido()))
                .addOnSuccessListener { Toast.makeText(this, R.string.adm_usuario_guardado, Toast.LENGTH_SHORT).show(); finish() }
                .addOnFailureListener { e -> binding.btnGuardar.isEnabled = true; Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEmail.error = getString(R.string.auth_email_invalido); binding.etEmail.requestFocus(); return }
        if (pass.length < 6) { binding.etPassword.error = getString(R.string.auth_password_corta); binding.etPassword.requestFocus(); return }

        binding.btnGuardar.isEnabled = false
        val secundaria = try {
            FirebaseApp.getInstance("registroAdmin")
        } catch (e: IllegalStateException) {
            FirebaseApp.initializeApp(this, FirebaseApp.getInstance().options, "registroAdmin")
        }
        val authSec = FirebaseAuth.getInstance(secundaria)
        authSec.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val nuevoUid = res.user!!.uid
                val datos = hashMapOf<String, Any>(
                    "uid" to nuevoUid, "nombres" to nombre, "email" to email,
                    "tRegistro" to "${Constantes().obtenerTiempoD()}", "imagen" to "",
                    "tipoUsuario" to rolElegido()
                )
                FirebaseDatabase.getInstance().getReference("Usuarios").child(nuevoUid).setValue(datos)
                    .addOnSuccessListener {
                        authSec.signOut()
                        Toast.makeText(this, R.string.adm_usuario_guardado, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        binding.btnGuardar.isEnabled = true
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.btnGuardar.isEnabled = true
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
