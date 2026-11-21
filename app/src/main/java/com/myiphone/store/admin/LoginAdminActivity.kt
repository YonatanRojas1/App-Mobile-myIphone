package com.myiphone.store.admin

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.myiphone.store.Db
import com.myiphone.store.R
import com.myiphone.store.databinding.ActivityLoginAdminBinding

/** Login del administrador: solo deja pasar cuentas con tipoUsuario = "administrador". */
class LoginAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginAdminBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var progreso: ProgressDialog

    /** Infla el layout y conecta los botones de ingresar y volver. */
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

    /** Valida correo y contraseña y, si están bien, inicia sesión en Firebase Authentication. */
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

    /**
     * Consulta el rol en Firestore (Usuarios/{uid}.tipoUsuario). Si es administrador abre
     * su panel; si no, cierra la sesión y avisa que no tiene permisos.
     */
    private fun verificarRol() {
        val uid = auth.uid ?: return
        Db.usuarios().document(uid).get()
            .addOnSuccessListener { doc ->
                progreso.dismiss()
                if (doc.getString("tipoUsuario") == Roles.ADMIN) {
                    startActivity(Intent(this, MainActivityAdmin::class.java))
                    finishAffinity()
                } else {
                    auth.signOut()
                    Toast.makeText(this, R.string.adm_no_es_admin, Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                progreso.dismiss()
                auth.signOut()
                Toast.makeText(this, R.string.adm_no_es_admin, Toast.LENGTH_LONG).show()
            }
    }
}
