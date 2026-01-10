package com.example.kisskh.data

import android.content.Context
import android.content.SharedPreferences
import com.example.kisskh.data.model.Episode
import com.example.kisskh.data.model.Movie
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LocalStorage {
    private const val PREF_NAME = "KissKH_Prefs"
    private const val KEY_WATCHLIST = "watchlist"
    private const val KEY_HISTORY = "history"
    
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // --- WATCHLIST ---
    fun getWatchlist(): List<Movie> {
        val json = prefs.getString(KEY_WATCHLIST, "[]")
        return try {
            val type = object : TypeToken<List<Movie>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveToWatchlist(movie: Movie) {
        val list = getWatchlist().toMutableList()
        if (list.none { it.id == movie.id }) {
            list.add(0, movie) // Add to top
            prefs.edit().putString(KEY_WATCHLIST, gson.toJson(list)).apply()
        }
    }

    fun removeFromWatchlist(movieId: String) {
        val list = getWatchlist().toMutableList()
        if (list.removeAll { it.id == movieId }) {
            prefs.edit().putString(KEY_WATCHLIST, gson.toJson(list)).apply()
        }
    }

    fun isWatchlisted(movieId: String): Boolean {
        return getWatchlist().any { it.id == movieId }
    }

    // --- HISTORY ---
    fun getHistory(): List<Episode> {
        val json = prefs.getString(KEY_HISTORY, "[]")
        return try {
            val type = object : TypeToken<List<Episode>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addToHistory(episode: Episode) {
        val list = getHistory().toMutableList()
        // Remove existing entry for this episode if any (to update position to top)
        list.removeAll { it.id == episode.id }
        // Remove any other episode from the same show (same movieId)
        // This ensures that when a new episode starts, it replaces the old one in history
        list.removeAll { it.movieId == episode.movieId && it.id != episode.id }
        
        // Add the episode to top
        list.add(0, episode)
        
        // Limit history size (e.g. 50 items)
        if (list.size > 50) {
            list.removeAt(list.size - 1)
        }
        
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
    }

    fun updateHistoryProgress(episodeId: String, timestamp: Long, duration: Long, episode: Episode? = null) {
        val list = getHistory().toMutableList()
        val index = list.indexOfFirst { it.id == episodeId }
        
        if (index != -1) {
            val oldEpisode = list[index]
            // Preserve movieTitle from old episode if new one doesn't have it, otherwise use new one
            val movieTitle = episode?.movieTitle ?: oldEpisode.movieTitle
            val newEpisode = oldEpisode.copy(
                timestamp = timestamp,
                duration = duration,
                movieTitle = movieTitle
            )
            
            // Remove current episode first to avoid index shifting issues
            list.removeAt(index)
            
            // Remove any other episode from the same show (same movieId)
            list.removeAll { it.movieId == oldEpisode.movieId }
            
            // Add updated episode to top
            list.add(0, newEpisode)
            
            prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
        } else if (episode != null) {
            // Episode not in history yet, and we have full episode data
            // Remove any other episode from the same show (same movieId)
            list.removeAll { it.movieId == episode.movieId }
            
            // Add the episode with updated timestamp and duration
            val newEpisode = episode.copy(timestamp = timestamp, duration = duration)
            list.add(0, newEpisode)
            
            // Limit history size
            if (list.size > 50) {
                list.removeAt(list.size - 1)
            }
            
            prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
        }
    }
    
    fun updateEpisodeMovieTitle(episodeId: String, movieTitle: String) {
        val list = getHistory().toMutableList()
        val index = list.indexOfFirst { it.id == episodeId }
        if (index != -1) {
            val episode = list[index]
            if (episode.movieTitle == null) {
                val updatedEpisode = episode.copy(movieTitle = movieTitle)
                list[index] = updatedEpisode
                prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
            }
        }
    }
}
