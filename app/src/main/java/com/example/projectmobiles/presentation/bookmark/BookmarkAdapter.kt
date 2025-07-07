package com.example.projectmobiles.presentation.bookmark

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.projectmobiles.R
import com.example.projectmobiles.data.model.Movie

class BookmarkAdapter(
    private var movies: List<Movie>,
    private val onItemClick: (Movie) -> Unit,
    private val onRemoveClick: (Movie) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    class BookmarkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val movieImage: ImageView = view.findViewById(R.id.cartMovieImage)
        val movieTitle: TextView = view.findViewById(R.id.cartMovieTitle)
        val movieYear: TextView = view.findViewById(R.id.cartMovieYear)
        val movieTime: TextView = view.findViewById(R.id.cartMovieYear)
        val movieSummary: TextView = view.findViewById(R.id.movieSummary)
        val removeButton: ImageButton = view.findViewById(R.id.removeFromCart)
        val movieRuntime: TextView = view.findViewById(R.id.movieTimeTxt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val movie = movies[position]

        holder.movieTitle.text = movie.title

        val year = movie.releaseDate?.substring(0, 4) ?: "Unknown Year"
        holder.movieYear.text = year

        holder.movieRuntime.text = movie.runtime?.let { "$it mins" } ?: "–"

        holder.movieTime.text = "${movie.releaseDate?.take(4)}"

        holder.movieSummary.text = movie.overview ?: "No summary available"

        Glide.with(holder.movieImage.context)
            .load("https://image.tmdb.org/t/p/w500${movie.poster_path}")
            .transform(CenterCrop(), RoundedCorners(20))
            .into(holder.movieImage)

        holder.itemView.setOnClickListener {
            onItemClick(movie)
        }

        holder.removeButton.setOnClickListener {
            onRemoveClick(movie)
        }
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }
}