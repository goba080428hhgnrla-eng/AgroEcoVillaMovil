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

class RegistroActivity : AppCompatActivity() {

    private val baseUrl: String= Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegistro = findViewById<Button>(R.id.btnRegistro)
        val tvIrLogin = findViewById<TextView>(R.id.tvIrLogin)

        btnRegistro.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registrarUsuario(nombre, correo, password)
        }

        tvIrLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun registrarUsuario(nombre: String, correo: String, password: String) {
        val url = "$baseUrl/api/registro_api/"
        val queue = Volley.newRequestQueue(this)

        val params = JSONObject()
        params.put("nombre", nombre)
        params.put("correo", correo)
        params.put("password", password)

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                try {
                    if (response.getString("status") == "ok") {
                        Toast.makeText(
                            this,
                            "Registro exitoso. Por favor inicia sesión.",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            response.optString("message", "Error en el registro"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error en el registro", Toast.LENGTH_LONG).show()
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