package com.example.agroconectavilla.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agroconectavilla.FragmentDetalle
import com.example.agroconectavilla.R
import com.example.agroconectavilla.network.Producto
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
        val entregableBadge: TextView = v.findViewById(R.id.txtEntregable)
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

        // Badge entregable
        holder.entregableBadge.visibility =
            if (p.entregable) View.VISIBLE else View.GONE

        // Imagen
        if (!p.imagen.isNullOrEmpty()) {

            val urlFinal = baseUrl + p.imagen

            Glide.with(context)
                .load(urlFinal)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.img)

        } else {

            holder.img.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {

            val fragment = FragmentDetalle.newInstance(p.id)  // Usar el método factory

            (context as AppCompatActivity)
                .supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getItemCount(): Int = lista.size
}