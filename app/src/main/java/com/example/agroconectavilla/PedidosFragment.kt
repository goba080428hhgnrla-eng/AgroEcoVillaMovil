package com.example.agroconectavilla

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.adapter.PedidoAdapter
import com.example.agroconectavilla.network.Pedido
import com.example.agroconectavilla.utils.Constants

class PedidosFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: PedidoAdapter

    private val listaPedidos = mutableListOf<Pedido>()

    private val baseUrl = Constants.BASE_URL

    private var usuarioId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usuarioId = arguments?.getInt("usuario_id") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_pedidos,
            container,
            false
        )

        recycler = view.findViewById(R.id.recyclerPedidos)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = PedidoAdapter(
            listaPedidos,
            requireContext()
        ) {

        }

        recycler.adapter = adapter

        cargarPedidos()

        return view
    }

    private fun cargarPedidos() {

        val url = "$baseUrl/api/pedidos/$usuarioId/"

        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response ->

                listaPedidos.clear()

                for (i in 0 until response.length()) {

                    val obj = response.getJSONObject(i)

                    val pedido = Pedido(
                        id = obj.getInt("id"),
                        estado = obj.getString("estado"),
                        total = obj.getDouble("total"),
                        fecha = obj.getString("fecha"),
                        direccion_entrega = obj.getString("direccion_entrega"),
                        nombre_cliente = obj.getString("nombre_cliente"),
                        telefono_cliente = obj.getString("telefono_cliente"),
                        repartidor_nombre = if (
                            obj.isNull("repartidor_nombre")
                        ) null else obj.getString("repartidor_nombre")
                    )

                    listaPedidos.add(pedido)
                }

                adapter.notifyDataSetChanged()
            },
            {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar pedidos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }
}
