package com.example.agroconectavilla.adapter

// Importacion de archivos
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

// Adaptador productos
class ProductoAdapter(

    // Lista productos
    private val lista: MutableList<Producto>,

    // Contexto
    private val context: Context,

    // URL base
    private val baseUrl: String = Constants.BASE_URL

) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    // Actualizar lista
    fun updateList(nuevaLista: List<Producto>) {
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

        // Badge entregable
        val entregableBadge: TextView = v.findViewById(R.id.txtEntregable)
    }

    // Crear vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)

        return ViewHolder(v)
    }

    // Mostrar datos
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val p = lista[position]

        holder.nombre.text = p.nombre
        holder.precio.text = "$${p.precio}"

        // Mostrar badge
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

        // Abrir detalle
        holder.itemView.setOnClickListener {

            val fragment = FragmentDetalle.newInstance(p.id)

            (context as AppCompatActivity)
                .supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    // Tamaño lista
    override fun getItemCount(): Int = lista.size
}