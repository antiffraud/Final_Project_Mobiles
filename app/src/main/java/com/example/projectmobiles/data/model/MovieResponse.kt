package com.example.projectmobiles.data.model

import android.os.Parcelable
import com.example.projectmobiles.domain.repository.GenreParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<Genre, GenreParceler>
data class MovieResponse(
    val results: List<Movie>
) : Parcelable
