package com.example.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.agroconectavilla.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject

class DetalleActivity : AppCompatActivity() {

    private lateinit var txtNombre: TextView

    private lateinit var btnFavorito: Button
    private lateinit var txtPrecio: TextView
    private lateinit var txtDescripcion: TextView
    private lateinit var txtUnidadVenta: TextView
    private lateinit var txtEntregable: TextView
    private lateinit var imagen: ImageView
    private lateinit var btnDetalles: Button
    private lateinit var btnVerMas: Button
    private lateinit var btnAgregarCarrito: Button

    private lateinit var btnagregar2: ImageButton

    private lateinit var sessionManager: SessionManager
    private var productoCompleto: JSONObject? = null
    private val baseUrl: String= Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)

        sessionManager = SessionManager(this)

        txtNombre = findViewById(R.id.txtNombre)
        txtPrecio = findViewById(R.id.txtPrecio)
        txtDescripcion = findViewById(R.id.txtDescripcion)
        txtUnidadVenta = findViewById(R.id.txtUnidadVenta)
        txtEntregable = findViewById(R.id.txtEntregable)
        imagen = findViewById(R.id.imgProducto)
        btnDetalles = findViewById(R.id.btDetalles)
        btnVerMas = findViewById(R.id.btVerMas)
        btnAgregarCarrito = findViewById(R.id.btnAgregarCarrito)
        btnFavorito = findViewById(R.id.btnFavorito)
        btnagregar2=findViewById<ImageButton>(R.id.btnCarritoIcon)

        val id = intent.getIntExtra("id", 0)
        cargarDetalle(id)

        btnDetalles.setOnClickListener {
            mostrarBottomSheetCompleto()
        }

        btnVerMas.setOnClickListener {
            mostrarBottomSheetDescripcion()
        }

        btnAgregarCarrito.setOnClickListener {
            agregarAlCarrito()
        }
        btnagregar2.setOnClickListener {
            agregarAlCarrito()
        }
        btnFavorito.setOnClickListener {
            btnFavorito = findViewById(R.id.btnFavorito)
            agregarAFavoritos()
        }
    }

    private fun cargarDetalle(id: Int) {
        val queue = Volley.newRequestQueue(this)
        val url = "$baseUrl" + "api/producto/$id/"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONObject ->
                productoCompleto = response
                mostrarInformacionBasica(response)
            },
            { error ->
                error.printStackTrace()
            }
        )

        queue.add(request)
    }

    private fun mostrarInformacionBasica(response: JSONObject) {
        // Información básica
        txtNombre.text = response.getString("nombre")
        txtPrecio.text = "$${response.getDouble("precio")}"

        // Descripción (resumida)
        val descripcionCompleta = response.getString("descripcion")
        txtDescripcion.text = if (descripcionCompleta.length > 100) {
            descripcionCompleta.substring(0, 100) + "..."
        } else {
            descripcionCompleta
        }

        // Unidad de venta
        txtUnidadVenta.text = "Unidad: ${response.getString("unidad_venta")}"

        // Mostrar badge de entregable
        if (response.getBoolean("entregable")) {
            txtEntregable.visibility = TextView.VISIBLE
        }

        // Cargar imagen principal
        val imagenes: JSONArray = response.getJSONArray("imagenes")
        if (imagenes.length() > 0) {
            val imgUrl = imagenes.getString(0)
            Glide.with(this)
                .load("$baseUrl$imgUrl")
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(imagen)
        }
    }

    private fun mostrarBottomSheetDescripcion() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_descripcion, null)

        val txtDescripcionCompleta = view.findViewById<TextView>(R.id.txtDescripcionCompleta)
        txtDescripcionCompleta.text = productoCompleto?.getString("descripcion") ?: "Sin descripción"

        dialog.setContentView(view)
        dialog.show()
    }

    private fun mostrarBottomSheetCompleto() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_detalle, null)

        productoCompleto?.let { producto ->
            // Descripción completa
            val txtDescripcionCompleta = view.findViewById<TextView>(R.id.txtDescripcionCompleta)
            txtDescripcionCompleta.text = producto.getString("descripcion")

            // Unidad de venta
            val txtUnidadVentaDetalle = view.findViewById<TextView>(R.id.txtUnidadVentaDetalle)
            txtUnidadVentaDetalle.text = producto.getString("unidad_venta")

            // Disponibilidad
            val txtDisponibilidad = view.findViewById<TextView>(R.id.txtDisponibilidad)
            txtDisponibilidad.text = producto.getString("disponibilidad")

            // Métodos de producción
            val txtMetodosProduccion = view.findViewById<TextView>(R.id.txtMetodosProduccion)
            val metodosArray = producto.getJSONArray("metodos_produccion")
            val metodos = StringBuilder()
            for (i in 0 until metodosArray.length()) {
                if (i > 0) metodos.append("\n• ")
                else metodos.append("• ")
                metodos.append(metodosArray.getString(i))
            }
            txtMetodosProduccion.text = if (metodos.isEmpty()) "No especificado" else metodos.toString()

            // Logística
            val txtLogistica = view.findViewById<TextView>(R.id.txtLogistica)
            txtLogistica.text = producto.getString("logistica")

            // Envío
            val txtEnvio = view.findViewById<TextView>(R.id.txtEnvio)
            txtEnvio.text = if (producto.getBoolean("entregable")) {
                " Envío a domicilio disponible"
            } else {
                " Solo retiro en punto de venta"
            }

            // Fecha
            val txtFecha = view.findViewById<TextView>(R.id.txtFecha)
            txtFecha.text = producto.getString("creado")
        }

        dialog.setContentView(view)
        dialog.show()
    }
    private fun agregarAlCarrito(){
        val usuarioId = sessionManager.getUsuarioId()

        if (usuarioId == -1) {
            Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_LONG).show()
            return
        }

        val productoId = intent.getIntExtra("id", 0)

        if (productoId == 0) {
            Toast.makeText(this, "Error: Producto no identificado", Toast.LENGTH_LONG).show()
            return
        }

        val url = "$baseUrl"+"api/carrito/agregar/"
        val queue = Volley.newRequestQueue(this)

        val params = JSONObject()
        params.put("usuario_id", usuarioId)
        params.put("producto_id", productoId)
        params.put("cantidad", 1)

        println(" Agregando al carrito: $params")

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                println(" Respuesta: $response")
                try {
                    if (response.getString("status") == "ok") {
                        Toast.makeText(this, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, response.getString("message"), Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al agregar al carrito", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                println(" Error: ${error.message}")
                error.printStackTrace()

                val mensaje = when {
                    error.networkResponse == null -> "Error de conexión. Verifica internet."
                    error.networkResponse?.statusCode == 404 -> "API no encontrada"
                    error.networkResponse?.statusCode == 500 -> "Error interno del servidor"
                    else -> "Error: ${error.message}"
                }
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        queue.add(request)
    }

    private fun compartirProducto(){
        val iconCompartir = findViewById<ImageView>(R.id.iconCompartir)

        iconCompartir.setOnClickListener {
            val textoCompartir = "Comparte este contenido https://tusitio.com"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textoCompartir)
                // Agregacion del Gmail
                putExtra(Intent.EXTRA_SUBJECT, "Asunto del mensaje")
            }
            // menu de selecciones de la aplicacion o dispositivo
            startActivity(Intent.createChooser(shareIntent, "Compartir usando:"))
        }
    }


    private fun agregarAFavoritos() {

        val usuarioId = sessionManager.getUsuarioId()
        val productoId = intent.getIntExtra("id", 0)

        // Validar usuario
        if (usuarioId == -1) {

            Toast.makeText(
                this,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Validar producto
        if (productoId == 0) {

            Toast.makeText(
                this,
                "Producto inválido",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val url = "${baseUrl}api/favoritos/agregar/"

        val queue = Volley.newRequestQueue(this)

        val params = JSONObject()

        params.put("usuario_id", usuarioId)
        params.put("producto_id", productoId)

        val request = JsonObjectRequest(

            Request.Method.POST,
            url,
            params,

            { response ->

                try {

                    val status = response.getString("status")

                    when (status) {

                        "ok" -> {

                            Toast.makeText(
                                this,
                                "Producto agregado a favoritos",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Desactivar botón
                            btnFavorito.isEnabled = false
                        }

                        "exists" -> {

                            Toast.makeText(
                                this,
                                "Ya está en favoritos",
                                Toast.LENGTH_SHORT
                            ).show()

                            btnFavorito.isEnabled = false
                        }

                        else -> {

                            Toast.makeText(
                                this,
                                response.getString("message"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {

                    e.printStackTrace()

                    Toast.makeText(
                        this,
                        "Error procesando respuesta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },

            { error ->

                error.printStackTrace()

                Toast.makeText(
                    this,
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }
}