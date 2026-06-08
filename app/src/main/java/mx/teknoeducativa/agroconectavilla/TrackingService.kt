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

class TrackingService : Service() {

    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var pedidoId: String = ""

    companion object {
        const val ACTION_ACTUALIZACION_GPS = "mx.teknoeducativa.agroconectavilla.GPS_ACTUALIZADO"
        const val EXTRA_LATITUD = "latitud"
        const val EXTRA_LONGITUD = "longitud"
        const val EXTRA_ESTADO = "estado"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            pedidoId = it.getStringExtra("pedido_id") ?: ""
            if (pedidoId.isNotEmpty()) {
                iniciarTracking()
            }
        }
        return START_STICKY
    }

    private fun iniciarTracking() {
        runnable = object : Runnable {
            override fun run() {
                obtenerUbicacionRepartidor()
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable!!)
    }

    private fun obtenerUbicacionRepartidor() {
        val url = "${Constants.BASE_URL}api/pedidos/obtener_gps/$pedidoId/"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.optString("status") == "ok") {
                    val latitud = response.optDouble("latitud")
                    val longitud = response.optDouble("longitud")
                    val estado = response.optString("estado_pedido")

                    val intent = Intent(ACTION_ACTUALIZACION_GPS).apply {
                        putExtra(EXTRA_LATITUD, latitud)
                        putExtra(EXTRA_LONGITUD, longitud)
                        putExtra(EXTRA_ESTADO, estado)
                    }
                    LocalBroadcastManager.getInstance(this@TrackingService).sendBroadcast(intent)
                }
            },
            { error ->
                Log.e("TrackingService", "Error: ${error.message}")
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }
}