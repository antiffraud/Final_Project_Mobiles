package com.example.projectmobiles.presentation.seeAll

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.projectmobiles.R
import com.example.projectmobiles.data.model.Movie

class MovieAdapter(
    private var movies: List<Movie>,
    private val onItemClick: (Movie) -> Unit,
    private val onAddToWatchlist: (Movie) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_CAROUSEL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (movies[position].isCarousel) TYPE_CAROUSEL else TYPE_NORMAL
    }

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivPoster)
        val title: TextView = view.findViewById(R.id.tvTitle)
    }

    class CarouselViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivPoster)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val genre: TextView = view.findViewById(R.id.tvGenre)
        val year: TextView = view.findViewById(R.id.tvYear)
        val duration: TextView = view.findViewById(R.id.tvDuration)
        val btnWatchlist: TextView = view.findViewById(R.id.btnWatchlist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CAROUSEL) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.movie_carousel, parent, false)
            CarouselViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_movie, parent, false)
            MovieViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val movie = movies[position]

        if (holder is MovieViewHolder) {
            holder.title.text = movie.title
            Glide.with(holder.poster.context)
                .load("https://image.tmdb.org/t/p/w500${movie.poster_path}")
                .transform(CenterCrop(), RoundedCorners(35))
                .into(holder.poster)

            holder.itemView.setOnClickListener {
                onItemClick(movie)
            }
        } else if (holder is CarouselViewHolder) {
            holder.title.text = movie.title
            val genreList = movie.genres?.joinToString(" • ") { it.name } ?: "No Genre"
            holder.genre.text = genreList

            val year = movie.releaseDate?.substring(0, 4) ?: "Unknown Year"
            holder.year.text = year

            val duration = movie.runtime?.let {
                val hours = it / 60
                val minutes = it % 60
                "$hours h ${minutes}m"
            } ?: "No Duration"
            holder.duration.text = duration

            holder.btnWatchlist.setOnClickListener {
                onAddToWatchlist(movie)
            }

            Glide.with(holder.poster.context)
                .load("https://image.tmdb.org/t/p/w780${movie.poster_path}")
                .centerCrop()
                .into(holder.poster)

            holder.itemView.setOnClickListener {
                onItemClick(movie)
            }
        }
    }

    fun updateMovies(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = movies.size
}