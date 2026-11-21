package com.myiphone.store.vendedor

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.storage.FirebaseStorage
import com.myiphone.store.Db
import com.myiphone.store.R
import com.myiphone.store.comprador.IphoneProducto
import com.myiphone.store.comprador.ProductosRepo
import com.myiphone.store.databinding.ActivityProductoFormBinding

/** Crear o editar un iPhone del catálogo (CRUD del vendedor). Si llega "id" es edición. */
class ProductoFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductoFormBinding
    private var idEditar: String? = null
    private var imagenUri: Uri? = null   // imagen elegida (cámara o galería) pendiente de subir

    /** Recibe la foto tomada o la imagen elegida y la muestra en la vista previa. */
    private val selectorImagen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        when (r.resultCode) {
            RESULT_OK -> r.data?.data?.let { imagenUri = it; Glide.with(this).load(it).into(binding.ivPreview) }
            ImagePicker.RESULT_ERROR -> Toast.makeText(this, ImagePicker.getError(r.data), Toast.LENGTH_SHORT).show()
        }
    }

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

        // Cámara o galería, con recorte cuadrado y compresión antes de subir
        binding.btnImagen.setOnClickListener {
            ImagePicker.with(this).cropSquare().compress(1024).maxResultSize(1080, 1080)
                .createIntent { intent -> selectorImagen.launch(intent) }
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
        if (p.imagenUrl.isNotBlank()) Glide.with(this).load(p.imagenUrl).into(binding.ivPreview)
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

        // Si es nuevo, Firestore genera el id; si es edición se conserva el existente
        val ref = Db.productos()
        val id = idEditar ?: ref.document().id
        val producto = IphoneProducto(
            id = id, nombre = nombre, serie = serie, almacenamiento = alm, color = color,
            descripcion = binding.etDescripcion.text.toString().trim(),
            precio = precio, stock = stock,
            imagenUrl = binding.etImagenUrl.text.toString().trim(),
            destacado = binding.swDestacado.isChecked
        )

        binding.btnGuardar.isEnabled = false
        val uri = imagenUri
        if (uri == null) {
            guardarEnFirestore(producto)
        } else {
            // Sube la imagen a Firebase Storage (productos/{id}.jpg) y guarda su URL de descarga
            val dialogo = ProgressDialog(this)
            dialogo.setMessage(getString(R.string.vend_subiendo))
            dialogo.setCancelable(false)
            dialogo.show()
            val sref = FirebaseStorage.getInstance().reference.child("productos/$id.jpg")
            sref.putFile(uri)
                .continueWithTask { tarea ->
                    if (!tarea.isSuccessful) throw tarea.exception!!
                    sref.downloadUrl
                }
                .addOnSuccessListener { url ->
                    dialogo.dismiss()
                    guardarEnFirestore(producto.copy(imagenUrl = url.toString()))
                }
                .addOnFailureListener { e ->
                    dialogo.dismiss()
                    binding.btnGuardar.isEnabled = true
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    /** Crea o actualiza el documento Productos/{id} en Firestore. */
    private fun guardarEnFirestore(producto: IphoneProducto) {
        Db.productos().document(producto.id).set(producto)
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
