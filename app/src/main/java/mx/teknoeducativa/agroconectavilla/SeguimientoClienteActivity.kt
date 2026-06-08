package mx.teknoeducativa.agroconectavilla

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mx.teknoeducativa.agroconectavilla.utils.Constants
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class SeguimientoClienteActivity : AppCompatActivity() {

    private val TAG = "SeguimientoCliente"
    private lateinit var mapView: MapView
    private lateinit var tvEstado: TextView
    private lateinit var tvTiempoEstimado: TextView
    private lateinit var tvDistancia: TextView

    private var pedidoId: String = ""
    private var clienteId: Int = -1
    private var handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private var markerRepartidor: Marker? = null
    private var markerMercado: Marker? = null
    private var markerDestino: Marker? = null
    private var rutaActual: Polyline? = null

    private var estadoPedidoActual: String = "pendiente"
    private var calcularRutaEnProgreso = false
    private var ultimaUbicacionRutaCalculada: GeoPoint? = null

    private lateinit var sessionManager: SessionManager

    companion object {
        const val ACTION_ACTUALIZACION = "mx.teknoeducativa.agroconectavilla.ACTUALIZACION_SEGUIMIENTO"
        const val EXTRA_LATITUD = "latitud"
        const val EXTRA_LONGITUD = "longitud"
        const val EXTRA_ESTADO = "estado"
        const val EXTRA_DISTANCIA = "distancia"
        const val EXTRA_TIEMPO = "tiempo"
    }

    private val seguimientoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ACTUALIZACION) {
                val lat = intent.getDoubleExtra(EXTRA_LATITUD, 0.0)
                val lon = intent.getDoubleExtra(EXTRA_LONGITUD, 0.0)
                val estado = intent.getStringExtra(EXTRA_ESTADO) ?: "pendiente"
                estadoPedidoActual = estado

                actualizarUI(estado, "", "")

                if (lat != 0.0 && lon != 0.0) {
                    actualizarMarcadorRepartidor(GeoPoint(lat, lon))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguimiento_cliente)

        // Inicializar OSMdroid
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        sessionManager = SessionManager(this)
        clienteId = sessionManager.getUsuarioId()
        pedidoId = intent.getStringExtra("pedido_id") ?: ""

        if (pedidoId.isEmpty()) {
            Toast.makeText(this, "Error: Pedido no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inicializarVistas()
        inicializarMapa()
        cargarInformacionInicial()
        iniciarTracking()
    }

    private fun inicializarVistas() {
        mapView = findViewById(R.id.mapView)
        tvEstado = findViewById(R.id.tvEstadoEnvio)
        tvTiempoEstimado = findViewById(R.id.tvTiempoEstimado)
        tvDistancia = findViewById(R.id.tvDistanciaEstimada)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Seguimiento de Pedido"
    }

    private fun inicializarMapa() {
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
    }

    private fun cargarInformacionInicial() {
        val url = "${Constants.BASE_URL}api/pedidos/seguimiento/$pedidoId/"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.optString("status") == "ok") {
                    val estado = response.optString("estado_pedido", "pendiente")
                    estadoPedidoActual = estado

                    val latMercado = response.optDouble("latitud_mercado", 19.727433)
                    val lonMercado = response.optDouble("longitud_mercado", -99.4627729)
                    val latDestino = response.optDouble("latitud_destino", 0.0)
                    val lonDestino = response.optDouble("longitud_destino", 0.0)
                    val direccion = response.optString("direccion", "")

                    agregarMarcadorMercado(GeoPoint(latMercado, lonMercado))

                    if (latDestino != 0.0 && lonDestino != 0.0) {
                        agregarMarcadorDestino(GeoPoint(latDestino, lonDestino), direccion)
                    }

                    actualizarUI(estado, "", "")
                }
            },
            { error ->
                Toast.makeText(this, "Error cargando informacion: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun iniciarTracking() {
        val serviceIntent = Intent(this, SeguimientoService::class.java).apply {
            putExtra("pedido_id", pedidoId)
            putExtra("cliente_id", clienteId)
        }
        startService(serviceIntent)

        val filter = IntentFilter(ACTION_ACTUALIZACION)
        LocalBroadcastManager.getInstance(this).registerReceiver(seguimientoReceiver, filter)

        iniciarPolling()
    }

    private fun iniciarPolling() {
        updateRunnable = object : Runnable {
            override fun run() {
                obtenerUbicacionRepartidor()
                handler.postDelayed(this, 10000) // Cambiado a 10s para no saturar tu servidor web
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun obtenerUbicacionRepartidor() {
        val url = "${Constants.BASE_URL}api/pedidos/obtener_gps/$pedidoId/"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.optString("status") == "ok") {
                    val lat = response.optDouble("latitud")
                    val lon = response.optDouble("longitud")
                    val estado = response.optString("estado_pedido", "pendiente")
                    estadoPedidoActual = estado

                    if (lat != 0.0 && lon != 0.0) {
                        actualizarMarcadorRepartidor(GeoPoint(lat, lon))
                    }
                    actualizarUI(estado, "", "")
                }
            },
            { error -> /* Error silencioso */ }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun actualizarMarcadorRepartidor(posicion: GeoPoint) {
        if (markerRepartidor == null) {
            markerRepartidor = Marker(mapView).apply {
                position = posicion
                title = "Mi Repartidor"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(markerRepartidor)
            mapView.controller.animateTo(posicion)
        } else {
            markerRepartidor?.position = posicion
        }

        // Proteger el consumo de OpenRouteService evaluando distancia
        if (!calcularRutaEnProgreso) {
            if (ultimaUbicacionRutaCalculada == null ||
                posicion.distanceToAsDouble(ultimaUbicacionRutaCalculada) > 40.0) {

                actualizarRutaProfesional(posicion)
            }
        }

        mapView.invalidate()
    }

    // =======================================================
    // NUEVO MÉTODO: CONEXIÓN REAL CON OPENROUTESERVICE
    // =======================================================
    private fun actualizarRutaProfesional(origen: GeoPoint) {
        // Definir el punto de destino según la fase del pedido
        val destino = when (estadoPedidoActual) {
            "aceptado", "en_camino" -> {
                if (markerMercado != null) markerMercado!!.position else return
            }
            "recolectado" -> {
                if (markerDestino != null) markerDestino!!.position else return
            }
            else -> return // Si está pendiente o entregado, no trazamos ruta dinámica
        }

        calcularRutaEnProgreso = true

        // REEMPLAZA CON TU TOKEN DE OPENROUTESERVICE ACTIVO
        val apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjA5ZTQzZDI1NDdmMjRjMzI4MzRmMzk4Y2ZmZGJkYzg1IiwiaCI6Im11cm11cjY0In0="
        val url = "https://api.openrouteservice.org/v2/directions/driving-car" +
                "?api_key=$apiKey" +
                "&start=${origen.longitude},${origen.latitude}" +
                "&end=${destino.longitude},${destino.latitude}"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                calcularRutaEnProgreso = false
                try {
                    val features = response.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val feature = features.getJSONObject(0)
                        val geometry = feature.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val points = mutableListOf<GeoPoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            points.add(GeoPoint(lat, lon))
                        }

                        rutaActual?.let { mapView.overlays.remove(it) }

                        val polyline = Polyline().apply {
                            setPoints(points)
                            outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
                            outlinePaint.strokeWidth = 10f
                        }

                        rutaActual = polyline
                        mapView.overlays.add(polyline)

                        // Metadatos reales del trayecto por carreteras
                        val properties = feature.getJSONObject("properties")
                        val summary = properties.getJSONObject("summary")
                        val distanciaKm = summary.optDouble("distance", 0.0) / 1000.0
                        val duracionSeg = summary.optDouble("duration", 0.0)
                        val tiempoMin = (duracionSeg / 60).toInt()

                        tvDistancia.text = String.format("Distancia restante: %.1f km", distanciaKm)
                        tvTiempoEstimado.text = String.format("Tiempo estimado: %d minutos", tiempoMin)

                        ultimaUbicacionRutaCalculada = origen
                        mapView.invalidate()
                    } else {
                        dibujarRutaLineaRectaRespaldo(origen, destino)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando JSON: ${e.message}")
                    dibujarRutaLineaRectaRespaldo(origen, destino)
                }
            },
            { error ->
                calcularRutaEnProgreso = false
                Log.e(TAG, "Error petición ORS: ${error.message}")
                dibujarRutaLineaRectaRespaldo(origen, destino)
            }
        )

        request.retryPolicy = com.android.volley.DefaultRetryPolicy(15000, 2, 1f)
        Volley.newRequestQueue(this).add(request)
    }

    private fun dibujarRutaLineaRectaRespaldo(origen: GeoPoint, destino: GeoPoint) {
        rutaActual?.let { mapView.overlays.remove(it) }

        val points = arrayListOf(origen, destino)
        val polyline = Polyline().apply {
            setPoints(points)
            outlinePaint.color = android.graphics.Color.parseColor("#FF9800") // Naranja = Error/Respaldo
            outlinePaint.strokeWidth = 8f
        }

        rutaActual = polyline
        mapView.overlays.add(polyline)

        val distanciaKm = origen.distanceToAsDouble(destino) / 1000.0
        val tiempoMin = (distanciaKm * 2).toInt()

        tvDistancia.text = String.format("Distancia restante: %.1f km (est.)", distanciaKm)
        tvTiempoEstimado.text = String.format("Tiempo estimado: %d minutos (est.)", tiempoMin)

        mapView.invalidate()
    }

    private fun agregarMarcadorMercado(posicion: GeoPoint) {
        markerMercado = Marker(mapView).apply {
            position = posicion
            title = "Mercado Central"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(markerMercado)
    }

    private fun agregarMarcadorDestino(posicion: GeoPoint, direccion: String) {
        markerDestino = Marker(mapView).apply {
            position = posicion
            title = "Mi Direccion"
            snippet = direccion
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(markerDestino)
    }

    private fun actualizarUI(estado: String, distancia: String, tiempo: String) {
        val (mensaje, color) = when (estado) {
            "pendiente" -> "Buscando repartidor..." to "#FF9800"
            "aceptado" -> "Repartidor asignado. Se dirige al mercado." to "#2196F3"
            "en_camino" -> "Repartidor en camino al mercado." to "#2196F3"
            "recolectado" -> "Productos recolectados. Va hacia ti!" to "#4CAF50"
            "entregado" -> "Pedido entregado. Gracias por comprar!" to "#4CAF50"
            else -> "Procesando pedido..." to "#9E9E9E"
        }

        tvEstado.text = mensaje
        tvEstado.setTextColor(android.graphics.Color.parseColor(color))

        if (distancia.isNotEmpty()) tvDistancia.text = distancia
        if (tiempo.isNotEmpty()) tvTiempoEstimado.text = tiempo
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(seguimientoReceiver)
        } catch (e: Exception) { }
        updateRunnable?.let { handler.removeCallbacks(it) }
        stopService(Intent(this, SeguimientoService::class.java))
    }
}