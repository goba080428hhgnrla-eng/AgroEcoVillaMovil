package com.example.agroconectavilla

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.agroconectavilla.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject

class FragmentDetalle : Fragment() {

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
    private lateinit var iconCompartir: ImageButton
    private lateinit var thumbnailContainer: LinearLayout
    private lateinit var txtImageIndicator: TextView

    private lateinit var sessionManager: SessionManager
    private var productoCompleto: JSONObject? = null
    private val baseUrl: String = Constants.BASE_URL
    private var productoId: Int = 0

    // Lista de URLs de imágenes
    private val listaImagenes = mutableListOf<String>()
    private var currentImageIndex = 0

    companion object {
        private const val ARG_PRODUCTO_ID = "producto_id"

        fun newInstance(productoId: Int): FragmentDetalle {
            val fragment = FragmentDetalle()
            val args = Bundle()
            args.putInt(ARG_PRODUCTO_ID, productoId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_detalle,
            container,
            false
        )

        initViews(view)
        setupClickListeners()
        loadProductData()

        return view
    }

    private fun initViews(view: View) {
        txtNombre = view.findViewById(R.id.txtNombre)
        txtPrecio = view.findViewById(R.id.txtPrecio)
        txtDescripcion = view.findViewById(R.id.txtDescripcion)
        txtUnidadVenta = view.findViewById(R.id.txtUnidadVenta)
        txtEntregable = view.findViewById(R.id.txtEntregable)
        imagen = view.findViewById(R.id.imgProducto)
        btnDetalles = view.findViewById(R.id.btDetalles)
        btnVerMas = view.findViewById(R.id.btVerMas)
        btnAgregarCarrito = view.findViewById(R.id.btnAgregarCarrito)
        btnFavorito = view.findViewById(R.id.btnFavorito)
        btnagregar2 = view.findViewById(R.id.btnCarritoIcon)
        iconCompartir = view.findViewById(R.id.iconCompartir)
        thumbnailContainer = view.findViewById(R.id.thumbnailContainer)
        txtImageIndicator = view.findViewById(R.id.txtImageIndicator)
    }

    private fun setupClickListeners() {
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
            agregarAFavoritos()
        }

        iconCompartir.setOnClickListener {
            compartirProducto()
        }

        // Click en la imagen principal para cambiar a la siguiente
        imagen.setOnClickListener {
            if (listaImagenes.size > 1) {
                mostrarSiguienteImagen()
            }
        }
    }

