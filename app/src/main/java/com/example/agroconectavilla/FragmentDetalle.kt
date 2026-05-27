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

/**
 * Fragmento encargado de mostrar la vista detallada de un producto específico.
 * Administra un carrusel de imágenes dinámico con miniaturas, la visualización de información básica
 * y extendida (vía BottomSheetDialog), y permite realizar interacciones clave como compartir,
 * agregar elementos al carrito de compras y registrar productos favoritos.
 */
class FragmentDetalle : Fragment() {

    // Componentes de la interfaz de usuario (UI)
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

    // Controladores de sesión, datos y estado del carrusel
    private lateinit var sessionManager: SessionManager
    private var productoCompleto: JSONObject? = null
    private val baseUrl: String = Constants.BASE_URL
    private var productoId: Int = 0

    // Lista de rutas URL de imágenes del producto y puntero del índice activo
    private val listaImagenes = mutableListOf<String>()
    private var currentImageIndex = 0

    companion object {
        private const val ARG_PRODUCTO_ID = "producto_id"

        /**
         * Genera una nueva instancia de [FragmentDetalle] asociando el identificador del producto solicitado.
         *
         * @param productoId ID único del producto a consultar.
         * @return Un fragmento parametrizado con los datos del producto.
         */
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
        // Inflar el diseño visual del fragmento de detalle
        val view = inflater.inflate(
            R.layout.fragment_detalle,
            container,
            false
        )

        // Inicialización coordinada del fragmento
        initViews(view)
        setupClickListeners()
        loadProductData()

        return view
    }

    /**
     * Vincula las variables locales de la clase con sus respectivos componentes XML mediante [View.findViewById].
     */
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

    /**
     * Define los comportamientos de click para todos los botones e interacciones táctiles de la UI.
     */
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

