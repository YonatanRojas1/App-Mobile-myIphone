package com.myiphone.store.cliente

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.myiphone.store.R
import com.myiphone.store.SeleccionarTipoActivity
import com.myiphone.store.comprador.CarritoManager
import com.myiphone.store.comprador.FragmentCarrito
import com.myiphone.store.comprador.FragmentCatalogo
import com.myiphone.store.comprador.FragmentHome
import com.myiphone.store.comprador.FragmentPedidos
import com.myiphone.store.comprador.FragmentPerfil
import com.myiphone.store.databinding.ActivityMainClienteBinding

/** Panel del comprador: Drawer (menú lateral) + Bottom Navigation (inicio, catálogo, carrito, perfil). */
class MainActivityCliente : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainClienteBinding
    private val auth = FirebaseAuth.getInstance()
    private val oyenteCarrito: () -> Unit = { actualizarInsignia() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            startActivity(Intent(this, SeleccionarTipoActivity::class.java))
            finishAffinity()
            return
        }

        setSupportActionBar(binding.toolbar)
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar,
            R.string.open_drawer, R.string.close_drawer)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)
        cargarCabecera()

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bn_inicio -> mostrar(FragmentHome(), R.string.comp_inicio)
                R.id.bn_catalogo -> mostrar(FragmentCatalogo(), R.string.comp_catalogo)
                R.id.bn_carrito -> mostrar(FragmentCarrito(), R.string.comp_carrito)
                R.id.bn_perfil -> mostrar(FragmentPerfil(), R.string.comp_perfil)
            }
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState == null) {
            if (intent.getStringExtra("abrir") == "pedidos") mostrar(FragmentPedidos(), R.string.comp_pedidos)
            else binding.bottomNav.selectedItemId = R.id.bn_inicio
        }
    }

    override fun onResume() {
        super.onResume()
        CarritoManager.agregarOyente(oyenteCarrito)
        actualizarInsignia()
    }

    override fun onPause() {
        CarritoManager.quitarOyente(oyenteCarrito)
        super.onPause()
    }

    private fun actualizarInsignia() {
        val n = CarritoManager.cantidadTotal()
        if (n > 0) binding.bottomNav.getOrCreateBadge(R.id.bn_carrito).number = n
        else binding.bottomNav.removeBadge(R.id.bn_carrito)
    }

    private fun cargarCabecera() {
        val cab = binding.navigationView.getHeaderView(0)
        val tvNombre = cab.findViewById<android.widget.TextView>(R.id.tvNombreCab)
        val tvEmail = cab.findViewById<android.widget.TextView>(R.id.tvEmailCab)
        tvEmail.text = auth.currentUser?.email
        FirebaseDatabase.getInstance().getReference("Usuarios").child(auth.uid!!).child("nombres")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(sn: DataSnapshot) {
                    tvNombre.text = sn.getValue(String::class.java).orEmpty()
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun mostrar(fragment: Fragment, titulo: Int) {
        supportActionBar?.title = getString(titulo)
        supportFragmentManager.beginTransaction().replace(R.id.contenedor, fragment).commit()
    }

    fun abrirCatalogo() { binding.bottomNav.selectedItemId = R.id.bn_catalogo }

    fun cerrarSesion() {
        CarritoManager.vaciar()
        auth.signOut()
        startActivity(Intent(this, SeleccionarTipoActivity::class.java))
        finishAffinity()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_inicio -> binding.bottomNav.selectedItemId = R.id.bn_inicio
            R.id.nav_catalogo -> binding.bottomNav.selectedItemId = R.id.bn_catalogo
            R.id.nav_perfil -> binding.bottomNav.selectedItemId = R.id.bn_perfil
            R.id.nav_pedidos -> mostrar(FragmentPedidos(), R.string.comp_pedidos)
            R.id.nav_cerrar -> cerrarSesion()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
