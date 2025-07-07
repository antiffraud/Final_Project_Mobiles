package com.example.projectmobiles.domain.repository

import android.os.Parcel
import com.example.projectmobiles.data.model.Genre
import kotlinx.parcelize.Parceler

class GenreParceler : Parceler<Genre> {
    override fun create(parcel: Parcel): Genre {
        val id = parcel.readInt()
        val name = parcel.readString() ?: ""
        return Genre(id, name)
    }

    override fun Genre.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
    }
}
