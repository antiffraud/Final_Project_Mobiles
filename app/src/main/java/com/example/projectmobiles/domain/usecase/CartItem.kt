package com.example.projectmobiles.domain.usecase

import com.example.projectmobiles.data.model.Movie

data class CartItem(
    val movie: Movie,
    var quantity: Int = 1
) {
    fun calculatePrice(): Double {
        return (movie.price ?: 0.0) * quantity
    }
}
