package com.example.agroconectavilla

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.adapter.ProductoAdapter
import com.example.agroconectavilla.network.Producto
import com.example.agroconectavilla.utils.Constants
import org.json.JSONArray

/**
 * Fragmento encargado de gestionar y mostrar el catálogo exclusivo de productos entregables.
 * Consume la API general de productos, aplica un filtro del lado del cliente para retener únicamente
 * aquellos artículos con la propiedad "entregable" activa, y proporciona capacidades de búsqueda
 * dinámicas en tiempo real a través de un campo de texto interactivo.
 */
class ProductosEntregablesFragment : Fragment() {

    // Componentes de la interfaz de usuario (UI)
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private val listaOriginal = mutableListOf<Producto>() // Respaldo local de los productos entregables
    private val listaFiltrada = mutableListOf<Producto>() // Colección mutada que abastece al adaptador
    private lateinit var txtVacio: TextView
    private lateinit var progressBar: View
    private lateinit var buscador: EditText

    // Endpoint de la API REST para el listado de productos
    private val url: String = Constants.BASE_URL + "api/productos/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el contenedor visual correspondiente a la vista de productos entregables
        val view = inflater.inflate(R.layout.fragment_productos_entregables, container, false)

        // Inicialización de componentes gráficos a partir del XML
        recycler = view.findViewById(R.id.recyclerProductos)
        txtVacio = view.findViewById(R.id.txtVacio)
        progressBar = view.findViewById(R.id.progressBar)
        buscador = view.findViewById(R.id.buscador)

        // Configuración de la cuadrícula del RecyclerView (2 columnas por fila)
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        // El adaptador apunta a 'listaFiltrada' para reaccionar inmediatamente a las búsquedas
        adapter = ProductoAdapter(listaFiltrada, requireContext())
        recycler.adapter = adapter

        // Ejecutar la petición asíncrona para descargar los productos desde el servidor
        cargarProductosEntregables()

        // Configurar los escuchadores del campo de búsqueda
        configurarBuscador()

        return view
    }

    /**
     * Acopla un escuchador [TextWatcher] al componente de texto para evaluar de forma
     * automatizada el contenido del buscador a medida que el usuario realiza modificaciones.
     */
    private fun configurarBuscador() {
        buscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Ejecutar la rutina de filtrado con los caracteres ingresados
                filtrarProductos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Discrimina los elementos de la lista de respaldo basándose en la coincidencia textual
     * entre el criterio de búsqueda y el nombre de cada artículo. Actualiza los estados de la UI.
     *
     * @param query Palabra o fragmento de cadena ingresado en la barra de búsqueda.
     */
    private fun filtrarProductos(query: String) {
        listaFiltrada.clear()

        // Si no hay texto, restauramos el listado completo de productos entregables previamente guardados
        if (query.isEmpty()) {
            listaFiltrada.addAll(listaOriginal)
        } else {
            val queryLower = query.lowercase()
            // Filtrado ignorando discrepancias de mayúsculas y minúsculas
            val filtrados = listaOriginal.filter { producto ->
                producto.nombre.lowercase().contains(queryLower)
            }
            listaFiltrada.addAll(filtrados)
        }

        // Notificar los cambios de volumen de datos al adaptador gráfico
        adapter.notifyDataSetChanged()

        // Controlar la visibilidad de carteles informativos si el filtro deja vacía la cuadrícula
        if (listaFiltrada.isEmpty()) {
            recycler.visibility = View.GONE
            txtVacio.visibility = View.VISIBLE
            txtVacio.text = if (query.isEmpty()) {
                "No hay productos entregables disponibles"
            } else {
                "No se encontraron productos entregables para \"$query\""
            }
        } else {
            recycler.visibility = View.VISIBLE
            txtVacio.visibility = View.GONE
        }
    }

    /**
     * Inicializa una petición de red GET estructurada a través de Volley.
     * Filtra los objetos de la respuesta JSON para almacenar exclusivamente aquellos que
     * posean la bandera "entregable" configurada en true.
     */
    private fun cargarProductosEntregables() {
        mostrarLoading(true)

        val queue = Volley.newRequestQueue(requireContext())

        // El endpoint devuelve una lista directa representada por un JsonArrayRequest
        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONArray ->
                try {
                    listaOriginal.clear()
                    listaFiltrada.clear()

                    // Iterar sobre los objetos JSON devueltos por la base de datos de Django
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)
                        val entregable = obj.optBoolean("entregable", false)

                        // REGLA DE NEGOCIO CLIENTE: SOLO incorporar el producto si es apto para entrega a domicilio
                        if (entregable) {
                            val producto = Producto(
                                id = obj.optInt("id", 0),
                                nombre = obj.optString("nombre", "Sin nombre"),
                                precio = obj.optDouble("precio", 0.0),
                                imagen = obj.optString("imagen", ""),
                                entregable = true
                            )
                            listaOriginal.add(producto)
                        }
                    }

                    mostrarLoading(false)

                    // Validar volumen de datos finales procesados para alternar componentes de la UI
                    if (listaOriginal.isEmpty()) {
                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                        txtVacio.text = "No hay productos entregables disponibles"
                    } else {
                        // Sincronizar estado inicial: Rellenar la lista del adaptador con la totalidad de entregables
                        listaFiltrada.addAll(listaOriginal)
                        adapter.notifyDataSetChanged()
                        recycler.visibility = View.VISIBLE
                        txtVacio.visibility = View.GONE
                    }

                } catch (e: Exception) {
                    mostrarLoading(false)
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error al cargar productos", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        )

        // Encolar la petición HTTP asíncrona para su posterior procesamiento en segundo plano
        queue.add(request)
    }

    /**
     * Alterna la visibilidad entre el componente de carga (ProgressBar) y los datos principales
     * para asegurar una experiencia de usuario limpia y libre de superposiciones visuales.
     *
     * @param mostrar true para activar el indicador de progreso en pantalla, false para restaurar las vistas.
     */
    private fun mostrarLoading(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recycler.visibility = if (mostrar) View.GONE else View.VISIBLE
        txtVacio.visibility = View.GONE
    }
}