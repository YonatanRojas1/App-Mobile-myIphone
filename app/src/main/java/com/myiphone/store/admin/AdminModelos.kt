package com.myiphone.store.admin

import com.myiphone.store.Db

/** Usuario de la app (colección "Usuarios" de Firestore). Los campos que no se listan aquí se ignoran. */
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

/** Lee todos los usuarios de Firestore, ordenados por nombre. */
fun cargarUsuarios(onOk: (List<UsuarioApp>) -> Unit, onError: (String) -> Unit) {
    Db.usuarios().get()
        .addOnSuccessListener { snap ->
            onOk(snap.documents.mapNotNull { d ->
                d.toObject(UsuarioApp::class.java)?.also { if (it.uid.isEmpty()) it.uid = d.id }
            }.sortedBy { it.nombres.lowercase() })
        }
        .addOnFailureListener { onError(it.message ?: "Error") }
}
