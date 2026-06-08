package mx.teknoeducativa.agroconectavilla.services

import android.content.Intent
import mx.teknoeducativa.agroconectavilla.OfertaViajeActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AgroMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Verificamos si la push contiene una estructura Data Message pura (Estilo Uber)
        if (remoteMessage.data.isNotEmpty()) {
            val pedidoId = remoteMessage.data["pedido_id"]
            val total = remoteMessage.data["total"]
            val direccion = remoteMessage.data["direccion"]

            // Levantamos de inmediato la interfaz interactiva con prioridad de sistema
            val intent = Intent(applicationContext, OfertaViajeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("pedido_id", pedidoId)
                putExtra("total", total)
                putExtra("destino", direccion)
            }
            startActivity(intent)
        }
    }
}