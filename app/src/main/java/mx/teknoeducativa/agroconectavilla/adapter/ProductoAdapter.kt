package mx.teknoeducativa.agroconectavilla.adapter

// Importacion de archivos
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mx.teknoeducativa.agroconectavilla.FragmentDetalle
import mx.teknoeducativa.agroconectavilla.R
import mx.teknoeducativa.agroconectavilla.network.Producto

// Adaptador productos
class ProductoAdapter(

    // Lista productos
    private val lista: MutableList<Producto>,

    // Contexto
    private val context: Context

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

        if (p.entregable) {

            holder.entregableBadge.visibility = View.VISIBLE
            holder.entregableBadge.text = "Entregable"
            holder.entregableBadge.setBackgroundColor(context.getColor(android.R.color.holo_green_dark))

        } else {
            holder.entregableBadge.visibility = View.VISIBLE
            holder.entregableBadge.text = "No entregable"

            holder.entregableBadge.setBackgroundColor(context.getColor(android.R.color.holo_orange_dark))
        }

        if (!p.imagen.isNullOrEmpty()) {

            Glide.with(context)
                .load(p.imagen)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.img)

        } else {

            holder.img.setImageResource(android.R.drawable.ic_menu_gallery)
        }

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