package com.example.agroconectavilla.network

data class PedidoItem(
    val id: Int,
    val producto: Producto,
    val cantidad: Int,
    val subtotal: Double
)
