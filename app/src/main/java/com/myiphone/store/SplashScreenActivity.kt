package com.myiphone.store

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.myiphone.store.cliente.MainActivityCliente
import com.myiphone.store.databinding.ActivitySplashScreenBinding
import com.myiphone.store.vendedor.MainActivityVendedor

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        animarEntrada()
        // Espera a que termine la animación y luego decide a qué pantalla ir
        handler.postDelayed({ comprobarTipoUsuario() }, DURACION_SPLASH)
    }

    /** Animación de entrada: el logo crece y aparece; el nombre y el lema suben suavemente. */
    private fun animarEntrada() {
        binding.contenedorLogo.apply {
            alpha = 0f
            scaleX = 0.7f
            scaleY = 0.7f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start()
        }
        binding.tvNombre.apply {
            alpha = 0f
            translationY = 24f
            animate().alpha(1f).translationY(0f).setStartDelay(400).setDuration(600).start()
        }
        binding.tvLema.apply {
            alpha = 0f
            animate().alpha(1f).setStartDelay(800).setDuration(600).start()
        }
    }

    private fun irASeleccionTipo() {
        startActivity(Intent(this, SeleccionarTipoActivity::class.java))
        finish()
    }

    private fun comprobarTipoUsuario() {
        val usuario = firebaseAuth.currentUser
        if (usuario == null) {
            irASeleccionTipo()
            return
        }

        FirebaseDatabase.getInstance().getReference("Usuarios").child(usuario.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    when (snapshot.child("tipoUsuario").value) {
                        "vendedor" -> {
                            startActivity(Intent(this@SplashScreenActivity, MainActivityVendedor::class.java))
                            finishAffinity()
                        }
                        // "cliente" es el valor que guarda el código base; "comprador" se unifica en la fase de autenticación
                        "cliente", "comprador" -> {
                            startActivity(Intent(this@SplashScreenActivity, MainActivityCliente::class.java))
                            finishAffinity()
                        }
                        // TODO (fase del administrador): "administrador" -> MainActivityAdmin
                        else -> irASeleccionTipo()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    irASeleccionTipo()
                }
            })
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val DURACION_SPLASH = 2200L
    }
}
