package com.myiphone.store.admin

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/** Usuario de la app (nodo "Usuarios"). Los campos que no se listan aquí se ignoran. */
data class UsuarioApp(
    var uid: String = "",
    var nombres: String = "",
    var email: String = "",
    var tipoUsuario: String = ""
)

object Roles {
    const val COMPRADOR = "cliente"      // valor que guarda el registro de comprador
    const val VENDEDOR = "vendedor"
    const val ADMIN = "administrador"

    fun etiqueta(rol: String) = when (rol) {
        VENDEDOR -> "Vendedor"
        ADMIN -> "Administrador"
        else -> "Comprador"
    }
}

fun cargarUsuarios(onOk: (List<UsuarioApp>) -> Unit, onError: (String) -> Unit) {
    FirebaseDatabase.getInstance().getReference("Usuarios")
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(sn: DataSnapshot) {
                onOk(sn.children.mapNotNull { ds ->
                    ds.getValue(UsuarioApp::class.java)?.also { if (it.uid.isEmpty()) it.uid = ds.key.orEmpty() }
                }.sortedBy { it.nombres.lowercase() })
            }
            override fun onCancelled(e: DatabaseError) = onError(e.message)
        })
}
