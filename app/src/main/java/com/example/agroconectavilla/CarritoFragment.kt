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

class CarritoFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CarritoAdapter
    private val listaItems = mutableListOf<CarritoItem>()
    private lateinit var progressBar: View
    private lateinit var layoutVacio: LinearLayout
    private lateinit var layoutResumen: LinearLayout
    private lateinit var txtTotal: TextView
    private lateinit var txtCantidadItems: TextView
    private lateinit var btnContinuar: Button

    private val baseUrl: String= Constants.BASE_URL
    private var usuarioId: Int = -1

    companion object {
        private const val ARG_USUARIO_ID = "usuario_id"

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
        // Obtener el usuarioId de los argumentos
        usuarioId = arguments?.getInt(ARG_USUARIO_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_carrito, container, false)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        recycler = view.findViewById(R.id.recyclerCarrito)
        progressBar = view.findViewById(R.id.progressBar)
        layoutVacio = view.findViewById(R.id.layoutVacio)
        layoutResumen = view.findViewById(R.id.layoutResumen)
        txtTotal = view.findViewById(R.id.txtTotal)
        txtCantidadItems = view.findViewById(R.id.txtCantidadItems)
        btnContinuar = view.findViewById(R.id.btnContinuar)

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CarritoAdapter(
            listaItems,
            requireContext(),
            baseUrl,
            { item, nuevaCantidad -> actualizarCantidad(item, nuevaCantidad) },
            { item -> eliminarItem(item) }
        )
        recycler.adapter = adapter

        btnContinuar.setOnClickListener {
            Toast.makeText(requireContext(), "Finalizar pedido", Toast.LENGTH_LONG).show()
        }

        // Verificar que tenemos un usuario válido
        if (usuarioId == -1) {
            Toast.makeText(requireContext(), "Error: Usuario no identificado", Toast.LENGTH_LONG).show()
            return view
        }

        cargarCarrito()

        return view
    }

    private fun cargarCarrito() {
        mostrarLoading(true)

        val url = "$baseUrl"+"api/carrito/$usuarioId/"
        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
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

    private fun actualizarUIDesdeJSON(response: JSONObject) {
        listaItems.clear()

        val itemsArray = response.getJSONArray("items")
        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.getJSONObject(i)
            val productoObj = itemObj.getJSONObject("producto")

            val producto = Producto(
                id = productoObj.getInt("id"),
                nombre = productoObj.getString("nombre"),
                precio = productoObj.getDouble("precio"),
                imagen = if (productoObj.has("imagen") && !productoObj.isNull("imagen"))
                    productoObj.getString("imagen") else null,
                entregable = productoObj.getBoolean("entregable")
            )

            val item = CarritoItem(
                id = itemObj.getInt("id"),
                producto = producto,
                cantidad = itemObj.getInt("cantidad"),
                subtotal = itemObj.getDouble("subtotal"),
                agregado = itemObj.getString("agregado")
            )

            listaItems.add(item)
        }

        adapter.notifyDataSetChanged()

        val total = response.getDouble("total")
        val totalItems = response.getInt("total_items")

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

    private fun actualizarCantidad(item: CarritoItem, nuevaCantidad: Int) {
        mostrarLoading(true)

        val url = "$baseUrl"+"api/carrito/actualizar/${item.id}/"
        val queue = Volley.newRequestQueue(requireContext())

        val params = JSONObject()
        params.put("cantidad", nuevaCantidad)

        val request = JsonObjectRequest(
            Request.Method.PUT,
            url,
            params,
            { response ->
                try {
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

    private fun eliminarItem(item: CarritoItem) {
        mostrarLoading(true)

        val url = "$baseUrl"+"api/carrito/eliminar/${item.id}/"
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

    private fun mostrarLoading(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recycler.visibility = if (mostrar) View.GONE else recycler.visibility
        layoutResumen.visibility = if (mostrar) View.GONE else layoutResumen.visibility
        layoutVacio.visibility = if (mostrar) View.GONE else layoutVacio.visibility
    }
}