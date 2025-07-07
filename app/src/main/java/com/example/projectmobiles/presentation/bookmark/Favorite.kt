package com.example.projectmobiles.presentation.bookmark

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmobiles.R
import com.example.projectmobiles.data.local.DatabaseHelper
import com.example.projectmobiles.presentation.detail.DetailActivity

class favorite : Fragment() {
    private lateinit var bookmarkAdapter: BookmarkAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("MovieApp", "Favorite Fragment onCreateView started")
        dbHelper = DatabaseHelper(requireContext())

        val root = inflater.inflate(R.layout.fragment_favorite, container, false)
        recyclerView = root.findViewById(R.id.recyclerViewFavorite)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)

        bookmarkAdapter = BookmarkAdapter(
            emptyList(),
            onItemClick = { movie ->
                val intent = Intent(requireContext(), DetailActivity::class.java)
                intent.putExtra("MOVIE_ID", movie.id)
                startActivity(intent)
            },
            onRemoveClick = { movie ->
                dbHelper.removeBookmark(movie.id)
                loadBookmarks()
            }
        )

        recyclerView.adapter = bookmarkAdapter
        loadBookmarks()
        return root
    }

    override fun onResume() {
        super.onResume()
        loadBookmarks()
    }

   private fun loadBookmarks() {
        val list = dbHelper.getAllBookmarks()
        Log.d("MovieApp", "Loaded ${list.size} bookmarks from SQLite")
        bookmarkAdapter.updateMovies(list)
        }
}

