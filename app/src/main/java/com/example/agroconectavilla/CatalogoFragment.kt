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

class CatalogoFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private val lista = mutableListOf<Producto>()
    private lateinit var txtVacio: TextView

   private val url: String = Constants.BASE_URL + "api/productos/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_catalogo, container, false)

        recycler = view.findViewById(R.id.recyclerProductos)
        txtVacio = view.findViewById(R.id.txtVacio)

        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ProductoAdapter(lista, requireContext())
        recycler.adapter = adapter

        cargarProductos()

        return view
    }

    private fun cargarProductos() {
        Log.d("MI_URL", "URL FINAL: $url")
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
                        val entregable = obj.optBoolean("entregable", false)

                        // SOLO agregar si es entregable
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

                    if (lista.isEmpty()) {
                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                        txtVacio.text = "No hay productos entregables disponibles"
                    } else {
                        recycler.visibility = View.VISIBLE
                        txtVacio.visibility = View.GONE
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

        queue.add(request)
    }
}