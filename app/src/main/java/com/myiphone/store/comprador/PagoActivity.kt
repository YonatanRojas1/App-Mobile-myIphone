package com.myiphone.store.comprador

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.myiphone.store.Db
import com.myiphone.store.R
import com.myiphone.store.cliente.MainActivityCliente
import com.myiphone.store.databinding.ActivityPagoBinding
import java.util.Locale

/**
 * Checkout del comprador: toma la dirección (a mano o con el GPS), cobra con ePayco y,
 * solo si la transacción fue aceptada, guarda el pedido en Firestore y descuenta el stock.
 */
class PagoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagoBinding
    private var nombreComprador = ""
    private var lat = 0.0
    private var lng = 0.0
    private var direccion = ""
    private var telefono = ""
    private var factura = ""

    /** Pide el permiso de ubicación; si lo dan, captura la posición. */
    private val permisoUbicacion = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) obtenerUbicacion() else Toast.makeText(this, R.string.pago_permiso_denegado, Toast.LENGTH_LONG).show()
    }

    /** Recibe el resultado del checkout de ePayco (aceptado o no). */
    private val checkoutEpayco = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            guardarPedido(r.data?.getStringExtra("ref").orEmpty(), r.data?.getStringExtra("estado").orEmpty())
        } else {
            val msg = r.data?.getStringExtra("mensaje") ?: getString(R.string.epayco_cancelado)
            MaterialAlertDialogBuilder(this).setTitle(R.string.epayco_no_completado)
                .setMessage(msg).setPositiveButton(R.string.vend_ok, null).show()
        }
    }

    /** Muestra el resumen de la compra y conecta los botones de ubicación y pago. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.comp_pago)

        if (CarritoManager.estaVacio()) { finish(); return }

        binding.tvResumen.text = CarritoManager.lista()
            .joinToString("\n") { "${it.cantidad} x ${it.producto.nombre} (${it.producto.almacenamiento})" }
        binding.tvTotal.text = Formato.cop(CarritoManager.total())

        FirebaseAuth.getInstance().uid?.let { uid -> Db.nombreDeUsuario(uid) { nombreComprador = it } }

        binding.btnPagar.text = getString(R.string.comp_pagar_monto, Formato.cop(CarritoManager.total()))
        binding.btnUbicacion.setOnClickListener { pedirUbicacion() }
        binding.btnPagar.setOnClickListener { validarYPagar() }
    }

    /** Si ya hay permiso captura la ubicación, si no lo solicita. */
    private fun pedirUbicacion() {
        val permiso = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED) obtenerUbicacion()
        else permisoUbicacion.launch(permiso)
    }

    /**
     * Obtiene la posición actual con FusedLocationProvider, la convierte en dirección con
     * Geocoder y la escribe en el campo de dirección de entrega.
     */
    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion() {
        Toast.makeText(this, R.string.pago_ubicando, Toast.LENGTH_SHORT).show()
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    Toast.makeText(this, R.string.pago_ubicacion_error, Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                lat = loc.latitude; lng = loc.longitude
                binding.tvUbicacion.text = getString(R.string.pago_ubicacion_ok, "%.5f".format(lat), "%.5f".format(lng))
                Thread {
                    // Geocoder puede tardar o fallar sin internet: se hace fuera del hilo principal
                    val texto = try {
                        @Suppress("DEPRECATION")
                        Geocoder(this, Locale("es", "CO")).getFromLocation(lat, lng, 1)
                            ?.firstOrNull()?.getAddressLine(0)
                    } catch (e: Exception) { null }
                    runOnUiThread { binding.etDireccion.setText(texto ?: "%.5f, %.5f".format(lat, lng)) }
                }.start()
            }
            .addOnFailureListener { Toast.makeText(this, R.string.pago_ubicacion_error, Toast.LENGTH_LONG).show() }
    }

    /** Valida dirección y teléfono y abre el checkout de ePayco con el total del carrito. */
    private fun validarYPagar() {
        direccion = binding.etDireccion.text.toString().trim()
        telefono = binding.etTelefono.text.toString().trim()
        if (direccion.length < 6) { binding.etDireccion.error = getString(R.string.comp_dir_invalida); binding.etDireccion.requestFocus(); return }
        if (telefono.length < 7) { binding.etTelefono.error = getString(R.string.comp_tel_invalido); binding.etTelefono.requestFocus(); return }
        if (EpaycoConfig.MODO_DEMO) {
            // Modo demostración: simula la respuesta "Aceptada" de ePayco
            factura = "MI-" + System.currentTimeMillis()
            guardarPedido("DEMO-$factura", "Aceptada (simulada)")
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        factura = "MI-" + System.currentTimeMillis()
        checkoutEpayco.launch(
            Intent(this, EpaycoActivity::class.java)
                .putExtra("monto", CarritoManager.total())
                .putExtra("descripcion", getString(R.string.pago_descripcion))
                .putExtra("factura", factura)
                .putExtra("email", user?.email.orEmpty())
                .putExtra("nombre", nombreComprador)
        )
    }

    /** Con el pago aceptado: crea el pedido (con coordenadas y referencia de ePayco), baja el stock y vacía el carrito. */
    private fun guardarPedido(refPago: String, estadoPago: String) {
        val dialogo = ProgressDialog(this)
        dialogo.setMessage(getString(R.string.comp_procesando))
        dialogo.setCancelable(false)
        dialogo.show()

        val user = FirebaseAuth.getInstance().currentUser
        val ref = Db.pedidos()
        val id = ref.document().id
        val items = CarritoManager.lista().map {
            ItemPedido(it.producto.id, it.producto.nombre, it.cantidad, it.producto.precio)
        }
        val pedido = Pedido(
            id = id,
            compradorUid = user?.uid.orEmpty(),
            compradorNombre = nombreComprador,
            compradorEmail = user?.email.orEmpty(),
            direccion = direccion,
            telefono = telefono,
            metodoPago = "ePayco",
            total = CarritoManager.total(),
            estado = "Pendiente",
            fecha = System.currentTimeMillis(),
            lat = lat, lng = lng,
            refPago = refPago, estadoPago = estadoPago,
            items = items
        )
        val aRestar = CarritoManager.lista().map { it.producto.id to it.cantidad }

        ref.document(id).set(pedido)
            .addOnSuccessListener {
                // Descuenta el stock de cada producto comprado (incremento atómico)
                aRestar.forEach { (pid, cant) ->
                    Db.productos().document(pid).update("stock", FieldValue.increment(-cant.toLong()))
                }
                dialogo.dismiss()
                CarritoManager.vaciar()
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.comp_pago_ok)
                    .setMessage(getString(R.string.comp_pago_ok_msg, id.takeLast(6).uppercase()) + "\nePayco: $refPago")
                    .setCancelable(false)
                    .setPositiveButton(R.string.comp_ver_pedidos) { _, _ ->
                        startActivity(Intent(this, MainActivityCliente::class.java)
                            .putExtra("abrir", "pedidos")
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    }
                    .show()
            }
            .addOnFailureListener { e ->
                dialogo.dismiss()
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
