package com.example.agroconectavilla.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agroconectavilla.R
import com.example.agroconectavilla.network.Producto
import com.example.agroconectavilla.DetalleActivity
import com.example.agroconectavilla.utils.Constants

class ProductoAdapter(
    private val lista: MutableList<Producto>,
    private val context: Context,
    private val baseUrl: String = Constants.BASE_URL
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    // Método para actualizar la lista
    fun updateList(nuevaLista: List<Producto>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgProducto)
        val nombre: TextView = v.findViewById(R.id.txtNombre)
        val precio: TextView = v.findViewById(R.id.txtPrecio)
        val entregableBadge: TextView = v.findViewById(R.id.txtEntregable) // ← AGREGADO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = lista[position]

        holder.nombre.text = p.nombre
        holder.precio.text = "$${p.precio}"

        // Mostrar badge si es entregable
        if (p.entregable) {
            holder.entregableBadge.visibility = View.VISIBLE
        } else {
            holder.entregableBadge.visibility = View.GONE
        }

        val imagen = p.imagen

        if (!imagen.isNullOrEmpty()) {
            val urlFinal = baseUrl + imagen
            Glide.with(context)
                .load(urlFinal)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.img)
        } else {
            holder.img.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetalleActivity::class.java)
            intent.putExtra("id", p.id)
            intent.putExtra("nombre", p.nombre)
            intent.putExtra("precio", p.precio)
            intent.putExtra("imagen", p.imagen)
            intent.putExtra("entregable", p.entregable)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = lista.size
}