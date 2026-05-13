package com.example.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainMenuActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sessionManager: SessionManager

    private var usuarioId: Int = -1
    private var usuarioNombre: String = ""
    private var usuarioCorreo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        sessionManager = SessionManager(this)

        // Verificar sesión
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        usuarioId = sessionManager.getUsuarioId()
        usuarioNombre = sessionManager.getUsuarioNombre()
        usuarioCorreo = sessionManager.getUsuarioCorreo()

        // Inicializar vistas
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNavigation)

        // Configurar Toolbar como ActionBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Configurar el toggle del menú
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configurar el header del menú con los datos del usuario
        val headerView = navView.getHeaderView(0)
        val txtUserName = headerView.findViewById<TextView>(R.id.txtUserName)
        val txtUserEmail = headerView.findViewById<TextView>(R.id.txtUserEmail)

        txtUserName.text = usuarioNombre.ifEmpty { "Usuario" }
        txtUserEmail.text = usuarioCorreo.ifEmpty { "correo@ejemplo.com" }

        // Configurar el manejo del botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Configurar el menú lateral
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio_drawer -> {
                    replaceFragment(InicioFragment())
                    bottomNav.selectedItemId = R.id.nav_inicio
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_favorito -> {
                    val favoritosFragment = FavoritosFragment.newInstance(usuarioId)
                    replaceFragment(favoritosFragment)
                    drawerLayout.closeDrawers()
                    true
                }
                //R.id.nav_Compras_drawer -> {
                  //  Toast.makeText(this@MainMenuActivity, "Mis Compras - Próximamente", Toast.LENGTH_SHORT).show()
                    //drawerLayout.closeDrawers()
                    //true
                //}
                //R.id.nav_cerrar_sesion -> {
                  //  cerrarSesion()
                    //true
                //}
                else -> false
            }
        }

        // Configurar Bottom Navigation
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, InicioFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    replaceFragment(InicioFragment())
                    true
                }
                R.id.nav_productos_entregables -> {
                    replaceFragment(ProductosEntregablesFragment())
                    true
                }
                R.id.nav_carrito -> {
                    val carritoFragment = CarritoFragment.newInstance(usuarioId)
                    replaceFragment(carritoFragment)
                    true
                }
                R.id.nav_favorito -> {
                    val favoritosFragment = FavoritosFragment.newInstance(usuarioId)
                    replaceFragment(favoritosFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }

    private fun cerrarSesion() {
        sessionManager.cerrarSesion()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }
}