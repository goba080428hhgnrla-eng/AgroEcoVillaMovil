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

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val baseUrl: String= Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

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

    private fun loginUsuario(correo: String, password: String) {
        val url = "$baseUrl/api/login_api/"
        val queue = Volley.newRequestQueue(this)

        val params = JSONObject()
        params.put("correo", correo)
        params.put("password", password)

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                try {
                    if (response.getString("status") == "ok") {
                        val id = response.getInt("id")
                        val nombre = response.getString("nombre")
                        val correo = response.getString("correo")
                        val rol = response.getString("rol")

                        // Guardar sesión
                        sessionManager.guardarSesion(id, nombre, correo, rol)

                        Toast.makeText(
                            this,
                            "Bienvenido $nombre",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this, MainMenuActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
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

        queue.add(request)
    }
}