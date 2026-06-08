package mx.teknoeducativa.agroconectavilla

import android.content.Intent
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
import mx.teknoeducativa.agroconectavilla.adapter.PedidoAdapter
import mx.teknoeducativa.agroconectavilla.network.Pedido
import mx.teknoeducativa.agroconectavilla.utils.Constants
import android.widget.TextView
import android.widget.Button
import com.bumptech.glide.Glide
import com.android.volley.toolbox.JsonArrayRequest

class PedidosFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: PedidoAdapter
    private val listaPedidos = mutableListOf<Pedido>()
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutVacio: LinearLayout
    private var usuarioId: Int = -1

    // Estados que permiten seguimiento en tiempo real
    private val estadosConSeguimiento = listOf("aceptado", "en_camino", "recolectado", "asignado")
    // Estados finales (solo mostrar detalle)
    private val estadosFinales = listOf("entregado", "cancelado")

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

        toolbar.setNavigationOnClickListener {
            if (isAdded) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = PedidoAdapter(listaPedidos, requireContext()) { pedidoSeleccionado ->
            // Decidir qué acción tomar según el estado del pedido
            when {
                estadosConSeguimiento.contains(pedidoSeleccionado.estado) -> {
                    // Pedido activo - abrir seguimiento en tiempo real
                    abrirSeguimientoEnTiempoReal(pedidoSeleccionado)
                }
                estadosFinales.contains(pedidoSeleccionado.estado) -> {
                    // Pedido finalizado - mostrar detalle
                    verDetalleDelPedido(pedidoSeleccionado)
                }
                else -> {
                    // Estado pendiente o desconocido - mostrar detalle simple
                    verDetalleDelPedido(pedidoSeleccionado)
                }
            }
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

    private fun abrirSeguimientoEnTiempoReal(pedido: Pedido) {
        val intent = Intent(requireContext(), SeguimientoClienteActivity::class.java).apply {
            putExtra("pedido_id", pedido.id.toString())
            putExtra("pedido_estado", pedido.estado)
        }
        startActivity(intent)
    }

    private fun cargarHistorialPedidos() {
        cambiarEstadoCarga(true)

        val url = "${Constants.BASE_URL}api/pedidos/cliente/$usuarioId/"
        Log.d("URL_FINAL", url)

        val queue = Volley.newRequestQueue(requireContext().applicationContext)

        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (isAdded) {
                    try {
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
                                telefono_cliente = if (obj.isNull("telefono_cliente")) "Sin telefono" else obj.getString("telefono_cliente"),
                                repartidor_nombre = if (obj.isNull("repartidor_nombre")) "No asignado" else obj.getString("repartidor_nombre"),
                                imagen = if (obj.isNull("imagen")) "" else obj.getString("imagen")
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
                        Toast.makeText(
                            requireContext(),
                            "Error procesando pedidos",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    cambiarEstadoCarga(false)
                }
            },
            { error ->
                if (isAdded) {
                    cambiarEstadoCarga(false)
                    error.printStackTrace()

                    val mensaje = when {
                        error.networkResponse != null -> {
                            val codigo = error.networkResponse.statusCode
                            "Error HTTP: $codigo"
                        }
                        error is com.android.volley.TimeoutError -> "Timeout del servidor"
                        error is com.android.volley.NoConnectionError -> "Sin conexion al servidor"
                        error is com.android.volley.AuthFailureError -> "Error de autenticacion"
                        error is com.android.volley.ServerError -> "Error interno del servidor"
                        error is com.android.volley.NetworkError -> "Error de red"
                        error is com.android.volley.ParseError -> "Error parseando JSON"
                        else -> "Error desconocido: ${error.message}"
                    }

                    Toast.makeText(
                        requireContext(),
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }
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
        if (!isAdded) return

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
        val btnSeguimiento = dialogView.findViewById<Button>(R.id.btnSeguimientoPedido)

        txtPedidoId.text = "Pedido #${pedido.id}"

        // Mostrar estado con formato legible
        val estadoFormateado = when (pedido.estado) {
            "pendiente" -> "Pendiente de asignacion"
            "aceptado" -> "Aceptado - Repartidor asignado"
            "en_camino" -> "En camino al mercado"
            "recolectado" -> "Productos recolectados - En camino a ti"
            "entregado" -> "Entregado"
            "cancelado" -> "Cancelado"
            else -> pedido.estado
        }
        txtEstado.text = "Estado: $estadoFormateado"
        txtFecha.text = "Fecha: ${pedido.fecha}"
        txtDireccion.text = "Direccion: ${pedido.direccion_entrega}"
        txtCliente.text = "Cliente: ${pedido.nombre_cliente}"
        txtTelefono.text = "Telefono: ${pedido.telefono_cliente}"
        txtTotal.text = "Total: $${pedido.total}"

        // Mostrar boton de seguimiento solo si el pedido esta activo
        if (estadosConSeguimiento.contains(pedido.estado)) {
            btnSeguimiento.visibility = View.VISIBLE
            btnSeguimiento.text = "Ver en mapa"
            btnSeguimiento.setOnClickListener {
                dialog.dismiss()
                abrirSeguimientoEnTiempoReal(pedido)
            }
        } else {
            btnSeguimiento.visibility = View.GONE
        }

        var urlImagenCompleta = pedido.imagen

        if (urlImagenCompleta.isNotEmpty()) {
            if (urlImagenCompleta.startsWith("/")) {
                val baseClean = if (Constants.BASE_URL.endsWith("/")) Constants.BASE_URL.dropLast(1) else Constants.BASE_URL
                urlImagenCompleta = baseClean + urlImagenCompleta
            } else if (!urlImagenCompleta.startsWith("http")) {
                val baseClean = if (Constants.BASE_URL.endsWith("/")) Constants.BASE_URL else "${Constants.BASE_URL}/"
                urlImagenCompleta = baseClean + urlImagenCompleta
            }
        }

        Glide.with(requireContext())
            .load(urlImagenCompleta.ifEmpty { null })
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(imgProducto)

        dialog.show()
    }

    private fun cambiarEstadoCarga(cargando: Boolean) {
        if (isAdded) {
            if (cargando) {
                progressBar.visibility = View.VISIBLE
                recycler.visibility = View.GONE
                layoutVacio.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
            }
        }
    }
}