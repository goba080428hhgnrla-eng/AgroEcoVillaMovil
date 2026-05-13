package com.example.agroconectavilla.network

import com.example.agroconectavilla.utils.Constants
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// =========================
// MODELOS
// =========================

data class UsuarioResponse(
    val status: String,
    val id: Int,
    val nombre: String? = null,
    val rol: String? = null,
    val message: String? = null,
    val correo: String?= null,
)

data class Producto(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val imagen: String?,
    val entregable: Boolean,
)

// =========================
// API INTERFACE
// =========================

interface ApiInterface {

    @Headers("Content-Type: application/json")
    @POST("api/login_api/")
    fun login(@Body body: Map<String, String>): Call<UsuarioResponse>

    @Headers("Content-Type: application/json")
    @POST("api/registro_api/")
    fun registro(@Body body: Map<String, String>): Call<UsuarioResponse>
}

// =========================
// RETROFIT CLIENT
// =========================

object RetrofitClient {

    private const val BASE_URL: String = Constants.BASE_URL


    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val instance: ApiInterface by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)
    }
}