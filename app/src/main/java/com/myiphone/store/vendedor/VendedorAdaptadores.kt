package com.myiphone.store.vendedor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.myiphone.store.R
import com.myiphone.store.comprador.Formato
import com.myiphone.store.comprador.IphoneProducto
import com.myiphone.store.comprador.Pedido
import com.myiphone.store.databinding.ItemPedidoBinding
import com.myiphone.store.databinding.ItemProductoVBinding

class AdaptadorProductoV(
    private val onEditar: (IphoneProducto) -> Unit,
    private val onEliminar: (IphoneProducto) -> Unit
) : RecyclerView.Adapter<AdaptadorProductoV.VH>() {

    class VH(val b: ItemProductoVBinding) : RecyclerView.ViewHolder(b.root)

    private var datos: List<IphoneProducto> = emptyList()
    fun actualizar(n: List<IphoneProducto>) { datos = n; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemProductoVBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = datos.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = datos[position]
        h.b.tvNombre.text = p.nombre
        h.b.tvDetalle.text = "${p.almacenamiento} · ${p.color}"
        h.b.tvPrecio.text = Formato.cop(p.precio)
        h.b.tvStock.text = h.b.root.context.getString(R.string.vend_stock_n, p.stock)
        h.b.tvStock.setTextColor(h.b.root.context.getColor(if (p.stock <= 3) R.color.color_error else R.color.color_text_secondary))
        if (p.imagenUrl.isNotBlank()) {
            Glide.with(h.b.ivProducto).load(p.imagenUrl).placeholder(R.drawable.ic_iphone).into(h.b.ivProducto)
        } else h.b.ivProducto.setImageResource(R.drawable.ic_iphone)
        h.b.btnEditar.setOnClickListener { onEditar(p) }
        h.b.btnEliminar.setOnClickListener { onEliminar(p) }
    }
}

class AdaptadorPedidoV(
    private val onClick: (Pedido) -> Unit
) : RecyclerView.Adapter<AdaptadorPedidoV.VH>() {

    class VH(val b: ItemPedidoBinding) : RecyclerView.ViewHolder(b.root)

    private var datos: List<Pedido> = emptyList()
    fun actualizar(n: List<Pedido>) { datos = n; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = datos.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = datos[position]
        h.b.tvReferencia.text = "#" + p.id.takeLast(6).uppercase() + " · " + p.compradorNombre
        h.b.tvFecha.text = Formato.fecha(p.fecha) + " · " + p.metodoPago
        h.b.tvEstado.text = p.estado
        h.b.tvResumen.text = p.items.joinToString("\n") { "${it.cantidad} x ${it.nombre}" } +
            "\n\n" + p.direccion + " · Tel. " + p.telefono
        h.b.tvTotal.text = Formato.cop(p.total)
        h.b.root.setOnClickListener { onClick(p) }
    }
}
