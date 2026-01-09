package com.example.kisskh.data.model

data class Movie(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val bannerUrl: String,
    val genre: List<String>,
    val year: Int,
    val country: String,
    val rating: Float,
    val status: String // "Ongoing", "Completed"
)

data class Episode(
    val id: String,
    val movieId: String,
    val number: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val timestamp: Long = 0, // Position in milliseconds or seconds (saved locally)
    val duration: Long = 0, // Duration in same units as timestamp
    val movieTitle: String? = null // Show/movie title for display in history
)

data class Category(
    val id: String,
    val name: String,
    val movies: List<Movie>
)
