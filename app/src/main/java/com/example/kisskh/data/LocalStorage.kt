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
        list.add(0, episode)
        
        // Limit history size (e.g. 50 items)
        if (list.size > 50) {
            list.removeAt(list.size - 1)
        }
        
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
    }

    fun updateHistoryProgress(episodeId: String, timestamp: Long, duration: Long) {
        val list = getHistory().toMutableList()
        val index = list.indexOfFirst { it.id == episodeId }
        
        if (index != -1) {
            val oldEpisode = list[index]
            val newEpisode = oldEpisode.copy(timestamp = timestamp, duration = duration)
            
            // Move to top to indicate recent activity
            list.removeAt(index)
            list.add(0, newEpisode)
            
            prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
        }
    }
}
