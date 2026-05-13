package com.example.agroconectavilla

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

abstract class BaseProductosFragment : Fragment() {

    protected lateinit var recycler: RecyclerView
    protected lateinit var adapter: ProductoAdapter
    protected val lista = mutableListOf<Producto>()
    protected lateinit var txtVacio: TextView
    protected lateinit var progressBar: View

    protected val url: String= Constants.BASE_URL + "api/productos/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_productos_base, container, false)

        recycler = view.findViewById(R.id.recyclerProductos)
        txtVacio = view.findViewById(R.id.txtVacio)
        progressBar = view.findViewById(R.id.progressBar)

        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ProductoAdapter(lista, requireContext())
        recycler.adapter = adapter

        cargarProductos()

        return view
    }

    protected fun cargarProductos() {
        Log.d("MI_URL", "URL FINAL: $url")
        mostrarLoading(true)
        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
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
                            entregable = obj.optBoolean("entregable", false) // ← CAMPO DE BD
                        )

                        // Filtrar según el tipo de fragmento
                        if (debeIncluirProducto(producto)) {
                            lista.add(producto)
                        }
                    }

                    mostrarLoading(false)

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
                    Toast.makeText(requireContext(), "Error al cargar productos", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    private fun mostrarLoading(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recycler.visibility = if (mostrar) View.GONE else View.VISIBLE
        txtVacio.visibility = View.GONE
    }

    // Métodos abstractos que cada fragmento debe implementar
    protected abstract fun debeIncluirProducto(producto: Producto): Boolean
    protected abstract fun obtenerMensajeVacio(): String
}