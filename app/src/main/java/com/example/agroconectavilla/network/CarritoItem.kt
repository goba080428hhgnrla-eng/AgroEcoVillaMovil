package com.example.agroconectavilla.network
data class CarritoItem(
    val id: Int,
    val producto: Producto,
    val cantidad: Int,
    val subtotal: Double,
    val agregado: String
)

data class Carrito(
    val id: Int,
    val items: List<CarritoItem>,
    val total: Double,
    val total_items: Int,
    val creado: String,
    val actualizado: String
)