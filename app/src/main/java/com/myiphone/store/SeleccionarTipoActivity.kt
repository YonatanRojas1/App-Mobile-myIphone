package com.myiphone.store

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.myiphone.store.cliente.LoginClienteActivity
import com.myiphone.store.databinding.ActivitySeleccionarTipoBinding
import com.myiphone.store.vendedor.LoginVendedorActivity

class SeleccionarTipoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeleccionarTipoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeleccionarTipoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tipoCliente.setOnClickListener {
            startActivity(Intent(this@SeleccionarTipoActivity, LoginClienteActivity::class.java))
        }

        binding.tipoVendedor.setOnClickListener {
            startActivity(Intent(this@SeleccionarTipoActivity, LoginVendedorActivity::class.java))
        }

        // El login del administrador se crea en una fase posterior
        binding.tipoAdmin.setOnClickListener {
            Toast.makeText(this, R.string.admin_proximamente, Toast.LENGTH_SHORT).show()
        }
    }
}
