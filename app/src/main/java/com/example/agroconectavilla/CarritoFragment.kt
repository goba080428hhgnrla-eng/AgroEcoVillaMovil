package com.example.agroconectavilla

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.adapter.CarritoAdapter
import com.example.agroconectavilla.network.CarritoItem
import com.example.agroconectavilla.network.Producto
import com.example.agroconectavilla.utils.Constants
import org.json.JSONObject

/**
 * Fragmento encargado de gestionar y mostrar el carrito de compras del usuario.
 * Permite visualizar los productos agregados, modificar sus cantidades, eliminar items
 * y proceder con la finalización del pedido interactuando con una API externa mediante Volley.
 */
class CarritoFragment : Fragment() {

    // Componentes de la interfaz de usuario (UI)
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CarritoAdapter
    private val listaItems = mutableListOf<CarritoItem>()
    private lateinit var progressBar: View
    private lateinit var layoutVacio: LinearLayout
    private lateinit var layoutResumen: LinearLayout
    private lateinit var txtTotal: TextView
    private lateinit var txtCantidadItems: TextView
    private lateinit var btnContinuar: Button

    // Configuración y variables de estado
    private val baseUrl: String = Constants.BASE_URL
    private var usuarioId: Int = -1

    companion object {
        private const val ARG_USUARIO_ID = "usuario_id"

        /**
         * Crea una nueva instancia de [CarritoFragment] pasando el ID del usuario como argumento.
         *
         * @param usuarioId ID único del usuario actual.
         * @return Una instancia configurada de CarritoFragment.
         */
        fun newInstance(usuarioId: Int): CarritoFragment {
            val fragment = CarritoFragment()
            val args = Bundle()
            args.putInt(ARG_USUARIO_ID, usuarioId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recuperar el usuarioId enviado a través de los argumentos del Fragmento
        usuarioId = arguments?.getInt(ARG_USUARIO_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el diseño del fragmento
        val view = inflater.inflate(R.layout.fragment_carrito, container, false)

        // Inicialización de las vistas de la interfaz
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        recycler = view.findViewById(R.id.recyclerCarrito)
        progressBar = view.findViewById(R.id.progressBar)
        layoutVacio = view.findViewById(R.id.layoutVacio)
        layoutResumen = view.findViewById(R.id.layoutResumen)
        txtTotal = view.findViewById(R.id.txtTotal)
        txtCantidadItems = view.findViewById(R.id.txtCantidadItems)
        btnContinuar = view.findViewById(R.id.btnContinuar)

        // Configurar acción del botón de retroceso en el Toolbar
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        // Configuración del RecyclerView y su adaptador con expresiones lambda para los eventos
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CarritoAdapter(
            listaItems,
            requireContext(),
            baseUrl,
            { item, nuevaCantidad -> actualizarCantidad(item, nuevaCantidad) }, // Lambda para modificar cantidad
            { item -> eliminarItem(item) }                                      // Lambda para borrar ítem
        )
        recycler.adapter = adapter

        // Acción al presionar el botón de finalizar pedido
        btnContinuar.setOnClickListener {
            Toast.makeText(requireContext(), "Finalizar pedido", Toast.LENGTH_LONG).show()
        }

        // Validar que el usuario sea válido antes de realizar la petición HTTP
        if (usuarioId == -1) {
            Toast.makeText(requireContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return view
        }

        // Cargar los datos iniciales del carrito desde el servidor
        cargarCarrito()

        return view
    }

    /**
     * Realiza una petición GET al servidor para obtener la lista de productos en el carrito del usuario.
     */
    private fun cargarCarrito() {
        mostrarLoading(true)

        val url = "$baseUrl" + "api/carrito/$usuarioId/"
        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    // Si el servidor responde de manera exitosa, procesar el JSON recibido
                    if (response.getString("status") == "ok") {
                        actualizarUIDesdeJSON(response)
                    } else {
                        Toast.makeText(requireContext(), response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                    mostrarLoading(false)
                } catch (e: Exception) {
                    mostrarLoading(false)
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error al cargar el carrito", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    /**
     * Procesa la respuesta JSON del servidor, mapea los datos a los modelos locales (Producto y CarritoItem),
     * actualiza la lista del adaptador y gestiona la visibilidad de las vistas según el estado del carrito.
     *
     * @param response Objeto [JSONObject] que contiene la información actualizada del carrito.
     */
    private fun actualizarUIDesdeJSON(response: JSONObject) {
        listaItems.clear()

        // Mapeo del arreglo de ítems del JSON a objetos de tipo CarritoItem
        val itemsArray = response.getJSONArray("items")
        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.getJSONObject(i)
            val productoObj = itemObj.getJSONObject("producto")

            // Construir el objeto de negocio Producto
            val producto = Producto(
                id = productoObj.getInt("id"),
                nombre = productoObj.getString("nombre"),
                precio = productoObj.getDouble("precio"),
                imagen = if (productoObj.has("imagen") && !productoObj.isNull("imagen"))
                    productoObj.getString("imagen") else null,
                entregable = productoObj.getBoolean("entregable")
            )

            // Construir el objeto contenedor del ítem del carrito
            val item = CarritoItem(
                id = itemObj.getInt("id"),
                producto = producto,
                cantidad = itemObj.getInt("cantidad"),
                subtotal = itemObj.getDouble("subtotal"),
                agregado = itemObj.getString("agregado")
            )

            listaItems.add(item)
        }

        // Notificar los cambios al adaptador para refrescar la lista en pantalla
        adapter.notifyDataSetChanged()

        // Obtener los totales generales del carrito
        val total = response.getDouble("total")
        val totalItems = response.getInt("total_items")

        // Alternar visibilidad de las vistas dependiendo de si el carrito tiene productos o está vacío
        if (listaItems.isEmpty()) {
            recycler.visibility = View.GONE
            layoutResumen.visibility = View.GONE
            layoutVacio.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            layoutResumen.visibility = View.VISIBLE
            layoutVacio.visibility = View.GONE
            txtTotal.text = "$${total}"
            txtCantidadItems.text = totalItems.toString()
        }
    }

    /**
     * Envía una petición PUT al servidor para actualizar la cantidad de un ítem específico del carrito.
     *
     * @param item El objeto [CarritoItem] que se desea modificar.
     * @param nuevaCantidad La nueva cantidad asignada al producto.
     */
    private fun actualizarCantidad(item: CarritoItem, nuevaCantidad: Int) {
        mostrarLoading(true)

        val url = "$baseUrl" + "api/carrito/actualizar/${item.id}/"
        val queue = Volley.newRequestQueue(requireContext())

        // Configuración del cuerpo de la petición con el nuevo valor de cantidad
        val params = JSONObject()
        params.put("cantidad", nuevaCantidad)

        val request = JsonObjectRequest(
            Request.Method.PUT,
            url,
            params,
            { response ->
                try {
                    // Al recibir respuesta correcta se vuelve a renderizar la UI con la respuesta actualizada
                    if (response.getString("status") == "ok") {
                        actualizarUIDesdeJSON(response)
                    } else {
                        Toast.makeText(requireContext(), response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                    mostrarLoading(false)
                } catch (e: Exception) {
                    mostrarLoading(false)
                    e.printStackTrace()
                }
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error al actualizar cantidad", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    /**
     * Envía una petición DELETE al servidor para remover por completo un ítem del carrito de compras.
     *
     * @param item El objeto [CarritoItem] que se procederá a eliminar.
     */
    private fun eliminarItem(item: CarritoItem) {
        mostrarLoading(true)

        val url = "$baseUrl" + "api/carrito/eliminar/${item.id}/"
        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(
            Request.Method.DELETE,
            url,
            null,
            { response ->
                try {
                    if (response.getString("status") == "ok") {
                        actualizarUIDesdeJSON(response)
                        Toast.makeText(requireContext(), response.getString("message"), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                    mostrarLoading(false)
                } catch (e: Exception) {
                    mostrarLoading(false)
                    e.printStackTrace()
                }
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error al eliminar producto", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    /**
     * Controla de manera centralizada la visibilidad del componente de carga (ProgressBar)
     * e interactúa con los contenedores de datos para evitar colisiones visuales durante las peticiones en red.
     *
     * @param mostrar true para visualizar el indicador de carga y ocultar la interfaz del carrito; false en caso contrario.
     */
    private fun mostrarLoading(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recycler.visibility = if (mostrar) View.GONE else recycler.visibility
        layoutResumen.visibility = if (mostrar) View.GONE else layoutResumen.visibility
        layoutVacio.visibility = if (mostrar) View.GONE else layoutVacio.visibility
    }
}