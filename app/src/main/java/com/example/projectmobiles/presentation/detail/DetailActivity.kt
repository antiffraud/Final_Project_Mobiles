package com.example.projectmobiles.presentation.detail

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.projectmobiles.databinding.ActivityDetailBinding
import eightbitlab.com.blurview.RenderScriptBlur
import androidx.activity.result.contract.ActivityResultContracts
import com.example.projectmobiles.BuildConfig
import com.example.projectmobiles.R
import com.example.projectmobiles.data.local.DatabaseHelper
import com.example.projectmobiles.data.model.CastResponse
import com.example.projectmobiles.data.model.Movie
import com.example.projectmobiles.data.remote.ApiClient
import com.midtrans.sdk.uikit.internal.util.UiKitConstants
import com.midtrans.sdk.uikit.api.model.TransactionResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private var isBookmarked = false
    private var isRestricted = false
    private var isPurchased  = false
    private lateinit var currentMovie: Movie
    private val cartManager = CartManager.getInstance()
    private lateinit var dbHelper: DatabaseHelper
    private var movieId: Int = -1
    private val apiKey = BuildConfig.TMDB_API_KEY

    private val midtransLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val tr = result.data
                    ?.getParcelableExtra<TransactionResult>(UiKitConstants.KEY_TRANSACTION_RESULT)
                tr?.let { handleMidtransResult(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = DatabaseHelper(this)

        cartManager.initialize(applicationContext)
        Log.d("MovieApp", "DetailActivity onCreate started")

        binding.backImg.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.genreView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.castView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        movieId = intent.getIntExtra("MOVIE_ID", -1)
        if (movieId != -1) {
            fetchMovieDetails(movieId)
            fetchMovieCredits(movieId)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.cart.setOnClickListener {
            if (::currentMovie.isInitialized) {
                cartManager.addToCart(currentMovie)
                Toast.makeText(this, "${currentMovie.title} ditambahkan ke keranjang", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Watch trailer button
        binding.watchTrailer.setOnClickListener {
            if (::currentMovie.isInitialized) {
                Toast.makeText(this, "Watching The Movies of ${currentMovie.title}", Toast.LENGTH_SHORT).show()
            }
        }

        isRestricted = intent.getBooleanExtra("IS_RESTRICTED", false)
        renderButtons()

        // 2) Check Firestore for an existing purchase record
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("payments")
                .whereEqualTo("userId", uid)
                .whereEqualTo("movieId", movieId.toString())
                .whereEqualTo("status", "settlement")
                .get()
                .addOnSuccessListener { snap ->
                    isPurchased = !snap.isEmpty
                    renderButtons()
                }
                .addOnFailureListener { e ->
                    isPurchased = false
                    renderButtons()
                }
        } ?: run {
            isPurchased = false
            renderButtons()
        }
    }

    private fun updateBookmarkIcon() {
        val iconRes = if (isBookmarked) R.drawable.bookmark_hitam else R.drawable.bookmark_putih
        binding.bookmarkImg.setImageResource(iconRes)
    }

    private fun fetchMovieDetails(movieId: Int) {
        ApiClient.tmdbApi.getMovieDetails(movieId, apiKey)
            .enqueue(object : Callback<Movie> {
                override fun onResponse(call: Call<Movie>, response: Response<Movie>) {
                    if (response.isSuccessful) {
                        response.body()?.let { movie ->
                            currentMovie = movie

                            if (currentMovie.price == null) {
                                currentMovie = currentMovie.copy(price = (movie.voteAverage ?: 0.0) * 5000)
                            }

                            binding.textView2.text = movie.title
                            binding.movieSummary.text = movie.overview
                            binding.movieTimeTxt.text = "${movie.releaseDate?.take(4)} - ${movie.runtime} mins"
                            val raw = movie.voteAverage ?: 0.0
                            binding.imdbTxt.text = "IMDB: ${"%.1f".format(raw)}/10"

                            Glide.with(this@DetailActivity)
                                .load("https://image.tmdb.org/t/p/w780${movie.poster_path}")
                                .into(binding.filmPic)

                            movie.genres?.let { genreList ->
                                val adapter = GenreAdapter(genreList)
                                binding.genreView.adapter = adapter
                            }

                            isBookmarked = dbHelper.isMovieBookmarked(movie.id)
                            updateBookmarkIcon()

                            binding.bookmarkImg.setOnClickListener {
                                toggleBookmark()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<Movie>, t: Throwable) {
                    t.printStackTrace()
                }
            })

        val radius = 10f
        val decorView: View = window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground: Drawable = decorView.background

        binding.blurView.setupWith(rootView, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)

        binding.blurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        binding.blurView.clipToOutline = true

        binding.backBlurView.setupWith(rootView, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
        binding.backBlurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        binding.backBlurView.clipToOutline = true
    }

    private fun toggleBookmark() {
        if (::currentMovie.isInitialized) {
            Log.d("MovieApp", "Toggling bookmark for ${currentMovie.title}, currently: $isBookmarked")
            if (isBookmarked) {
                dbHelper.removeBookmark(currentMovie.id)
            } else {
                dbHelper.addBookmark(currentMovie)
            }
            isBookmarked = !isBookmarked
            updateBookmarkIcon()
            Log.d("MovieApp", "After toggle, bookmark status: $isBookmarked")
        }
    }

    private fun fetchMovieCredits(movieId: Int) {
        ApiClient.tmdbApi.getMovieCredits(movieId, apiKey)
            .enqueue(object : Callback<CastResponse> {
                override fun onResponse(call: Call<CastResponse>, response: Response<CastResponse>) {
                    if (response.isSuccessful) {
                        val castList = response.body()?.cast ?: emptyList()
                        val adapter = CastAdapter(castList)
                        binding.castView.adapter = adapter
                    }
                }

                override fun onFailure(call: Call<CastResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun handleMidtransResult(tr: TransactionResult) {
        if (tr.status == UiKitConstants.STATUS_SUCCESS ||
            tr.status == UiKitConstants.STATUS_SETTLEMENT) {

            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                FirebaseFirestore.getInstance()
                    .collection("payments")
                    .add(mapOf(
                        "userId"      to user.uid,
                        "movieId"     to movieId.toString(),
                        "status"      to "settlement",
                        "createdAt"   to FieldValue.serverTimestamp()
                    ))
                    .addOnSuccessListener { docRef ->
                        isPurchased = true
                        renderButtons()
                    }
                    .addOnFailureListener { e ->
                        isPurchased = true
                        renderButtons()
                    }
            } else {
                isPurchased = true
                renderButtons()
            }

                Toast.makeText(this, "Pembayaran Berhasil!", Toast.LENGTH_LONG).show()
                cartManager.clearCart()

        } else if (tr.status == UiKitConstants.STATUS_PENDING) {

            Toast.makeText(this, "Pembayaran Tertunda", Toast.LENGTH_LONG).show()

        } else if (tr.status == UiKitConstants.STATUS_FAILED ||
            tr.status == UiKitConstants.STATUS_CANCELED) {

            Toast.makeText(this, "Pembayaran Gagal atau Dibatalkan", Toast.LENGTH_LONG).show()

        } else {

            Toast.makeText(this, "Status: ${tr.status}", Toast.LENGTH_LONG).show()
        }
    }


    private fun renderButtons() {
        val shouldLock = isRestricted && !isPurchased
        val cartVisible = shouldLock

        binding.cart.visibility = if (cartVisible) View.VISIBLE else View.GONE
        binding.watchTrailer.visibility = View.VISIBLE

        binding.watchTrailer.apply {
            text = "Watch Now"
            val icon = if (shouldLock) R.drawable.close_lock else R.drawable.op_lock
            setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
            post {
                if (!cartVisible) {
                    val parentView = parent as View
                    val parentWidth = parentView.width
                    val btnWidth    = width
                    val centeredX = (parentWidth - btnWidth) / 2f
                    x = centeredX
                } else {
                    x = left.toFloat()
                }
            }
        }
    }

}

