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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

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

        usuarioId =
            arguments?.getInt(ARG_USUARIO_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_carrito,
            container,
            false
        )

        val toolbar =
            view.findViewById<Toolbar>(R.id.toolbar)

        recycler =
            view.findViewById(R.id.recyclerCarrito)

        progressBar =
            view.findViewById(R.id.progressBar)

        layoutVacio =
            view.findViewById(R.id.layoutVacio)

        layoutResumen =
            view.findViewById(R.id.layoutResumen)

        txtTotal =
            view.findViewById(R.id.txtTotal)

        txtCantidadItems =
            view.findViewById(R.id.txtCantidadItems)

        btnContinuar =
            view.findViewById(R.id.btnContinuar)

        toolbar.setNavigationOnClickListener {

            requireActivity().onBackPressed()
        }

        recycler.layoutManager =
            LinearLayoutManager(requireContext())

        adapter = CarritoAdapter(

            listaItems,

            requireContext(),

            { item, nuevaCantidad ->

                actualizarCantidad(
                    item,
                    nuevaCantidad
                )
            },

            { item ->

                eliminarItem(item)
            }
        )

        recycler.adapter = adapter

        btnContinuar.setOnClickListener {
            finalizarPedido()
        }

        if (usuarioId == -1) {

            Toast.makeText(
                requireContext(),
                "Error: Usuario no identificado",
                Toast.LENGTH_SHORT
            ).show()

            return view
        }

        cargarCarrito()

        return view
    }

    private fun cargarCarrito() {

        mostrarLoading(true)

        val url =
            "${baseUrl}api/carrito/$usuarioId/"

        val queue =
            Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(

            Request.Method.GET,

            url,

            null,

            { response ->

                try {

                    if (
                        response.getString("status")
                        == "ok"
                    ) {

                        actualizarUIDesdeJSON(response)

                    } else {

                        Toast.makeText(
                            requireContext(),
                            response.getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    mostrarLoading(false)

                } catch (e: Exception) {

                    mostrarLoading(false)

                    e.printStackTrace()

                    Toast.makeText(
                        requireContext(),
                        "Error al cargar el carrito",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },

            { error ->

                mostrarLoading(false)

                error.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }

    private fun actualizarUIDesdeJSON(
        response: JSONObject
    ) {

        listaItems.clear()

        val itemsArray =
            response.getJSONArray("items")

        for (i in 0 until itemsArray.length()) {

            val itemObj =
                itemsArray.getJSONObject(i)

            val productoObj =
                itemObj.getJSONObject("producto")

            val producto = Producto(

                id =
                    productoObj.getInt("id"),

                nombre =
                    productoObj.getString("nombre"),

                precio =
                    productoObj.getDouble("precio"),

                imagen =
                    if (
                        productoObj.has("imagen")
                        &&
                        !productoObj.isNull("imagen")
                    )
                        productoObj.getString("imagen")
                    else null,

                entregable =
                    productoObj.getBoolean(
                        "entregable"
                    )
            )

            val item = CarritoItem(

                id =
                    itemObj.getInt("id"),

                producto = producto,

                cantidad =
                    itemObj.getInt("cantidad"),

                subtotal =
                    itemObj.getDouble("subtotal"),

                agregado =
                    itemObj.getString("agregado")
            )

            listaItems.add(item)
        }

        adapter.notifyDataSetChanged()

        val total =
            response.getDouble("total")

        val totalItems =
            response.getInt("total_items")

        if (listaItems.isEmpty()) {

            recycler.visibility = View.GONE

            layoutResumen.visibility = View.GONE

            layoutVacio.visibility = View.VISIBLE

        } else {

            recycler.visibility = View.VISIBLE

            layoutResumen.visibility = View.VISIBLE

            layoutVacio.visibility = View.GONE

            txtTotal.text = "$${total}"

            txtCantidadItems.text =
                totalItems.toString()
        }
    }

    private fun actualizarCantidad(
        item: CarritoItem,
        nuevaCantidad: Int
    ) {

        mostrarLoading(true)

        val url =
            "${baseUrl}api/carrito/actualizar/${item.id}/"

        val queue =
            Volley.newRequestQueue(requireContext())

        val params = JSONObject()

        params.put("cantidad", nuevaCantidad)

        val request = JsonObjectRequest(

            Request.Method.PUT,

            url,

            params,

            { response ->

                try {

                    if (
                        response.getString("status")
                        == "ok"
                    ) {

                        actualizarUIDesdeJSON(response)

                    } else {

                        Toast.makeText(
                            requireContext(),
                            response.getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()
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

                Toast.makeText(
                    requireContext(),
                    "Error al actualizar cantidad",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }

    private fun eliminarItem(item: CarritoItem) {

        mostrarLoading(true)

        val url =
            "${baseUrl}api/carrito/eliminar/${item.id}/"

        val queue =
            Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(

            Request.Method.DELETE,

            url,

            null,

            { response ->

                try {

                    if (
                        response.getString("status")
                        == "ok"
                    ) {

                        actualizarUIDesdeJSON(response)

                        Toast.makeText(
                            requireContext(),
                            response.getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            requireContext(),
                            response.getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()
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

                Toast.makeText(
                    requireContext(),
                    "Error al eliminar producto",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }

    private fun mostrarLoading(mostrar: Boolean) {

        progressBar.visibility =
            if (mostrar)
                View.VISIBLE
            else
                View.GONE

        recycler.visibility =
            if (mostrar)
                View.GONE
            else
                recycler.visibility

        layoutResumen.visibility =
            if (mostrar)
                View.GONE
            else
                layoutResumen.visibility

        layoutVacio.visibility =
            if (mostrar)
                View.GONE
            else
                layoutVacio.visibility
    }

    private fun finalizarPedido() {
        // Verificar si el usuario ha otorgado permisos de GPS
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no hay permisos, los solicitamos al sistema
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        // Activar barra de carga mientras lee el GPS
        mostrarLoading(true)

        // Inicializar el lector de ubicación de Google Play Services
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                mostrarLoading(false)
                if (location != null) {
                    // Si obtuvo la ubicación con éxito, abrimos la confirmación tipo Figma
                    mostrarDialogoConfirmacionPedido(location.latitude, location.longitude)
                } else {
                    // Ubicación por defecto si el GPS está apagado momentáneamente en el cel
                    Toast.makeText(requireContext(), "No se pudo obtener la ubicación exacta. Usando ubicación base.", Toast.LENGTH_SHORT).show()
                    mostrarDialogoConfirmacionPedido(19.7294, -99.4601) // Centro de Villa del Carbón
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

    /**
     * Despliega la ventana de confirmación interactiva para el cliente,
     * solicitando confirmación de su mapa/dirección tal como tu Wireframe.
     */
    private fun mostrarDialogoConfirmacionPedido(latitud: Double, longitud: Double) {
        // Crear la instancia del contenedor deslizable moderno
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val viewSheet = layoutInflater.inflate(R.layout.bottom_sheet_confirmar_pedido, null)
        bottomSheetDialog.setContentView(viewSheet)

        // Vincular los elementos de la UI del BottomSheet
        val mapView = viewSheet.findViewById<com.google.android.gms.maps.MapView>(R.id.mapViewConfirmar)
        val etDireccion = viewSheet.findViewById<EditText>(R.id.etDireccionConfirmar)
        val btnConfirmar = viewSheet.findViewById<Button>(R.id.btnConfirmarPedidoFinal)

        // Inicializar y pintar el Mapa dentro del cuadro
        mapView.onCreate(null)
        mapView.onResume()
        mapView.getMapAsync { googleMap ->
            val ubicacionCliente = LatLng(latitud, longitud)

            // Añadir marcador rojo en la ubicación del GPS
            googleMap.addMarker(
                MarkerOptions()
                    .position(ubicacionCliente)
                    .title("Tu ubicación actual")
            )

            // Mover la cámara del mapa y hacer un acercamiento (Zoom 16)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionCliente, 16f))

            // Configurar opciones visuales del mapa
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isCompassEnabled = true
        }

        // Acción al presionar el botón moderno de confirmación
        btnConfirmar.setOnClickListener {
            val direccionEscrita = etDireccion.text.toString().trim()

            if (direccionEscrita.isEmpty()) {
                etDireccion.error = "Por favor ingresa referencias para el repartidor"
                Toast.makeText(requireContext(), "La dirección es obligatoria", Toast.LENGTH_SHORT).show()
            } else {
                // Cerrar la ventana y mandar los datos limpios a Django
                bottomSheetDialog.dismiss()
                enviarPedidoServidor(latitud, longitud, direccionEscrita)
            }
        }

        // Desplegar la interfaz elegante en la pantalla
        bottomSheetDialog.show()
    }

    /**
     * Procesa la comunicación final con el backend de Django mandando los datos dinámicos.
     */
    private fun enviarPedidoServidor(latitud: Double, longitud: Double, direccion: String) {
        mostrarLoading(true)
        val url = "${baseUrl}api/pedidos/crear/"
        val queue = Volley.newRequestQueue(requireContext())
        val params = JSONObject()

        try {
            params.put("usuario_id", usuarioId)
            params.put("direccion", direccion) // Agarra lo que escribió el cliente
            params.put("latitud", latitud)     // Agarra el GPS real del teléfono
            params.put("longitud", longitud)   // Agarra el GPS real del teléfono
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                mostrarLoading(false)
                Toast.makeText(requireContext(), "¡Pedido realizado con éxito!", Toast.LENGTH_LONG).show()
                cargarCarrito() // Vacía la UI del carrito
            },
            { error ->
                mostrarLoading(false)
                error.printStackTrace()
                Toast.makeText(requireContext(), "Error al procesar la orden en el servidor", Toast.LENGTH_LONG).show()
            }
        )
        queue.add(request)
    }

    // Escuchador por si el cliente acepta los permisos de ubicación en la alerta nativa del celular
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            finalizarPedido() // Reintenta el flujo si ya dio el permiso
        } else {
            Toast.makeText(requireContext(), "Se requiere el permiso de ubicación para calcular el envío", Toast.LENGTH_LONG).show()
        }
    }

}