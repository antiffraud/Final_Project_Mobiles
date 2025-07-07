package com.example.projectmobiles.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Movie(
    val id: Int,
    val title: String,

    @SerializedName("poster_path")
    val poster_path: String?,

    @SerializedName("release_date")
    val releaseDate: String?,

    val runtime: Int?,

    val genres: List<Genre>? = emptyList(),

    @SerializedName("vote_average")
    val voteAverage: Double? = null,

    val overview: String? = "",

    val isCarousel: Boolean = false,

    var price: Double? = null
) : Parcelable