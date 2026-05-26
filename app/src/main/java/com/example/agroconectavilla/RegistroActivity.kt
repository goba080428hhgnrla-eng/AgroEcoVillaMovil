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
 * Pantalla encargada de gestionar la creación de nuevas cuentas de usuario (RegistroActivity).
 * Recopila los datos del formulario (nombre, correo y contraseña), realiza una validación básica local,
 * y los envía mediante una petición HTTP POST asíncrona (usando Volley) al backend.
 * Tras un registro exitoso, redirige al usuario a la pantalla de autenticación para que inicie sesión.
 */
class RegistroActivity : AppCompatActivity() {

    // URL base extraída del archivo de configuraciones globales de la app
    private val baseUrl: String = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Vincular el archivo de diseño XML correspondiente al formulario de registro
        setContentView(R.layout.activity_registro)

        // Inicialización y vinculación de componentes de la interfaz de usuario (UI)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegistro = findViewById<Button>(R.id.btnRegistro)
        val tvIrLogin = findViewById<TextView>(R.id.tvIrLogin)

        // Configurar el evento de escucha para el botón de confirmación de registro
        btnRegistro.setOnClickListener {
            // Capturar las entradas de texto removiendo espacios en blanco en los extremos
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validación de precondición local: Asegurar que ningún campo esté vacío
            if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Invocar la rutina encargada de la comunicación con el servidor remoto
            registrarUsuario(nombre, correo, password)
        }

        // Configurar la navegación de retorno para usuarios que ya poseen una cuenta activa
        tvIrLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            // Finalizar esta actividad para retirarla del historial de navegación (backstack)
            finish()
        }
    }

    /**
     * Envía las credenciales del nuevo usuario al endpoint de la API REST mediante un método POST.
     * Evalúa la respuesta en formato JSON para notificar el éxito de la operación o desplegar errores de servidor.
     *
     * @param nombre Nombre completo o alias del nuevo usuario.
     * @param correo Dirección de correo electrónico única para el inicio de sesión.
     * @param password Contraseña elegida por el usuario para resguardar su cuenta.
     */
    private fun registrarUsuario(nombre: String, correo: String, password: String) {
        val url = "$baseUrl/api/registro_api/"
        val queue = Volley.newRequestQueue(this)

        // Empaquetar los datos del formulario en un objeto estructurado JSONObject
        val params = JSONObject()
        params.put("nombre", nombre)
        params.put("correo", correo)
        params.put("password", password)

        // Crear la petición de tipo JsonObjectRequest ya que se envía y se espera recibir un objeto JSON
        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                try {
                    // Validar si el estado lógico de confirmación devuelto por Django es satisfactorio
                    if (response.getString("status") == "ok") {
                        Toast.makeText(
                            this,
                            "Registro exitoso. Por favor inicia sesión.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Transferir el flujo de control hacia la pantalla de login principal
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // El servidor procesó la solicitud pero rechazó los datos (ej: el correo ya existe)
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
                // Controlar escenarios de falta de internet, DNS caídos o errores HTTP 500
                error.printStackTrace()
                Toast.makeText(this, "Error de conexión: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        // Añadir el objeto de petición a la cola de procesamiento asíncrono de Volley
        queue.add(request)
    }
}