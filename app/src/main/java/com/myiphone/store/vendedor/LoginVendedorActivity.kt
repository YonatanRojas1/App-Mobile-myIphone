package com.myiphone.store.vendedor

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.myiphone.store.BiometricAuth
import com.myiphone.store.RecuperarPasswordActivity
import com.myiphone.store.SesionUtil
import com.myiphone.store.R
import com.myiphone.store.databinding.ActivityLoginVendedorBinding
import com.google.firebase.auth.FirebaseAuth



class LoginVendedorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginVendedorBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginVendedorBinding.inflate(layoutInflater)

        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.btnLoginV.setOnClickListener{
            validarInfo()
        }


        /*Huella: solo si ya hay una sesión guardada en el dispositivo*/
        binding.btnLoginHuella.setOnClickListener { ingresarConHuella() }

        binding.tvOlvidePassword.setOnClickListener {
            startActivity(Intent(this, RecuperarPasswordActivity::class.java))
        }

        binding.tvRegistrarV.setOnClickListener{
            startActivity(Intent(applicationContext, RegistroVendedorActivity::class.java))
        }

    }

    private var email = ""
    private var password = ""

    private fun validarInfo() {
        email=binding.etEmail.text.toString().trim()
        password=binding.etPassword.text.toString().trim()
        if (email.isEmpty()){
            binding.etEmail.error = "Ingrese su email"
            binding.etEmail.requestFocus()
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.etEmail.error = "Ingrese un email valido"
            binding.etEmail.requestFocus()
        }
        else if (password.isEmpty()){
            binding.etPassword.error = "Ingrese su contraseña"
            binding.etPassword.requestFocus()
        }
        else{
            loginVendedor()
        }

    }

    private fun loginVendedor() {

        progressDialog.setMessage("Iniciando sesión...")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, R.string.auth_bienvenido, Toast.LENGTH_SHORT).show()
                SesionUtil.abrirPanel(this)
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this, "No se pudo iniciar sesion debidoa a: ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }


    private fun ingresarConHuella() {
        if (firebaseAuth.currentUser == null) {
            Toast.makeText(this, R.string.auth_sin_sesion_huella, Toast.LENGTH_LONG).show()
        } else if (!BiometricAuth.disponible(this)) {
            Toast.makeText(this, R.string.bio_no_disponible, Toast.LENGTH_LONG).show()
        } else {
            BiometricAuth.autenticar(
                this,
                onExito = { SesionUtil.abrirPanel(this) },
                onFallo = { Toast.makeText(this, R.string.bio_fallo, Toast.LENGTH_SHORT).show() }
            )
        }
    }

}