    private fun loadProductData() {
        sessionManager = SessionManager(requireContext())
        productoId = arguments?.getInt(ARG_PRODUCTO_ID) ?: 0

        if (productoId != 0) {
            cargarDetalle(productoId)
        } else {
            Toast.makeText(
                requireContext(),
                "ID de producto no válido",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarSiguienteImagen() {
        if (listaImagenes.isEmpty()) return

        currentImageIndex = (currentImageIndex + 1) % listaImagenes.size
        cargarImagenPrincipal(listaImagenes[currentImageIndex])
        actualizarMiniaturasSeleccion(currentImageIndex)
        actualizarIndicador()
    }

    private fun mostrarImagenAnterior() {
        if (listaImagenes.isEmpty()) return

        currentImageIndex = if (currentImageIndex - 1 < 0) {
            listaImagenes.size - 1
        } else {
            currentImageIndex - 1
        }
        cargarImagenPrincipal(listaImagenes[currentImageIndex])
        actualizarMiniaturasSeleccion(currentImageIndex)
        actualizarIndicador()
    }

    private fun cargarImagenPrincipal(url: String) {
        Glide.with(requireContext())
            .load(url)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(imagen)
    }

    private fun actualizarMiniaturasSeleccion(selectedIndex: Int) {
        for (i in 0 until thumbnailContainer.childCount) {
            val thumbnail = thumbnailContainer.getChildAt(i) as ImageView
            val isSelected = i == selectedIndex
            thumbnail.setBackgroundColor(
                if (isSelected) {
                    android.graphics.Color.parseColor("#4CAF50")
                } else {
                    android.graphics.Color.TRANSPARENT
                }
            )
            thumbnail.setPadding(4, 4, 4, 4)
        }
    }

    private fun actualizarIndicador() {
        if (listaImagenes.size > 1) {
            txtImageIndicator.visibility = View.VISIBLE
            txtImageIndicator.text = "${currentImageIndex + 1}/${listaImagenes.size}"
        } else {
            txtImageIndicator.visibility = View.GONE
        }
    }

    private fun compartirProducto() {
        val nombreProducto = productoCompleto?.optString("nombre", "Producto") ?: "Producto"
        val precioProducto = productoCompleto?.optDouble("precio", 0.0) ?: 0.0

        val textoCompartir = """
            ¡Mira este producto en AgroConectaVilla!
            
            *$nombreProducto*
            Precio: $$precioProducto
            
            Ver más detalles aquí: ${baseUrl}api/producto/$productoId/
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textoCompartir)
            putExtra(Intent.EXTRA_SUBJECT, "Te comparto este gran producto")
        }

        startActivity(Intent.createChooser(shareIntent, "Compartir usando"))
    }

    private fun cargarDetalle(id: Int) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "${baseUrl}api/producto/$id/"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                productoCompleto = response
                mostrarInformacionBasica(response)
                procesarImagenes(response)
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error al cargar detalle",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }

    private fun procesarImagenes(response: JSONObject) {
        listaImagenes.clear()
        val imagenes: JSONArray = response.getJSONArray("imagenes")

        for (i in 0 until imagenes.length()) {
            val imgUrl = imagenes.getString(i)
            val urlCompleta = "$baseUrl$imgUrl"
            listaImagenes.add(urlCompleta)
        }

        if (listaImagenes.isNotEmpty()) {
            cargarImagenPrincipal(listaImagenes[0])
            currentImageIndex = 0
            crearMiniaturas()
            actualizarIndicador()
        }
    }

    private fun crearMiniaturas() {
        thumbnailContainer.removeAllViews()

        for (i in listaImagenes.indices) {
            val imagenUrl = listaImagenes[i]
            val thumbnail = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    80,  // Ancho en dp
                    80   // Alto en dp
                ).apply {
                    marginEnd = 8
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(2, 2, 2, 2)

                // Cargar miniatura
                Glide.with(requireContext())
                    .load(imagenUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(this)

                // Click para cambiar a esta imagen
                setOnClickListener {
                    currentImageIndex = i
                    cargarImagenPrincipal(imagenUrl)
                    actualizarMiniaturasSeleccion(currentImageIndex)
                    actualizarIndicador()
                }
            }
            thumbnailContainer.addView(thumbnail)
        }

        // Seleccionar la primera miniatura por defecto
        actualizarMiniaturasSeleccion(0)
    }

    private fun mostrarInformacionBasica(response: JSONObject) {
        txtNombre.text = response.getString("nombre")
        txtPrecio.text = "$${response.getDouble("precio")}"

        val descripcionCompleta = response.getString("descripcion")
        txtDescripcion.text = if (descripcionCompleta.length > 100) {
            descripcionCompleta.substring(0, 100) + "..."
        } else {
            descripcionCompleta
        }

        txtUnidadVenta.text = "Unidad: ${response.getString("unidad_venta")}"

        if (response.getBoolean("entregable")) {
            txtEntregable.visibility = View.VISIBLE
        }
    }

    private fun mostrarBottomSheetDescripcion() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_descripcion, null)
        val txtDescripcionCompleta = view.findViewById<TextView>(R.id.txtDescripcionCompleta)

        txtDescripcionCompleta.text = productoCompleto?.getString("descripcion")
            ?: "Sin descripción"

        dialog.setContentView(view)
        dialog.show()
    }

    private fun mostrarBottomSheetCompleto() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_detalle, null)

        productoCompleto?.let { producto ->
            val txtDescripcionCompleta = view.findViewById<TextView>(R.id.txtDescripcionCompleta)
            txtDescripcionCompleta.text = producto.getString("descripcion")

            val txtUnidadVentaDetalle = view.findViewById<TextView>(R.id.txtUnidadVentaDetalle)
            txtUnidadVentaDetalle.text = producto.getString("unidad_venta")

            val txtDisponibilidad = view.findViewById<TextView>(R.id.txtDisponibilidad)
            txtDisponibilidad.text = producto.getString("disponibilidad")

            val txtMetodosProduccion = view.findViewById<TextView>(R.id.txtMetodosProduccion)
            val metodosArray = producto.getJSONArray("metodos_produccion")
            val metodos = StringBuilder()

            for (i in 0 until metodosArray.length()) {
                if (i > 0) {
                    metodos.append("\n• ")
                } else {
                    metodos.append("• ")
                }
                metodos.append(metodosArray.getString(i))
            }

            txtMetodosProduccion.text = if (metodos.isEmpty()) {
                "No especificado"
            } else {
                metodos.toString()
            }

            val txtLogistica = view.findViewById<TextView>(R.id.txtLogistica)
            txtLogistica.text = producto.getString("logistica")

            val txtEnvio = view.findViewById<TextView>(R.id.txtEnvio)
            txtEnvio.text = if (producto.getBoolean("entregable")) {
                "Envío a domicilio disponible"
            } else {
                "Solo retiro en punto de venta"
            }

            val txtFecha = view.findViewById<TextView>(R.id.txtFecha)
            txtFecha.text = producto.getString("creado")
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun agregarAlCarrito() {
        val usuarioId = sessionManager.getUsuarioId()

        if (usuarioId == -1) {
            Toast.makeText(
                requireContext(),
                "Debes iniciar sesión primero",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (productoId == 0) {
            Toast.makeText(
                requireContext(),
                "Producto inválido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val url = "${baseUrl}api/carrito/agregar/"
        val queue = Volley.newRequestQueue(requireContext())
        val params = JSONObject()

        params.put("usuario_id", usuarioId)
        params.put("producto_id", productoId)
        params.put("cantidad", 1)

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                Toast.makeText(
                    requireContext(),
                    "Producto agregado al carrito",
                    Toast.LENGTH_SHORT
                ).show()
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error al agregar al carrito",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun agregarAFavoritos() {
        val usuarioId = sessionManager.getUsuarioId()

        if (usuarioId == -1) {
            Toast.makeText(
                requireContext(),
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val url = "${baseUrl}api/favoritos/agregar/"
        val queue = Volley.newRequestQueue(requireContext())
        val params = JSONObject()

        params.put("usuario_id", usuarioId)
        params.put("producto_id", productoId)

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                Toast.makeText(
                    requireContext(),
                    "Producto agregado a favoritos",
                    Toast.LENGTH_SHORT
                ).show()
                btnFavorito.isEnabled = false
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }
}