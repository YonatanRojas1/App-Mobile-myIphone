package com.myiphone.store.admin

import android.content.Intent
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.myiphone.store.R
import com.myiphone.store.SeleccionarTipoActivity
import com.myiphone.store.databinding.ActivityMainAdminBinding

/** Panel del administrador: Drawer + Bottom Navigation (dashboard, usuarios, productos, reporte). */
class MainActivityAdmin : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainAdminBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainAdminBinding.inflate(layoutInflater)
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
                R.id.ba_dashboard -> mostrar(ADashboardFragment(), R.string.adm_dashboard)
                R.id.ba_usuarios -> mostrar(AUsuariosFragment(), R.string.adm_usuarios)
                R.id.ba_productos -> mostrar(AProductosFragment(), R.string.adm_productos)
                R.id.ba_reporte -> mostrar(AReporteFragment(), R.string.adm_reporte)
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

        if (savedInstanceState == null) binding.bottomNav.selectedItemId = R.id.ba_dashboard
    }

    private fun cargarCabecera() {
        val cab = binding.navigationView.getHeaderView(0)
        cab.findViewById<TextView>(R.id.tvEmailCab).text = auth.currentUser?.email
        val tvNombre = cab.findViewById<TextView>(R.id.tvNombreCab)
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

    fun cerrarSesion() {
        auth.signOut()
        startActivity(Intent(this, SeleccionarTipoActivity::class.java))
        finishAffinity()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.na_dashboard -> binding.bottomNav.selectedItemId = R.id.ba_dashboard
            R.id.na_usuarios -> binding.bottomNav.selectedItemId = R.id.ba_usuarios
            R.id.na_productos -> binding.bottomNav.selectedItemId = R.id.ba_productos
            R.id.na_reporte -> binding.bottomNav.selectedItemId = R.id.ba_reporte
            R.id.na_cerrar -> cerrarSesion()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
