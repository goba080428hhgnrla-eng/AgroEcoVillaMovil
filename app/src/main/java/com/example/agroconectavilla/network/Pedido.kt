package com.example.agroconectavilla.network

data class Pedido(
    val id: Int,
    val estado: String,
    val total: Double,
    val fecha: String,
    val direccion_entrega: String,
    val nombre_cliente: String,
    val telefono_cliente: String,
    val repartidor_nombre: String?
)
