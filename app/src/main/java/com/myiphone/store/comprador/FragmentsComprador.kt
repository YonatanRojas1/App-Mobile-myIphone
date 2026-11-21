package com.myiphone.store.comprador

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.myiphone.store.Db
import com.myiphone.store.R
import com.myiphone.store.cliente.MainActivityCliente
import com.myiphone.store.databinding.FragCCarritoBinding
import com.myiphone.store.databinding.FragCCatalogoBinding
import com.myiphone.store.databinding.FragCHomeBinding
import com.myiphone.store.databinding.FragCPedidosBinding
import com.myiphone.store.databinding.FragCPerfilBinding

private fun Fragment.agregarAlCarrito(p: IphoneProducto) {
    if (CarritoManager.agregar(p)) {
        Toast.makeText(requireContext(), getString(R.string.comp_agregado, p.nombre), Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(requireContext(), R.string.comp_sin_stock, Toast.LENGTH_SHORT).show()
    }
}

private fun Fragment.abrirDetalle(p: IphoneProducto) {
    startActivity(Intent(requireContext(), ProductoDetalleActivity::class.java).putExtra("id", p.id))
}

/* ------------------------------------------------------------------ INICIO */
class FragmentHome : Fragment() {
    private var _b: FragCHomeBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragCHomeBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        val adaptador = AdaptadorIphone(emptyList(), 180, { abrirDetalle(it) }, { agregarAlCarrito(it) })
        b.rvDestacados.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvDestacados.adapter = adaptador
        b.btnVerCatalogo.setOnClickListener { (activity as? MainActivityCliente)?.abrirCatalogo() }

        val uid = FirebaseAuth.getInstance().uid
        if (uid != null) {
            Db.nombreDeUsuario(uid) { n ->
                _b?.tvSaludo?.text = getString(R.string.comp_saludo, n.substringBefore(" "))
            }
        }

        ProductosRepo.cargar({ lista ->
            if (_b != null) adaptador.actualizar(lista.filter { it.destacado })
        }, { msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() })
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ---------------------------------------------------------------- CATALOGO */
class FragmentCatalogo : Fragment() {
    private var _b: FragCCatalogoBinding? = null
    private val b get() = _b!!
    private var todos: List<IphoneProducto> = emptyList()
    private lateinit var adaptador: AdaptadorIphone

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragCCatalogoBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        adaptador = AdaptadorIphone(emptyList(), null, { abrirDetalle(it) }, { agregarAlCarrito(it) })
        b.rvCatalogo.layoutManager = GridLayoutManager(requireContext(), 2)
        b.rvCatalogo.adapter = adaptador

        b.etBuscar.doAfterTextChanged { filtrar() }
        b.chipsSerie.setOnCheckedStateChangeListener { _, _ -> filtrar() }

        ProductosRepo.cargar({ lista ->
            if (_b != null) { todos = lista; filtrar() }
        }, { msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() })
    }

    private fun filtrar() {
        val texto = b.etBuscar.text.toString().trim().lowercase()
        val serie = when (b.chipsSerie.checkedChipId) {
            R.id.chip17 -> "iPhone 17"
            R.id.chip16 -> "iPhone 16"
            R.id.chip15 -> "iPhone 15"
            else -> ""
        }
        val res = todos.filter {
            (serie.isEmpty() || it.serie == serie) &&
                (texto.isEmpty() || it.nombre.lowercase().contains(texto) || it.color.lowercase().contains(texto))
        }
        adaptador.actualizar(res)
        b.tvVacio.visibility = if (res.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ----------------------------------------------------------------- CARRITO */
class FragmentCarrito : Fragment() {
    private var _b: FragCCarritoBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: AdaptadorCarritoIphone
    private val oyente: () -> Unit = { pintar() }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragCCarritoBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        adaptador = AdaptadorCarritoIphone { pintar() }
        b.rvCarrito.layoutManager = LinearLayoutManager(requireContext())
        b.rvCarrito.adapter = adaptador
        b.btnPagar.setOnClickListener {
            startActivity(Intent(requireContext(), PagoActivity::class.java))
        }
        b.btnSeguirComprando.setOnClickListener { (activity as? MainActivityCliente)?.abrirCatalogo() }
    }

    override fun onResume() { super.onResume(); CarritoManager.agregarOyente(oyente); pintar() }
    override fun onPause() { CarritoManager.quitarOyente(oyente); super.onPause() }

    private fun pintar() {
        if (_b == null) return
        adaptador.refrescar()
        val vacio = CarritoManager.estaVacio()
        b.grupoVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        b.grupoResumen.visibility = if (vacio) View.GONE else View.VISIBLE
        b.tvTotal.text = Formato.cop(CarritoManager.total())
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ----------------------------------------------------------------- PEDIDOS */
class FragmentPedidos : Fragment() {
    private var _b: FragCPedidosBinding? = null
    private val b get() = _b!!
    private val adaptador = AdaptadorPedido()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragCPedidosBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.rvPedidos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPedidos.adapter = adaptador
        val uid = FirebaseAuth.getInstance().uid ?: return
        // Pedidos del comprador logueado (Firestore: Pedidos donde compradorUid == uid)
        Db.pedidos().whereEqualTo("compradorUid", uid).get()
            .addOnSuccessListener { snap ->
                if (_b == null) return@addOnSuccessListener
                val lista = snap.documents.mapNotNull { it.toObject(Pedido::class.java) }
                    .sortedByDescending { it.fecha }
                adaptador.actualizar(lista)
                b.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                if (_b != null) Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}

/* ------------------------------------------------------------------ PERFIL */
class FragmentPerfil : Fragment() {
    private var _b: FragCPerfilBinding? = null
    private val b get() = _b!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragCPerfilBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        val usuario = auth.currentUser ?: return
        b.tvEmail.text = usuario.email
        val ref = Db.usuarios().document(usuario.uid)

        // Carga el nombre guardado en Firestore
        ref.get().addOnSuccessListener { doc ->
            run {
                val n = doc.getString("nombres").orEmpty()
                _b?.etNombre?.setText(n)
                _b?.tvInicial?.text = n.take(1).uppercase()
                _b?.tvNombre?.text = n
            }
        }

        b.btnGuardar.setOnClickListener {
            val nuevo = b.etNombre.text.toString().trim()
            if (nuevo.isEmpty()) {
                b.etNombre.error = getString(R.string.auth_ingrese_nombre)
                return@setOnClickListener
            }
            ref.update("nombres", nuevo).addOnSuccessListener {
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

        b.btnCerrarSesion.setOnClickListener { (activity as? MainActivityCliente)?.cerrarSesion() }
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}
