package com.myiphone.store.vendedor

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.myiphone.store.R
import com.myiphone.store.comprador.Formato
import com.myiphone.store.comprador.IphoneProducto
import com.myiphone.store.comprador.Pedido
import com.myiphone.store.comprador.ProductosRepo
import com.myiphone.store.databinding.FragVPedidosBinding
import com.myiphone.store.databinding.FragVPerfilBinding
import com.myiphone.store.databinding.FragVProductosBinding
import com.myiphone.store.databinding.FragVResumenBinding

val ESTADOS_PEDIDO = arrayOf("Pendiente", "Despachado", "Entregado", "Cancelado")

/** Lee todos los pedidos de la tienda (nodo "Pedidos"), del más reciente al más antiguo. */
fun cargarPedidos(onOk: (List<Pedido>) -> Unit, onError: (String) -> Unit) {
    FirebaseDatabase.getInstance().getReference("Pedidos")
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(sn: DataSnapshot) {
                onOk(sn.children.mapNotNull { it.getValue(Pedido::class.java) }.sortedByDescending { it.fecha })
            }
            override fun onCancelled(e: DatabaseError) = onError(e.message)
        })
}

/* ----------------------------------------------------------------- RESUMEN */
class VResumenFragment : Fragment() {
    private var _b: FragVResumenBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragVResumenBinding.inflate(i, c, false)
        return b.root
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos({ pedidos ->
            val vigentes = pedidos.filter { it.estado != "Cancelado" }
            _b?.tvVentas?.text = Formato.cop(vigentes.sumOf { it.total })
            _b?.tvPedidos?.text = pedidos.size.toString()
            _b?.tvPendientes?.text = pedidos.count { it.estado == "Pendiente" }.toString()
        }, { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() })

        ProductosRepo.cargar({ productos ->
            _b?.tvProductos?.text = productos.size.toString()
            _b?.tvStockBajo?.text = productos.count { it.stock <= 3 }.toString()
        }, { })
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ---------------------------------------------------------------- PRODUCTOS */
class VProductosFragment : Fragment() {
    private var _b: FragVProductosBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: AdaptadorProductoV

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragVProductosBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        adaptador = AdaptadorProductoV(
            onEditar = { p ->
                startActivity(Intent(requireContext(), ProductoFormActivity::class.java).putExtra("id", p.id))
            },
            onEliminar = { p -> confirmarEliminar(p) }
        )
        b.rvProductos.layoutManager = LinearLayoutManager(requireContext())
        b.rvProductos.adapter = adaptador
        b.fabNuevo.setOnClickListener {
            startActivity(Intent(requireContext(), ProductoFormActivity::class.java))
        }
    }

    override fun onResume() { super.onResume(); cargar() }

    private fun cargar() {
        ProductosRepo.cargar({ lista ->
            if (_b != null) {
                adaptador.actualizar(lista.sortedBy { it.nombre })
                b.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }, { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() })
    }

    private fun confirmarEliminar(p: IphoneProducto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vend_eliminar_t)
            .setMessage(getString(R.string.vend_eliminar_m, p.nombre))
            .setNegativeButton(R.string.vend_cancelar, null)
            .setPositiveButton(R.string.vend_eliminar) { _, _ ->
                FirebaseDatabase.getInstance().getReference("Productos").child(p.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), R.string.vend_eliminado, Toast.LENGTH_SHORT).show()
                        cargar()
                    }
            }
            .show()
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ------------------------------------------------------------------ PEDIDOS */
class VPedidosFragment : Fragment() {
    private var _b: FragVPedidosBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: AdaptadorPedidoV

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragVPedidosBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        adaptador = AdaptadorPedidoV { cambiarEstado(it) }
        b.rvPedidos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPedidos.adapter = adaptador
        cargar()
    }

    private fun cargar() {
        cargarPedidos({ lista ->
            if (_b != null) {
                adaptador.actualizar(lista)
                b.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }, { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() })
    }

    private fun cambiarEstado(p: Pedido) {
        val actual = ESTADOS_PEDIDO.indexOf(p.estado).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vend_cambiar_estado)
            .setSingleChoiceItems(ESTADOS_PEDIDO, actual) { dialogo, cual ->
                FirebaseDatabase.getInstance().getReference("Pedidos").child(p.id)
                    .child("estado").setValue(ESTADOS_PEDIDO[cual])
                    .addOnSuccessListener { cargar() }
                dialogo.dismiss()
            }
            .setNegativeButton(R.string.vend_cancelar, null)
            .show()
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ------------------------------------------------------------------- PERFIL */
class VPerfilFragment : Fragment() {
    private var _b: FragVPerfilBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragVPerfilBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        val auth = FirebaseAuth.getInstance()
        val usuario = auth.currentUser ?: return
        b.tvEmail.text = usuario.email
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(usuario.uid)

        ref.child("nombres").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(sn: DataSnapshot) {
                val n = sn.getValue(String::class.java).orEmpty()
                _b?.etNombre?.setText(n)
                _b?.tvNombre?.text = n
                _b?.tvInicial?.text = n.take(1).uppercase()
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        b.btnGuardar.setOnClickListener {
            val nuevo = b.etNombre.text.toString().trim()
            if (nuevo.isEmpty()) {
                b.etNombre.error = getString(R.string.auth_ingrese_nombre)
                return@setOnClickListener
            }
            ref.child("nombres").setValue(nuevo).addOnSuccessListener {
                b.tvNombre.text = nuevo
                b.tvInicial.text = nuevo.take(1).uppercase()
                Toast.makeText(requireContext(), R.string.comp_perfil_guardado, Toast.LENGTH_SHORT).show()
            }
        }

        b.btnCambiarPassword.setOnClickListener {
            val email = usuario.email.orEmpty()
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                auth.sendPasswordResetEmail(email).addOnSuccessListener {
                    Toast.makeText(requireContext(), R.string.auth_enlace_enviado, Toast.LENGTH_LONG).show()
                }
            }
        }
        b.btnCerrarSesion.setOnClickListener { (activity as? MainActivityVendedor)?.cerrarSesion() }
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}
