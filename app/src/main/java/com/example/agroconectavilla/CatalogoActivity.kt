package com.example.agroconectavilla

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley

import com.example.agroconectavilla.adapter.ProductoAdapter
import com.example.agroconectavilla.network.Producto
import com.example.agroconectavilla.utils.Constants
import org.json.JSONArray

class CatalogoActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private lateinit var txtVacio: TextView

    private val lista = mutableListOf<Producto>()

    private val url: String = Constants.BASE_URL + "api/productos/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogo)

        recycler = findViewById(R.id.recyclerProductos)
        txtVacio = findViewById(R.id.txtVacio)

        recycler.layoutManager = GridLayoutManager(this, 2)

        adapter = ProductoAdapter(lista, this)
        recycler.adapter = adapter

        cargarProductos()
    }

    private fun cargarProductos() {
        Log.d("MI_URL", "URL FINAL: $url")
        val queue = Volley.newRequestQueue(this)

        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response: JSONArray ->

                try {

                    lista.clear()


                    if (response.length() == 0) {
                        recycler.visibility = View.GONE
                        txtVacio.visibility = View.VISIBLE
                        txtVacio.text = "No hay productos disponibles"
                        return@JsonArrayRequest
                    }


                    recycler.visibility = View.VISIBLE
                    txtVacio.visibility = View.GONE

                    for (i in 0 until response.length()) {

                        val obj = response.getJSONObject(i)

                        val producto = Producto(
                            id = obj.optInt("id", 0),
                            nombre = obj.optString("nombre", "Sin nombre"),
                            precio = obj.optDouble("precio", 0.0),
                            imagen = obj.optString("imagen", ""),
                            entregable = obj.optBoolean("entregable", false)
                        )

                        lista.add(producto)
                    }

                    adapter.notifyDataSetChanged()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al cargar productos", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }
}