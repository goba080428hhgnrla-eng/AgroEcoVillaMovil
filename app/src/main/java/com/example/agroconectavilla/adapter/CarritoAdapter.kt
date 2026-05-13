package com.example.agroconectavilla.adapter

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

class CarritoAdapter(
    private val lista: MutableList<CarritoItem>,
    private val context: Context,
    private val baseUrl: String = Constants.BASE_URL + "api/carrito/",
    private val onCantidadChange: (CarritoItem, Int) -> Unit,
    private val onEliminar: (CarritoItem) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.ViewHolder>() {

    fun updateList(nuevaLista: List<CarritoItem>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgProducto)
        val nombre: TextView = v.findViewById(R.id.txtNombre)
        val precio: TextView = v.findViewById(R.id.txtPrecio)
        val subtotal: TextView = v.findViewById(R.id.txtSubtotal)
        val txtCantidad: TextView = v.findViewById(R.id.txtCantidad)
        val btnMenos: ImageButton = v.findViewById(R.id.btnMenos)
        val btnMas: ImageButton = v.findViewById(R.id.btnMas)
        val btnEliminar: ImageButton = v.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return ViewHolder(v)
    }

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

        // Botones de cantidad
        holder.btnMenos.setOnClickListener {
            if (item.cantidad > 1) {
                onCantidadChange(item, item.cantidad - 1)
            }
        }

        holder.btnMas.setOnClickListener {
            onCantidadChange(item, item.cantidad + 1)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminar(item)
        }
    }

    override fun getItemCount() = lista.size
}