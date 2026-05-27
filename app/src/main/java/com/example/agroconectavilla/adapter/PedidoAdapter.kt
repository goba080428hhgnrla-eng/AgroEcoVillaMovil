package com.example.agroconectavilla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.agroconectavilla.R
import com.example.agroconectavilla.network.Pedido

class PedidoAdapter(
    private val lista: MutableList<Pedido>,
    private val context: Context,
    private val onClick: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val txtPedidoId: TextView = v.findViewById(R.id.txtPedidoId)
        val txtEstado: TextView = v.findViewById(R.id.txtEstado)
        val txtTotal: TextView = v.findViewById(R.id.txtTotal)
        val btnVer: Button = v.findViewById(R.id.btnVer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val pedido = lista[position]

        holder.txtPedidoId.text = "Pedido #${pedido.id}"
        holder.txtEstado.text = pedido.estado
        holder.txtTotal.text = "$${pedido.total}"

        holder.btnVer.setOnClickListener {
            onClick(pedido)
        }
    }

    override fun getItemCount(): Int = lista.size
}
