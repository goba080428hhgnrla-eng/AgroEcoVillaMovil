package com.example.agroconectavilla.network

data class Favorito(
    val id: Int,
    val producto: Producto,
    val fecha_agregado: String
)

data class FavoritoResponse(
    val status: String,
    val message: String? = null,
    val favoritos: List<Favorito>? = null
)