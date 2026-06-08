package mx.teknoeducativa.agroconectavilla

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
        private const val KEY_ES_REPARTIDOR = "es_repartidor"
        private const val KEY_ES_VENDEDOR = "es_vendedor"
        private const val KEY_ES_COMPRADOR = "es_comprador"
    }

    fun guardarSesion(
        usuarioId: Int,
        nombre: String?,
        correo: String?,
        rol: String?,
        esRepartidor: Boolean = false,
        esVendedor: Boolean = false,
        esComprador: Boolean = true
    ) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, usuarioId)
            putString(KEY_USER_NOMBRE, nombre ?: "")
            putString(KEY_USER_CORREO, correo ?: "")
            putString(KEY_USER_ROL, rol ?: "comprador")
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_ES_REPARTIDOR, esRepartidor)
            putBoolean(KEY_ES_VENDEDOR, esVendedor)
            putBoolean(KEY_ES_COMPRADOR, esComprador)
            apply()
        }
    }

    fun guardarSesion(usuarioId: Int, nombre: String?, correo: String?, rol: String?) {
        val esRepartidor = rol == "repartidor"
        val esVendedor = rol == "productor" || rol == "vendedor"
        val esComprador = rol == "comprador" || (!esRepartidor && !esVendedor)
        guardarSesion(usuarioId, nombre, correo, rol, esRepartidor, esVendedor, esComprador)
    }

    fun getUsuarioId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUsuarioNombre(): String = prefs.getString(KEY_USER_NOMBRE, "") ?: ""
    fun getUsuarioCorreo(): String = prefs.getString(KEY_USER_CORREO, "") ?: ""
    fun getUsuarioRol(): String = prefs.getString(KEY_USER_ROL, "comprador") ?: "comprador"
    fun esRepartidor(): Boolean = prefs.getBoolean(KEY_ES_REPARTIDOR, false)
    fun esVendedor(): Boolean = prefs.getBoolean(KEY_ES_VENDEDOR, false)
    fun esComprador(): Boolean = prefs.getBoolean(KEY_ES_COMPRADOR, true)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun actualizarRolARepartidor() {
        prefs.edit().apply {
            putString(KEY_USER_ROL, "repartidor")
            putBoolean(KEY_ES_REPARTIDOR, true)
            putBoolean(KEY_ES_COMPRADOR, false) // Apagamos el rol comprador activo para evitar conflictos
            apply()
        }
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    data class UsuarioInfo(
        val id: Int,
        val nombre: String,
        val correo: String,
        val rol: String,
        val esRepartidor: Boolean,
        val esVendedor: Boolean,
        val esComprador: Boolean
    )
}