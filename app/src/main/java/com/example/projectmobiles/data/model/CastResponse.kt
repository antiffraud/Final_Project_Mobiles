package com.example.projectmobiles.data.model

data class CastResponse(
    val cast: List<Cast>
)

data class Cast(
    val name: String,
    val profile_path: String?
)
