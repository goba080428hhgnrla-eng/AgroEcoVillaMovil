package mx.teknoeducativa.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import mx.teknoeducativa.agroconectavilla.utils.Constants
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val baseUrl: String = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        // Deep Links
        val data = intent?.data
        if (data != null) {
            val productoId = data.lastPathSegment?.toIntOrNull()
            if (productoId != null) {
                val intent = Intent(this, MainMenuActivity::class.java)
                intent.putExtra("id_producto", productoId)
                startActivity(intent)
                finish()
                return
            }
        }

        // Sesion existente
        if (sessionManager.isLoggedIn()) {
            val esRepartidor = sessionManager.esRepartidor()
            val usuarioId = sessionManager.getUsuarioId()

            if (esRepartidor) {
                iniciarWebSocketService(usuarioId)
                // Abrir directamente el modo repartidor
                abrirModoRepartidorDirectamente(usuarioId)
            } else {
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            }
            return
        }

        // Login manual
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvIrRegistro = findViewById<TextView>(R.id.tvIrRegistro)

        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUsuario(correo, password)
        }

        tvIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun abrirModoRepartidorDirectamente(usuarioId: Int) {
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            putExtra("abrir_modo_repartidor", true)
            putExtra("repartidor_id", usuarioId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loginUsuario(correo: String, password: String) {
        mostrarLoading(true)

        val url = "${baseUrl}api/login_api/"
        val queue = Volley.newRequestQueue(this)
        val params = JSONObject()

        params.put("correo", correo)
        params.put("password", password)

        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            { response ->
                mostrarLoading(false)
                try {
                    if (response.getString("status") == "ok") {
                        val id = response.getInt("id")
                        val nombre = response.getString("nombre")
                        val correoResp = response.getString("correo")
                        val rol = response.getString("rol")
                        val esRepartidor = response.optBoolean("es_repartidor", false)

                        sessionManager.guardarSesion(id, nombre, correoResp, rol)

                        if (esRepartidor) {
                            iniciarWebSocketService(id)
                            // Abrir directamente el modo repartidor
                            abrirModoRepartidorDirectamente(id)
                        } else {
                            Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainMenuActivity::class.java))
                            finish()
                        }
                    } else {
                        Toast.makeText(this, response.optString("message", "Credenciales incorrectas"), Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al procesar la respuesta", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()
                Toast.makeText(this, "Error de conexion: ${error.localizedMessage ?: "Servidor tardo demasiado"}", Toast.LENGTH_LONG).show()
            }
        )

        request.retryPolicy = DefaultRetryPolicy(70000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(request)
    }

    private fun iniciarWebSocketService(repartidorId: Int) {
        try {
            val serviceIntent = Intent(this, WebSocketService::class.java).apply {
                putExtra("repartidor_id", repartidorId)
            }
            startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mostrarLoading(mostrar: Boolean) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarLogin)
        progressBar?.visibility = if (mostrar) android.view.View.VISIBLE else android.view.View.GONE
    }
}