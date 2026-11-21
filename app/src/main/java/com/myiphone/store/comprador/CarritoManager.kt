package com.myiphone.store.comprador

import com.google.firebase.auth.FirebaseAuth
import com.myiphone.store.Db

/**
 * Carrito del comprador. Vive en memoria para que la pantalla responda rápido y se
 * sincroniza con Firestore en Usuarios/{uid}/carrito/actual, así no se pierde al cerrar la app.
 * CRUD: agregar (create), lista/cargarDesdeNube (read), cambiarCantidad (update), quitar/vaciar (delete).
 */
object CarritoManager {

    /** Línea del carrito: un producto y cuántas unidades lleva. */
    class ItemCarrito(val producto: IphoneProducto, var cantidad: Int)

    private val items = LinkedHashMap<String, ItemCarrito>()
    private val oyentes = mutableListOf<() -> Unit>()

    fun agregarOyente(o: () -> Unit) { oyentes.add(o) }
    fun quitarOyente(o: () -> Unit) { oyentes.remove(o) }
    private fun avisar() { oyentes.toList().forEach { it() } }

    /** Se llama después de cada cambio local: guarda en la nube y avisa a las pantallas. */
    private fun cambio() { sincronizar(); avisar() }

    fun lista(): List<ItemCarrito> = items.values.toList()
    fun total(): Long = items.values.sumOf { it.producto.precio * it.cantidad }
    fun cantidadTotal(): Int = items.values.sumOf { it.cantidad }
    fun estaVacio(): Boolean = items.isEmpty()

    /** Agrega unidades de un producto. Devuelve false si se supera el stock disponible. */
    fun agregar(p: IphoneProducto, cantidad: Int = 1): Boolean {
        val actual = items[p.id]
        val nueva = (actual?.cantidad ?: 0) + cantidad
        if (nueva > p.stock) return false
        if (actual == null) items[p.id] = ItemCarrito(p, cantidad) else actual.cantidad = nueva
        cambio()
        return true
    }

    /** Suma o resta unidades; si llega a cero la línea se elimina. Respeta el stock. */
    fun cambiarCantidad(id: String, delta: Int) {
        val item = items[id] ?: return
        val nueva = item.cantidad + delta
        if (nueva <= 0) items.remove(id)
        else if (nueva <= item.producto.stock) item.cantidad = nueva
        cambio()
    }

    /** Elimina un producto del carrito. */
    fun quitar(id: String) { items.remove(id); cambio() }

    /** Vacía el carrito (también en la nube). Se usa al terminar una compra. */
    fun vaciar() { items.clear(); cambio() }

    /** Limpia solo la memoria local (cerrar sesión): el carrito sigue guardado en la nube. */
    fun limpiarLocal() { items.clear(); avisar() }

    /** Guarda el carrito actual en Firestore como lista de {id, cantidad}. */
    private fun sincronizar() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val lista = items.values.map { mapOf("id" to it.producto.id, "cantidad" to it.cantidad) }
        Db.usuarios().document(uid).collection("carrito").document("actual").set(mapOf("items" to lista))
    }

    /** Recupera el carrito guardado en Firestore y lo reconstruye con los datos actuales de cada producto. */
    @Suppress("UNCHECKED_CAST")
    fun cargarDesdeNube() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        Db.usuarios().document(uid).collection("carrito").document("actual").get()
            .addOnSuccessListener { doc ->
                val lista = doc.get("items") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                items.clear()
                if (lista.isEmpty()) { avisar(); return@addOnSuccessListener }
                var pendientes = lista.size
                lista.forEach { m ->
                    val id = m["id"] as? String ?: ""
                    val cant = (m["cantidad"] as? Number)?.toInt() ?: 1
                    ProductosRepo.porId(id) { p ->
                        if (p != null && p.stock > 0) items[p.id] = ItemCarrito(p, minOf(cant, p.stock))
                        pendientes--
                        if (pendientes == 0) avisar()
                    }
                }
            }
    }
}
