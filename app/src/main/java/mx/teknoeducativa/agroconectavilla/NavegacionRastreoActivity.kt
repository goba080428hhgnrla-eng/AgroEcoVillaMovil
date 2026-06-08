package mx.teknoeducativa.agroconectavilla

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mx.teknoeducativa.agroconectavilla.utils.Constants
import com.google.android.gms.location.*
import org.json.JSONObject

class NavegacionRastreoActivity : AppCompatActivity() {

    private lateinit var fusedGPSClient: FusedLocationProviderClient
    private lateinit var gpsCallback: LocationCallback
    private var pedidoId: String = ""
    private var usuarioId: Int = -1 // Dinámico
    private val handler = Handler(Looper.getMainLooper())
    private var runnableGPS: Runnable? = null

    private var miLatitud: Double = 0.0
    private var miLongitud: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navegacion_rastreo)

        pedidoId = intent.getStringExtra("pedido_id") ?: ""
        usuarioId = intent.getIntExtra("usuario_id", -1)

        val btnRutaMercado = findViewById<Button>(R.id.btnRutaMercado)
        val btnMarcarRecolectado = findViewById<Button>(R.id.btnMarcarRecolectado)
        val btnRutaCliente = findViewById<Button>(R.id.btnRutaCliente)
        val btnMarcarEntregado = findViewById<Button>(R.id.btnMarcarEntregado)

        // FASE 1: Ir al mercado de origen simulado de Django (17.9868, -92.9303)
        btnRutaMercado.setOnClickListener {
            abrirNavegacionNativaGoogleMaps(17.9868, -92.9303)
        }

        btnMarcarRecolectado.setOnClickListener {
            cambiarFaseServidor("recolectado") {
                btnRutaCliente.isEnabled = true
                btnMarcarEntregado.isEnabled = true
                btnRutaMercado.isEnabled = false
                btnMarcarRecolectado.isEnabled = false
            }
        }

        // FASE 2: Ir con el cliente
        btnRutaCliente.setOnClickListener {
            abrirNavegacionNativaGoogleMaps(17.9942, -92.9415)
        }

        btnMarcarEntregado.setOnClickListener {
            cambiarFaseServidor("entregado") {
                Toast.makeText(this, "¡Felicidades, entrega concluida!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        iniciarCapturaGPS()
        iniciarTransmisionBucleBackend()
    }

    private fun abrirNavegacionNativaGoogleMaps(lat: Double, lon: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Instala Google Maps para ver la ruta guiada por voz", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cambiarFaseServidor(estado: String, alTerminar: () -> Unit) {
        // Apunta al path unificado en tu urls.py para actualizar_estado_logistica_api
        val url = "${Constants.BASE_URL}api/pedidos/actualizar_estado/"

        val body = JSONObject().apply {
            put("pedido_id", pedidoId)
            put("estado", estado)
        }
        val req = JsonObjectRequest(Request.Method.POST, url, body, { alTerminar() }, {})
        Volley.newRequestQueue(this).add(req)
    }

    @SuppressLint("MissingPermission")
    private fun iniciarCapturaGPS() {
        fusedGPSClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()

        gpsCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                val loc = p0.lastLocation ?: return
                miLatitud = loc.latitude
                miLongitud = loc.longitude
            }
        }
        fusedGPSClient.requestLocationUpdates(request, gpsCallback, Looper.getMainLooper())
    }

    private fun iniciarTransmisionBucleBackend() {
        runnableGPS = object : Runnable {
            override fun run() {
                if (miLatitud != 0.0 && usuarioId != -1) {
                    val url = "${Constants.BASE_URL}api/pedidos/transmitir_gps/"
                    val body = JSONObject().apply {
                        put("usuario_id", usuarioId) // Enviamos el ID dinámico de la sesión activa
                        put("latitud", miLatitud)
                        put("longitud", miLongitud)
                    }
                    val req = JsonObjectRequest(Request.Method.POST, url, body, {}, {})
                    Volley.newRequestQueue(applicationContext).add(req)
                }
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnableGPS!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedGPSClient.removeLocationUpdates(gpsCallback)
        runnableGPS?.let { handler.removeCallbacks(it) }
    }
}