package com.example.agroconectavilla

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.adapter.FavoritosAdapter
import com.example.agroconectavilla.network.Favorito
import com.example.agroconectavilla.network.Producto
import com.example.agroconectavilla.utils.Constants

class FavoritosFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FavoritosAdapter
    private val listaFavoritos = mutableListOf<Favorito>()
    private lateinit var txtVacio: TextView
    private lateinit var progressBar: View
    private lateinit var toolbar: Toolbar
    private val baseUrl: String = Constants.BASE_URL
    private var usuarioId: Int = -1

    companion object {
        private const val ARG_USUARIO_ID = "usuario_id"
        fun newInstance(usuarioId: Int): FavoritosFragment {
            val fragment = FavoritosFragment()
            val args = Bundle()
            args.putInt(ARG_USUARIO_ID, usuarioId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usuarioId = arguments?.getInt(ARG_USUARIO_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_favoritos, container, false)
        toolbar = view.findViewById(R.id.toolbarFavoritos)
        recycler = view.findViewById(R.id.recyclerFavoritos)
        txtVacio = view.findViewById(R.id.txtVacio)
        progressBar = view.findViewById(R.id.progressBar)

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        recycler.layoutManager = GridLayoutManager(requireContext(), 1)
        adapter = FavoritosAdapter(listaFavoritos, requireContext(), { favorito ->
            eliminarFavorito(favorito)
        })
        recycler.adapter = adapter

        cargarFavoritos()
        return view
    }

    private fun cargarFavoritos() {
        mostrarLoading(true)
        val url = "${baseUrl}api/favoritos/$usuarioId/"
        val queue = Volley.newRequestQueue(requireContext())
        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    listaFavoritos.clear()
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)
                        val productoObj = obj.getJSONObject("producto")
                        val producto = Producto(
                            id = productoObj.getInt("id"),
                            nombre = productoObj.getString("nombre"),
                            precio = productoObj.getDouble("precio"),
                            imagen = if (productoObj.has("imagen") && !productoObj.isNull("imagen"))
                                productoObj.getString("imagen") else null,
                            entregable = productoObj.getBoolean("entregable")
                        )
                        val favorito = Favorito(
                            id = obj.getInt("id"),
                            producto = producto,
                            fecha_agregado = obj.getString("fecha_agregado")
                        )
                        listaFavoritos.add(favorito)
                    }
                    mostrarLoading(false)
                    if (listaFavoritos.isEmpty()) {
                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                    } else {
                        recycler.visibility = View.VISIBLE
                        txtVacio.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    mostrarLoading(false)
                    e.printStackTrace()
                }
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()
            }
        )
        queue.add(request)
    }

    private fun eliminarFavorito(favorito: Favorito) {
        mostrarLoading(true)
        val url = "${baseUrl}api/favoritos/eliminar/${favorito.id}/"
        val queue = Volley.newRequestQueue(requireContext())
        val request = JsonObjectRequest(
            Request.Method.DELETE, url, null,
            { response ->
                try {
                    if (response.getString("status") == "ok") {
                        listaFavoritos.remove(favorito)
                        adapter.notifyDataSetChanged()

                        if (listaFavoritos.isEmpty()) {
                            recycler.visibility = View.GONE
                            txtVacio.visibility = View.VISIBLE
                        } else {
                            recycler.visibility = View.VISIBLE
                            txtVacio.visibility = View.GONE
                        }

                        Toast.makeText(requireContext(), "Eliminado", Toast.LENGTH_SHORT).show()
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
            }
        )
        queue.add(request)
    }

    private fun mostrarLoading(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        if (!mostrar) {
            if (listaFavoritos.isEmpty()) {
                recycler.visibility = View.GONE
                txtVacio.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                txtVacio.visibility = View.GONE
            }
        }
    }
}