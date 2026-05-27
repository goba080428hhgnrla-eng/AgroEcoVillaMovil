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
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.utils.Constants
import org.json.JSONObject

/**
 * Pantalla encargada de gestionar la creación de nuevas cuentas de usuario (RegistroActivity).
 * Captura un único campo de Nombre Completo y envía los datos estructurados a Django en Render.
 */
class RegistroActivity : AppCompatActivity() {

    // URL base extraída del archivo de configuraciones globales de la app (Sin barra '/' al final)
    private val baseUrl: String = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Vinculación de componentes de la interfaz de usuario (UI)
        // IMPORTANTE: Asegúrate de que el ID en tu XML sea etNombreCompleto
        val etNombreCompleto = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegistro = findViewById<Button>(R.id.btnRegistro)
        val tvIrLogin = findViewById<TextView>(R.id.tvIrLogin)

        // Evento de escucha para el botón de confirmación de registro
        btnRegistro.setOnClickListener {
            val nombreCompleto = etNombreCompleto.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validación local básica: Asegurar que ningún campo esté vacío
            if (nombreCompleto.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Invocar el método de registro
            registrarUsuario(nombreCompleto, correo, password)
        }

        // Configurar la navegación de retorno para usuarios que ya poseen una cuenta
        tvIrLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    /**
     * Envía las credenciales al endpoint de la API REST mediante un StringRequest (POST).
     * Separa el nombre completo y añade los roles por defecto de la Fase 1.
     */
    private fun registrarUsuario(nombreCompleto: String, correo: String, password: String) {
        // Se construye la URL asegurando que la concatenación no duplique barras diagonales
        val url = if (baseUrl.endsWith("/")) "${baseUrl}api/registro_api/" else "$baseUrl/api/registro_api/"
        val queue = Volley.newRequestQueue(this)

        // --- LÓGICA PARA SEPARAR EL NOMBRE COMPLETO ---
        val partesNombre = nombreCompleto.split("\\s+".toRegex())
        val nombre: String
        val apellidoPaterno: String
        val apellidoMaterno: String

        if (partesNombre.size >= 3) {
            // Ejemplo: "Juan Pérez López" o "Juan Carlos Pérez López"
            nombre = partesNombre[0]
            apellidoPaterno = partesNombre[1]
            // Une el resto en el apellido materno en caso de segundos nombres o apellidos compuestos
            apellidoMaterno = partesNombre.subList(2, partesNombre.size).joinToString(" ")
        } else if (partesNombre.size == 2) {
            // Ejemplo: "Juan Pérez"
            nombre = partesNombre[0]
            apellidoPaterno = partesNombre[1]
            apellidoMaterno = ""
        } else {
            // Ejemplo: "Juan"
            nombre = nombreCompleto
            apellidoPaterno = ""
            apellidoMaterno = ""
        }

        // --- CONSTRUIR EL OBJETO JSON CON LOS PARÁMETROS DE LA FASE 1 ---
        val params = JSONObject()
        try {
            params.put("nombre", nombre)
            params.put("apellido_paterno", apellidoPaterno)
            params.put("apellido_materno", apellidoMaterno)
            params.put("correo", correo)
            params.put("password", password)

            // Flags de roles requeridas por el nuevo modelo de Django
            params.put("es_comprador", true)
            params.put("es_vendedor", false)
            params.put("es_repartidor", false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // --- PETICIÓN STRINGREQUEST (Evita conflictos de cabeceras y captura respuestas HTML de error) ---
        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)

                    // Validar si el estado devuelto por Django es satisfactorio
                    if (jsonResponse.optString("status") == "ok") {
                        Toast.makeText(
                            this,
                            "Registro exitoso. Por favor inicia sesión.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Redirigir al login
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // El servidor rechazó los datos (ej: el correo ya existe)
                        val mensajeError = jsonResponse.optString("message", "Error en el registro")
                        Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al procesar la respuesta del servidor", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                error.printStackTrace()

                // Inspección detallada del error de red
                if (error.networkResponse != null) {
                    val statusCode = error.networkResponse.statusCode
                    println("CÓDIGO HTTP DESDE RENDER: $statusCode")
                    Toast.makeText(this, "Error en el servidor (Código: $statusCode)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        this,
                        "Error de conexión: Servidor no disponible o inactivo",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        ) {
            // Definir de forma explícita que el cuerpo es un JSON estructurado
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            // Transformar el JSONObject en un arreglo de Bytes seguro para la transmisión web
            override fun getBody(): ByteArray {
                return params.toString().toByteArray(Charsets.UTF_8)
            }
        }

        // --- CONFIGURAR POLÍTICA DE TIEMPOS DE ESPERA (TIMEOUT) ---
        // Se le asignan 70 segundos para tolerar el "Cold Start" (despertar) de las instancias gratuitas de Render
        request.retryPolicy = DefaultRetryPolicy(
            70000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        // Añadir el objeto a la cola de procesamiento de Volley
        queue.add(request)
    }
}