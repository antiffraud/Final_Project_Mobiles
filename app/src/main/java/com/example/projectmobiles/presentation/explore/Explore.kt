package com.example.projectmobiles.presentation.explore

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.PagerSnapHelper
import com.bumptech.glide.Glide
import com.example.projectmobiles.BuildConfig
import com.example.projectmobiles.domain.usecase.DotIndicatorUtil
import com.example.projectmobiles.R
import com.example.projectmobiles.data.local.DatabaseHelper
import com.example.projectmobiles.data.model.GenreResponse
import com.example.projectmobiles.data.model.Movie
import com.example.projectmobiles.data.model.MovieResponse
import com.example.projectmobiles.data.remote.ApiClient
import com.example.projectmobiles.presentation.detail.DetailActivity
import com.example.projectmobiles.presentation.seeAll.Allmovies
import com.example.projectmobiles.presentation.seeAll.MovieAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class explore : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var upcomingRecyclerView: RecyclerView
    private lateinit var nowRecycler: RecyclerView
    private lateinit var categoryContainer: LinearLayout
    private lateinit var dotLayout: LinearLayout
    private lateinit var etSearch: EditText
    private var allMovies: List<Movie> = listOf()
    private var selectedCategoryId: Int? = null
    private lateinit var imgProfile: ImageView
    private lateinit var tvGreeting: TextView
    private lateinit var tvEmail: TextView
    private val nowPlayingIds   = mutableSetOf<Int>()
    private val upcomingIds     = mutableSetOf<Int>()
    private lateinit var dbHelper: DatabaseHelper
    private val apiKey = BuildConfig.TMDB_API_KEY

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_explore, container, false)
        imgProfile = view.findViewById(R.id.imgProfile)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvEmail = view.findViewById(R.id.tvEmail)
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            val displayName = user.displayName ?: "User"
            val email = user.email ?: "unknown@email.com"
            val photoUrl = user.photoUrl

            tvGreeting.text = "Hello, $displayName"
            tvEmail.text = email

            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .into(imgProfile)
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile)
            }
        }
        val btnSeeAllNow = view.findViewById<Button>(R.id.btnNowSeeAll)
        dbHelper = DatabaseHelper(requireContext())
        btnSeeAllNow.setOnClickListener {
            val intent = Intent(requireContext(), Allmovies::class.java)
                intent.putExtra("MOVIE_TYPE", "NOW")
                startActivity(intent)
        }
        val btnSeeAllTopMovies = view.findViewById<Button>(R.id.btnRecommendationSeeAll)
        val btnSeeAllUpcomingMovies = view.findViewById<Button>(R.id.btnUpcomingSeeAll)

        btnSeeAllTopMovies.setOnClickListener {
            val intent = Intent(requireContext(), Allmovies::class.java)
            intent.putExtra("MOVIE_TYPE", "TOP")
            startActivity(intent)
        }

        btnSeeAllUpcomingMovies.setOnClickListener {
            val intent = Intent(requireContext(), Allmovies::class.java)
            intent.putExtra("MOVIE_TYPE", "UPCOMING")
            startActivity(intent)
        }

        nowRecycler = view.findViewById(R.id.rvNowplay)
        nowRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView = view.findViewById(R.id.rvMovies)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        upcomingRecyclerView = view.findViewById(R.id.rvUpcoming)
        upcomingRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        categoryContainer = view.findViewById(R.id.categoryContainer)

        dotLayout = view.findViewById(R.id.dotIndicator)

        etSearch = view.findViewById(R.id.etSearch)

        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchMovies(query)

                    etSearch.clearFocus()
                    val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                }
                true
            } else {
                false
            }
        }

        fetchMovies()
        fetchNowPlaying()
        fetchUpcomingMovies()
        fetchGenres()
        fetchMarvelMovies(view)

        return view
    }

    private fun fetchMovies() {
        ApiClient.tmdbApi.getTopRatedMovies(apiKey).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val movies = response.body()?.results ?: emptyList()
                    allMovies = movies
                    recyclerView.adapter = MovieAdapter( movies, onItemClick = { movie ->
                        val isRestricted = movie.id in nowPlayingIds || movie.id in upcomingIds
                        val intent = Intent(requireContext(), DetailActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("IS_RESTRICTED", isRestricted)
                        startActivity(intent) }
                    )
                }
            }

            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(context, "Gagal ambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchNowPlaying() {
        ApiClient.tmdbApi.getNowPlayingMovies(apiKey, page = 1).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val movies = response.body()?.results ?: emptyList()
                    nowPlayingIds.clear()
                    nowPlayingIds.addAll(movies.map { it.id })
                    nowRecycler.adapter = MovieAdapter( movies, onItemClick = { movie ->
                        val isRestricted = movie.id in nowPlayingIds || movie.id in upcomingIds
                        val intent = Intent(requireContext(), DetailActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("IS_RESTRICTED", isRestricted)
                        startActivity(intent) }
                    )
            }
        }
            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(context, "Gagal ambil data Now Playing", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUpcomingMovies() {
        ApiClient.tmdbApi.getUpcomingMovies(apiKey).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val movies = response.body()?.results ?: emptyList()
                    upcomingIds.clear()
                    upcomingIds.addAll(movies.map { it.id })
                    upcomingRecyclerView.adapter = MovieAdapter( movies, onItemClick = { movie ->
                        val isRestricted = movie.id in nowPlayingIds || movie.id in upcomingIds
                        val intent = Intent(requireContext(), DetailActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("IS_RESTRICTED", isRestricted)
                        startActivity(intent) }
                    )
                }
            }

            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(context, "Gagal ambil data upcoming", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchMoviesByGenre(genreId: Int) {
        ApiClient.tmdbApi.getMoviesByGenre(apiKey, genreId).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val movies = response.body()?.results ?: emptyList()
                    recyclerView.adapter = MovieAdapter( movies, onItemClick = { movie ->
                        val isRestricted = movie.id in nowPlayingIds || movie.id in upcomingIds
                        val intent = Intent(requireContext(), DetailActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("IS_RESTRICTED", isRestricted)
                        startActivity(intent) }
                    )
                }
            }

            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(context, "Gagal ambil film genre", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchGenres() {
        ApiClient.tmdbApi.getGenres(apiKey).enqueue(object : Callback<GenreResponse> {
            override fun onResponse(call: Call<GenreResponse>, response: Response<GenreResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val genres = response.body()?.genres ?: emptyList()
                    for (genre in genres) {
                        val chip = TextView(context).apply {
                            text = genre.name
                            tag = genre.id
                            setPadding(32, 16, 32, 16)
                            setTextColor(resources.getColor(android.R.color.white, null))
                            background = resources.getDrawable(R.drawable.category_bg, null)
                            layoutParams = ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                rightMargin = 16
                            }

                            setOnClickListener {
                                val clickedGenreId = tag as Int

                                if (selectedCategoryId == clickedGenreId) {
                                    selectedCategoryId = null

                                    // Reset tampilan semua kategori
                                    for (j in 0 until categoryContainer.childCount) {
                                        val otherCategory = categoryContainer.getChildAt(j)
                                        otherCategory.setBackgroundResource(R.drawable.category_bg)
                                        (otherCategory as TextView).setTextColor(ContextCompat.getColor(context,
                                            R.color.category_text_inactive
                                        ))
                                    }

                                    fetchMovies()
                                } else {
                                    selectedCategoryId = clickedGenreId

                                    for (j in 0 until categoryContainer.childCount) {
                                        val otherCategory = categoryContainer.getChildAt(j)
                                        val genreId = otherCategory.tag as Int

                                        if (genreId == clickedGenreId) {
                                            otherCategory.setBackgroundResource(R.drawable.category_selected_bg)
                                            (otherCategory as TextView).setTextColor(ContextCompat.getColor(context,
                                                R.color.button_ungu
                                            ))
                                        } else {
                                            otherCategory.setBackgroundResource(R.drawable.category_bg)
                                            (otherCategory as TextView).setTextColor(ContextCompat.getColor(context,
                                                R.color.white
                                            ))
                                        }
                                    }

                                    fetchMoviesByGenre(clickedGenreId)
                                }
                            }
                        }
                        categoryContainer.addView(chip)
                    }
                }
            }

            override fun onFailure(call: Call<GenreResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(context, "Gagal ambil genre", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchMarvelMovies(view: View) {
        ApiClient.tmdbApi.getMarvelStudiosMovies(apiKey).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val movieResults = response.body()?.results?.take(10) ?: emptyList()
                    val detailedMovies = mutableListOf<Movie>()

                    // Ambil detail tiap film secara paralel
                    for (movie in movieResults) {
                        ApiClient.tmdbApi.getMovieDetails(movie.id, apiKey).enqueue(object : Callback<Movie> {
                            override fun onResponse(call: Call<Movie>, detailResponse: Response<Movie>) {
                                if (detailResponse.isSuccessful) {
                                    val details = detailResponse.body()
                                    if (details != null) {
                                        val enrichedMovie = movie.copy(
                                            genres = details.genres,
                                            runtime = details.runtime,
                                            releaseDate = movie.releaseDate,
                                            isCarousel = true
                                        )
                                        detailedMovies.add(enrichedMovie)

                                        // Saat semua detail film sudah terkumpul
                                        if (detailedMovies.size == movieResults.size) {
                                            setupCarousel(view, detailedMovies)
                                        }
                                    }
                                }
                            }

                            override fun onFailure(call: Call<Movie>, t: Throwable) {
                                Log.e("FetchMovieDetail", "Gagal ambil detail film: ${movie.id}")
                            }
                        })
                    }
                }
            }

            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(context, "Gagal ambil film Marvel", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupCarousel(view: View, movies: List<Movie>) {
        val loopedMovies = mutableListOf<Movie>()
        repeat(30) { loopedMovies.addAll(movies) }

        val marvelCarousel = view.findViewById<RecyclerView>(R.id.rvCarousel)
        marvelCarousel.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        marvelCarousel.adapter = MovieAdapter(
            loopedMovies,
            onItemClick = { movie ->
                val isRestricted = movie.id in nowPlayingIds || movie.id in upcomingIds
                val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra("MOVIE_ID", movie.id)
                    putExtra("IS_RESTRICTED", isRestricted)
                }
                startActivity(intent)
                },
            onAddToWatchlist = { movie ->
                dbHelper.addBookmark(movie)
                Toast.makeText(requireContext(),
                    "Added “${movie.title}” to Watchlist",
                    Toast.LENGTH_SHORT
                            ).show()
                }
        )
        marvelCarousel.scrollToPosition(loopedMovies.size / 2)

        applyCarouselEffect(marvelCarousel)

        if (marvelCarousel.onFlingListener == null) {
            PagerSnapHelper().attachToRecyclerView(marvelCarousel)
        }
        marvelCarousel.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (position != RecyclerView.NO_POSITION) {
                    DotIndicatorUtil.updateMovingDots(requireContext(), dotLayout, position)
                }
            }
        })

        startAutoScroll(marvelCarousel)
        DotIndicatorUtil.updateMovingDots(requireContext(), dotLayout, 0)
    }


    private fun startAutoScroll(recyclerView: RecyclerView) {
        val scrollInterval = 3000L // scroll setiap 3 detik
        val handler = Handler(Looper.getMainLooper())
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val adapter = recyclerView.adapter ?: return
        val itemCount = adapter.itemCount

        if (itemCount == 0) return

        // Mulai dari tengah agar infinite scroll kelihatan halus
        val initialPosition = itemCount / 2
        recyclerView.scrollToPosition(initialPosition)
        val autoScrollRunnable = object : Runnable {
            override fun run() {
                val currentPosition = layoutManager.findFirstCompletelyVisibleItemPosition()

                val nextPosition = if (currentPosition == RecyclerView.NO_POSITION) {
                    initialPosition
                } else {
                    currentPosition + 1
                }

                recyclerView.smoothScrollToPosition(nextPosition)
                handler.postDelayed(this, scrollInterval)
            }
        }
        // Start the loop
        handler.postDelayed(autoScrollRunnable, scrollInterval)
    }

    private fun applyCarouselEffect(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val center = recyclerView.width / 2
                val d0 = 0f
                val d1 = 0.9f * center // batas maksimum pengaruh efek
                val s0 = 1f
                val s1 = 0.85f // ukuran terkecil item di pinggir

                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(i)
                    val childCenter = (child.left + child.right) / 2
                    val distance = kotlin.math.min(d1, kotlin.math.abs(center - childCenter).toFloat())
                    val scale = s0 + (s1 - s0) * (distance - d0) / (d1 - d0)
                    child.scaleX = scale
                    child.scaleY = scale
                }
            }
        })
    }

    private fun searchMovies(query: String) {
        ApiClient.tmdbApi.searchMovies(apiKey, query).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val movies = response.body()?.results ?: emptyList()
                    recyclerView.adapter = MovieAdapter( movies, onItemClick = { movie ->
                        val isRestricted = movie.id in nowPlayingIds || movie.id in upcomingIds
                        val intent = Intent(requireContext(), DetailActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("IS_RESTRICTED", isRestricted)
                        startActivity(intent) }
                    )
                }
            }

            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(context, "Gagal mencari film", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
