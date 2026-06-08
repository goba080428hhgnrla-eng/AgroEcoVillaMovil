package mx.teknoeducativa.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.view.View
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

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        usuarioId = sessionManager.getUsuarioId()
        usuarioNombre = sessionManager.getUsuarioNombre()
        usuarioCorreo = sessionManager.getUsuarioCorreo()

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNavigation)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = navView.getHeaderView(0)
        val txtUserName = headerView.findViewById<TextView>(R.id.txtUserName)
        val txtUserEmail = headerView.findViewById<TextView>(R.id.txtUserEmail)

        txtUserName.text = usuarioNombre.ifEmpty { "Usuario" }
        txtUserEmail.text = usuarioCorreo.ifEmpty { "correo@ejemplo.com" }

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

        configurarMenuSegunRol()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio_drawer -> {
                    replaceFragment(InicioFragment())
                    bottomNav.selectedItemId = R.id.nav_inicio
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_favorito -> {
                    replaceFragment(FavoritosFragment.newInstance(usuarioId))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_repartidor -> {
                    manejarAccesoModoRepartidor()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_soporte -> {
                    replaceFragment(SoporteFragment())
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_privacidad -> {
                    replaceFragment(PrivacidadFragment())
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

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    replaceFragment(InicioFragment())
                    true
                }
                R.id.nav_productos_entregables -> {
                    if (!sessionManager.esRepartidor()) {
                        replaceFragment(ProductosEntregablesFragment())
                    }
                    true
                }
                R.id.nav_carrito -> {
                    if (!sessionManager.esRepartidor()) {
                        replaceFragment(CarritoFragment.newInstance(usuarioId))
                    }
                    true
                }
                R.id.nav_pedidos -> {
                    replaceFragment(PedidosFragment.newInstance(usuarioId))
                    true
                }
                else -> false
            }
        }

        // ==========================================
        // INICIALIZAR FRAGMENT SEGUN EL ROL
        // ==========================================
        if (savedInstanceState == null) {
            val abrirModoRepartidor = intent.getBooleanExtra("abrir_modo_repartidor", false)
            val productoId = intent.getIntExtra("id_producto", 0)

            when {
                abrirModoRepartidor || sessionManager.esRepartidor() -> {
                    // Abrir directamente modo repartidor
                    iniciarWebSocketService()
                    val repartidorFragment = ModoRepartidorFragment.newInstance(usuarioId)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, repartidorFragment)
                        .commit()
                    bottomNav.visibility = View.GONE
                    toolbar.title = "Modo Repartidor"
                }
                productoId != 0 -> {
                    // Deep link - abrir detalle de producto
                    val fragment = FragmentDetalle()
                    val bundle = Bundle()
                    bundle.putInt("id_producto", productoId)
                    fragment.arguments = bundle
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, fragment)
                        .commit()
                }
                else -> {
                    // Usuario normal - mostrar inicio
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainer, InicioFragment())
                        .commit()
                }
            }
        }
        if (intent.getBooleanExtra("abrir_repartidor", false) || sessionManager.esRepartidor()) {
            val repartidorId = sessionManager.getUsuarioId()
            val repartidorFragment = ModoRepartidorFragment.newInstance(repartidorId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameContainer, repartidorFragment)
                .commit()
            bottomNav.visibility = View.GONE
            toolbar.title = "Modo Repartidor"
        }
    }

    private fun configurarMenuSegunRol() {
        val esRepartidor = sessionManager.esRepartidor()
        val bottomMenu = bottomNav.menu

        if (esRepartidor) {
            bottomMenu.findItem(R.id.nav_inicio)?.isVisible = false
            bottomMenu.findItem(R.id.nav_productos_entregables)?.isVisible = false
            bottomMenu.findItem(R.id.nav_carrito)?.isVisible = false
            bottomMenu.findItem(R.id.nav_pedidos)?.isVisible = false

            val menuDrawer = navView.menu
            menuDrawer.findItem(R.id.nav_inicio_drawer)?.isVisible = false
            menuDrawer.findItem(R.id.nav_favorito)?.isVisible = false
            menuDrawer.findItem(R.id.nav_repartidor)?.title = "Mi Viaje Actual"
            menuDrawer.findItem(R.id.nav_soporte)?.isVisible = true
            menuDrawer.findItem(R.id.nav_privacidad)?.isVisible = true
            menuDrawer.findItem(R.id.nav_cerrar_sesion)?.isVisible = true
        } else {
            bottomMenu.findItem(R.id.nav_inicio)?.isVisible = true
            bottomMenu.findItem(R.id.nav_productos_entregables)?.isVisible = true
            bottomMenu.findItem(R.id.nav_carrito)?.isVisible = true
            bottomMenu.findItem(R.id.nav_pedidos)?.isVisible = true

            val menuDrawer = navView.menu
            menuDrawer.findItem(R.id.nav_inicio_drawer)?.isVisible = true
            menuDrawer.findItem(R.id.nav_favorito)?.isVisible = true
            menuDrawer.findItem(R.id.nav_repartidor)?.title = "Ser Repartidor"
            menuDrawer.findItem(R.id.nav_soporte)?.isVisible = true
            menuDrawer.findItem(R.id.nav_privacidad)?.isVisible = true
            menuDrawer.findItem(R.id.nav_cerrar_sesion)?.isVisible = true
        }
    }

    private fun manejarAccesoModoRepartidor() {
        val esRepartidor = sessionManager.esRepartidor()

        if (esRepartidor) {
            iniciarWebSocketService()
            replaceFragment(ModoRepartidorFragment.newInstance(usuarioId))
            bottomNav.visibility = View.GONE
            toolbar.title = "Modo Repartidor"
        } else {
            replaceFragment(RepartidoresFragment.newInstance(usuarioId))
            bottomNav.visibility = View.VISIBLE
            toolbar.title = "AgroConecta"
        }
        drawerLayout.closeDrawers()
    }

    private fun iniciarWebSocketService() {
        try {
            val serviceIntent = Intent(this, WebSocketService::class.java).apply {
                putExtra("repartidor_id", usuarioId)
            }
            startService(serviceIntent)
        } catch (e: Exception) {
            // Error silencioso
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }

    private fun cerrarSesion() {
        try {
            stopService(Intent(this, WebSocketService::class.java))
        } catch (e: Exception) { }
        sessionManager.cerrarSesion()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        Toast.makeText(this, "Sesion cerrada", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        configurarMenuSegunRol()

        val fragmentActual = supportFragmentManager.findFragmentById(R.id.frameContainer)
        if (fragmentActual != null && fragmentActual !is ModoRepartidorFragment) {
            bottomNav.visibility = View.VISIBLE
            toolbar.title = "AgroConecta"
        } else if (fragmentActual is ModoRepartidorFragment) {
            bottomNav.visibility = View.GONE
            toolbar.title = "Modo Repartidor"
        }
    }
}