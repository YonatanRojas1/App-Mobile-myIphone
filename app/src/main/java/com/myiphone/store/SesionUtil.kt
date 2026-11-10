package com.myiphone.store

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.myiphone.store.admin.MainActivityAdmin
import com.myiphone.store.cliente.MainActivityCliente
import com.myiphone.store.vendedor.MainActivityVendedor

/** Decide a qué panel entrar según el tipoUsuario guardado en Realtime Database. */
object SesionUtil {

    fun abrirPanel(activity: AppCompatActivity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            irA(activity, SeleccionarTipoActivity::class.java)
            return
        }
        FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    when (snapshot.child("tipoUsuario").value) {
                        "vendedor" -> irA(activity, MainActivityVendedor::class.java)
                        "cliente", "comprador" -> irA(activity, MainActivityCliente::class.java)
                        "administrador" -> irA(activity, MainActivityAdmin::class.java)
                        else -> irA(activity, SeleccionarTipoActivity::class.java)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    irA(activity, SeleccionarTipoActivity::class.java)
                }
            })
    }

    private fun irA(activity: AppCompatActivity, destino: Class<*>) {
        activity.startActivity(Intent(activity, destino))
        activity.finishAffinity()
    }
}
