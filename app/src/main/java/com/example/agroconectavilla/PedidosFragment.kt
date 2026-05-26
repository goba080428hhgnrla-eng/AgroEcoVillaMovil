package com.example.agroconectavilla

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Fragmento encargado de gestionar y mostrar el historial o estado de los pedidos del usuario.
 * Actúa como un contenedor de vista básico que infla el diseño correspondiente a la sección de pedidos.
 */
class PedidosFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el diseño XML correspondiente a este fragmento sin añadirlo inmediatamente al contenedor padre
        return inflater.inflate(R.layout.fragment_pedidos, container, false)
    }
}