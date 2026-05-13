package com.example.agroconectavilla

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("agroconecta_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NOMBRE = "user_nombre"
        private const val KEY_USER_CORREO = "user_correo"
        private const val KEY_USER_ROL = "user_rol"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun guardarSesion(usuarioId: Int, nombre: String?, correo: String?, rol: String?) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, usuarioId)
            putString(KEY_USER_NOMBRE, nombre ?: "")
            putString(KEY_USER_CORREO, correo ?: "")
            putString(KEY_USER_ROL, rol ?: "")
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUsuarioId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getUsuarioNombre(): String {
        return prefs.getString(KEY_USER_NOMBRE, "") ?: ""
    }

    fun getUsuarioCorreo(): String {
        return prefs.getString(KEY_USER_CORREO, "") ?: ""
    }

    fun getUsuarioRol(): String {
        return prefs.getString(KEY_USER_ROL, "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}