package com.example.agroconectavilla

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
 * Fragmento principal de la aplicación (Pantalla de Inicio).
 * Se encarga de descargar el catálogo completo de productos desde el servidor,
 * mostrarlos en una cuadrícula (Grid) y ofrecer un sistema de búsqueda y filtrado dinámico
 * en tiempo real a medida que el usuario escribe en la barra de búsqueda.
 */
class InicioFragment : Fragment() {

    // Componentes de la interfaz de usuario (UI)
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private val listaOriginal = mutableListOf<Producto>() // Respaldo inmutable local de los productos
    private val listaFiltrada = mutableListOf<Producto>() // Lista mutada que consume el adaptador
    private lateinit var txtVacio: TextView
    private lateinit var progressBar: View
    private lateinit var buscador: EditText

    // Endpoint de la API para obtener los productos
    private val url: String = Constants.BASE_URL + "api/productos/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el contenedor de la pantalla de inicio
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        // Inicialización de las vistas del XML
        recycler = view.findViewById(R.id.recyclerProductos)
        txtVacio = view.findViewById(R.id.txtVacio)
        progressBar = view.findViewById(R.id.progressBar)
        buscador = view.findViewById(R.id.buscador)

        // Configuración de la cuadrícula del RecyclerView (2 columnas)
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        // El adaptador apunta a 'listaFiltrada' para que los cambios de búsqueda se reflejen de inmediato
        adapter = ProductoAdapter(listaFiltrada, requireContext())
        recycler.adapter = adapter

        // Descargar los productos desde el servidor remoto
        cargarProductos()

        // Inicializar el escuchador de eventos de la barra de búsqueda
        configurarBuscador()

        return view
    }

    /**
     * Vincula un objeto [TextWatcher] al campo de texto del buscador para interceptar
     * los cambios en la entrada de texto y ejecutar la lógica de filtrado de manera síncrona.
     */
    private fun configurarBuscador() {
        buscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Invocar el filtro con el texto actual del input
                filtrarProductos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Filtra la colección original de productos comparando el término de búsqueda con el nombre.
     * Modifica la visibilidad de la UI si la búsqueda no arroja coincidencias.
     *
     * @param query Palabra o cadena de texto ingresada por el usuario.
     */
    private fun filtrarProductos(query: String) {
        listaFiltrada.clear()

        // Si el buscador está vacío, volvemos a mostrar la totalidad de los productos descargados
        if (query.isEmpty()) {
            listaFiltrada.addAll(listaOriginal)
        } else {
            val queryLower = query.lowercase()
            // Filtrar ignorando mayúsculas/minúsculas
            val filtrados = listaOriginal.filter { producto ->
                producto.nombre.lowercase().contains(queryLower)
            }
            listaFiltrada.addAll(filtrados)
        }

        // Refrescar los elementos visuales del RecyclerView
        adapter.notifyDataSetChanged()

        // Gestión de estados visuales alternativos si la búsqueda queda en cero
        if (listaFiltrada.isEmpty()) {
            recycler.visibility = View.GONE
            txtVacio.visibility = View.VISIBLE
            txtVacio.text = if (query.isEmpty()) {
                "No hay productos disponibles"
            } else {
                "No se encontraron productos para \"$query\""
            }
        } else {
            recycler.visibility = View.VISIBLE
            txtVacio.visibility = View.GONE
        }
    }

    /**
     * Consume el servicio web mediante una petición GET para inicializar el catálogo del fragmento.
     * Almacena los resultados en el búfer local original y clona la información a la lista filtrada.
     */
    private fun cargarProductos() {
        Log.d("MI_URL", "URL FINAL: $url")
        mostrarLoading(true)

        val queue = Volley.newRequestQueue(requireContext())

        // Se inicializa JsonArrayRequest para interpretar un listado plano de productos del API
        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONArray ->
                try {
                    listaOriginal.clear()
                    listaFiltrada.clear()

                    // Deserealizar cada elemento del JSONArray a objetos Producto utilizando valores por defecto seguro (opt)
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)
                        val producto = Producto(
                            id = obj.optInt("id", 0),
                            nombre = obj.optString("nombre", "Sin nombre"),
                            precio = obj.optDouble("precio", 0.0),
                            imagen = obj.optString("imagen", ""),
                            entregable = obj.optBoolean("entregable", false)
                        )
                        listaOriginal.add(producto)
                    }

                    mostrarLoading(false)

                    // Controlar la UI dependiendo de si el servidor retornó o no información
                    if (listaOriginal.isEmpty()) {
                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                        txtVacio.text = "No hay productos disponibles"
                    } else {
                        // Sincronizar el estado inicial: poblar la lista del adaptador con todos los elementos
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

        queue.add(request)
    }

    /**
     * Alterna de manera coordinada los estados de visibilidad de los componentes
     * para mitigar interacciones accidentales mientras se ejecutan procesos en red.
     *
     * @param mostrar true para activar la barra de progreso y ocultar la cuadrícula; false en caso inverso.
     */
    private fun mostrarLoading(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recycler.visibility = if (mostrar) View.GONE else View.VISIBLE
        txtVacio.visibility = View.GONE
    }
}