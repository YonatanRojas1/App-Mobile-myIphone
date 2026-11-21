package com.myiphone.store.comprador

/** Producto del catálogo (colección "Productos" en Firestore). Solo iPhone nuevos. */
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

/** Pedido (colección "Pedidos" en Firestore). El vendedor y el administrador lo leen en sus paneles. */
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
    var lat: Double = 0.0,          // latitud capturada con el GPS al comprar
    var lng: Double = 0.0,          // longitud capturada con el GPS al comprar
    var refPago: String = "",       // referencia ref_payco de la transaccion en ePayco
    var estadoPago: String = "",    // respuesta de ePayco (Aceptada, Rechazada...)
    var items: List<ItemPedido> = emptyList()
)
