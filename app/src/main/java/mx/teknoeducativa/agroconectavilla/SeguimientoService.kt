package mx.teknoeducativa.agroconectavilla

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mx.teknoeducativa.agroconectavilla.utils.Constants
import org.json.JSONObject

class SeguimientoService : Service() {

    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var pedidoId: String = ""
    private var clienteId: Int = -1

    companion object {
        const val ACTION_ACTUALIZACION = SeguimientoClienteActivity.ACTION_ACTUALIZACION
        const val EXTRA_LATITUD = "latitud"
        const val EXTRA_LONGITUD = "longitud"
        const val EXTRA_ESTADO = "estado"
        const val EXTRA_DISTANCIA = "distancia"
        const val EXTRA_TIEMPO = "tiempo"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            pedidoId = it.getStringExtra("pedido_id") ?: ""
            clienteId = it.getIntExtra("cliente_id", -1)

            if (pedidoId.isNotEmpty()) {
                iniciarSeguimiento()
            }
        }
        return START_STICKY
    }

    private fun iniciarSeguimiento() {
        runnable = object : Runnable {
            override fun run() {
                obtenerActualizacion()
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable!!)
    }

    private fun obtenerActualizacion() {
        val url = "${Constants.BASE_URL}api/pedidos/seguimiento/$pedidoId/"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.optString("status") == "ok") {
                    val lat = response.optDouble("repartidor_lat", 0.0)
                    val lon = response.optDouble("repartidor_lon", 0.0)
                    val estado = response.optString("estado_pedido", "pendiente")
                    val distancia = response.optString("distancia_restante", "")
                    val tiempo = response.optString("tiempo_restante", "")

                    val intent = Intent(ACTION_ACTUALIZACION).apply {
                        putExtra(EXTRA_LATITUD, lat)
                        putExtra(EXTRA_LONGITUD, lon)
                        putExtra(EXTRA_ESTADO, estado)
                        putExtra(EXTRA_DISTANCIA, distancia)
                        putExtra(EXTRA_TIEMPO, tiempo)
                    }
                    LocalBroadcastManager.getInstance(this@SeguimientoService).sendBroadcast(intent)
                }
            },
            { error ->
                Log.e("SeguimientoService", "Error: ${error.message}")
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }
}