package com.example.agroconectavilla.adapter
// Importacion de diferentes archivos
import com.example.agroconectavilla.utils.Constants
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agroconectavilla.R
import com.example.agroconectavilla.network.CarritoItem

// Adaptador del carrito
class CarritoAdapter(

    // Lista de productos
    private val lista: MutableList<CarritoItem>,

    // Contexto de la app
    private val context: Context,

    // URL base
    private val baseUrl: String = Constants.BASE_URL + "api/carrito/",

    // Cambiar cantidad
    private val onCantidadChange: (CarritoItem, Int) -> Unit,

    // Eliminar producto
    private val onEliminar: (CarritoItem) -> Unit

) : RecyclerView.Adapter<CarritoAdapter.ViewHolder>() {

    // Actualiza la lista
    fun updateList(nuevaLista: List<CarritoItem>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    // ViewHolder
    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        // Imagen producto
        val img: ImageView = v.findViewById(R.id.imgProducto)

        // Nombre producto
        val nombre: TextView = v.findViewById(R.id.txtNombre)

        // Precio producto
        val precio: TextView = v.findViewById(R.id.txtPrecio)

        // Subtotal
        val subtotal: TextView = v.findViewById(R.id.txtSubtotal)

        // Cantidad
        val txtCantidad: TextView = v.findViewById(R.id.txtCantidad)

        // Botón menos
        val btnMenos: ImageButton = v.findViewById(R.id.btnMenos)

        // Botón más
        val btnMas: ImageButton = v.findViewById(R.id.btnMas)

        // Botón eliminar
        val btnEliminar: ImageButton = v.findViewById(R.id.btnEliminar)
    }

    // Crear vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)

        return ViewHolder(v)
    }

    // Mostrar datos
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = lista[position]
        val producto = item.producto

        holder.nombre.text = producto.nombre
        holder.precio.text = "$${producto.precio}"
        holder.txtCantidad.text = item.cantidad.toString()
        holder.subtotal.text = "Subtotal: $${item.subtotal}"

        // Cargar imagen
        val imagen = producto.imagen

        if (!imagen.isNullOrEmpty()) {

            val urlFinal = baseUrl + imagen

            Glide.with(context)
                .load(urlFinal)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.img)
        }

        // Botón menos
        holder.btnMenos.setOnClickListener {

            if (item.cantidad > 1) {
                onCantidadChange(item, item.cantidad - 1)
            }
        }

        // Botón más
        holder.btnMas.setOnClickListener {
            onCantidadChange(item, item.cantidad + 1)
        }

        // Botón eliminar
        holder.btnEliminar.setOnClickListener {
            onEliminar(item)
        }
    }

    // Tamaño lista
    override fun getItemCount() = lista.size
}