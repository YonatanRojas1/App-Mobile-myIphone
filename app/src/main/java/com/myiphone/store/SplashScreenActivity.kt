package com.myiphone.store

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.myiphone.store.databinding.ActivitySplashScreenBinding

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

    /**
     * Si ya hay sesión de Firebase y el celular tiene biometría, pide la huella antes de entrar.
     * Si no hay biometría, entra directo; si falla o cancela, vuelve a la pantalla de bienvenida.
     */
    private fun comprobarTipoUsuario() {
        if (firebaseAuth.currentUser == null) {
            irASeleccionTipo()
            return
        }
        if (BiometricAuth.disponible(this)) {
            BiometricAuth.autenticar(
                this,
                onExito = { SesionUtil.abrirPanel(this) },
                onFallo = { irASeleccionTipo() }
            )
        } else {
            SesionUtil.abrirPanel(this)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val DURACION_SPLASH = 2200L
    }
}
