package com.myiphone.store.comprador

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.myiphone.store.R
import com.myiphone.store.databinding.ItemCarritoIphoneBinding
import com.myiphone.store.databinding.ItemIphoneBinding
import com.myiphone.store.databinding.ItemPedidoBinding

/** Tarjeta de producto (catálogo en grilla y destacados en horizontal). */
class AdaptadorIphone(
    private var lista: List<IphoneProducto>,
    private val anchoDp: Int? = null,
    private val onClick: (IphoneProducto) -> Unit,
    private val onAgregar: (IphoneProducto) -> Unit
) : RecyclerView.Adapter<AdaptadorIphone.VH>() {

    class VH(val b: ItemIphoneBinding) : RecyclerView.ViewHolder(b.root)

    fun actualizar(nueva: List<IphoneProducto>) { lista = nueva; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemIphoneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (anchoDp != null) {
            b.root.layoutParams.width = (anchoDp * parent.resources.displayMetrics.density).toInt()
        }
        return VH(b)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = lista[position]
        h.b.tvNombre.text = p.nombre
        h.b.tvDetalle.text = "${p.almacenamiento} · ${p.color}"
        h.b.tvPrecio.text = Formato.cop(p.precio)
        if (p.imagenUrl.isNotBlank()) {
            Glide.with(h.b.ivProducto).load(p.imagenUrl).placeholder(R.drawable.ic_iphone).into(h.b.ivProducto)
        } else {
            h.b.ivProducto.setImageResource(R.drawable.ic_iphone)
        }
        val agotado = p.stock <= 0
        h.b.btnAgregar.isEnabled = !agotado
        h.b.btnAgregar.setText(if (agotado) R.string.comp_agotado else R.string.comp_agregar)
        h.b.root.setOnClickListener { onClick(p) }
        h.b.btnAgregar.setOnClickListener { onAgregar(p) }
    }
}

class AdaptadorCarritoIphone(
    private val onCambio: () -> Unit
) : RecyclerView.Adapter<AdaptadorCarritoIphone.VH>() {

    class VH(val b: ItemCarritoIphoneBinding) : RecyclerView.ViewHolder(b.root)

    private var datos: List<CarritoManager.ItemCarrito> = CarritoManager.lista()

    fun refrescar() { datos = CarritoManager.lista(); notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCarritoIphoneBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = datos.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val it = datos[position]
        val p = it.producto
        h.b.tvNombre.text = p.nombre
        h.b.tvDetalle.text = "${p.almacenamiento} · ${p.color}"
        h.b.tvCantidad.text = it.cantidad.toString()
        h.b.tvSubtotal.text = Formato.cop(p.precio * it.cantidad)
        h.b.ivProducto.setImageResource(R.drawable.ic_iphone)
        h.b.btnMas.setOnClickListener { CarritoManager.cambiarCantidad(p.id, 1); onCambio() }
        h.b.btnMenos.setOnClickListener { CarritoManager.cambiarCantidad(p.id, -1); onCambio() }
        h.b.btnQuitar.setOnClickListener { CarritoManager.quitar(p.id); onCambio() }
    }
}

class AdaptadorPedido : RecyclerView.Adapter<AdaptadorPedido.VH>() {

    class VH(val b: ItemPedidoBinding) : RecyclerView.ViewHolder(b.root)

    private var datos: List<Pedido> = emptyList()
    fun actualizar(n: List<Pedido>) { datos = n; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = datos.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = datos[position]
        h.b.tvReferencia.text = "Pedido #" + p.id.takeLast(6).uppercase()
        h.b.tvFecha.text = Formato.fecha(p.fecha)
        h.b.tvEstado.text = p.estado
        h.b.tvResumen.text = p.items.joinToString("\n") { "${it.cantidad} x ${it.nombre}" }
        h.b.tvTotal.text = Formato.cop(p.total)
    }
}
