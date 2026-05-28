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

/**
 * Actividad principal del menú de navegación (MainMenuActivity).
 * Actúa como el contenedor central (Host) de la aplicación, coordinando e interconectando:
 * 1. Un menú lateral desplegable (Navigation Drawer) para accesos secundarios y perfil.
 * 2. Una barra de navegación inferior (Bottom Navigation View) para las secciones principales.
 * 3. El enrutamiento de fragmentos dinámicos en base a interacciones o redirecciones por Deep Links.
 * 4. El control del ciclo de vida de la sesión activa del usuario.
 */
class MainMenuActivity : AppCompatActivity() {

    // Componentes de infraestructura visual de navegación
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sessionManager: SessionManager

    // Almacenamiento local temporal del perfil del usuario en sesión
    private var usuarioId: Int = -1
    private var usuarioNombre: String = ""
    private var usuarioCorreo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cargar el esquema XML de contenedores e interfaces
        setContentView(R.layout.activity_main_menu)

        sessionManager = SessionManager(this)

        // ==========================================
        // 1. CONTROL DE SEGURIDAD Y ACCESO
        // ==========================================
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Recuperar los metadatos de identidad del usuario actual
        usuarioId = sessionManager.getUsuarioId()
        usuarioNombre = sessionManager.getUsuarioNombre()
        usuarioCorreo = sessionManager.getUsuarioCorreo()

        // Inicializar componentes visuales desde el XML
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNavigation)

        // ==========================================
        // 2. CONFIGURACIÓN DEL TOOLBAR Y DRAWERS
        // ==========================================
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Sincronizar el ícono de hamburguesa con el estado del menú lateral (abierto/cerrado)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Vincular los datos del usuario al encabezado (Header) del menú lateral
        val headerView = navView.getHeaderView(0)
        val txtUserName = headerView.findViewById<TextView>(R.id.txtUserName)
        val txtUserEmail = headerView.findViewById<TextView>(R.id.txtUserEmail)

        txtUserName.text = usuarioNombre.ifEmpty { "Usuario" }
        txtUserEmail.text = usuarioCorreo.ifEmpty { "correo@ejemplo.com" }

        // ==========================================
        // 3. CONTROL CENTRALIZADO DEL BOTÓN ATRÁS
        // ==========================================
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Si el menú lateral está desplegado, la primera pulsación lo cierra de forma segura
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Si está cerrado, deshabilitar temporalmente este callback y delegar la acción al sistema
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // ==========================================
        // 4. ESCUCHADOR DE EVENTOS: MENÚ LATERAL (DRAWER)
        // ==========================================
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio_drawer -> {
                    replaceFragment(InicioFragment())
                    // Sincronizar la barra inferior para reflejar el cambio de sección
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
                R.id.nav_favorito -> {
                    val favoritosFragment = FavoritosFragment.newInstance(usuarioId)
                    replaceFragment(favoritosFragment)
                drawerLayout.closeDrawers()
                true
                }
                R.id.nav_cerrar_sesion -> {
                  cerrarSesion()
                true
                }
                else -> false
            }
        }

        // ==========================================
        // 5. ENRUTAMIENTO INICIAL Y MANEJO DE DEEP LINKS
        // ==========================================
        if (savedInstanceState == null) {
            // Evaluar si MainActivity interceptó un ID de producto a través de un Deep Link
            val productoId = intent.getIntExtra("id_producto", 0)

            if (productoId != 0) {
                // Inyectar el ID de destino en un Bundle para abrir directamente el detalle del producto
                val fragment = FragmentDetalle()
                val bundle = Bundle()
                bundle.putInt("id_producto", productoId)
                fragment.arguments = bundle

                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameContainer, fragment)
                    .commit()
            } else {
                // Carga inicial por defecto: Mostrar la pantalla de Inicio
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameContainer, InicioFragment())
                    .commit()
            }
        }

        // ==========================================
        // 6. ESCUCHADOR DE EVENTOS: BARRA INFERIOR (BOTTOM NAV)
        // ==========================================
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

    /**
     * Reemplaza el fragmento contenido en el contenedor principal ([R.id.frameContainer])
     * de forma atómica y confirma la transacción inmediatamente.
     *
     * @param fragment Nueva instancia de tipo [Fragment] que será posicionada en la pantalla.
     */
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }

    /**
     * Purga los datos guardados en el administrador de sesión, destruye la pila
     * de navegación de la actividad y redirige el flujo de control hacia [MainActivity].
     */
    private fun cerrarSesion() {
        sessionManager.cerrarSesion()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }
}