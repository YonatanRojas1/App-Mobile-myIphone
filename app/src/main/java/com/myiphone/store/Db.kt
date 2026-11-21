package com.myiphone.store

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Punto único de acceso a Cloud Firestore. Centraliza los nombres de las colecciones
 * para no repetir textos en todo el proyecto.
 *
 * Colecciones:
 *  - Usuarios/{uid}   datos del usuario y su rol (tipoUsuario)
 *  - Productos/{id}   catálogo de iPhone
 *  - Pedidos/{id}     órdenes de compra
 */
object Db {

    /** Instancia compartida de Firestore. */
    val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** Colección de usuarios (comprador, vendedor y administrador). */
    fun usuarios(): CollectionReference = firestore.collection("Usuarios")

    /** Colección del catálogo de productos. */
    fun productos(): CollectionReference = firestore.collection("Productos")

    /** Colección de pedidos realizados por los compradores. */
    fun pedidos(): CollectionReference = firestore.collection("Pedidos")

    /** Lee el nombre de un usuario y lo entrega por el callback (cadena vacía si no existe). */
    fun nombreDeUsuario(uid: String, onOk: (String) -> Unit) {
        usuarios().document(uid).get()
            .addOnSuccessListener { onOk(it.getString("nombres").orEmpty()) }
    }
}
