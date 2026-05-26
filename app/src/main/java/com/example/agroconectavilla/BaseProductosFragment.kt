package com.example.agroconectavilla

// Importaciones
import android.os.Bundle
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
import android.util.Log

// Fragment base productos
abstract class BaseProductosFragment : Fragment() {

    // RecyclerView
    protected lateinit var recycler: RecyclerView

    // Adaptador
    protected lateinit var adapter: ProductoAdapter

    // Lista productos
    protected val lista = mutableListOf<Producto>()

    // Texto vacío
    protected lateinit var txtVacio: TextView

    // Loading
    protected lateinit var progressBar: View

    // URL productos
    protected val url: String= Constants.BASE_URL + "api/productos/"

    // Crear vista
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_productos_base, container, false)

        recycler = view.findViewById(R.id.recyclerProductos)
        txtVacio = view.findViewById(R.id.txtVacio)
        progressBar = view.findViewById(R.id.progressBar)

        // Grid 2 columnas
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)

        // Adaptador
        adapter = ProductoAdapter(lista, requireContext())
        recycler.adapter = adapter

        // Cargar productos
        cargarProductos()

        return view
    }

    // Obtener productos
    protected fun cargarProductos() {

        Log.d("MI_URL", "URL FINAL: $url")

        mostrarLoading(true)

        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,

            // Respuesta servidor
            { response: JSONArray ->

                try {

                    lista.clear()

                    for (i in 0 until response.length()) {

                        val obj = response.getJSONObject(i)

                        val producto = Producto(
                            id = obj.optInt("id", 0),
                            nombre = obj.optString("nombre", "Sin nombre"),
                            precio = obj.optDouble("precio", 0.0),
                            imagen = obj.optString("imagen", null),
                            entregable = obj.optBoolean("entregable", false)
                        )

                        // Filtrar productos
                        if (debeIncluirProducto(producto)) {
                            lista.add(producto)
                        }
                    }

                    mostrarLoading(false)

                    // Lista vacía
                    if (lista.isEmpty()) {

                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                        txtVacio.text = obtenerMensajeVacio()

                    } else {

                        recycler.visibility = View.VISIBLE
                        txtVacio.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    }

                } catch (e: Exception) {

                    mostrarLoading(false)
                    e.printStackTrace()

                    Toast.makeText(
                        requireContext(),
                        "Error al cargar productos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },

            // Error conexión
            { error ->

                mostrarLoading(false)
                error.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "Error de conexión con el servidor",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }

    // Mostrar loading
    private fun mostrarLoading(mostrar: Boolean) {

        progressBar.visibility =
            if (mostrar) View.VISIBLE else View.GONE

        recycler.visibility =
            if (mostrar) View.GONE else View.VISIBLE

        txtVacio.visibility = View.GONE
    }

    // Filtrar producto
    protected abstract fun debeIncluirProducto(producto: Producto): Boolean

    // Mensaje vacío
    protected abstract fun obtenerMensajeVacio(): String
}