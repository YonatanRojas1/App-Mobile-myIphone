package com.myiphone.store

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.myiphone.store.admin.MainActivityAdmin
import com.myiphone.store.cliente.MainActivityCliente
import com.myiphone.store.vendedor.MainActivityVendedor

/** Decide a qué panel entrar según el campo tipoUsuario guardado en Firestore. */
object SesionUtil {

    /**
     * Lee Usuarios/{uid}.tipoUsuario del usuario autenticado y abre su panel:
     * "vendedor", "cliente" (comprador) o "administrador". Si no hay sesión o el rol
     * no existe, vuelve a la pantalla de bienvenida.
     */
    fun abrirPanel(activity: AppCompatActivity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            irA(activity, SeleccionarTipoActivity::class.java)
            return
        }
        Db.usuarios().document(uid).get()
            .addOnSuccessListener { doc ->
                when (doc.getString("tipoUsuario")) {
                    "vendedor" -> irA(activity, MainActivityVendedor::class.java)
                    "cliente", "comprador" -> irA(activity, MainActivityCliente::class.java)
                    "administrador" -> irA(activity, MainActivityAdmin::class.java)
                    else -> irA(activity, SeleccionarTipoActivity::class.java)
                }
            }
            .addOnFailureListener { irA(activity, SeleccionarTipoActivity::class.java) }
    }

    /** Abre la pantalla destino y cierra todas las anteriores. */
    private fun irA(activity: AppCompatActivity, destino: Class<*>) {
        activity.startActivity(Intent(activity, destino))
        activity.finishAffinity()
    }
}
