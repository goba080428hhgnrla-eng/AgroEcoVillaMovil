package com.example.agroconectavilla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agroconectavilla.FragmentDetalle
import com.example.agroconectavilla.R
import com.example.agroconectavilla.network.Favorito
import com.example.agroconectavilla.utils.Constants

class FavoritosAdapter(
    private val lista: MutableList<Favorito>,
    private val context: Context,
    private val baseUrl: String = Constants.BASE_URL,
    private val onEliminar: (Favorito) -> Unit
) : RecyclerView.Adapter<FavoritosAdapter.ViewHolder>() {

    fun updateList(nuevaLista: List<Favorito>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val imgProducto: ImageView = v.findViewById(R.id.imgProducto)
        val txtNombre: TextView = v.findViewById(R.id.txtNombre)
        val txtPrecio: TextView = v.findViewById(R.id.txtPrecio)
        val btnEliminar: ImageButton = v.findViewById(R.id.btnEliminarFavorito)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorito, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favorito = lista[position]
        val producto = favorito.producto

        holder.txtNombre.text = producto.nombre
        holder.txtPrecio.text = "$${producto.precio}"

        // Cargar imagen
        val imagen = producto.imagen
        if (!imagen.isNullOrEmpty()) {
            val urlFinal = baseUrl + imagen
            Glide.with(context)
                .load(urlFinal)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.imgProducto)
        }

        // Click para ver detalle del producto
        holder.itemView.setOnClickListener {
            val fragment = FragmentDetalle.newInstance(producto.id)  // Usar producto.id

            (context as AppCompatActivity)
                .supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Click para eliminar de favoritos
        holder.btnEliminar.setOnClickListener {
            onEliminar(favorito)
        }
    }

    override fun getItemCount() = lista.size
}