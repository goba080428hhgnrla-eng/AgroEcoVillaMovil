package mx.teknoeducativa.agroconectavilla

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mx.teknoeducativa.agroconectavilla.utils.Constants
import okhttp3.*
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.concurrent.TimeUnit

class MenuRepartidorActivity : AppCompatActivity() {

    private val TAG = "MenuRepartidor"
    private val WS_URL = "wss://agroconectavilla.onrender.com/ws/viajes/"
    private val HTTP_BASE_URL = Constants.BASE_URL

    private var miRepartidorId = -1

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var miPosicionActual = GeoPoint(0.0, 0.0)
    private var handlerGPS = Handler(Looper.getMainLooper())
    private var runnableEnviarGPS: Runnable? = null

    // Vistas
    private lateinit var mapNavegacion: MapView
    private lateinit var cardIndicaciones: CardView
    private lateinit var tvInstruccionPaso: TextView
    private lateinit var tvDetalleManifiesto: TextView
    private lateinit var btnSiguienteAccion: Button
    private lateinit var tvTiempoDistancia: TextView
    private lateinit var btnCentrarMapa: FloatingActionButton

    // Rutas
    private var rutaOverlayActual: Polyline? = null
    private var estadoViajePaso = 0
    private var roadManager: RoadManager? = null

    // Coordenadas logísticas
    private lateinit var puntoMercado: GeoPoint
    private lateinit var puntoCliente: GeoPoint
    private var mercadoNombre = ""
    private var clienteDireccion = ""
    private var listaArticulosText = ""
    private var currentPedidoId = ""

