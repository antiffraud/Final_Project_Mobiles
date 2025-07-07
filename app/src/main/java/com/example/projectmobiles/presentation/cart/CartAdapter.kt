package com.example.projectmobiles.presentation.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectmobiles.domain.usecase.CartItem
import com.example.projectmobiles.databinding.ItemCartBinding

class CartAdapter(
    private var cartItems: MutableList<CartItem>,
    private val listener: CartItemListener
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    interface CartItemListener {
        fun onQuantityChanged(position: Int, newQuantity: Int)
        fun onRemoveItem(position: Int)
    }

    fun updateCartItems(newItems: List<CartItem>) {
        cartItems.clear()
        cartItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position], position)
    }

    override fun getItemCount(): Int = cartItems.size

    inner class CartViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem, position: Int) {
            val movie = cartItem.movie
            binding.apply {
                cartMovieTitle.text = movie.title
                cartMovieYear.text = movie.releaseDate?.take(4)
                cartMoviePrice.text = "Rp ${formatPrice(movie.price)}"
                quantityText.text = cartItem.quantity.toString()

                Glide.with(itemView.context)
                    .load("https://image.tmdb.org/t/p/w185${movie.poster_path}")
                    .into(cartMovieImage)

                decreaseQuantity.setOnClickListener {
                    if (cartItem.quantity > 1) {
                        listener.onQuantityChanged(position, cartItem.quantity - 1)
                    }
                }

                increaseQuantity.setOnClickListener {
                    listener.onQuantityChanged(position, cartItem.quantity + 1)
                }

                removeFromCart.setOnClickListener {
                    listener.onRemoveItem(position)
                }
            }
        }

        private fun formatPrice(price: Double?): String {
            return String.format("%,.0f", price)
        }
    }
}