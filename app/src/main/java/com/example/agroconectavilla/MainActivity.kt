package com.example.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.utils.Constants
import org.json.JSONObject

/**
 * Actividad principal de entrada a la aplicación (MainActivity).
 * Controla el flujo de acceso inicial mediante tres responsabilidades clave:
 * 1. Capturar e interceptar enlaces profundos (Deep Links) para redirigir a un producto específico.
 * 2. Comprobar si existe una sesión activa persistente para omitir el Login (Auto-login).
 * 3. Gestionar el formulario de inicio de sesión manual consumiendo el servicio de autenticación vía Volley.
 */
class MainActivity : AppCompatActivity() {

    // Administrador de preferencias para la persistencia de la sesión
    private lateinit var sessionManager: SessionManager
    private val baseUrl: String = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cargar la interfaz gráfica (reutiliza el diseño del login)
        setContentView(R.layout.activity_login)

        // Inicializar el manejador de sesión en el contexto de la actividad
        sessionManager = SessionManager(this)

        // ==========================================
        // 1. DETECTAR Y PROCESAR DEEP LINK
        // ==========================================
        val data = intent?.data

        if (data != null) {
            // Intentar extraer el último segmento de la URI (se asume que es el id del producto)
            val productoId = data.lastPathSegment?.toIntOrNull()

            if (productoId != null) {
                // Si el ID es válido, saltar al menú principal enviando la referencia del producto
                val intent = Intent(this, MainMenuActivity::class.java)
                intent.putExtra("id_producto", productoId)
                startActivity(intent)

                // Finalizar MainActivity para que no quede en el historial de navegación
                finish()
                return
            }
        }

        // ==========================================
        // 2. VERIFICACIÓN DE SESIÓN PERSISTENTE
        // ==========================================
        if (sessionManager.isLoggedIn()) {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // ==========================================
        // 3. CONTROL DEL FORMULARIO DE LOGUEO MANUAL
        // ==========================================
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvIrRegistro = findViewById<TextView>(R.id.tvIrRegistro)

        // Configuración de la acción de envío
        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validación de precondición en campos obligatorios
            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Invocar la rutina asíncrona de conexión con el backend
            loginUsuario(correo, password)
        }

        // Configuración del enlace para navegar a la pantalla de creación de cuentas
        tvIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    /**
     * Realiza una petición de red POST estructurada mediante un [JsonObjectRequest] de Volley.
     * Envía las credenciales al servidor y, si el estado lógico es "ok", almacena los datos de perfil
     * en el [SessionManager] antes de transferir al usuario hacia la zona principal del ecosistema.
     *
     * @param correo Dirección de correo electrónico del usuario.
     * @param password Contraseña plana provista en el formulario.
     */
    private fun loginUsuario(correo: String, password: String) {
        val url = "$baseUrl/api/login_api/"
        val queue = Volley.newRequestQueue(this)

        // Empaquetar las credenciales en un objeto estructurado JSON
        val params = JSONObject()
        params.put("correo", correo)
        params.put("password", password)

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                try {
                    // Si las credenciales concuerdan con los registros del backend de Django
                    if (response.getString("status") == "ok") {
                        val id = response.getInt("id")
                        val nombre = response.getString("nombre")
                        val correoResp = response.getString("correo")
                        val rol = response.getString("rol")

                        // Persistir los datos de sesión localmente (SharedPreferences implícito)
                        sessionManager.guardarSesion(id, nombre, correoResp, rol)

                        Toast.makeText(
                            this,
                            "Bienvenido $nombre",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navegación hacia la actividad contenedora de la app
                        val intent = Intent(this, MainMenuActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Manejo de rechazo por el backend (ej. contraseña incorrecta)
                        Toast.makeText(
                            this,
                            response.optString("message", "Credenciales incorrectas"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al procesar la respuesta", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Error de conexión: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        // Encolar la transacción en la tubería de Volley para su despacho en segundo plano
        queue.add(request)
    }
}