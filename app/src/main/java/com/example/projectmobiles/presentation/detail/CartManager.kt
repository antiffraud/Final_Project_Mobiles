package com.example.projectmobiles.presentation.detail

import android.content.Context
import android.util.Log
import com.example.projectmobiles.domain.usecase.CartItem
import com.example.projectmobiles.data.local.DatabaseHelper
import com.example.projectmobiles.data.model.Movie

class CartManager private constructor() {
    private lateinit var dbHelper: DatabaseHelper

    companion object {
        private var instance: CartManager? = null

        fun getInstance(): CartManager {
            if (instance == null) {
                instance = CartManager()
            }
            return instance!!
        }
    }

    fun initialize(context: Context) {
        if (!::dbHelper.isInitialized) {
            dbHelper = DatabaseHelper(context)
        }
    }

    fun addToCart(movie: Movie) {
        try {
            ensureDbInitialized()
            Log.d("CartManager", ">>> addToCart(${movie.id})")
            val existing = dbHelper.getAllCartItems()
                .find { it.movie.id == movie.id }

            if (existing != null) {
                Log.d("CartManager", "…found existing qty=${existing.quantity}, updating")
                val newQty = existing.quantity + 1
                dbHelper.updateCartQuantity(movie.id, newQty)
                Log.d("CartManager", "…updated quantity to $newQty")
            } else {
                Log.d("CartManager", "…no existing, inserting new")
                val price = movie.price ?: ((movie.voteAverage ?: 0.0) * 5000)
                dbHelper.addCartItem(movie.copy(price = price), 1)
                Log.d("CartManager", "…inserted with qty=1")
            }
        } catch (e: Exception) {
            Log.e("CartManager", "Error in addToCart()", e)
            throw e  // re-throw if you want the crash to still happen, or remove this line to swallow it
        }
    }


    fun getCartItems(): List<CartItem> {
        ensureDbInitialized()
        return dbHelper.getAllCartItems()
    }

    fun updateQuantity(position: Int, newQuantity: Int) {
        ensureDbInitialized()
        val items = dbHelper.getAllCartItems()
        if (position in items.indices) {
            val movieId = items[position].movie.id
            dbHelper.updateCartQuantity(movieId, newQuantity)
        }
    }

    fun removeItem(position: Int) {
        ensureDbInitialized()
        val items = dbHelper.getAllCartItems()
        if (position in items.indices) {
            val movieId = items[position].movie.id
            dbHelper.removeCartItem(movieId)
        }
    }

    fun clearCart() {
        ensureDbInitialized()
        dbHelper.clearCart()
    }

    fun calculateSubtotal(): Double {
        ensureDbInitialized()
        return dbHelper.getAllCartItems().sumOf { it.calculatePrice() }
    }

    private fun ensureDbInitialized() {
        if (!::dbHelper.isInitialized) {
            throw IllegalStateException("CartManager not initialized. Call initialize(context) first.")
        }
    }
}
