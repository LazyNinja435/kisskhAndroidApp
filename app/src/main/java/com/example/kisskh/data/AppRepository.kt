package com.example.kisskh.data

import com.example.kisskh.data.model.Episode
import com.example.kisskh.data.model.Movie
import com.example.kisskh.data.network.NetworkModule

object AppRepository {
    private val api = NetworkModule.api

    suspend fun searchMovies(query: String): List<Movie> {
        return try {
            val results = api.search(query)
            results.map { 
                Movie(
                    id = it.id?.toString() ?: "0",
                    title = it.title ?: "Unknown Title",
                    description = "", // Search doesn't return full details
                    posterUrl = it.image ?: "",
                    bannerUrl = it.image ?: "",
                    genre = emptyList(),
                    year = 0,
                    country = "",
                    rating = 0f,
                    status = ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(
                Movie(
                    id = "error",
                    title = "Err: ${e.message ?: e.toString()}",
                    description = e.stackTraceToString(),
                    posterUrl = "",
                    bannerUrl = "",
                    genre = emptyList(),
                    year = 0,
                    country = "",
                    rating = 0f,
                    status = ""
                )
            )
        }
    }

    suspend fun getMovieDetails(id: String): Movie? {
        return try {
            val details = api.getDramaDetails(id)
            Movie(
                id = details.id.toString(),
                title = details.title,
                description = details.description ?: "No description available.",
                posterUrl = details.image ?: "",
                bannerUrl = details.image ?: "", // Banner might be different in real app
                genre = listOf(), // Detail typically implies genre but simple DTO didn't show it
                year = 0,
                country = details.country ?: "",
                rating = 0f,
                status = details.status ?: ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getEpisodes(movieId: String): List<Episode> {
        return try {
            val details = api.getDramaDetails(movieId)
            details.episodes?.map { 
                Episode(
                    id = it.id.toString(),
                    movieId = movieId,
                    number = it.number.toString(),
                    title = "Episode ${it.number}",
                    videoUrl = "", // We fetch this on click
                    thumbnailUrl = null
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getStreamUrl(episodeId: String): String {
        return try {
            val response = api.getStreamUrl(episodeId)
            val url = response.url
            android.util.Log.d("AppRepository", "getStreamUrl: ID=$episodeId, ParsedURL=$url, Response=$response")
            url
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to fetch stream URL for ID=$episodeId", e)
            e.printStackTrace()
            ""
        }
    }
}
