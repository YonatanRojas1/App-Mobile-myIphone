package com.myiphone.store.comprador

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Acceso a los productos en Firebase. Si el nodo "Productos" está vacío se siembra
 * un catálogo inicial de iPhone nuevos (datos de ejemplo) para poder probar la app.
 */
object ProductosRepo {

    private val ref get() = FirebaseDatabase.getInstance().getReference("Productos")

    fun cargar(onOk: (List<IphoneProducto>) -> Unit, onError: (String) -> Unit) {
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val semilla = catalogoInicial()
                    semilla.forEach { ref.child(it.id).setValue(it) }
                    onOk(semilla)
                } else {
                    val lista = snapshot.children.mapNotNull { it.getValue(IphoneProducto::class.java) }
                    onOk(lista)
                }
            }

            override fun onCancelled(error: DatabaseError) = onError(error.message)
        })
    }

    fun porId(id: String, onOk: (IphoneProducto?) -> Unit) {
        ref.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onOk(snapshot.getValue(IphoneProducto::class.java))
            }

            override fun onCancelled(error: DatabaseError) = onOk(null)
        })
    }

    private fun p(id: String, nombre: String, serie: String, alm: String, color: String,
                  precio: Long, destacado: Boolean, desc: String) =
        IphoneProducto(id, nombre, serie, alm, color, desc, precio, 10, "", destacado)

    private fun catalogoInicial() = listOf(
        p("ip17promax256", "iPhone 17 Pro Max", "iPhone 17", "256 GB", "Naranja cósmico", 7299000, true,
            "Pantalla Super Retina XDR de 6,9 pulgadas, chip A19 Pro y sistema de cámaras Pro con zoom de gran alcance."),
        p("ip17pro256", "iPhone 17 Pro", "iPhone 17", "256 GB", "Azul profundo", 6399000, true,
            "Pantalla de 6,3 pulgadas, chip A19 Pro, ProMotion y cámara trasera triple de 48 MP."),
        p("ipair256", "iPhone Air", "iPhone 17", "256 GB", "Negro espacial", 5299000, true,
            "El iPhone más delgado hasta ahora: diseño en titanio, pantalla de 6,5 pulgadas y chip A19 Pro."),
        p("ip17256", "iPhone 17", "iPhone 17", "256 GB", "Lavanda", 4399000, false,
            "Pantalla de 6,3 pulgadas con ProMotion, chip A19 y cámara frontal Center Stage."),
        p("ip16128", "iPhone 16", "iPhone 16", "128 GB", "Ultramarino", 3599000, true,
            "Chip A18, botón de acción y botón de control de la cámara. Hecho para Apple Intelligence."),
        p("ip16e128", "iPhone 16e", "iPhone 16", "128 GB", "Blanco", 2999000, false,
            "Chip A18, cámara Fusion de 48 MP y batería de todo el día en un diseño compacto."),
        p("ip15128", "iPhone 15", "iPhone 15", "128 GB", "Rosado", 3099000, false,
            "Dynamic Island, cámara principal de 48 MP y conector USB-C.")
    )
}
