package com.example.agroconectavilla

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.app.AlertDialog
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.example.agroconectavilla.adapter.PedidoAdapter
import com.example.agroconectavilla.network.Pedido
import com.example.agroconectavilla.utils.Constants
import android.widget.TextView
import com.bumptech.glide.Glide
import com.android.volley.toolbox.JsonArrayRequest

class PedidosFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: PedidoAdapter
    private val listaPedidos = mutableListOf<Pedido>()
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutVacio: LinearLayout
    private var usuarioId: Int = -1

    companion object {
        private const val ARG_USUARIO_ID = "usuario_id"

        fun newInstance(usuarioId: Int): PedidosFragment {
            val fragment = PedidosFragment()
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

        val view = inflater.inflate(R.layout.fragment_pedidos, container, false)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbarPedidos)
        recycler = view.findViewById(R.id.recyclerPedidosUsuario)
        progressBar = view.findViewById(R.id.progressPedidos)
        layoutVacio = view.findViewById(R.id.layoutPedidosVacio)

        // Botón regresar
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // RecyclerView
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = PedidoAdapter(listaPedidos, requireContext()) { pedidoSeleccionado ->
            verDetalleDelPedido(pedidoSeleccionado)
        }

        recycler.adapter = adapter

        if (usuarioId == -1) {
            Toast.makeText(
                requireContext(),
                "Error: Usuario no identificado",
                Toast.LENGTH_SHORT
            ).show()

            layoutVacio.visibility = View.VISIBLE

        } else {
            cargarHistorialPedidos()
        }

        return view
    }

    private fun cargarHistorialPedidos() {

        cambiarEstadoCarga(true)

        val url = "${Constants.BASE_URL}"+"api/pedidos/cliente/$usuarioId/"
        Log.d("URL_FINAL", url)

        Log.d("PEDIDOS_API", "URL: $url")

        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,

            { response ->

                try {

                    listaPedidos.clear()

                    Log.d("PEDIDOS_JSON", response.toString())

                    for (i in 0 until response.length()) {

                        val obj = response.getJSONObject(i)

                        // Obtener la primera imagen del primer item
                        val items = obj.getJSONArray("items")
                        val primerItem = items.getJSONObject(0)
                        val producto = primerItem.getJSONObject("producto")
                        val imagenProducto = producto.getString("imagen")

                        val pedido = Pedido(
                            id = obj.getInt("id"),
                            estado = obj.getString("estado"),
                            total = obj.getDouble("total"),
                            fecha = obj.getString("creado"),
                            direccion_entrega = obj.getString("direccion"),
                            nombre_cliente = "Cliente",
                            telefono_cliente = "Sin teléfono",
                            repartidor_nombre = if (obj.isNull("repartidor")) null else obj.getString("repartidor"),
                            imagen = imagenProducto  // ← Este es el campo que faltaba
                        )

                        listaPedidos.add(pedido)
                    }

                    adapter.notifyDataSetChanged()

                    if (listaPedidos.isEmpty()) {
                        layoutVacio.visibility = View.VISIBLE
                        recycler.visibility = View.GONE
                    } else {
                        layoutVacio.visibility = View.GONE
                        recycler.visibility = View.VISIBLE
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("PEDIDOS_ERROR", e.toString())
                    Toast.makeText(
                        requireContext(),
                        "Error procesando pedidos",
                        Toast.LENGTH_LONG
                    ).show()
                }

                cambiarEstadoCarga(false)

            },

            { error ->

                cambiarEstadoCarga(false)

                error.printStackTrace()

                val mensaje = when {

                    error.networkResponse != null -> {

                        val codigo = error.networkResponse.statusCode

                        "Error HTTP: $codigo"
                    }

                    error is com.android.volley.TimeoutError -> {
                        "Timeout del servidor"
                    }

                    error is com.android.volley.NoConnectionError -> {
                        "Sin conexión al servidor"
                    }

                    error is com.android.volley.AuthFailureError -> {
                        "Error de autenticación"
                    }

                    error is com.android.volley.ServerError -> {
                        "Error interno del servidor"
                    }

                    error is com.android.volley.NetworkError -> {
                        "Error de red"
                    }

                    error is com.android.volley.ParseError -> {
                        "Error parseando JSON"
                    }

                    else -> {
                        "Error desconocido: ${error.message}"
                    }
                }

                Log.e("VOLLEY_ERROR", mensaje)

                Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        request.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        queue.add(request)
    }

    private fun verDetalleDelPedido(pedido: Pedido) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_detalle_pedido, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val imgProducto = dialogView.findViewById<ImageView>(R.id.imgProductoDetalle)
        val txtPedidoId = dialogView.findViewById<TextView>(R.id.txtPedidoId)
        val txtEstado = dialogView.findViewById<TextView>(R.id.txtEstado)
        val txtFecha = dialogView.findViewById<TextView>(R.id.txtFecha)
        val txtDireccion = dialogView.findViewById<TextView>(R.id.txtDireccion)
        val txtCliente = dialogView.findViewById<TextView>(R.id.txtCliente)
        val txtTelefono = dialogView.findViewById<TextView>(R.id.txtTelefono)
        val txtTotal = dialogView.findViewById<TextView>(R.id.txtTotal)

        txtPedidoId.text = "Pedido #${pedido.id}"
        txtEstado.text = "Estado: ${pedido.estado}"
        txtFecha.text = "Fecha: ${pedido.fecha}"
        txtDireccion.text = "Dirección: ${pedido.direccion_entrega}"
        txtCliente.text = "Cliente: ${pedido.nombre_cliente}"
        txtTelefono.text = "Teléfono: ${pedido.telefono_cliente}"
        txtTotal.text = "Total: $${pedido.total}"

        Glide.with(requireContext())
            .load(pedido.imagen)
            .into(imgProducto)

        dialog.show()
    }


    private fun cambiarEstadoCarga(cargando: Boolean) {

        if (cargando) {

            progressBar.visibility = View.VISIBLE
            recycler.visibility = View.GONE
            layoutVacio.visibility = View.GONE

        } else {

            progressBar.visibility = View.GONE
        }
    }
}