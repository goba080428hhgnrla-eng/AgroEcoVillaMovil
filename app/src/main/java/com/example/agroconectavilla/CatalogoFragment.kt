package com.example.agroconectavilla

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * Fragmento encargado de mostrar el catálogo de productos disponibles en la aplicación.
 * Consume un servicio web para listar los artículos y los presenta en una cuadrícula (Grid),
 * aplicando un filtro para mostrar únicamente aquellos productos marcados como entregables.
 */
class CatalogoFragment : Fragment() {

    // Componentes de la interfaz de usuario (UI)
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private val lista = mutableListOf<Producto>()
    private lateinit var txtVacio: TextView

    // Endpoint de la API para obtener el listado de productos
    private val url: String = Constants.BASE_URL + "api/productos/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el contenedor visual del fragmento del catálogo
        val view = inflater.inflate(R.layout.fragment_catalogo, container, false)

        // Inicialización de componentes visuales
        recycler = view.findViewById(R.id.recyclerProductos)
        txtVacio = view.findViewById(R.id.txtVacio)

        // Configuración de la cuadrícula del RecyclerView (2 columnas por fila)
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ProductoAdapter(lista, requireContext())
        recycler.adapter = adapter

        // Ejecutar la carga de datos desde el backend
        cargarProductos()

        return view
    }

    /**
     * Consume la API REST utilizando Volley mediante una petición GET.
     * Procesa el arreglo JSON recibido, filtra los productos que no son entregables y actualiza la UI.
     */
    private fun cargarProductos() {
        // Registro en log de la URL final para propósitos de depuración
        Log.d("MI_URL", "URL FINAL: $url")
        val queue = Volley.newRequestQueue(requireContext())

        // Petición de tipo JsonArrayRequest debido a que el endpoint devuelve una lista directa `[...]`
        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONArray ->
                try {
                    // Limpiar la lista previa para evitar duplicados al recargar
                    lista.clear()

                    // Iterar sobre el arreglo de objetos JSON devuelto por el servidor
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)
                        val entregable = obj.optBoolean("entregable", false)

                        // REGLA DE NEGOCIO: SOLO agregar el producto si cumple la condición de entregable
                        if (entregable) {
                            val producto = Producto(
                                id = obj.optInt("id", 0),
                                nombre = obj.optString("nombre", "Sin nombre"),
                                precio = obj.optDouble("precio", 0.0),
                                imagen = obj.optString("imagen", ""),
                                entregable = true
                            )
                            lista.add(producto)
                        }
                    }

                    // Evaluar el estado final de la lista para alternar componentes en la pantalla
                    if (lista.isEmpty()) {
                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                        txtVacio.text = "No hay productos entregables disponibles"
                    } else {
                        recycler.visibility = View.VISIBLE
                        txtVacio.visibility = View.GONE
                        // Notificar al adaptador que los datos cambiaron para refrescar la vista
                        adapter.notifyDataSetChanged()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error al cargar productos", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        )

        // Añadir la petición estructurada a la cola de Volley para su ejecución asíncrona
        queue.add(request)
    }
}