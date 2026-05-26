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
import org.json.JSONObject

/**
 * Fragmento encargado de gestionar y mostrar la lista de productos favoritos del usuario.
 * Permite visualizar los productos marcados como favoritos, recuperados desde el backend,
 * y removerlos de la lista mediante una petición asíncrona interactuando con una API REST.
 */
class FavoritosFragment : Fragment() {

    // Componentes de la interfaz de usuario (UI)
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FavoritosAdapter
    private val listaFavoritos = mutableListOf<Favorito>()
    private lateinit var txtVacio: TextView
    private lateinit var progressBar: View
    private lateinit var toolbar: Toolbar

    // Configuración de red y estado del usuario
    private val baseUrl: String = Constants.BASE_URL
    private var usuarioId: Int = -1

    companion object {
        private const val ARG_USUARIO_ID = "usuario_id"

        /**
         * Crea una nueva instancia de [FavoritosFragment] inyectando el ID del usuario en los argumentos.
         *
         * @param usuarioId ID único del usuario logueado.
         * @return Instancia configurada de FavoritosFragment.
         */
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
        // Recuperar el ID del usuario desde los argumentos enviados al fragmento
        usuarioId = arguments?.getInt(ARG_USUARIO_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el diseño XML del fragmento de favoritos
        val view = inflater.inflate(R.layout.fragment_favoritos, container, false)

        // Inicializar los componentes de la vista
        toolbar = view.findViewById(R.id.toolbarFavoritos)
        recycler = view.findViewById(R.id.recyclerFavoritos)
        txtVacio = view.findViewById(R.id.txtVacio)
        progressBar = view.findViewById(R.id.progressBar)

        // Configurar el evento de click en el botón de retroceso del Toolbar
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        // Configurar el RecyclerView con una cuadrícula de 1 sola columna (comportamiento de lista vertical)
        recycler.layoutManager = GridLayoutManager(requireContext(), 1)
        adapter = FavoritosAdapter(
            listaFavoritos,
            requireContext(),
            baseUrl,
            { favorito -> eliminarFavorito(favorito) } // Lambda para manejar la eliminación de un favorito
        )
        recycler.adapter = adapter

        // Realizar la petición inicial para cargar los elementos desde el servidor
        cargarFavoritos()

        return view
    }

    /**
     * Consume la API mediante una petición GET para traer todos los productos favoritos del usuario actual.
     * Mapea la respuesta a la lista local y maneja la alternancia de la UI si el listado está vacío.
     */
    private fun cargarFavoritos() {
        mostrarLoading(true)

        val url = "$baseUrl" + "api/favoritos/$usuarioId/"
        val queue = Volley.newRequestQueue(requireContext())

        // Se utiliza JsonArrayRequest debido a que el servidor retorna un arreglo directo de objetos favoritos
        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    listaFavoritos.clear()

                    // Recorrer el arreglo JSON y parsear las estructuras anidadas
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)
                        val productoObj = obj.getJSONObject("producto")

                        // Mapear los datos internos del objeto Producto
                        val producto = Producto(
                            id = productoObj.getInt("id"),
                            nombre = productoObj.getString("nombre"),
                            precio = productoObj.getDouble("precio"),
                            imagen = if (productoObj.has("imagen") && !productoObj.isNull("imagen"))
                                productoObj.getString("imagen") else null,
                            entregable = productoObj.getBoolean("entregable")
                        )

                        // Mapear el contenedor Favorito
                        val favorito = Favorito(
                            id = obj.getInt("id"),
                            producto = producto,
                            fecha_agregado = obj.getString("fecha_agregado")
                        )

                        listaFavoritos.add(favorito)
                    }

                    mostrarLoading(false)

                    // Controlar visibilidades según el tamaño de la lista obtenida
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
                    Toast.makeText(requireContext(), "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
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

    /**
     * Realiza una petición DELETE al endpoint de la API para desvincular un producto de la lista de favoritos.
     * Si la operación en el servidor es exitosa, remueve el objeto localmente y actualiza el adaptador.
     *
     * @param favorito Objeto de tipo [Favorito] que se desea eliminar.
     */
    private fun eliminarFavorito(favorito: Favorito) {
        mostrarLoading(true)

        val url = "$baseUrl/api/favoritos/eliminar/${favorito.id}/"
        val queue = Volley.newRequestQueue(requireContext())

        // Se usa JsonObjectRequest ya que la respuesta esperada es un objeto de estado del tipo { "status": "ok", ... }
        val request = JsonObjectRequest(
            Request.Method.DELETE,
            url,
            null,
            { response ->
                try {
                    if (response.getString("status") == "ok") {
                        // Optimización: Eliminar directamente de la lista en memoria sin reconsultar toda la API
                        listaFavoritos.remove(favorito)
                        adapter.notifyDataSetChanged()

                        // Si tras borrar el elemento la lista queda vacía, mutar la vista
                        if (listaFavoritos.isEmpty()) {
                            recycler.visibility = View.GONE
                            txtVacio.visibility = View.VISIBLE
                        }

                        Toast.makeText(requireContext(), "Producto eliminado de favoritos", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Error al eliminar de favoritos", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    /**
     * Modifica de manera coordinada la visibilidad del indicador de progreso (ProgressBar)
     * y los contenedores de datos principales para evitar parpadeos visuales superpuestos.
     *
     * @param mostrar true para visualizar la barra de carga, false para ocultarla y restablecer el RecyclerView.
     */
    private fun mostrarLoading(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        recycler.visibility = if (mostrar) View.GONE else recycler.visibility
        txtVacio.visibility = View.GONE
    }
}