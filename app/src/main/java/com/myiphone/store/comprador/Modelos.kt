package com.myiphone.store.comprador

/** Producto del catálogo (nodo "Productos" en Realtime Database). Solo iPhone nuevos. */
data class IphoneProducto(
    var id: String = "",
    var nombre: String = "",
    var serie: String = "",
    var almacenamiento: String = "",
    var color: String = "",
    var descripcion: String = "",
    var precio: Long = 0,
    var stock: Int = 0,
    var imagenUrl: String = "",
    var destacado: Boolean = false
)

data class ItemPedido(
    var idProducto: String = "",
    var nombre: String = "",
    var cantidad: Int = 0,
    var precio: Long = 0
)

/** Pedido (nodo "Pedidos"). El vendedor y el administrador lo leen en sus paneles. */
data class Pedido(
    var id: String = "",
    var compradorUid: String = "",
    var compradorNombre: String = "",
    var compradorEmail: String = "",
    var direccion: String = "",
    var telefono: String = "",
    var metodoPago: String = "",
    var total: Long = 0,
    var estado: String = "Pendiente",
    var fecha: Long = 0,
    var items: List<ItemPedido> = emptyList()
)