    // WebSocket
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var markerRepartidor: Marker? = null
    private var markerDestino: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )

        setContentView(R.layout.activity_menu_repartidor)

        miRepartidorId = intent.getIntExtra("usuario_id", -1)

        Log.d(TAG, "🔑 miRepartidorId al iniciar: $miRepartidorId")

        if (miRepartidorId == -1) {
            Log.e(TAG, "❌ ERROR: No se recibió usuario_id en el intent")
            Toast.makeText(this, "Error: Sesión inválida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inicializarVistas()
        inicializarMapa()
        inicializarGPS()
        conectarWebSocket()  // ← WebSocket solo para recibir notificaciones
        iniciarEnvioGPSPeriodico()
        // Verificar si viene de aceptar viaje
        if (intent.getBooleanExtra("iniciar_navegacion", false)) {
            iniciarNavegacion(
                intent.getStringExtra("pedido_id") ?: "",
                intent.getDoubleExtra("origen_lat", 0.0),
                intent.getDoubleExtra("origen_lon", 0.0),
                intent.getDoubleExtra("destino_lat", 0.0),
                intent.getDoubleExtra("destino_lon", 0.0),
                intent.getStringExtra("nombre_mercado") ?: "",
                intent.getStringExtra("direccion_cliente") ?: "",
                intent.getStringExtra("productos") ?: ""
            )
        }
    }

    private fun inicializarVistas() {
        mapNavegacion = findViewById(R.id.maplNavegacion)
        cardIndicaciones = findViewById(R.id.cardIndicaciones)
        tvInstruccionPaso = findViewById(R.id.tvInstruccionPaso)
        tvDetalleManifiesto = findViewById(R.id.tvDetalleManifiesto)
        btnSiguienteAccion = findViewById(R.id.btnSiguienteAccion)
        tvTiempoDistancia = findViewById(R.id.tvTiempoDistancia)
        btnCentrarMapa = findViewById(R.id.btnCentrarMapa)

        btnSiguienteAccion.setOnClickListener {
            cambiarFaseLogisticaDeEntrega()
        }

        btnCentrarMapa.setOnClickListener {
            centrarEnMiUbicacion()
        }
    }

    private fun inicializarMapa() {
        mapNavegacion.setMultiTouchControls(true)
        mapNavegacion.controller.setZoom(17.0)
        roadManager = OSRMRoadManager(this, "AgroConectaVilla")
        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_CAR)
    }

    @SuppressLint("MissingPermission")
    private fun inicializarGPS() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 1000

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    miPosicionActual = GeoPoint(location.latitude, location.longitude)
                    actualizarMarcadorRepartidor()
                    if (rutaOverlayActual != null) {
                        mapNavegacion.controller.animateTo(miPosicionActual)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun actualizarMarcadorRepartidor() {
        if (markerRepartidor == null) {
            markerRepartidor = Marker(mapNavegacion).apply {
                position = miPosicionActual
                title = "Tu ubicación"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapNavegacion.overlays.add(markerRepartidor)
        } else {
            markerRepartidor?.position = miPosicionActual
        }
        mapNavegacion.invalidate()
    }

    private fun centrarEnMiUbicacion() {
        if (miPosicionActual.latitude != 0.0) {
            mapNavegacion.controller.animateTo(miPosicionActual)
        }
    }

    private fun iniciarEnvioGPSPeriodico() {
        runnableEnviarGPS = object : Runnable {
            override fun run() {
                enviarUbicacionAlServidor()
                handlerGPS.postDelayed(this, 3000)
            }
        }
        handlerGPS.post(runnableEnviarGPS!!)
    }

    private fun enviarUbicacionAlServidor() {
        if (miPosicionActual.latitude != 0.0 && miRepartidorId != -1) {
            val url = "${HTTP_BASE_URL}api/pedidos/transmitir_gps/"
            val body = JSONObject().apply {
                put("usuario_id", miRepartidorId)
                put("latitud", miPosicionActual.latitude)
                put("longitud", miPosicionActual.longitude)
            }

            val request = JsonObjectRequest(
                Request.Method.POST, url, body,
                { },
                { error -> Log.e(TAG, "Error enviando GPS: ${error.message}") }
            )
            Volley.newRequestQueue(this).add(request)
        }
    }

    // ========== WEBSOCKET PARA RECIBIR NOTIFICACIONES ==========
    private fun conectarWebSocket() {
        Log.d(TAG, "Conectando WebSocket...")
        val request = okhttp3.Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✅ WebSocket conectado")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "📨 Mensaje RAW recibido: $text")

                try {
                    val json = JSONObject(text)

                    if (json.has("type") && json.getString("type") == "enviar_notificacion_viaje") {
                        val pedidoId = json.optString("pedido_id", "0")
                        val destino = json.optString("destino", "Destino no especificado")
                        val total = json.optString("total", "0.00")

                        Log.d(TAG, "📦 PedidoID: $pedidoId")
                        Log.d(TAG, "📦 miRepartidorId ANTES de enviar: $miRepartidorId")  // ← VERIFICAR

                        runOnUiThread {
                            val intent = Intent(this@MenuRepartidorActivity, OfertaViajeActivity::class.java).apply {
                                putExtra("pedido_id", pedidoId)
                                putExtra("usuario_id", miRepartidorId)  // ← Este valor debe ser > 0
                                putExtra("destino", destino)
                                putExtra("total", total)
                            }

                            Log.d(TAG, "📤 Enviando intent con usuario_id: $miRepartidorId")
                            startActivity(intent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "❌ WebSocket falló: ${t.message}")
                Handler(Looper.getMainLooper()).postDelayed({ conectarWebSocket() }, 5000)
            }
        })
    }

    // ========== LÓGICA DE NAVEGACIÓN (después de aceptar viaje) ==========
    fun iniciarNavegacion(pedidoId: String, origenLat: Double, origenLon: Double,
                          destinoLat: Double, destinoLon: Double,
                          nombreMercado: String, direccionCliente: String, productos: String) {
        currentPedidoId = pedidoId
        puntoMercado = GeoPoint(origenLat, origenLon)
        puntoCliente = GeoPoint(destinoLat, destinoLon)
        mercadoNombre = nombreMercado
        clienteDireccion = direccionCliente
        listaArticulosText = productos

        iniciarModoRutaUber(0)
    }

    private fun iniciarModoRutaUber(fase: Int) {
        estadoViajePaso = fase
        cardIndicaciones.visibility = View.VISIBLE
        btnSiguienteAccion.visibility = View.VISIBLE
        tvTiempoDistancia.visibility = View.VISIBLE

        val destinoRuta = if (fase == 0) puntoMercado else puntoCliente

        if (fase == 0) {
            tvInstruccionPaso.text = "📦 Recoger en: $mercadoNombre"
            tvDetalleManifiesto.text = "Productos: $listaArticulosText"
            btnSiguienteAccion.text = "✅ YA RECOGÍ EL PEDIDO"
        } else {
            tvInstruccionPaso.text = "🚚 Entregar en: $clienteDireccion"
            tvDetalleManifiesto.text = "Entregar todos los productos al cliente"
            btnSiguienteAccion.text = "🎉 CONFIRMAR ENTREGA"
        }

        mapNavegacion.overlays.removeAll { it is Polyline || (it is Marker && it != markerRepartidor) }

        markerDestino = Marker(mapNavegacion).apply {
            position = destinoRuta
            title = if (fase == 0) "Mercado" else "Cliente"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapNavegacion.overlays.add(markerDestino)

        calcularRuta()
    }

    private fun calcularRuta() {
        val origenRuta = miPosicionActual
        val destinoRuta = if (estadoViajePaso == 0) puntoMercado else puntoCliente

        Thread {
            try {
                val waypoints = arrayListOf(origenRuta, destinoRuta)
                val road = roadManager?.getRoad(waypoints)

                runOnUiThread {
                    if (road != null && road.mStatus == Road.STATUS_OK) {
                        rutaOverlayActual = RoadManager.buildRoadOverlay(road)
                        rutaOverlayActual?.outlinePaint?.color = android.graphics.Color.parseColor("#2196F3")
                        rutaOverlayActual?.outlinePaint?.strokeWidth = 12f
                        mapNavegacion.overlays.add(rutaOverlayActual)

                        val distanciaKm = road.mLength / 1000.0
                        val tiempoMin = (road.mDuration / 60).toInt()
                        tvTiempoDistancia.text = String.format("📏 %.1f km • ⏱️ %d min", distanciaKm, tiempoMin)
                        mapNavegacion.controller.animateTo(origenRuta)
                    } else {
                        tvTiempoDistancia.text = "📍 Ruta no encontrada"
                    }
                    mapNavegacion.invalidate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculando ruta: ${e.message}")
            }
        }.start()
    }

    private fun cambiarFaseLogisticaDeEntrega() {
        if (estadoViajePaso == 0) {
            cambiarEstadoPedido("recolectado") {
                iniciarModoRutaUber(1)
            }
        } else {
            cambiarEstadoPedido("entregado") {
                cardIndicaciones.visibility = View.GONE
                btnSiguienteAccion.visibility = View.GONE
                tvTiempoDistancia.visibility = View.GONE
                mapNavegacion.overlays.clear()
                Toast.makeText(this, "🎉 ¡Viaje completado!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun cambiarEstadoPedido(estado: String, onSuccess: () -> Unit) {
        val url = "${HTTP_BASE_URL}api/pedidos/actualizar_estado/"
        val body = JSONObject().apply {
            put("pedido_id", currentPedidoId)
            put("estado", estado)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, body,
            { onSuccess() },
            { error -> Log.e(TAG, "Error: ${error.message}") }
        )
        Volley.newRequestQueue(this).add(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        runnableEnviarGPS?.let { handlerGPS.removeCallbacks(it) }
        webSocket?.close(1000, "Cerrando")
    }
}