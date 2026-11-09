package com.myiphone.store

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

/** Autenticación biométrica (huella / rostro) con BiometricPrompt. */
object BiometricAuth {

    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK

    /** true si el celular tiene biometría y ya hay una registrada. */
    fun disponible(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun autenticar(activity: AppCompatActivity, onExito: () -> Unit, onFallo: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onExito()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFallo()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.bio_titulo))
            .setSubtitle(activity.getString(R.string.bio_subtitulo))
            .setNegativeButtonText(activity.getString(R.string.bio_cancelar))
            .setAllowedAuthenticators(AUTH)
            .build()
        prompt.authenticate(info)
    }
}
