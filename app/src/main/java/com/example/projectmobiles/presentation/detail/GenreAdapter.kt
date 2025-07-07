package com.example.projectmobiles.presentation.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmobiles.R
import com.example.projectmobiles.data.model.Genre

class GenreAdapter(private val genres: List<Genre>) :
    RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    class GenreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val genreText: TextView = itemView.findViewById(R.id.genreName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_genre, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        holder.genreText.text = genres[position].name
    }

    override fun getItemCount() = genres.size
}
