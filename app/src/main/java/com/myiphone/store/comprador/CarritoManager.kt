package com.myiphone.store.comprador

/** Carrito en memoria del comprador. Se vacía al pagar o al cerrar sesión. */
object CarritoManager {

    class ItemCarrito(val producto: IphoneProducto, var cantidad: Int)

    private val items = LinkedHashMap<String, ItemCarrito>()
    private val oyentes = mutableListOf<() -> Unit>()

    fun agregarOyente(o: () -> Unit) { oyentes.add(o) }
    fun quitarOyente(o: () -> Unit) { oyentes.remove(o) }
    private fun avisar() { oyentes.toList().forEach { it() } }

    fun lista(): List<ItemCarrito> = items.values.toList()
    fun total(): Long = items.values.sumOf { it.producto.precio * it.cantidad }
    fun cantidadTotal(): Int = items.values.sumOf { it.cantidad }
    fun estaVacio(): Boolean = items.isEmpty()

    /** Devuelve false si se supera el stock disponible. */
    fun agregar(p: IphoneProducto, cantidad: Int = 1): Boolean {
        val actual = items[p.id]
        val nueva = (actual?.cantidad ?: 0) + cantidad
        if (nueva > p.stock) return false
        if (actual == null) items[p.id] = ItemCarrito(p, cantidad) else actual.cantidad = nueva
        avisar()
        return true
    }

    fun cambiarCantidad(id: String, delta: Int) {
        val item = items[id] ?: return
        val nueva = item.cantidad + delta
        if (nueva <= 0) items.remove(id)
        else if (nueva <= item.producto.stock) item.cantidad = nueva
        avisar()
    }

    fun quitar(id: String) { items.remove(id); avisar() }
    fun vaciar() { items.clear(); avisar() }
}
