package com.example.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agroconectavilla.network.RetrofitClient
import com.example.agroconectavilla.network.UsuarioResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Pantalla de inicio de sesión de la aplicación (LoginActivity).
 * Se encarga de capturar las credenciales de acceso del usuario, validarlas localmente,
 * enviarlas al backend (Django) mediante una petición HTTP asíncrona con Retrofit y,
 * en caso de éxito, dar acceso a la aplicación redirigiendo al usuario a la pantalla principal.
 */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establecer el diseño visual asociado a esta actividad
        setContentView(R.layout.activity_login)

        // Vinculación de los componentes de la interfaz de usuario (UI) mediante sus IDs
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvIrRegistro = findViewById<TextView>(R.id.tvIrRegistro)

        // Manejo del evento de click en el botón de iniciar sesión
        btnLogin.setOnClickListener {
            // Obtener el texto ingresado eliminando espacios en blanco innecesarios al inicio o final
            val correo = etCorreo.text.toString().trim()
            val password = etPass.text.toString().trim()

            // Validación de precondición local: Verificar que los campos no estén vacíos
            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Estructurar los datos en un mapa llave-valor para ser serializados en el cuerpo de la petición (JSON)
            val datosLogin = mapOf(
                "correo" to correo,
                "password" to password
            )

            // Encolar la petición HTTP asíncrona a Django usando la instancia de Retrofit
            RetrofitClient.instance.login(datosLogin).enqueue(object : Callback<UsuarioResponse> {

                /**
                 * Se ejecuta cuando se recibe una respuesta del servidor (sea exitosa o un código de error HTTP).
                 */
                override fun onResponse(call: Call<UsuarioResponse>, response: Response<UsuarioResponse>) {
                    // Validar si el código de estado HTTP es satisfactorio (2xx) y la API retornó un estado lógico exitoso
                    if (response.isSuccessful && response.body()?.status == "ok") {
                        val user = response.body()

                        // Configurar el Intent para navegar hacia la actividad o contenedor principal (Home)
                        val intent = Intent(this@LoginActivity, InicioFragment::class.java)
                        // Adjuntar información complementaria (nombre del usuario) para ser recuperada en el destino
                        intent.putExtra("USER_NAME", user?.nombre)
                        startActivity(intent)

                        // Finalizar la actividad actual para removerla de la pila de navegación (backstack)
                        finish()
                    } else {
                        // El servidor respondió pero las credenciales no coincidieron o falló la lógica de negocio
                        Toast.makeText(this@LoginActivity, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }

                /**
                 * Se ejecuta ante fallos de hardware, timeout o ausencia absoluta de conectividad a la red.
                 */
                override fun onFailure(call: Call<UsuarioResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Configurar la redirección a la pantalla de creación de nuevas cuentas (Registro)
        tvIrRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
}