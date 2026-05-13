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

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvIrRegistro = findViewById<TextView>(R.id.tvIrRegistro)

        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPass.text.toString().trim()

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Datos para enviar a la API
            val datosLogin = mapOf(
                "correo" to correo,
                "password" to password
            )

            // Petición a Django
            RetrofitClient.instance.login(datosLogin).enqueue(object : Callback<UsuarioResponse> {
                override fun onResponse(call: Call<UsuarioResponse>, response: Response<UsuarioResponse>) {
                    if (response.isSuccessful && response.body()?.status == "ok") {
                        val user = response.body()

                        // Ir al Home y enviar el nombre del usuario
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        intent.putExtra("USER_NAME", user?.nombre)
                        startActivity(intent)
                        finish() // Cerramos el login para que no pueda volver atrás
                    } else {
                        Toast.makeText(this@LoginActivity, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UsuarioResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Navegar a la pantalla de Registro
        tvIrRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
}