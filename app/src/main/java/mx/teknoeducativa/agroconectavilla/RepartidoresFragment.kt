package mx.teknoeducativa.agroconectavilla

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mx.teknoeducativa.agroconectavilla.utils.Constants
import org.json.JSONObject
import android.content.Intent

class RepartidoresFragment : Fragment() {

    private var usuarioId: Int = 1

    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etAutomovil: EditText
    private lateinit var btnEnviar: Button

    private val baseUrl: String = Constants.BASE_URL

    // Sesión local conectada
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_repartidores,
            container,
            false
        )

        // Inicializar SessionManager de forma segura
        sessionManager = SessionManager(requireContext())

        // RECIBIR ID DEL USUARIO
        usuarioId = arguments?.getInt("usuarioId") ?: 1

        // VISTAS
        etNombre = view.findViewById(R.id.etNombre)
        etCorreo = view.findViewById(R.id.etCorreo)
        etTelefono = view.findViewById(R.id.etTelefono)
        etAutomovil = view.findViewById(R.id.etAutomovil)
        btnEnviar = view.findViewById(R.id.btnEnviar)

        // BLOQUEAR CAMPOS ÚNICAMENTE DE LECTURA FIJA SIEMPRE
        bloquearCampo(etNombre)
        bloquearCampo(etCorreo)

        // CARGAR DATOS INICIALES DESDE EL SERVIDOR
        cargarDatosUsuario()

        return view
    }

    private fun bloquearCampo(editText: EditText) {
        editText.isFocusable = false
        editText.isClickable = false
        editText.isCursorVisible = false
    }

    // Configura la UI de manera dinámica dependiendo de si ya está registrado o no
    private fun configurarInterfazUsuarioRegistrado(esRepartidor: Boolean) {
        if (esRepartidor) {
            btnEnviar.text = "Entrar a Modo Repartidor"
            btnEnviar.isEnabled = true

            bloquearCampo(etTelefono)
            bloquearCampo(etAutomovil)

            // Si ya es repartidor registrado, entra al menú principal configurado como repartidor
            btnEnviar.setOnClickListener {
                redirigirAMenuRepartidor()
            }
        } else {
            btnEnviar.text = "Enviar Solicitud"
            btnEnviar.isEnabled = true

            etTelefono.isFocusableInTouchMode = true
            etAutomovil.isFocusableInTouchMode = true

            // Comportamiento normal: Enviar formulario por POST
            btnEnviar.setOnClickListener {
                val telefono = etTelefono.text.toString().trim()
                val automovil = etAutomovil.text.toString().trim()

                if (telefono.isEmpty()) {
                    etTelefono.error = "Ingresa tu número de teléfono"
                    return@setOnClickListener
                }
                if (automovil.isEmpty()) {
                    etAutomovil.error = "Ingresa tu automóvil"
                    return@setOnClickListener
                }

                actualizarDatosRepartidor(automovil, telefono)
            }
        }
    }

    // =============================================================
    // REDIRECCIÓN ARQUITECTÓNICA AL FRAGMENT DE MODO REPARTIDOR
    // =============================================================
    private fun redirigirAMenuRepartidor() {
        // Apuntamos al contenedor real (MainMenuActivity) inyectando flags de apertura
        val intent = Intent(requireContext(), MainMenuActivity::class.java).apply {
            putExtra("usuario_id", usuarioId)
            putExtra("abrir_modo_repartidor", true)

            // Limpia por completo la pila para que la sesión de repartidor inicie de cero
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish() // Cierra la pantalla vieja de cliente
    }

    // TRAER DATOS DEL USUARIO (GET)
    private fun cargarDatosUsuario() {
        val url = "${baseUrl}api/repartidor/$usuarioId/"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (!isAdded) return@JsonObjectRequest

                try {
                    if (response.optString("status") == "error") {
                        configurarInterfazUsuarioRegistrado(false)
                        return@JsonObjectRequest
                    }

                    etNombre.setText(response.optString("nombre", ""))
                    etCorreo.setText(response.optString("correo", ""))

                    val tel = response.optString("telefono", "")
                    if (tel.isNotEmpty() && tel != "null") {
                        etTelefono.setText(tel)
                    }

                    val auto = response.optString("automovil", "")
                    if (auto.isNotEmpty() && auto != "null") {
                        etAutomovil.setText(auto)
                    }

                    // LEER SI YA ES REPARTIDOR DESDE EL BACKEND
                    val esRepartidor = response.optBoolean("es_repartidor", false)

                    if (esRepartidor) {
                        sessionManager.actualizarRolARepartidor()
                    }

                    configurarInterfazUsuarioRegistrado(esRepartidor)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                error.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error al cargar datos del perfil", Toast.LENGTH_SHORT).show()
                }
            }
        )

        context?.let {
            Volley.newRequestQueue(it).add(request)
        }
    }

    // ENVIAR ACTUALIZACIÓN Y ACTIVAR REPARTIDOR (POST)
    private fun actualizarDatosRepartidor(automovil: String, telefono: String) {
        val url = "${baseUrl}api/repartidor/actualizar/"

        val jsonBody = JSONObject().apply {
            put("usuario_id", usuarioId)
            put("automovil", automovil)
            put("telefono", telefono)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                if (!isAdded) return@JsonObjectRequest

                val status = response.optString("status", "error")
                if (status == "ok") {
                    Toast.makeText(requireContext(), "¡Solicitud procesada! Ahora eres repartidor.", Toast.LENGTH_SHORT).show()

                    // 1. Modificar Preferences de manera persistente
                    sessionManager.actualizarRolARepartidor()

                    // 2. Transición automática inmediata al Modo Repartidor
                    redirigirAMenuRepartidor()

                } else {
                    val msg = response.optString("message", "Error desconocido")
                    Toast.makeText(requireContext(), "Error: $msg", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error en la solicitud de actualización", Toast.LENGTH_SHORT).show()
                }
            }
        )

        context?.let {
            Volley.newRequestQueue(it).add(request)
        }
    }

    companion object {
        fun newInstance(usuarioId: Int): RepartidoresFragment {
            val fragment = RepartidoresFragment()
            val bundle = Bundle()
            bundle.putInt("usuarioId", usuarioId)
            fragment.arguments = bundle
            return fragment
        }
    }
}