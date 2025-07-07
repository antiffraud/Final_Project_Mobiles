package com.example.projectmobiles.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class GenreResponse(
    val genres: List<Genre>
)
@Parcelize
data class Genre(
    val id: Int,
    val name: String
) : Parcelable
