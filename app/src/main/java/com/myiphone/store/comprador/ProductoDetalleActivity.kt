package com.myiphone.store.comprador

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.myiphone.store.R
import com.myiphone.store.databinding.ActivityProductoDetalleBinding

class ProductoDetalleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductoDetalleBinding
    private var producto: IphoneProducto? = null
    private var cantidad = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductoDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.comp_detalle)

        val id = intent.getStringExtra("id").orEmpty()
        ProductosRepo.porId(id) { p ->
            if (p == null) {
                Toast.makeText(this, R.string.comp_no_encontrado, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                producto = p
                mostrar(p)
            }
        }

        binding.btnMas.setOnClickListener {
            val p = producto ?: return@setOnClickListener
            if (cantidad < p.stock) { cantidad++; binding.tvCantidad.text = cantidad.toString() }
        }
        binding.btnMenos.setOnClickListener {
            if (cantidad > 1) { cantidad--; binding.tvCantidad.text = cantidad.toString() }
        }
        binding.btnAgregar.setOnClickListener { agregar(false) }
        binding.btnComprarAhora.setOnClickListener { agregar(true) }
    }

    private fun mostrar(p: IphoneProducto) {
        binding.tvNombre.text = p.nombre
        binding.tvPrecio.text = Formato.cop(p.precio)
        binding.chipAlmacenamiento.text = p.almacenamiento
        binding.chipColor.text = p.color
        binding.tvDescripcion.text = p.descripcion
        binding.tvStock.text =
            if (p.stock > 0) getString(R.string.comp_disponibles, p.stock) else getString(R.string.comp_agotado)
        if (p.imagenUrl.isNotBlank()) {
            Glide.with(this).load(p.imagenUrl).placeholder(R.drawable.ic_iphone).into(binding.ivProducto)
        } else {
            binding.ivProducto.setImageResource(R.drawable.ic_iphone)
        }
        if (p.stock <= 0) {
            binding.btnAgregar.isEnabled = false
            binding.btnComprarAhora.isEnabled = false
        }
        binding.contenido.visibility = View.VISIBLE
    }

    private fun agregar(ir: Boolean) {
        val p = producto ?: return
        if (CarritoManager.agregar(p, cantidad)) {
            if (ir) startActivity(Intent(this, PagoActivity::class.java))
            else Toast.makeText(this, getString(R.string.comp_agregado, p.nombre), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.comp_sin_stock, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