        // Evento táctil sobre la imagen principal para avanzar secuencialmente en la galería
        imagen.setOnClickListener {
            if (listaImagenes.size > 1) {
                mostrarSiguienteImagen()
            }
        }
    }

    /**
     * Inicializa el gestor de sesión local y recupera el identificador del producto
     * guardado en los argumentos para proceder con su carga remota.
     */
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

    /**
     * Avanza el carrusel a la siguiente imagen disponible de forma circular
     * y refresca los indicadores y estados visuales del contenedor.
     */
    private fun mostrarSiguienteImagen() {
        if (listaImagenes.isEmpty()) return

        currentImageIndex = (currentImageIndex + 1) % listaImagenes.size
        cargarImagenPrincipal(listaImagenes[currentImageIndex])
        actualizarMiniaturasSeleccion(currentImageIndex)
        actualizarIndicador()
    }

    /**
     * Retrocede el carrusel a la imagen anterior de forma circular
     * y actualiza la sincronización de las miniaturas.
     */
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

    /**
     * Utiliza la biblioteca Glide para descargar y renderizar una imagen por su URL
     * en el componente visual principal, definiendo imágenes de marcador de posición predeterminadas.
     *
     * @param url Dirección web completa de la imagen.
     */
    private fun cargarImagenPrincipal(url: String) {
        Glide.with(requireContext())
            .load(url)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(imagen)
    }

    /**
     * Modifica el color de fondo de las miniaturas del carrusel para destacar
     * visualmente el elemento que se encuentra activo en la imagen principal.
     *
     * @param selectedIndex Posición del elemento que está seleccionado.
     */
    private fun actualizarMiniaturasSeleccion(selectedIndex: Int) {
        for (i in 0 until thumbnailContainer.childCount) {
            val thumbnail = thumbnailContainer.getChildAt(i) as ImageView
            val isSelected = i == selectedIndex
            thumbnail.setBackgroundColor(
                if (isSelected) {
                    android.graphics.Color.parseColor("#4CAF50") // Color verde de selección
                } else {
                    android.graphics.Color.TRANSPARENT
                }
            )
            thumbnail.setPadding(4, 4, 4, 4)
        }
    }

    /**
     * Actualiza el indicador numérico textual (ej. 1/3) según la posición del carrusel.
     * Si el producto cuenta con una única imagen, el indicador se oculta de la pantalla.
     */
    private fun actualizarIndicador() {
        if (listaImagenes.size > 1) {
            txtImageIndicator.visibility = View.VISIBLE
            txtImageIndicator.text = "${currentImageIndex + 1}/${listaImagenes.size}"
        } else {
            txtImageIndicator.visibility = View.GONE
        }
    }

    /**
     * Genera un mensaje estructurado con la información básica del producto y un enlace profundo (Deep Link),
     * invocando el selector nativo del sistema para compartir texto plano en aplicaciones de terceros.
     */
    private fun compartirProducto() {
        val nombreProducto =
            productoCompleto?.optString("nombre", "Producto") ?: "Producto"

        val precioProducto =
            productoCompleto?.optDouble("precio", 0.0) ?: 0.0

        // Definición del Deep Link de la aplicación
        val link = "https://ritalin-detonator-womb.ngrok-free.dev/producto/\$productoId/"

        val textoCompartir = """
        ¡Mira este producto en AgroConectaVilla!

        *$nombreProducto*
        Precio: $$precioProducto

        Ver más detalles aquí:
        $link
    """.trimIndent()

        val shareIntent = Intent(
            Intent.ACTION_SEND
        ).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textoCompartir)
            putExtra(Intent.EXTRA_SUBJECT, "Producto AgroConectaVilla")
        }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Compartir usando"
            )
        )
    }

    /**
     * Realiza una solicitud HTTP GET a través de Volley para obtener la estructura completa del producto
     * desde el servidor y delegar el procesamiento de datos a las funciones encargadas de renderizar la UI.
     *
     * @param id Código único del producto a consultar en la base de datos remota.
     */
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

    /**
     * Extrae el arreglo de URLs de imágenes proveniente del JSON, reconstruye las rutas absolutas
     * utilizando la dirección base y pone en marcha la visualización inicial del carrusel gráfico.
     *
     * @param response Objeto [JSONObject] del servidor con la información del producto.
     */
    private fun procesarImagenes(response: JSONObject) {

        // Limpiar lista previa
        listaImagenes.clear()

        // Obtener arreglo de imágenes desde el JSON
        val imagenes: JSONArray = response.getJSONArray("imagenes")

        // Recorrer imágenes
        for (i in 0 until imagenes.length()) {

            // La URL ya viene COMPLETA desde Django
            val imgUrl = imagenes.getString(i)

            // Agregar directamente
            listaImagenes.add(imgUrl)
        }

        // Mostrar primera imagen
        if (listaImagenes.isNotEmpty()) {

            currentImageIndex = 0

            cargarImagenPrincipal(listaImagenes[0])

            crearMiniaturas()

            actualizarIndicador()
        }
    }

    /**
     * Genera dinámicamente vistas de tipo [ImageView] por cada ruta de imagen disponible,
     * las inserta en el contenedor lineal horizontal (thumbnails) y les asigna su respectivo evento táctil.
     */
    private fun crearMiniaturas() {
        thumbnailContainer.removeAllViews()

        for (i in listaImagenes.indices) {
            val imagenUrl = listaImagenes[i]
            val thumbnail = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    80,  // Ancho explícito en píxeles/dp equivalentes
                    80   // Alto explícito en píxeles/dp equivalentes
                ).apply {
                    marginEnd = 8
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(2, 2, 2, 2)

                // Cargar de forma asíncrona la imagen miniatura
                Glide.with(requireContext())
                    .load(imagenUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(this)

                // Interacción para cambiar la imagen principal al pulsar la miniatura
                setOnClickListener {
                    currentImageIndex = i
                    cargarImagenPrincipal(imagenUrl)
                    actualizarMiniaturasSeleccion(currentImageIndex)
                    actualizarIndicador()
                }
            }
            thumbnailContainer.addView(thumbnail)
        }

        // Resaltar por defecto la primera miniatura añadida
        actualizarMiniaturasSeleccion(0)
    }

    /**
     * Mapea los atributos esenciales del JSON (nombre, precio, unidad) a los textos de la pantalla.
     * Aplica adicionalmente una regla de recorte para limitar la longitud de la descripción en la vista general.
     *
     * @param response Estructura JSON del producto.
     */
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

    /**
     * Despliega un diálogo emergente inferior (BottomSheet) dedicado exclusivamente
     * a mostrar el bloque completo de la descripción del producto sin restricciones de longitud.
     */
    private fun mostrarBottomSheetDescripcion() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_descripcion, null)
        val txtDescripcionCompleta = view.findViewById<TextView>(R.id.txtDescripcionCompleta)

        txtDescripcionCompleta.text = productoCompleto?.getString("descripcion")
            ?: "Sin descripción"

        dialog.setContentView(view)
        dialog.show()
    }

    /**
     * Infla y presenta un diálogo emergente inferior estructurado (BottomSheet) con la ficha técnica completa
     * del artículo, parseando elementos como métodos de producción (desde un JSONArray), logística y fechas.
     */
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

            // Convertir el arreglo JSON de métodos en un string formateado por viñetas
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

    /**
     * Envía una petición POST autenticada con los parámetros del usuario y del producto actual
     * para añadir de forma remota una unidad de este artículo al carrito de compras.
     */
    private fun agregarAlCarrito() {
        val usuarioId = sessionManager.getUsuarioId()

        // Validaciones de precondición críticas
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

        // Creación de objeto anónimo extendiendo JsonObjectRequest para sobreescribir los encabezados (Headers)
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

    /**
     * Consume la API de favoritos mediante un método POST enviando las referencias cruzadas
     * del usuario y del producto para indexarlo en su lista personal de preferidos.
     */
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
                // Deshabilitar botón para mitigar llamadas repetitivas innecesarias
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