package com.example.agroconectavilla.adapter
//Importacion de diferentes archivos
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

// Adaptador favoritos
class FavoritosAdapter(

    // Lista favoritos
    private val lista: MutableList<Favorito>,

    // Contexto app
    private val context: Context,

    // URL base
    private val baseUrl: String = Constants.BASE_URL,

    // Eliminar favorito
    private val onEliminar: (Favorito) -> Unit

) : RecyclerView.Adapter<FavoritosAdapter.ViewHolder>() {

    // Actualizar lista
    fun updateList(nuevaLista: List<Favorito>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    // ViewHolder
    inner class ViewHolder(v: android.view.View) : RecyclerView.ViewHolder(v) {

        // Imagen producto
        val imgProducto: ImageView = v.findViewById(R.id.imgProducto)

        // Nombre producto
        val txtNombre: TextView = v.findViewById(R.id.txtNombre)

        // Precio producto
        val txtPrecio: TextView = v.findViewById(R.id.txtPrecio)

        // Botón eliminar
        val btnEliminar: ImageButton = v.findViewById(R.id.btnEliminarFavorito)
    }

    // Crear vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorito, parent, false)

        return ViewHolder(v)
    }

    // Mostrar datos
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

        // Abrir detalle
        holder.itemView.setOnClickListener {

            val fragment = FragmentDetalle.newInstance(producto.id)

            (context as AppCompatActivity)
                .supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Eliminar productos de favoritos
        holder.btnEliminar.setOnClickListener {
            onEliminar(favorito)
        }
    }

    // Tamaño lista
    override fun getItemCount() = lista.size
}