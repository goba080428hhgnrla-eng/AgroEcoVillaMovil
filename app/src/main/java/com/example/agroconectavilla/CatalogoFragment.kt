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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.adapter.ProductoAdapter
import com.example.agroconectavilla.network.Producto
import com.example.agroconectavilla.utils.Constants

class CatalogoFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private val lista = mutableListOf<Producto>()
    private lateinit var txtVacio: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_catalogo, container, false)

        recycler = view.findViewById(R.id.recyclerProductos)
        txtVacio = view.findViewById(R.id.txtVacio)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ProductoAdapter(lista, requireContext())
        recycler.adapter = adapter

        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark
        )

        swipeRefresh.setOnRefreshListener {
            cargarProductos()
        }

        cargarProductos()
        return view
    }

    private fun cargarProductos() {
        val url = Constants.BASE_URL + "api/productos/"

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    lista.clear()
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)
                        if (obj.optBoolean("entregable", false)) {
                            lista.add(Producto(
                                id = obj.optInt("id"),
                                nombre = obj.optString("nombre"),
                                precio = obj.optDouble("precio"),
                                imagen = obj.optString("imagen"),
                                entregable = true
                            ))
                        }
                    }

                    if (lista.isEmpty()) {
                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                    } else {
                        recycler.visibility = View.VISIBLE
                        txtVacio.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    swipeRefresh.isRefreshing = false
                }
            },
            { error ->
                swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }
}