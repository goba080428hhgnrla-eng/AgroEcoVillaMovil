package mx.teknoeducativa.agroconectavilla

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketService : Service() {

    private val TAG = "WebSocketService"
    private val WS_URL = "wss://agroconectavilla.onrender.com/ws/viajes/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var repartidorId: Int = -1
    private var reconnectAttempts = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isConnecting = false

    companion object {
        const val ACTION_NUEVA_OFERTA = "mx.teknoeducativa.agroconectavilla.NUEVA_OFERTA"
        const val EXTRA_PEDIDO_ID = "pedido_id"
        const val EXTRA_DESTINO = "destino"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_LATITUD_MERCADO = "latitud_mercado"
        const val EXTRA_LONGITUD_MERCADO = "longitud_mercado"
        const val EXTRA_LATITUD_CLIENTE = "latitud_cliente"
        const val EXTRA_LONGITUD_CLIENTE = "longitud_cliente"
        const val EXTRA_REFERENCIA = "referencia"

        private const val NOTIFICATION_CHANNEL_ID = "repartidor_ofertas"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            repartidorId = it.getIntExtra("repartidor_id", -1)
            Log.d(TAG, "Service iniciado con repartidorId: $repartidorId")
            if (repartidorId != -1 && !isConnecting) {
                crearCanalNotificacion()
                conectarWebSocket()
            }
        }
        return START_STICKY
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Ofertas de Viaje",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevas entregas disponibles"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun mostrarNotificacionPush(titulo: String, mensaje: String, pedidoId: String) {
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("abrir_repartidor", true)
            putExtra("abrir_oferta", true)
            putExtra("pedido_id", pedidoId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun conectarWebSocket() {
        if (isConnecting) {
            Log.d(TAG, "Ya hay una conexion en proceso...")
            return
        }

        isConnecting = true
        Log.d(TAG, "Conectando WebSocket a: $WS_URL con repartidorId: $repartidorId")

        val urlConId = "$WS_URL?repartidor_id=$repartidorId"

        val request = Request.Builder()
            .url(urlConId)
            .addHeader("User-Agent", "AgroConecta-Android")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket CONECTADO exitosamente con repartidorId: $repartidorId")
                reconnectAttempts = 0
                isConnecting = false

                val identMsg = JSONObject().apply {
                    put("type", "identificar")
                    put("repartidor_id", repartidorId)
                    put("device", "android")
                }
                webSocket.send(identMsg.toString())
                Log.d(TAG, "Identificacion enviada: $identMsg")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Mensaje WebSocket recibido: $text")

                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")

                    when (type) {
                        "enviar_notificacion_viaje" -> {
                            val pedidoId = json.optString("pedido_id", "0")
                            val destino = json.optString("destino", "Destino no especificado")
                            val total = json.optString("total", "0.00")
                            val latMercado = json.optDouble("latitud_mercado", 19.727433)
                            val lonMercado = json.optDouble("longitud_mercado", -99.4627729)
                            val latCliente = json.optDouble("latitud_cliente", 0.0)
                            val lonCliente = json.optDouble("longitud_cliente", 0.0)
                            val referencia = json.optString("referencia", "")

                            Log.d(TAG, "NUEVA OFERTA RECIBIDA! Pedido: $pedidoId, Total: $$total, Destino: $destino")

                            // Enviar broadcast para la app
                            val broadcastIntent = Intent(ACTION_NUEVA_OFERTA).apply {
                                putExtra(EXTRA_PEDIDO_ID, pedidoId)
                                putExtra(EXTRA_DESTINO, destino)
                                putExtra(EXTRA_TOTAL, total)
                                putExtra(EXTRA_LATITUD_MERCADO, latMercado)
                                putExtra(EXTRA_LONGITUD_MERCADO, lonMercado)
                                putExtra(EXTRA_LATITUD_CLIENTE, latCliente)
                                putExtra(EXTRA_LONGITUD_CLIENTE, lonCliente)
                                putExtra(EXTRA_REFERENCIA, referencia)
                            }
                            LocalBroadcastManager.getInstance(this@WebSocketService).sendBroadcast(broadcastIntent)
                            Log.d(TAG, "Broadcast enviado para pedido $pedidoId")

                            // Mostrar notificación push
                            mostrarNotificacionPush("Nueva Entrega", "Pedido #$pedidoId - $$total a $destino", pedidoId)
                        }
                        "pong" -> {
                            Log.d(TAG, "Pong recibido del servidor")
                        }
                        else -> {
                            Log.d(TAG, "Tipo de mensaje: $type")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando mensaje: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket cerrando: $code - $reason")
                isConnecting = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket cerrado: $code - $reason")
                isConnecting = false
                programarReconexion()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket fallo: ${t.message}")
                isConnecting = false
                programarReconexion()
            }
        })
    }

    private fun programarReconexion() {
        val delay = 5000L * (reconnectAttempts + 1)
        reconnectAttempts++
        if (reconnectAttempts > 10) reconnectAttempts = 5
        Log.d(TAG, "Programando reconexion en ${delay}ms (intento $reconnectAttempts)")

        handler.postDelayed({
            if (repartidorId != -1) {
                conectarWebSocket()
            }
        }, delay)
    }

    fun actualizarRepartidorId(nuevoId: Int) {
        repartidorId = nuevoId
        Log.d(TAG, "Repartidor ID actualizado a: $repartidorId")
        if (repartidorId != -1) {
            webSocket?.close(1000, "Reconectando con nuevo ID")
            conectarWebSocket()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Service destroying")
        handler.removeCallbacksAndMessages(null)
    }
}