package com.example.agroconectavilla
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tvSaludo = findViewById<TextView>(R.id.tvSaludo)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        // 1. Recuperar el nombre enviado desde el Login
        val nombreUsuario = intent.getStringExtra("USER_NAME")

        // 2. Mostrar el saludo personalizado
        if (!nombreUsuario.isNullOrEmpty()) {
            tvSaludo.text = "Hola, $nombreUsuario"
        }

        // 3. Botón para cerrar sesión
        btnCerrarSesion.setOnClickListener {
            // Regresamos al Login y limpiamos el historial de pantallas
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}