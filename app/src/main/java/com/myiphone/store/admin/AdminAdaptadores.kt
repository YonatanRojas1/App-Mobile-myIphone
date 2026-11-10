package com.myiphone.store.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.myiphone.store.databinding.ItemUsuarioBinding

class AdaptadorUsuario(
    private val onEditar: (UsuarioApp) -> Unit,
    private val onEliminar: (UsuarioApp) -> Unit
) : RecyclerView.Adapter<AdaptadorUsuario.VH>() {

    class VH(val b: ItemUsuarioBinding) : RecyclerView.ViewHolder(b.root)

    private var datos: List<UsuarioApp> = emptyList()
    fun actualizar(n: List<UsuarioApp>) { datos = n; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = datos.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val u = datos[position]
        h.b.tvInicial.text = u.nombres.take(1).uppercase()
        h.b.tvNombre.text = u.nombres.ifBlank { "(sin nombre)" }
        h.b.tvEmail.text = u.email
        h.b.tvRol.text = Roles.etiqueta(u.tipoUsuario)
        h.b.btnEditar.setOnClickListener { onEditar(u) }
        h.b.btnEliminar.setOnClickListener { onEliminar(u) }
    }
}
