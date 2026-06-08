package mx.teknoeducativa.agroconectavilla

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mx.teknoeducativa.agroconectavilla.utils.Constants
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class ModoRepartidorFragment : Fragment() {

    private val TAG = "ModoRepartidor"
    private var repartidorId: Int = -1
    private var currentPedidoId: String = ""
    private var estadoViaje = -1

    private val MERCADO_LAT = 19.727433
    private val MERCADO_LON = -99.4627729

    private lateinit var puntoMercado: GeoPoint
    private lateinit var puntoCliente: GeoPoint
    private var listaArticulosText = ""
    private var direccionCliente = ""

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var miPosicionActual = GeoPoint(0.0, 0.0)

    // VARIABLE CLAVE ESTRATEGIA 1: Guarda el punto donde se calculó la ruta por última vez
    private var ultimaUbicacionRutaCalculada: GeoPoint? = null

    private lateinit var mapView: MapView
    private lateinit var cardIndicaciones: CardView
    private lateinit var tvBienvenida: TextView
    private lateinit var tvInstruccion: TextView
    private lateinit var tvDetalle: TextView
    private lateinit var tvDistanciaTiempo: TextView
    private lateinit var btnSiguiente: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnCentrar: FloatingActionButton
    private lateinit var toolbar: Toolbar
    private lateinit var rvOfertas: RecyclerView
    private lateinit var cardOfertas: CardView
    private lateinit var tvNoOfertas: TextView

    private var markerRepartidor: Marker? = null
    private var markerMercado: Marker? = null
    private var markerCliente: Marker? = null
    private var rutaActual: Polyline? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var calcularRutaEnProgreso = false

    private val ofertasPendientes = mutableListOf<OfertaViaje>()
    private lateinit var ofertasAdapter: OfertasAdapter

    data class OfertaViaje(
        val pedidoId: String,
        val destino: String,
        val total: String,
        val latMercado: Double,
        val lonMercado: Double,
        val latCliente: Double,
        val lonCliente: Double
    )

    inner class OfertasAdapter(private val ofertas: MutableList<OfertaViaje>) :
        RecyclerView.Adapter<OfertasAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDestino: TextView = view.findViewById(R.id.tvDestinoOferta)
            val tvTotal: TextView = view.findViewById(R.id.tvTotalOferta)
            val btnAceptar: Button = view.findViewById(R.id.btnAceptarOferta)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_oferta_viaje, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val oferta = ofertas[position]
            holder.tvDestino.text = "Destino: ${oferta.destino}"
            holder.tvTotal.text = "Total: $${oferta.total}"
            holder.btnAceptar.setOnClickListener {
                aceptarViaje(oferta)
            }
        }

        override fun getItemCount() = ofertas.size
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        fun newInstance(repartidorId: Int): ModoRepartidorFragment {
            val fragment = ModoRepartidorFragment()
            val args = Bundle()
            args.putInt("repartidor_id", repartidorId)
            fragment.arguments = args
            return fragment
        }
    }

    private val ofertaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WebSocketService.ACTION_NUEVA_OFERTA && estadoViaje == -1) {
                val pedidoId = intent.getStringExtra(WebSocketService.EXTRA_PEDIDO_ID) ?: ""
                val destino = intent.getStringExtra(WebSocketService.EXTRA_DESTINO) ?: ""
                val total = intent.getStringExtra(WebSocketService.EXTRA_TOTAL) ?: "0"
                val latMercado = intent.getDoubleExtra(WebSocketService.EXTRA_LATITUD_MERCADO, MERCADO_LAT)
                val lonMercado = intent.getDoubleExtra(WebSocketService.EXTRA_LONGITUD_MERCADO, MERCADO_LON)
                val latCliente = intent.getDoubleExtra(WebSocketService.EXTRA_LATITUD_CLIENTE, 0.0)
                val lonCliente = intent.getDoubleExtra(WebSocketService.EXTRA_LONGITUD_CLIENTE, 0.0)

                Log.d(TAG, "Oferta recibida en fragmento: Pedido $pedidoId, Destino: $destino, Total: $$total")

                if (latCliente != 0.0 && lonCliente != 0.0 && isAdded) {
                    val oferta = OfertaViaje(pedidoId, destino, total, latMercado, lonMercado, latCliente, lonCliente)

                    requireActivity().runOnUiThread {
                        val existe = ofertasPendientes.any { it.pedidoId == pedidoId }
                        if (!existe) {
                            ofertasPendientes.add(0, oferta)
                            ofertasAdapter.notifyItemInserted(0)
                            cardOfertas.visibility = View.VISIBLE
                            tvNoOfertas.visibility = View.GONE

                            Toast.makeText(requireContext(), "Nueva oferta: ${oferta.destino} - $${oferta.total}", Toast.LENGTH_LONG).show()
                            mostrarDialogoOferta(oferta)
                        }
                    }
                }
            }
        }
    }

    private fun mostrarDialogoOferta(oferta: OfertaViaje) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Nueva Entrega Disponible")
            .setMessage("Destino: ${oferta.destino}\nTotal: $${oferta.total}\n\nAceptar viaje?")
            .setPositiveButton("Aceptar") { _, _ ->
                aceptarViaje(oferta)
            }
            .setNegativeButton("Rechazar", null)
            .setNeutralButton("Ver mas tarde") { _, _ -> }
            .create()

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) dialog.dismiss()
        }, 30000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repartidorId = arguments?.getInt("repartidor_id") ?: -1
        Log.d(TAG, "Fragment creado con repartidorId: $repartidorId")

        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_modo_repartidor, container, false)

        inicializarVistas(view)
        inicializarMapa()
        verificarPermisosUbicacion()

        return view
    }

    private fun inicializarVistas(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        mapView = view.findViewById(R.id.mapView)
        cardIndicaciones = view.findViewById(R.id.cardIndicaciones)
        cardOfertas = view.findViewById(R.id.cardOfertas)
        tvBienvenida = view.findViewById(R.id.tvBienvenida)
        tvInstruccion = view.findViewById(R.id.tvInstruccionPaso)
        tvDetalle = view.findViewById(R.id.tvDetalleManifiesto)
        tvDistanciaTiempo = view.findViewById(R.id.tvTiempoDistancia)
        btnSiguiente = view.findViewById(R.id.btnSiguienteAccion)
        btnCancelar = view.findViewById(R.id.btnCerrarViaje)
        btnCentrar = view.findViewById(R.id.btnCentrarMapa)
        rvOfertas = view.findViewById(R.id.rvOfertas)
        tvNoOfertas = view.findViewById(R.id.tvNoOfertas)

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnSiguiente.setOnClickListener { siguienteFase() }
        btnCancelar.setOnClickListener { cancelarViaje() }
        btnCentrar.setOnClickListener { centrarEnMiUbicacion() }

        cardIndicaciones.visibility = View.GONE
        cardOfertas.visibility = View.VISIBLE

        rvOfertas.layoutManager = LinearLayoutManager(requireContext())
        ofertasAdapter = OfertasAdapter(ofertasPendientes)
        rvOfertas.adapter = ofertasAdapter

        tvBienvenida.text = "Bienvenido Repartidor\nEsperando nuevas entregas..."
    }

    private fun verificarPermisosUbicacion() {
        val permisos = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val tienePermisos = permisos.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (tienePermisos) {
            iniciarGPS()
            cargarOfertasPendientesServidor()
            registrarReceiver()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permisos,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(requireContext(), "Permisos de ubicacion concedidos", Toast.LENGTH_SHORT).show()
                    iniciarGPS()
                    cargarOfertasPendientesServidor()
                    registrarReceiver()
                } else {
                    Toast.makeText(requireContext(), "Se necesitan permisos de ubicacion para usar el modo repartidor", Toast.LENGTH_LONG).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun cargarOfertasPendientesServidor() {
        val url = "${Constants.BASE_URL}api/pedidos/pendientes/"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val pedidos = response.optJSONArray("pedidos")
                if (pedidos != null && pedidos.length() > 0) {
                    ofertasPendientes.clear()
                    for (i in 0 until pedidos.length()) {
                        val pedido = pedidos.getJSONObject(i)
                        val oferta = OfertaViaje(
                            pedidoId = pedido.getString("id"),
                            destino = pedido.getString("direccion"),
                            total = pedido.getString("total"),
                            latMercado = MERCADO_LAT,
                            lonMercado = MERCADO_LON,
                            latCliente = pedido.optDouble("latitud", 0.0),
                            lonCliente = pedido.optDouble("longitud", 0.0)
                        )
                        if (oferta.latCliente != 0.0 && oferta.lonCliente != 0.0) {
                            ofertasPendientes.add(oferta)
                        }
                    }
                    ofertasAdapter.notifyDataSetChanged()
                    if (ofertasPendientes.isNotEmpty()) {
                        cardOfertas.visibility = View.VISIBLE
                        tvNoOfertas.visibility = View.GONE
                    } else {
                        tvNoOfertas.visibility = View.VISIBLE
                    }
                    Log.d(TAG, "Cargadas ${ofertasPendientes.size} ofertas pendientes del servidor")
                } else {
                    tvNoOfertas.visibility = View.VISIBLE
                }
            },
            { error ->
                Log.e(TAG, "Error cargando ofertas: ${error.message}")
                tvNoOfertas.visibility = View.VISIBLE
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun inicializarMapa() {
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        myLocationOverlay?.enableMyLocation()
        myLocationOverlay?.enableFollowLocation()
        mapView.overlays.add(myLocationOverlay)

        val compassOverlay = CompassOverlay(requireContext(), InternalCompassOrientationProvider(requireContext()), mapView)
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)
    }

    @SuppressLint("MissingPermission")
    private fun iniciarGPS() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    miPosicionActual = GeoPoint(location.latitude, location.longitude)
                    actualizarMarcadorRepartidor()

                    if (estadoViaje != -1) {
                        enviarGPSAlServidor()
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun actualizarMarcadorRepartidor() {
        if (markerRepartidor == null) {
            markerRepartidor = Marker(mapView).apply {
                position = miPosicionActual
                title = "Tu ubicacion"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(markerRepartidor)
        } else {
            markerRepartidor?.position = miPosicionActual
        }
        mapView.invalidate()
    }

    private fun centrarEnMiUbicacion() {
        if (miPosicionActual.latitude != 0.0) {
            mapView.controller.animateTo(miPosicionActual)
        }
    }

    private fun registrarReceiver() {
        val filter = IntentFilter(WebSocketService.ACTION_NUEVA_OFERTA)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(ofertaReceiver, filter)
        Log.d(TAG, "Receiver registrado para ofertas")
    }

    private fun aceptarViaje(oferta: OfertaViaje) {
        val url = "${Constants.BASE_URL}api/pedidos/aceptar/"
        val body = JSONObject().apply {
            put("pedido_id", oferta.pedidoId.toInt())
            put("repartidor_id", repartidorId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                if (response.optString("status") == "ok") {
                    Toast.makeText(requireContext(), "Viaje aceptado", Toast.LENGTH_SHORT).show()
                    ofertasPendientes.remove(oferta)
                    ofertasAdapter.notifyDataSetChanged()
                    if (ofertasPendientes.isEmpty()) {
                        tvNoOfertas.visibility = View.VISIBLE
                    }

                    currentPedidoId = oferta.pedidoId
                    puntoMercado = GeoPoint(oferta.latMercado, oferta.lonMercado)
                    puntoCliente = GeoPoint(oferta.latCliente, oferta.lonCliente)
                    direccionCliente = oferta.destino

                    cargarProductosPedido(currentPedidoId)
                    iniciarFaseMercado()
                } else {
                    Toast.makeText(requireContext(), response.optString("message", "Error"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun cargarProductosPedido(pedidoId: String) {
        val url = "${Constants.BASE_URL}api/pedido_items/$pedidoId/"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val items = response.optJSONArray("items")
                if (items != null) {
                    val sb = StringBuilder()
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        sb.append("${item.optString("nombre")} x${item.optInt("cantidad")}\n")
                    }
                    listaArticulosText = sb.toString()
                    actualizarDetalle()
                }
            },
            { error ->
                listaArticulosText = "Productos pendientes"
                actualizarDetalle()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun iniciarFaseMercado() {
        estadoViaje = 0
        cardIndicaciones.visibility = View.VISIBLE
        cardOfertas.visibility = View.GONE
        tvBienvenida.visibility = View.GONE
        tvInstruccion.text = "PASO 1: Recoger en el Mercado Agromexiquense"
        btnSiguiente.text = "YA RECOGI"

        agregarMarcadorMercado()

        // Forzar primer cálculo de ruta inmediato
        ultimaUbicacionRutaCalculada = null
        recalcularRuta()

        actualizarDetalle()
        iniciarActualizacionRuta()
        centrarEnMiUbicacion()
    }

    private fun iniciarFaseCliente() {
        estadoViaje = 1
        tvInstruccion.text = "PASO 2: Entregar al Cliente"
        btnSiguiente.text = "CONFIRMAR ENTREGA"

        agregarMarcadorCliente()

        // Forzar recalculo inmediato para el cliente
        ultimaUbicacionRutaCalculada = null
        recalcularRuta()

        actualizarDetalle()
    }

    private fun actualizarDetalle() {
        tvDetalle.text = "PRODUCTOS A RECOGER:\n$listaArticulosText\n\n" +
                if (estadoViaje == 0) "UBICACION DEL MERCADO:\nMercado Agromexiquense de Villa del Carbon"
                else "UBICACION DEL CLIENTE:\n$direccionCliente\n\nENTREGAR TODOS LOS PRODUCTOS"
    }

    private fun agregarMarcadorMercado() {
        markerMercado?.let { mapView.overlays.remove(it) }
        markerMercado = Marker(mapView).apply {
            position = puntoMercado
            title = "Mercado Central"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(markerMercado)
    }

    private fun agregarMarcadorCliente() {
        markerCliente?.let { mapView.overlays.remove(it) }
        markerCliente = Marker(mapView).apply {
            position = puntoCliente
            title = "Cliente: $direccionCliente"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(markerCliente)
    }

    // ==========================================
    // CAMBIO CLAVE: CÁLCULO POR CALLES REALES CON OPENROUTESERVICE
    // ==========================================
    private fun calcularRutaConOpenRouteService(origen: GeoPoint, destino: GeoPoint) {
        if (origen.latitude == 0.0 || origen.longitude == 0.0 || destino.latitude == 0.0 || destino.longitude == 0.0) {
            return
        }

        calcularRutaEnProgreso = true

        // REMPLAZA "TU_API_KEY_AQUI" con el token de OpenRouteService
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
                            outlinePaint.color = Color.parseColor("#2196F3")
                            outlinePaint.strokeWidth = 14f
                        }

                        rutaActual = polyline
                        mapView.overlays.add(polyline)

                        // Extraer distancia y duración real de la ruta
                        val properties = feature.getJSONObject("properties")
                        val summary = properties.getJSONObject("summary")
                        val distanciaKm = summary.optDouble("distance", 0.0) / 1000.0 // metros a KM
                        val duracionSeg = summary.optDouble("duration", 0.0)
                        val tiempoMin = (duracionSeg / 60).toInt()

                        tvDistanciaTiempo.text = String.format("%.1f km • %d min", distanciaKm, tiempoMin)

                        // Guardar exitosamente la última posición procesada
                        ultimaUbicacionRutaCalculada = origen

                        mapView.invalidate()
                    } else {
                        dibujarRutaLineaRecta(origen, destino)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando ORS: ${e.message}")
                    dibujarRutaLineaRecta(origen, destino)
                }
            },
            { error ->
                calcularRutaEnProgreso = false
                Log.e(TAG, "Error ORS: ${error.message}")
                dibujarRutaLineaRecta(origen, destino)
            }
        )

        request.retryPolicy = com.android.volley.DefaultRetryPolicy(15000, 2, 1f)
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun dibujarRutaLineaRecta(origen: GeoPoint, destino: GeoPoint) {
        rutaActual?.let { mapView.overlays.remove(it) }

        val points = arrayListOf(origen, destino)
        val polyline = Polyline().apply {
            setPoints(points)
            outlinePaint.color = Color.parseColor("#FF9800")
            outlinePaint.strokeWidth = 14f
        }

        rutaActual = polyline
        mapView.overlays.add(polyline)

        val distanciaKm = origen.distanceToAsDouble(destino) / 1000.0
        val tiempoMin = (distanciaKm * 3).toInt()
        tvDistanciaTiempo.text = String.format("%.1f km • %d min (estimado)", distanciaKm, tiempoMin)

        mapView.invalidate()
    }

    private fun recalcularRuta() {
        if (estadoViaje == 0) {
            calcularConORSProteger(miPosicionActual, puntoMercado)
        } else if (estadoViaje == 1) {
            calcularConORSProteger(miPosicionActual, puntoCliente)
        }
    }

    // Filtro intermedio para interceptar y no gastar peticiones si no se ha movido
    private fun calcularConORSProteger(origen: GeoPoint, destino: GeoPoint) {
        if (origen.latitude == 0.0 || origen.longitude == 0.0) return

        // Si es el primer cálculo o el repartidor ya avanzó más de 40 metros desde la última petición
        if (ultimaUbicacionRutaCalculada == null ||
            origen.distanceToAsDouble(ultimaUbicacionRutaCalculada) > 40.0) {
            calcularRutaConOpenRouteService(origen, destino)
        }
    }

    // El Bucle corre cada 10 segundos para verificar movimiento, pero el filtro superior evita consumir la API
    private fun iniciarActualizacionRuta() {
        updateRunnable = object : Runnable {
            override fun run() {
                if (estadoViaje != -1 && isAdded && !calcularRutaEnProgreso) {
                    recalcularRuta()
                    handler.postDelayed(this, 10000)
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun enviarGPSAlServidor() {
        if (currentPedidoId.isNotEmpty() && miPosicionActual.latitude != 0.0) {
            val url = "${Constants.BASE_URL}api/pedidos/transmitir_gps/"
            val body = JSONObject().apply {
                put("usuario_id", repartidorId)
                put("latitud", miPosicionActual.latitude)
                put("longitud", miPosicionActual.longitude)
                put("pedido_id", currentPedidoId.toInt())
            }

            val request = JsonObjectRequest(Request.Method.POST, url, body,
                { Log.d(TAG, "GPS enviado") },
                { error -> Log.e(TAG, "Error GPS: ${error.message}") }
            )
            Volley.newRequestQueue(requireContext()).add(request)
        }
    }

    private fun siguienteFase() {
        if (estadoViaje == 0) {
            cambiarEstadoPedido("recolectado") {
                iniciarFaseCliente()
            }
        } else if (estadoViaje == 1) {
            cambiarEstadoPedido("entregado") {
                finalizarViaje()
            }
        }
    }

    private fun cambiarEstadoPedido(estado: String, onSuccess: () -> Unit) {
        val url = "${Constants.BASE_URL}api/pedidos/actualizar_estado/"
        val body = JSONObject().apply {
            put("pedido_id", currentPedidoId.toInt())
            put("estado", estado)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                if (response.optString("status") == "ok") {
                    Toast.makeText(requireContext(), if (estado == "recolectado") "Productos recolectados" else "Pedido entregado", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(requireContext(), "Error al actualizar estado", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun cancelarViaje() {
        cambiarEstadoPedido("pendiente") {
            finalizarViaje()
            Toast.makeText(requireContext(), "Viaje cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finalizarViaje() {
        estadoViaje = -1
        currentPedidoId = ""
        cardIndicaciones.visibility = View.GONE
        cardOfertas.visibility = View.VISIBLE
        tvBienvenida.visibility = View.VISIBLE

        markerMercado?.let { mapView.overlays.remove(it) }
        markerCliente?.let { mapView.overlays.remove(it) }
        rutaActual?.let { mapView.overlays.remove(it) }

        markerMercado = null
        markerCliente = null
        rutaActual = null
        ultimaUbicacionRutaCalculada = null

        cargarOfertasPendientesServidor()
        Toast.makeText(requireContext(), "Viaje completado", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        if (estadoViaje == -1 && isAdded) {
            try {
                val filter = IntentFilter(WebSocketService.ACTION_NUEVA_OFERTA)
                LocalBroadcastManager.getInstance(requireContext()).registerReceiver(ofertaReceiver, filter)
                Log.d(TAG, "Receiver registrado en onResume")
                cargarOfertasPendientesServidor()
            } catch (e: Exception) {
                Log.e(TAG, "Error registrando receiver: ${e.message}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(ofertaReceiver)
            Log.d(TAG, "Receiver desregistrado en onPause")
        } catch (e: Exception) {
            // Ignorar
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        updateRunnable?.let { handler.removeCallbacks(it) }
    }
}