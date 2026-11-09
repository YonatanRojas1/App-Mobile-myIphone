package com.myiphone.store.vendedor

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.myiphone.store.R
import com.myiphone.store.comprador.IphoneProducto
import com.myiphone.store.comprador.ProductosRepo
import com.myiphone.store.databinding.ActivityProductoFormBinding

/** Crear o editar un iPhone del catálogo (CRUD del vendedor). Si llega "id" es edición. */
class ProductoFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductoFormBinding
    private var idEditar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductoFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        idEditar = intent.getStringExtra("id")
        supportActionBar?.title = getString(if (idEditar == null) R.string.vend_nuevo else R.string.vend_editar)

        idEditar?.let { id ->
            ProductosRepo.porId(id) { p -> p?.let { llenar(it) } }
        }

        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun llenar(p: IphoneProducto) {
        binding.etNombre.setText(p.nombre)
        binding.etSerie.setText(p.serie)
        binding.etAlmacenamiento.setText(p.almacenamiento)
        binding.etColor.setText(p.color)
        binding.etPrecio.setText(p.precio.toString())
        binding.etStock.setText(p.stock.toString())
        binding.etImagenUrl.setText(p.imagenUrl)
        binding.etDescripcion.setText(p.descripcion)
        binding.swDestacado.isChecked = p.destacado
    }

    private fun guardar() {
        val nombre = binding.etNombre.text.toString().trim()
        val serie = binding.etSerie.text.toString().trim()
        val alm = binding.etAlmacenamiento.text.toString().trim()
        val color = binding.etColor.text.toString().trim()
        val precio = binding.etPrecio.text.toString().trim().toLongOrNull()
        val stock = binding.etStock.text.toString().trim().toIntOrNull()

        if (nombre.isEmpty()) { binding.etNombre.error = getString(R.string.vend_req); binding.etNombre.requestFocus(); return }
        if (serie.isEmpty()) { binding.etSerie.error = getString(R.string.vend_req); binding.etSerie.requestFocus(); return }
        if (alm.isEmpty()) { binding.etAlmacenamiento.error = getString(R.string.vend_req); binding.etAlmacenamiento.requestFocus(); return }
        if (color.isEmpty()) { binding.etColor.error = getString(R.string.vend_req); binding.etColor.requestFocus(); return }
        if (precio == null || precio <= 0) { binding.etPrecio.error = getString(R.string.vend_precio_inv); binding.etPrecio.requestFocus(); return }
        if (stock == null || stock < 0) { binding.etStock.error = getString(R.string.vend_stock_inv); binding.etStock.requestFocus(); return }

        val ref = FirebaseDatabase.getInstance().getReference("Productos")
        val id = idEditar ?: (ref.push().key ?: System.currentTimeMillis().toString())
        val producto = IphoneProducto(
            id = id, nombre = nombre, serie = serie, almacenamiento = alm, color = color,
            descripcion = binding.etDescripcion.text.toString().trim(),
            precio = precio, stock = stock,
            imagenUrl = binding.etImagenUrl.text.toString().trim(),
            destacado = binding.swDestacado.isChecked
        )

        binding.btnGuardar.isEnabled = false
        ref.child(id).setValue(producto)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.vend_guardado, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnGuardar.isEnabled = true
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
