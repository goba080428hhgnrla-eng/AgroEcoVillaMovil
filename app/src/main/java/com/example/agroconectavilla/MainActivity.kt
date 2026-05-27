package com.example.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.utils.Constants
import org.json.JSONObject

/**
 * Actividad principal de entrada a la aplicación.
 * Controla:
 * 1. Deep Links
 * 2. Sesión persistente
 * 3. Inicio de sesión
 */
class MainActivity : AppCompatActivity() {

    // Administrador de sesión
    private lateinit var sessionManager: SessionManager

    // URL base del backend
    private val baseUrl: String = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout principal
        setContentView(R.layout.activity_login)

        // Inicializar sesión
        sessionManager = SessionManager(this)

        // ==========================================
        // 1. DETECTAR DEEP LINK
        // ==========================================
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

        // ==========================================
        // 2. VERIFICAR SESIÓN EXISTENTE
        // ==========================================
        if (sessionManager.isLoggedIn()) {

            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)

            finish()
            return
        }

        // ==========================================
        // 3. LOGIN MANUAL
        // ==========================================
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvIrRegistro = findViewById<TextView>(R.id.tvIrRegistro)

        // Botón login
        btnLogin.setOnClickListener {

            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validar campos
            if (correo.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Ejecutar login
            loginUsuario(correo, password)
        }

        // Ir a registro
        tvIrRegistro.setOnClickListener {

            startActivity(
                Intent(this, RegistroActivity::class.java)
            )
        }
    }

    /**
     * Login de usuario mediante Volley.
     */
    private fun loginUsuario(correo: String, password: String) {

        val url = "${baseUrl}api/login_api/"

        // Cola de peticiones Volley
        val queue = Volley.newRequestQueue(this)

        // JSON a enviar
        val params = JSONObject()

        params.put("correo", correo)
        params.put("password", password)

        // Petición POST
        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,

            // ==========================================
            // RESPUESTA EXITOSA
            // ==========================================
            { response ->

                try {

                    if (response.getString("status") == "ok") {

                        val id = response.getInt("id")
                        val nombre = response.getString("nombre")
                        val correoResp = response.getString("correo")
                        val rol = response.getString("rol")

                        // Guardar sesión
                        sessionManager.guardarSesion(
                            id,
                            nombre,
                            correoResp,
                            rol
                        )

                        Toast.makeText(
                            this,
                            "Bienvenido $nombre",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Ir al menú principal
                        val intent = Intent(
                            this,
                            MainMenuActivity::class.java
                        )

                        startActivity(intent)

                        finish()

                    } else {

                        Toast.makeText(
                            this,
                            response.optString(
                                "message",
                                "Credenciales incorrectas"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {

                    e.printStackTrace()

                    Toast.makeText(
                        this,
                        "Error al procesar la respuesta",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },

            // ==========================================
            // ERROR DE CONEXIÓN
            // ==========================================
            { error ->

                error.printStackTrace()

                val networkResponse = error.networkResponse

                if (networkResponse != null) {

                    println("Código HTTP: ${networkResponse.statusCode}")
                }

                Toast.makeText(
                    this,
                    "Error de conexión: ${
                        error.localizedMessage ?: "Servidor tardó demasiado"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        // ==========================================
        // CONFIGURAR TIMEOUT
        // ==========================================
        request.retryPolicy = DefaultRetryPolicy(

            70000, // 60 segundos

            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,

            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        // Ejecutar petición
        queue.add(request)
    }
}