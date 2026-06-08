package mx.teknoeducativa.agroconectavilla // Cambia por tu paquete real

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.net.URLEncoder

class SoporteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_soporte, container, false)

        val btnWhatsapp = view.findViewById<Button>(R.id.btn_whatsapp)
        val btnCorreo = view.findViewById<Button>(R.id.btn_correo)

        // Acción para abrir WhatsApp
        btnWhatsapp.setOnClickListener {
            abrirWhatsApp()
        }

        // Acción para abrir el Correo
        btnCorreo.setOnClickListener {
            abrirCorreo()
        }

        return view
    }

    private fun abrirWhatsApp() {
        val numeroTelefono = getString(R.string.contacto_whatsapp)
        val mensaje = getString(R.string.whatsapp_mensaje_predeterminado)

        try {
            // Codificamos el texto para que sea seguro en una URL
            val mensajeCodificado = URLEncoder.encode(mensaje, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$numeroTelefono&text=$mensajeCodificado"

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp no está instalado en este dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirCorreo() {
        val correoDestino = getString(R.string.contacto_correo)
        val asunto = getString(R.string.correo_asunto_predeterminado)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Solo apps de correo electrónico responderán a esto
            putExtra(Intent.EXTRA_EMAIL, arrayOf(correoDestino))
            putExtra(Intent.EXTRA_SUBJECT, asunto)
        }

        // Verifica si hay alguna aplicación que pueda manejar este intent de correo
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            // En dispositivos modernos resolveActivity puede fallar por políticas de visibilidad,
            // así que lanzamos el startActivity directamente envuelto en un try-catch por seguridad.
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No tienes una aplicación de correo configurada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}