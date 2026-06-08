package mx.teknoeducativa.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mx.teknoeducativa.agroconectavilla.utils.Constants
import org.json.JSONArray
import org.json.JSONObject

class OfertaViajeActivity : AppCompatActivity() {

    private val TAG = "OfertaViajeActivity"
    private lateinit var timer: CountDownTimer
    private lateinit var sessionManager: SessionManager
    private var usuarioId: Int = -1
    private var pedidoId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oferta_viaje)

        Log.d(TAG, "=== OFERTA VIAJE ACTIVITY INICIALIZADA ===")

        // Inicializar SessionManager real leyendo obligatoriamente "agroconecta_prefs"
        sessionManager = SessionManager(this)
        usuarioId = sessionManager.getUsuarioId()

        // Rescatar los datos enviados por la alerta de WebSocket
        pedidoId = intent.getStringExtra("pedido_id") ?: ""
        val destino = intent.getStringExtra("destino") ?: "Destino No Especificado"
        val total = intent.getStringExtra("total") ?: "0.00"

        Log.d(TAG, "📌 REPARTIDOR DE DETECCIÓN -> ID LOCAL EN PREFS: $usuarioId | Nombre: ${sessionManager.getUsuarioNombre()}")

        // Si el ID se pierde, evitamos enviar basura al servidor
        if (usuarioId == -1 || pedidoId.isEmpty()) {
            Log.e(TAG, "❌ Error crítico: Datos locales corruptos (id_usuario=-1 o pedido vacio).")
            Toast.makeText(this, "Sesión no válida. Por favor, re-inicia sesión.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val tvDestino = findViewById<TextView>(R.id.tvDestinoOferta)
        val tvTotal = findViewById<TextView>(R.id.tvTotalOferta)
        val tvTimer = findViewById<TextView>(R.id.tvTimerRegresivo)
        val btnAceptar = findViewById<Button>(R.id.btnAceptarOferta)

        tvDestino.text = destino
        tvTotal.text = "$$total"

        timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "${millisUntilFinished / 1000}s"
            }
            override fun onFinish() {
                Log.w(TAG, "Oferta expirada.")
                finish()
            }
        }.start()

        btnAceptar.setOnClickListener {
            timer.cancel()
            aceptarViajeServidor()
        }
    }

    private fun aceptarViajeServidor() {
        val url = "${Constants.BASE_URL.removeSuffix("/")}/api/pedidos/aceptar/"

        // Construir JSON mapeando explícitamente a las propiedades de tu base de datos
        val body = JSONObject().apply {
            put("pedido_id", pedidoId.toInt())
            put("repartidor_id", usuarioId) // Envía el ID limpio del repartidor actual
        }

        Log.d(TAG, "✈️ ENVIANDO PAYLOAD AL SERVIDOR: $body")
        Log.d(TAG, "🔍 Usuario ID desde SessionManager: $usuarioId")
        Log.d(TAG, "🔍 Pedido ID: $pedidoId")

        val request = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                Log.d(TAG, "📥 RESPUESTA RECIBIDA DESDE RENDER: $response")

                if (response.optString("status") == "ok") {
                    Toast.makeText(this, "¡Viaje asignado con éxito!", Toast.LENGTH_SHORT).show()
                    try {
                        val pedidoJson = response.getJSONObject("pedido")
                        val origen = pedidoJson.getJSONObject("origen_mercado")
                        val destino = pedidoJson.getJSONObject("destino_cliente")

                        val intentNavegacion = Intent(this, MenuRepartidorActivity::class.java).apply {
                            putExtra("iniciar_navegacion", true)
                            putExtra("pedido_id", pedidoId)
                            putExtra("origen_lat", origen.optDouble("latitud", 0.0))
                            putExtra("origen_lon", origen.optDouble("longitud", 0.0))
                            putExtra("destino_lat", destino.optDouble("latitud", 0.0))
                            putExtra("destino_lon", destino.optDouble("longitud", 0.0))
                            putExtra("nombre_mercado", origen.optString("nombre_vendedor", "Mercado"))
                            putExtra("direccion_cliente", destino.optString("direccion", "Dirección de Entrega"))
                            putExtra("productos", obtenerTextoProductos(pedidoJson.optJSONArray("productos")))
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intentNavegacion)
                        finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error procesando JSON de éxito: ${e.message}")
                        finish()
                    }
                } else {
                    // Si el servidor manda un fallo, se muestra el String exacto de la respuesta
                    val mensajeBackend = response.optString("message", "Error desconocido.")
                    Log.e(TAG, "🔴 RECHAZO DEL BACKEND: $mensajeBackend")
                    Toast.makeText(this, "Rechazo: $mensajeBackend", Toast.LENGTH_LONG).show()
                    finish()
                }
            },
            { error ->
                Log.e(TAG, "❌ FALLA DE RED VOLLEY: ${error.message}")
                Toast.makeText(this, "Error de red con el servidor.", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        request.retryPolicy = DefaultRetryPolicy(70000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Volley.newRequestQueue(this).add(request)
    }

    private fun obtenerTextoProductos(productos: JSONArray?): String {
        if (productos == null || productos.length() == 0) return "Productos"
        val builder = StringBuilder()
        for (i in 0 until productos.length()) {
            try {
                val item = productos.getJSONObject(i)
                builder.append("${item.optString("nombre")} x${item.optInt("cantidad")}, ")
            } catch (e: Exception) {}
        }
        return builder.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::timer.isInitialized) timer.cancel()
    }
}