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

class InicioFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private val listaOriginal = mutableListOf<Producto>()
    private val listaFiltrada = mutableListOf<Producto>()
    private lateinit var txtVacio: TextView
    private lateinit var progressBar: View
    private lateinit var buscador: EditText

    private val url: String= Constants.BASE_URL + "api/productos/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        recycler = view.findViewById(R.id.recyclerProductos)
        txtVacio = view.findViewById(R.id.txtVacio)
        progressBar = view.findViewById(R.id.progressBar)
        buscador = view.findViewById(R.id.buscador)

        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ProductoAdapter(listaFiltrada, requireContext())
        recycler.adapter = adapter

        cargarProductos()

        // Configurar buscador
        configurarBuscador()

        return view
    }

    private fun configurarBuscador() {
        buscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarProductos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarProductos(query: String) {
        listaFiltrada.clear()

        if (query.isEmpty()) {
            listaFiltrada.addAll(listaOriginal)
        } else {
            val queryLower = query.lowercase()
            val filtrados = listaOriginal.filter { producto ->
                producto.nombre.lowercase().contains(queryLower)
            }
            listaFiltrada.addAll(filtrados)
        }

        adapter.notifyDataSetChanged()

        // Mostrar mensaje si no hay resultados
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

    private fun cargarProductos() {
        Log.d("MI_URL", "URL FINAL: $url")
        mostrarLoading(true)

        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONArray ->
                try {
                    listaOriginal.clear()
                    listaFiltrada.clear()

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

                    if (listaOriginal.isEmpty()) {
                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                        txtVacio.text = "No hay productos disponibles"
                    } else {
                        // Cargar todos los productos inicialmente
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

    private fun mostrarLoading(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recycler.visibility = if (mostrar) View.GONE else View.VISIBLE
        txtVacio.visibility = View.GONE
    }
}