package mx.teknoeducativa.agroconectavilla

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import mx.teknoeducativa.agroconectavilla.adapter.CarritoAdapter
import mx.teknoeducativa.agroconectavilla.network.CarritoItem
import mx.teknoeducativa.agroconectavilla.network.Producto
import mx.teknoeducativa.agroconectavilla.utils.Constants
import org.json.JSONObject
import java.util.Locale

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

    private val baseUrl: String = Constants.BASE_URL
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
        val ctx = requireContext().applicationContext
        val nombreSharedPrefs = "${ctx.packageName}_preferences"
        val prefs = ctx.getSharedPreferences(nombreSharedPrefs, Context.MODE_PRIVATE)
        org.osmdroid.config.Configuration.getInstance().load(ctx, prefs)

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
            { item, nuevaCantidad -> actualizarCantidad(item, nuevaCantidad) },
            { item -> eliminarItem(item) }
        )
        recycler.adapter = adapter

        btnContinuar.setOnClickListener {
            finalizarPedido()
        }

        if (usuarioId == -1) {
            Toast.makeText(requireContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return view
        }

        cargarCarrito()
        return view
    }

    private fun cargarCarrito() {
        mostrarLoading(true)
        val url = "${baseUrl}api/carrito/$usuarioId/"
        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
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
                imagen = if (productoObj.has("imagen") && !productoObj.isNull("imagen")) productoObj.getString("imagen") else null,
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
            txtTotal.text = String.format(Locale.getDefault(), "$%.2f", total)
            txtCantidadItems.text = totalItems.toString()
        }
    }

    private fun actualizarCantidad(item: CarritoItem, nuevaCantidad: Int) {
        mostrarLoading(true)
        val url = "${baseUrl}api/carrito/actualizar/${item.id}/"
        val queue = Volley.newRequestQueue(requireContext())
        val params = JSONObject()
        params.put("cantidad", nuevaCantidad)

        val request = JsonObjectRequest(
            Request.Method.PUT, url, params,
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
        val url = "${baseUrl}api/carrito/eliminar/${item.id}/"
        val queue = Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(
            Request.Method.DELETE, url, null,
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
        if (mostrar) {
            progressBar.visibility = View.VISIBLE
            recycler.visibility = View.GONE
            layoutResumen.visibility = View.GONE
            layoutVacio.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            if (listaItems.isEmpty()) {
                recycler.visibility = View.GONE
                layoutResumen.visibility = View.GONE
                layoutVacio.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                layoutResumen.visibility = View.VISIBLE
                layoutVacio.visibility = View.GONE
            }
        }
    }

    private fun finalizarPedido() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        mostrarLoading(true)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                mostrarLoading(false)
                if (location != null) {
                    mostrarDialogoConfirmacionPedido(location.latitude, location.longitude)
                } else {
                    Toast.makeText(requireContext(), "No se pudo obtener la ubicación exacta. Usando ubicación base.", Toast.LENGTH_SHORT).show()
                    mostrarDialogoConfirmacionPedido(19.7294, -99.4601) // Villa del Carbón Centro
                }
            }.addOnFailureListener {
                mostrarLoading(false)
                Toast.makeText(requireContext(), "Error al activar el GPS", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            mostrarLoading(false)
            e.printStackTrace()
        }
    }

    private fun mostrarDialogoConfirmacionPedido(latitud: Double, longitud: Double) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val viewSheet = layoutInflater.inflate(R.layout.bottom_sheet_confirmar_pedido, null)
        bottomSheetDialog.setContentView(viewSheet)

        val mapView = viewSheet.findViewById<org.osmdroid.views.MapView>(R.id.mapViewConfirmar)
        val etDireccion = viewSheet.findViewById<EditText>(R.id.etDireccionConfirmar)
        val etReferencia = viewSheet.findViewById<EditText>(R.id.etReferenciaConfirmar)
        val btnConfirmar = viewSheet.findViewById<Button>(R.id.btnConfirmarPedidoFinal)

        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(17.5)

        val ubicacionCliente = org.osmdroid.util.GeoPoint(latitud, longitud)
        mapController.setCenter(ubicacionCliente)

        val marcador = org.osmdroid.views.overlay.Marker(mapView)
        marcador.position = ubicacionCliente
        marcador.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
        marcador.title = "Tu ubicación actual"
        mapView.overlays.add(marcador)

        btnConfirmar.setOnClickListener {
            val direccionEscrita = etDireccion.text.toString().trim()
            val referenciaEscrita = etReferencia?.text?.toString()?.trim() ?: "Sin referencias adicionales"

            if (direccionEscrita.isEmpty()) {
                etDireccion.error = "La dirección es obligatoria"
            } else {
                bottomSheetDialog.dismiss()
                enviarPedidoServidor(latitud, longitud, direccionEscrita, referenciaEscrita)
            }
        }

        bottomSheetDialog.setOnDismissListener { mapView.onDetach() }
        bottomSheetDialog.show()
    }

    private fun enviarPedidoServidor(latitud: Double, longitud: Double, direccion: String, referencia: String) {
        mostrarLoading(true)
        val url = "${baseUrl}api/pedidos/crear/"
        val queue = Volley.newRequestQueue(requireContext())
        val params = JSONObject()

        try {
            params.put("usuario_id", usuarioId)
            params.put("direccion", direccion)
            params.put("referencia", referencia)
            params.put("latitud", latitud)
            params.put("longitud", longitud)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            { response ->
                try {
                    mostrarLoading(false)
                    if (response.has("status") && response.getString("status") == "ok") {
                        Toast.makeText(requireContext(), "¡Pedido realizado con éxito! Buscando repartidor...", Toast.LENGTH_LONG).show()

                        listaItems.clear()
                        adapter.notifyDataSetChanged()

                        cargarCarrito()
                    } else {
                        val msg = if (response.has("message")) response.getString("message") else "Error de procesamiento"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()

                val networkResponse = error.networkResponse
                if (networkResponse != null) {
                    val errorData = String(networkResponse.data)
                    Log.e("VOLLEY_DJANGO_ERR", "Código: ${networkResponse.statusCode} - Detalle: $errorData")
                    try {
                        val errorJson = JSONObject(errorData)
                        if (errorJson.has("message")) {
                            Toast.makeText(requireContext(), errorJson.getString("message"), Toast.LENGTH_LONG).show()
                            return@JsonObjectRequest
                        }
                    } catch (parseEx: Exception) {
                        parseEx.printStackTrace()
                    }
                }
                Toast.makeText(requireContext(), "Error al procesar la orden en el servidor", Toast.LENGTH_LONG).show()
            }
        )
        queue.add(request)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            finalizarPedido()
        } else {
            Toast.makeText(requireContext(), "Se requiere el permiso de ubicación para calcular el envío", Toast.LENGTH_LONG).show()
        }
    }
}