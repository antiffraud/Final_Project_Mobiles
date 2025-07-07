package com.example.projectmobiles.presentation.seeAll

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmobiles.BuildConfig
import com.example.projectmobiles.R
import com.example.projectmobiles.data.local.DatabaseHelper
import com.example.projectmobiles.data.model.GenreResponse
import com.example.projectmobiles.data.model.Movie
import com.example.projectmobiles.data.model.MovieResponse
import com.example.projectmobiles.data.remote.ApiClient
import com.example.projectmobiles.presentation.detail.DetailActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Allmovies : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var tvTitle: TextView
    private lateinit var dbHelper: DatabaseHelper

    private var movieList = mutableListOf<Movie>()
    private var isLoading = false
    private var currentPage = 1

    private val apiKey = BuildConfig.TMDB_API_KEY
    private var movieType: String? = null

    private lateinit var categoryContainer: LinearLayout
    private var selectedCategoryId: Int? = null

    private val nowPlayingIds = mutableSetOf<Int>()
    private val upcomingIds   = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allmovies)

        dbHelper = DatabaseHelper(this)

        recyclerView = findViewById(R.id.rvAllMovies)
        tvTitle      = findViewById(R.id.tvTitle)
        categoryContainer = findViewById(R.id.categoryContainer)

        movieType = intent.getStringExtra("MOVIE_TYPE")
        tvTitle.text = when (movieType) {
            "TOP"      -> "Top Rated Movies"
            "NOW"      -> "Now Playing Movies"
            "UPCOMING" -> "Upcoming Movies"
            else       -> "Daftar Film"
        }

        prefetchNowPlaying()
        prefetchUpcoming()

        movieAdapter = MovieAdapter(
            movies = movieList,
            onItemClick = { m ->
                val isRestricted = m.id in nowPlayingIds || m.id in upcomingIds
                Intent(this, DetailActivity::class.java).apply {
                    putExtra("MOVIE_ID", m.id)
                    putExtra("IS_RESTRICTED", isRestricted)
                }.also(::startActivity)
            },
            onAddToWatchlist = { m ->
                dbHelper.addBookmark(m)
                Toast.makeText(this, "${m.title} added to watchlist", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@Allmovies, 2)
            adapter = movieAdapter
            setHasFixedSize(true)
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (movieType == "UPCOMING") {
            categoryContainer.visibility = View.GONE
        } else {
            fetchGenres()
        }

        setupScrollListener()
        fetchMovies()
    }

    private fun prefetchNowPlaying() {
        ApiClient.tmdbApi.getNowPlayingMovies(apiKey, page = 1)
            .enqueue(object: Callback<MovieResponse> {
                override fun onResponse(c: Call<MovieResponse>, r: Response<MovieResponse>) {
                    r.body()?.results?.map { it.id }
                        ?.let { nowPlayingIds.clear(); nowPlayingIds.addAll(it) }
                }
                override fun onFailure(c: Call<MovieResponse>, t: Throwable) = Unit
            })
    }

    private fun prefetchUpcoming() {
        ApiClient.tmdbApi.getUpcomingMovies(apiKey, region="US")
            .enqueue(object: Callback<MovieResponse> {
                override fun onResponse(c: Call<MovieResponse>, r: Response<MovieResponse>) {
                    r.body()?.results?.map { it.id }
                        ?.let { upcomingIds.clear(); upcomingIds.addAll(it) }
                }
                override fun onFailure(c: Call<MovieResponse>, t: Throwable) = Unit
            })
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as GridLayoutManager
                val visible = lm.childCount
                val total   = lm.itemCount
                val first   = lm.findFirstVisibleItemPosition()
                if (!isLoading && selectedCategoryId == null &&
                    (visible + first) >= total && first >= 0) {
                    fetchMovies()
                }
            }
        })
    }

    private fun fetchMovies() {
        if (isLoading || selectedCategoryId != null) return
        isLoading = true

        val call = when (movieType) {
            "TOP"      -> ApiClient.tmdbApi.getTopRatedMovies(apiKey, page = currentPage)
            "NOW"      -> ApiClient.tmdbApi.getNowPlayingMovies(apiKey, page = currentPage)
            "UPCOMING" -> ApiClient.tmdbApi.getUpcomingMovies(apiKey, region="US", page = currentPage)
            else       -> null
        }
        call?.enqueue(object: Callback<MovieResponse> {
            override fun onResponse(c: Call<MovieResponse>, r: Response<MovieResponse>) {
                isLoading = false
                if (r.isSuccessful) {
                    r.body()?.results?.let {
                        movieList.addAll(it)
                        movieAdapter.notifyDataSetChanged()
                        currentPage++
                    }
                } else {
                    Toast.makeText(this@Allmovies, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(c: Call<MovieResponse>, t: Throwable) {
                isLoading = false
                Toast.makeText(this@Allmovies, "Kesalahan jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchGenres() {
        ApiClient.tmdbApi.getGenres(apiKey)
            .enqueue(object: Callback<GenreResponse> {
                override fun onResponse(c: Call<GenreResponse>, r: Response<GenreResponse>) {
                    r.body()?.genres?.forEach { genre ->
                        val chip = TextView(this@Allmovies).apply {
                            text = genre.name
                            tag = genre.id
                            setPadding(32,16,32,16)
                            setTextColor(resources.getColor(android.R.color.white,null))
                            background = resources.getDrawable(R.drawable.category_bg,null)
                            layoutParams = ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply { rightMargin = 16 }

                            setOnClickListener {
                                val gid = genre.id
                                if (selectedCategoryId == gid) {
                                    selectedCategoryId = null
                                    resetGenreChips()
                                    movieList.clear()
                                    movieAdapter.notifyDataSetChanged()
                                    currentPage = 1
                                    fetchMovies()
                                } else {
                                    selectedCategoryId = gid
                                    updateGenreChips(gid)
                                    movieList.clear()
                                    movieAdapter.notifyDataSetChanged()
                                    fetchMoviesByGenre(gid)
                                }
                            }
                        }
                        categoryContainer.addView(chip)
                    }
                }
                override fun onFailure(c: Call<GenreResponse>, t: Throwable) {
                    Toast.makeText(this@Allmovies, "Gagal ambil genre", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchMoviesByGenre(genreId: Int) {
        ApiClient.tmdbApi.getMoviesByGenre(apiKey, genreId)
            .enqueue(object: Callback<MovieResponse> {
                override fun onResponse(c: Call<MovieResponse>, r: Response<MovieResponse>) {
                    if (r.isSuccessful) {
                        r.body()?.results?.let {
                            movieList.addAll(it)
                            movieAdapter.notifyDataSetChanged()
                        }
                    }
                }
                override fun onFailure(c: Call<MovieResponse>, t: Throwable) {
                    Toast.makeText(this@Allmovies, "Gagal ambil film genre", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun resetGenreChips() {
        for (i in 0 until categoryContainer.childCount) {
            val chip = categoryContainer.getChildAt(i) as TextView
            chip.setBackgroundResource(R.drawable.category_bg)
            chip.setTextColor(ContextCompat.getColor(this, R.color.category_text_inactive))
        }
    }

    private fun updateGenreChips(selectedId: Int) {
        for (i in 0 until categoryContainer.childCount) {
            val chip = categoryContainer.getChildAt(i) as TextView
            val gid  = chip.tag as Int
            if (gid == selectedId) {
                chip.setBackgroundResource(R.drawable.category_selected_bg)
                chip.setTextColor(ContextCompat.getColor(this, R.color.button_ungu))
            } else {
                chip.setBackgroundResource(R.drawable.category_bg)
                chip.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
        }
    }
}
