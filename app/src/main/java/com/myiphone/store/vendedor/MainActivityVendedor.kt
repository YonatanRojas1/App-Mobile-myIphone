package com.myiphone.store.vendedor

import android.content.Intent
import com.myiphone.store.Db
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.myiphone.store.R
import com.myiphone.store.SeleccionarTipoActivity
import com.myiphone.store.databinding.ActivityMainVendedorBinding

/** Panel del vendedor: Drawer + Bottom Navigation (resumen, productos, pedidos, perfil). */
class MainActivityVendedor : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainVendedorBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainVendedorBinding.inflate(layoutInflater)
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
                R.id.bv_resumen -> mostrar(VResumenFragment(), R.string.vend_resumen)
                R.id.bv_productos -> mostrar(VProductosFragment(), R.string.vend_productos)
                R.id.bv_pedidos -> mostrar(VPedidosFragment(), R.string.vend_pedidos)
                R.id.bv_perfil -> mostrar(VPerfilFragment(), R.string.vend_perfil)
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

        if (savedInstanceState == null) binding.bottomNav.selectedItemId = R.id.bv_resumen
    }

    private fun cargarCabecera() {
        val cab = binding.navigationView.getHeaderView(0)
        cab.findViewById<TextView>(R.id.tvEmailCab).text = auth.currentUser?.email
        val tvNombre = cab.findViewById<TextView>(R.id.tvNombreCab)
        // Nombre del usuario desde Firestore para el encabezado del menú lateral
        Db.nombreDeUsuario(auth.uid!!) { tvNombre.text = it }
    }

    private fun mostrar(fragment: Fragment, titulo: Int) {
        supportActionBar?.title = getString(titulo)
        supportFragmentManager.beginTransaction().replace(R.id.contenedor, fragment).commit()
    }

    fun cerrarSesion() {
        auth.signOut()
        startActivity(Intent(this, SeleccionarTipoActivity::class.java))
        finishAffinity()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nv_resumen -> binding.bottomNav.selectedItemId = R.id.bv_resumen
            R.id.nv_productos -> binding.bottomNav.selectedItemId = R.id.bv_productos
            R.id.nv_pedidos -> binding.bottomNav.selectedItemId = R.id.bv_pedidos
            R.id.nv_perfil -> binding.bottomNav.selectedItemId = R.id.bv_perfil
            R.id.nv_cerrar -> cerrarSesion()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
