package com.myiphone.store


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.myiphone.store.cliente.LoginClienteActivity
import com.myiphone.store.databinding.ActivitySeleccionarTipoBinding
import com.myiphone.store.vendedor.LoginVendedorActivity


class SeleccionarTipoActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySeleccionarTipoBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeleccionarTipoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tipoVendedor.setOnClickListener {
            startActivity(Intent(this@SeleccionarTipoActivity, LoginVendedorActivity::class.java))
        }

        binding.tipoCliente.setOnClickListener {
            startActivity(Intent(this@SeleccionarTipoActivity, LoginClienteActivity::class.java))
        }
    }
}