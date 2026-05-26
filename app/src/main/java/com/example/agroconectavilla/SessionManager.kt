package com.example.agroconectavilla

import android.content.Context
import android.content.SharedPreferences

// Administrador de sesión de la aplicación (SessionManager)
// Utiliza la API local de SharedPreferences para persistir y recuperar la información del perfil del usuario
// Permite mantener la sesión activa entre cierres de la app guardando los datos en el almacenamiento privado
class SessionManager(context: Context) {

    // Archivo de preferencias compartido en modo privado (accesible únicamente por esta aplicación)
    private val prefs: SharedPreferences = context.getSharedPreferences("agroconecta_prefs", Context.MODE_PRIVATE)

    companion object {
        // Claves string (Llaves) para indexar y guardar cada propiedad en el XML local
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NOMBRE = "user_nombre"
        private const val KEY_USER_CORREO = "user_correo"
        private const val KEY_USER_ROL = "user_rol"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Escribe en el disco de manera asíncrona la información básica del usuario provista por el backend
    // Si los valores textuales son nulos, aplica un operador elvis (?:) para salvar cadenas vacías seguras
    fun guardarSesion(usuarioId: Int, nombre: String?, correo: String?, rol: String?) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, usuarioId)
            putString(KEY_USER_NOMBRE, nombre ?: "")
            putString(KEY_USER_CORREO, correo ?: "")
            putString(KEY_USER_ROL, rol ?: "")
            putBoolean(KEY_IS_LOGGED_IN, true) // Bandera de control para saltar el login (Auto-login)
            apply() // Persiste los cambios en disco de manera asíncrona en segundo plano
        }
    }

    // Recupera el identificador único del usuario en sesión activa (-1 por defecto si no existe)
    fun getUsuarioId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    // Recupera el nombre del usuario almacenado en las preferencias (cadena vacía si no existe)
    fun getUsuarioNombre(): String {
        return prefs.getString(KEY_USER_NOMBRE, "") ?: ""
    }

    // Recupera el correo electrónico del usuario desde el almacenamiento local (cadena vacía si no existe)
    fun getUsuarioCorreo(): String {
        return prefs.getString(KEY_USER_CORREO, "") ?: ""
    }

    // Recupera el rol o nivel de permisos del usuario (cadena vacía por defecto)
    fun getUsuarioRol(): String {
        return prefs.getString(KEY_USER_ROL, "") ?: ""
    }

    // Evalúa el estado lógico de conexión del cliente en el dispositivo
    // Esta función es consumida durante el arranque por MainActivity para decidir si se omite el login
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Remueve de forma absoluta todas las llaves y valores contenidos dentro del archivo de preferencias
    // Restablece por completo el estado de la aplicación a "Cerrado de sesión"
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}