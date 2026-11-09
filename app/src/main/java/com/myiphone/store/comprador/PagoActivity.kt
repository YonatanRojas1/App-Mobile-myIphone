package com.myiphone.store.comprador

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.myiphone.store.R
import com.myiphone.store.cliente.MainActivityCliente
import com.myiphone.store.databinding.ActivityPagoBinding

/**
 * Pasarela de pago SIMULADA (modo pruebas): valida el formulario, "aprueba" el pago
 * y guarda el pedido. No se almacenan datos de tarjeta.
 */
class PagoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagoBinding
    private var nombreComprador = ""

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

        val uid = FirebaseAuth.getInstance().uid
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("Usuarios").child(uid).child("nombres")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(sn: DataSnapshot) {
                        nombreComprador = sn.getValue(String::class.java).orEmpty()
                    }
                    override fun onCancelled(e: DatabaseError) {}
                })
        }

        binding.rgMetodo.setOnCheckedChangeListener { _, id ->
            binding.grupoTarjeta.visibility = if (id == R.id.rbTarjeta) View.VISIBLE else View.GONE
        }
        binding.btnPagar.text = getString(R.string.comp_pagar_monto, Formato.cop(CarritoManager.total()))
        binding.btnPagar.setOnClickListener { validarYPagar() }
    }

    private fun metodo(): String = when (binding.rgMetodo.checkedRadioButtonId) {
        R.id.rbPse -> "PSE"
        R.id.rbNequi -> "Nequi"
        else -> "Tarjeta"
    }

    private fun validarYPagar() {
        val dir = binding.etDireccion.text.toString().trim()
        val tel = binding.etTelefono.text.toString().trim()
        if (dir.length < 6) { binding.etDireccion.error = getString(R.string.comp_dir_invalida); binding.etDireccion.requestFocus(); return }
        if (tel.length < 7) { binding.etTelefono.error = getString(R.string.comp_tel_invalido); binding.etTelefono.requestFocus(); return }
        if (metodo() == "Tarjeta") {
            val num = binding.etTarjeta.text.toString().replace(" ", "")
            if (num.length < 13 || !num.all { it.isDigit() }) { binding.etTarjeta.error = getString(R.string.comp_tarjeta_invalida); return }
            if (binding.etVence.text.toString().length < 4) { binding.etVence.error = getString(R.string.comp_vence_invalido); return }
            if (binding.etCvv.text.toString().length < 3) { binding.etCvv.error = getString(R.string.comp_cvv_invalido); return }
        }

        val dialogo = ProgressDialog(this)
        dialogo.setMessage(getString(R.string.comp_procesando))
        dialogo.setCancelable(false)
        dialogo.show()
        // Simula el tiempo de respuesta de la pasarela
        Handler(Looper.getMainLooper()).postDelayed({ guardarPedido(dialogo, dir, tel) }, 1800)
    }

    private fun guardarPedido(dialogo: ProgressDialog, dir: String, tel: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val ref = FirebaseDatabase.getInstance().getReference("Pedidos")
        val id = ref.push().key ?: System.currentTimeMillis().toString()
        val items = CarritoManager.lista().map {
            ItemPedido(it.producto.id, it.producto.nombre, it.cantidad, it.producto.precio)
        }
        val pedido = Pedido(
            id = id,
            compradorUid = user?.uid.orEmpty(),
            compradorNombre = nombreComprador,
            compradorEmail = user?.email.orEmpty(),
            direccion = dir,
            telefono = tel,
            metodoPago = metodo(),
            total = CarritoManager.total(),
            estado = "Pendiente",
            fecha = System.currentTimeMillis(),
            items = items
        )
        val aRestar = CarritoManager.lista().map { it.producto.id to it.cantidad }

        ref.child(id).setValue(pedido)
            .addOnSuccessListener {
                aRestar.forEach { (pid, cant) ->
                    FirebaseDatabase.getInstance().getReference("Productos").child(pid).child("stock")
                        .setValue(ServerValue.increment(-cant.toLong()))
                }
                dialogo.dismiss()
                CarritoManager.vaciar()
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.comp_pago_ok)
                    .setMessage(getString(R.string.comp_pago_ok_msg, id.takeLast(6).uppercase()))
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
