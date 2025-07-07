package com.example.projectmobiles.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.projectmobiles.domain.usecase.CartItem
import com.example.projectmobiles.data.model.Movie

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "MovieApp.db"
        const val DATABASE_VERSION = 7


        const val TABLE_BOOKMARK = "bookmark"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_OVERVIEW = "overview"
        const val COLUMN_POSTER_PATH = "poster_path"
        const val COLUMN_RELEASE   = "release_date"
        const val COLUMN_RUNTIME ="runtime"

        const val TABLE_CART = "cart"
        const val CART_COL_ID = "id"
        const val CART_COL_TITLE = "title"
        const val CART_COL_POSTER_PATH = "poster_path"
        const val CART_COL_PRICE = "price"
        const val CART_COL_QUANTITY = "quantity"
        const val CART_COL_RELEASE = "release_date"

    }

    override fun onCreate(db: SQLiteDatabase) {
        val createBookmarkTable = """
            CREATE TABLE $TABLE_BOOKMARK (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_TITLE TEXT,
                $COLUMN_OVERVIEW TEXT,
                $COLUMN_POSTER_PATH TEXT,
                $COLUMN_RELEASE  TEXT,
                $COLUMN_RUNTIME INT
            )
        """.trimIndent()
        db.execSQL(createBookmarkTable)

        val createCartTable = """
            CREATE TABLE $TABLE_CART (
                $CART_COL_ID INTEGER PRIMARY KEY,
                $CART_COL_TITLE TEXT,
                $CART_COL_POSTER_PATH TEXT,
                $CART_COL_PRICE REAL,
                $CART_COL_QUANTITY INTEGER,
                $CART_COL_RELEASE  TEXT   
            )
        """.trimIndent()
        db.execSQL(createCartTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARK")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CART")
        onCreate(db)
    }

    // Add bookmark using Movie model
    fun addBookmark(movie: Movie) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, movie.id)
            put(COLUMN_TITLE, movie.title)
            put(COLUMN_OVERVIEW, movie.overview)
            put(COLUMN_POSTER_PATH, movie.poster_path)
            put(COLUMN_RELEASE, movie.releaseDate)
            put(COLUMN_RUNTIME, movie.runtime)
        }
        db.insert(TABLE_BOOKMARK, null, values)
        db.close()
    }

    // Remove bookmark by ID
    fun removeBookmark(movieId: Int) {
        val db = writableDatabase
        db.delete(TABLE_BOOKMARK, "$COLUMN_ID=?", arrayOf(movieId.toString()))
        db.close()
    }

    // Get all bookmarks as List<Movie>
    fun getAllBookmarks(): List<Movie> {
        val bookmarks = mutableListOf<Movie>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_BOOKMARK", null)

        if (cursor.moveToFirst()) {
            do {
                val movie = Movie(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    poster_path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POSTER_PATH)),
                    releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RELEASE)),
                    runtime = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RUNTIME)),
                    genres = emptyList(),
                    voteAverage = null,
                    overview = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OVERVIEW)),
                    isCarousel = false,
                    price = null
                )
                bookmarks.add(movie)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return bookmarks
    }

    // Check if a movie is bookmarked
    fun isMovieBookmarked(movieId: Int): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKMARK,
            arrayOf(COLUMN_ID),
            "$COLUMN_ID=?",
            arrayOf(movieId.toString()),
            null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }

    fun addCartItem(movie: Movie, quantity: Int = 1) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(CART_COL_ID, movie.id)
            put(CART_COL_TITLE, movie.title)
            put(CART_COL_POSTER_PATH, movie.poster_path)
            put(CART_COL_PRICE, movie.price)
            put(CART_COL_QUANTITY, quantity)
            put(CART_COL_RELEASE, movie.releaseDate)
        }
        db.insertWithOnConflict(TABLE_CART, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun updateCartQuantity(movieId: Int, quantity: Int) {
        val db = writableDatabase
        val values = ContentValues().apply { put(CART_COL_QUANTITY, quantity) }
        db.update(TABLE_CART, values, "$CART_COL_ID=?", arrayOf(movieId.toString()))
        db.close()
    }

    fun removeCartItem(movieId: Int) {
        val db = writableDatabase
        db.delete(TABLE_CART, "$CART_COL_ID=?", arrayOf(movieId.toString()))
        db.close()
    }

    fun clearCart() {
        val db = writableDatabase
        db.delete(TABLE_CART, null, null)
        db.close()
    }

    fun getAllCartItems(): List<CartItem> {
        val items = mutableListOf<CartItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CART", null)
        if (cursor.moveToFirst()) {
            do {
                val movie = Movie(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(CART_COL_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(CART_COL_TITLE)),
                    poster_path = cursor.getString(cursor.getColumnIndexOrThrow(CART_COL_POSTER_PATH)),
                    releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(CART_COL_RELEASE)),
                    runtime = null,
                    genres = emptyList(),
                    voteAverage = null,
                    overview = "",
                    isCarousel = false,
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow(CART_COL_PRICE))
                )
                val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(CART_COL_QUANTITY))
                items.add(CartItem(movie, quantity))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return items
    }

    fun logCartSchema() {
        val db = readableDatabase
        val cursor = db.rawQuery("PRAGMA table_info($TABLE_CART)", null)
        Log.d("DB_SCHEMA", "—— cart table columns ——")
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
            Log.d("DB_SCHEMA", "  $name : $type")
        }
        cursor.close()
    }
}
