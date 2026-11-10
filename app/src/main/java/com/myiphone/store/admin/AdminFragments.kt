package com.myiphone.store.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.myiphone.store.R
import com.myiphone.store.comprador.Formato
import com.myiphone.store.comprador.IphoneProducto
import com.myiphone.store.comprador.Pedido
import com.myiphone.store.comprador.ProductosRepo
import com.myiphone.store.databinding.FragAProductosBinding
import com.myiphone.store.databinding.FragADashboardBinding
import com.myiphone.store.databinding.FragAReporteBinding
import com.myiphone.store.databinding.FragAUsuariosBinding
import com.myiphone.store.vendedor.AdaptadorPedidoV
import com.myiphone.store.vendedor.AdaptadorProductoV
import com.myiphone.store.vendedor.ProductoFormActivity
import com.myiphone.store.vendedor.cargarPedidos

/* --------------------------------------------------------------- DASHBOARD */
class ADashboardFragment : Fragment() {
    private var _b: FragADashboardBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragADashboardBinding.inflate(i, c, false)
        return b.root
    }

    override fun onResume() {
        super.onResume()
        cargarUsuarios({ us ->
            _b?.tvUsuarios?.text = us.size.toString()
            _b?.tvCompradores?.text = us.count { it.tipoUsuario == Roles.COMPRADOR || it.tipoUsuario == "comprador" }.toString()
            _b?.tvVendedores?.text = us.count { it.tipoUsuario == Roles.VENDEDOR }.toString()
        }, { })
        ProductosRepo.cargar({ ps ->
            _b?.tvProductos?.text = ps.size.toString()
            _b?.tvStockBajo?.text = ps.count { it.stock <= 3 }.toString()
        }, { })
        cargarPedidos({ pe ->
            val vigentes = pe.filter { it.estado != "Cancelado" }
            _b?.tvVentas?.text = Formato.cop(vigentes.sumOf { it.total })
            _b?.tvPedidos?.text = pe.size.toString()
        }, { })
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ---------------------------------------------------------------- USUARIOS */
class AUsuariosFragment : Fragment() {
    private var _b: FragAUsuariosBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: AdaptadorUsuario

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragAUsuariosBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        adaptador = AdaptadorUsuario(
            onEditar = { u ->
                startActivity(Intent(requireContext(), UsuarioFormActivity::class.java)
                    .putExtra("uid", u.uid).putExtra("nombres", u.nombres)
                    .putExtra("email", u.email).putExtra("rol", u.tipoUsuario))
            },
            onEliminar = { confirmarEliminar(it) }
        )
        b.rvUsuarios.layoutManager = LinearLayoutManager(requireContext())
        b.rvUsuarios.adapter = adaptador
        b.fabNuevo.setOnClickListener {
            startActivity(Intent(requireContext(), UsuarioFormActivity::class.java))
        }
    }

    override fun onResume() { super.onResume(); cargar() }

    private fun cargar() {
        cargarUsuarios({ lista ->
            if (_b != null) {
                adaptador.actualizar(lista)
                b.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }, { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() })
    }

    private fun confirmarEliminar(u: UsuarioApp) {
        if (u.uid == FirebaseAuth.getInstance().uid) {
            Toast.makeText(requireContext(), R.string.adm_no_autoeliminar, Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.adm_eliminar_usuario)
            .setMessage(getString(R.string.adm_eliminar_usuario_m, u.nombres))
            .setNegativeButton(R.string.vend_cancelar, null)
            .setPositiveButton(R.string.vend_eliminar) { _, _ ->
                FirebaseDatabase.getInstance().getReference("Usuarios").child(u.uid).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), R.string.adm_usuario_eliminado, Toast.LENGTH_SHORT).show()
                        cargar()
                    }
            }
            .show()
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* --------------------------------------------------------------- PRODUCTOS */
class AProductosFragment : Fragment() {
    private var _b: FragAProductosBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: AdaptadorProductoV

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragAProductosBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        adaptador = AdaptadorProductoV(
            onEditar = { p ->
                startActivity(Intent(requireContext(), ProductoFormActivity::class.java).putExtra("id", p.id))
            },
            onEliminar = { confirmarEliminar(it) }
        )
        b.rvProductos.layoutManager = LinearLayoutManager(requireContext())
        b.rvProductos.adapter = adaptador
    }

    override fun onResume() { super.onResume(); cargar() }

    private fun cargar() {
        ProductosRepo.cargar({ lista ->
            if (_b != null) adaptador.actualizar(lista.sortedBy { it.nombre })
        }, { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() })
    }

    private fun confirmarEliminar(p: IphoneProducto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vend_eliminar_t)
            .setMessage(getString(R.string.vend_eliminar_m, p.nombre))
            .setNegativeButton(R.string.vend_cancelar, null)
            .setPositiveButton(R.string.vend_eliminar) { _, _ ->
                FirebaseDatabase.getInstance().getReference("Productos").child(p.id).removeValue()
                    .addOnSuccessListener { cargar() }
            }
            .show()
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ----------------------------------------------------------------- REPORTE */
class AReporteFragment : Fragment() {
    private var _b: FragAReporteBinding? = null
    private val b get() = _b!!
    private var todos: List<Pedido> = emptyList()
    private var textoReporte = ""
    private val adaptador = AdaptadorPedidoV { }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragAReporteBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.rvPedidos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPedidos.adapter = adaptador
        b.chipsPeriodo.setOnCheckedStateChangeListener { _, _ -> pintar() }
        b.btnCompartir.setOnClickListener {
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, textoReporte),
                getString(R.string.adm_compartir)))
        }
        cargarPedidos({ lista -> todos = lista; pintar() },
            { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() })
    }

    private fun pintar() {
        if (_b == null) return
        val ahora = System.currentTimeMillis()
        val dia = 24L * 60 * 60 * 1000
        val (desde, nombrePeriodo) = when (b.chipsPeriodo.checkedChipId) {
            R.id.chipHoy -> (ahora - dia) to "Últimas 24 horas"
            R.id.chip7 -> (ahora - 7 * dia) to "Últimos 7 días"
            R.id.chip30 -> (ahora - 30 * dia) to "Últimos 30 días"
            else -> 0L to "Todo el historial"
        }
        val periodo = todos.filter { it.fecha >= desde }
        val vigentes = periodo.filter { it.estado != "Cancelado" }
        val total = vigentes.sumOf { it.total }
        val ticket = if (vigentes.isEmpty()) 0L else total / vigentes.size

        // Ranking de modelos más vendidos
        val ranking = vigentes.flatMap { it.items }.groupBy { it.nombre }
            .mapValues { e -> e.value.sumOf { it.cantidad } }
            .entries.sortedByDescending { it.value }.take(5)

        b.tvTotal.text = Formato.cop(total)
        b.tvNumPedidos.text = vigentes.size.toString()
        b.tvTicket.text = Formato.cop(ticket)
        b.tvTop.text = if (ranking.isEmpty()) getString(R.string.adm_sin_ventas)
        else ranking.mapIndexed { i, e -> "${i + 1}. ${e.key} — ${e.value} und." }.joinToString("\n")
        adaptador.actualizar(periodo)

        textoReporte = buildString {
            appendLine("MyIphone - Reporte de ventas")
            appendLine("Periodo: $nombrePeriodo")
            appendLine("Pedidos (sin cancelados): ${vigentes.size}")
            appendLine("Ventas totales: ${Formato.cop(total)}")
            appendLine("Ticket promedio: ${Formato.cop(ticket)}")
            appendLine("Más vendidos:")
            if (ranking.isEmpty()) appendLine("- sin ventas")
            ranking.forEach { appendLine("- ${it.key}: ${it.value} und.") }
        }
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}
