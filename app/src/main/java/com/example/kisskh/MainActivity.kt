package com.example.kisskh

import android.app.UiModeManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kisskh.data.AppRepository
import com.example.kisskh.ui.screens.DetailScreen
import com.example.kisskh.ui.screens.HomeScreen
import com.example.kisskh.ui.screens.PlayerScreen
import com.example.kisskh.ui.theme.KissKhTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set orientation based on device type
        if (isTvDevice()) {
            // Lock to landscape for TV devices
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            // Allow auto-rotation for mobile devices
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        setContent {
            KissKhTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    
                    composable("home") {
                        com.example.kisskh.ui.screens.MainScreen(
                            onMovieClick = { movieId ->
                                navController.navigate("detail/$movieId")
                            },
                            onEpisodeClick = { episode ->
                                val encodedUrl = URLEncoder.encode(episode.videoUrl, StandardCharsets.UTF_8.toString())
                                val encodedTitle = URLEncoder.encode(episode.title, StandardCharsets.UTF_8.toString())
                                navController.navigate("player/$encodedUrl/${episode.movieId}/${episode.id}/${episode.number}/$encodedTitle")
                            }
                        )
                    }

                    composable(
                        "detail/{movieId}",
                        arguments = listOf(navArgument("movieId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                        DetailScreen(
                            movieId = movieId,
                            onEpisodeClick = { _, _ -> }, // Deprecated
                            onVideoReady = { videoUrl, episodeId, episodeNum, episodeTitle ->
                                val encodedUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString())
                                val encodedTitle = URLEncoder.encode(episodeTitle, StandardCharsets.UTF_8.toString())
                                navController.navigate("player/$encodedUrl/${movieId}/$episodeId/$episodeNum/$encodedTitle")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "player/{videoUrl}/{movieId}/{episodeId}/{episodeNum}/{episodeTitle}",
                        arguments = listOf(
                            navArgument("videoUrl") { type = NavType.StringType },
                            navArgument("movieId") { type = NavType.StringType },
                            navArgument("episodeId") { type = NavType.StringType },
                            navArgument("episodeNum") { type = NavType.StringType },
                            navArgument("episodeTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                        val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                        val episodeId = backStackEntry.arguments?.getString("episodeId") ?: ""
                        val episodeNum = backStackEntry.arguments?.getString("episodeNum") ?: ""
                        val episodeTitle = backStackEntry.arguments?.getString("episodeTitle") ?: ""
                        
                        // Handler for next episode auto-play
                        val onNextEpisode: (String, String) -> Unit = { movieIdParam, currentEpisodeNumber ->
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    // Fetch episodes list
                                    val episodes = AppRepository.getEpisodes(movieIdParam)
                                    if (episodes.isEmpty()) return@launch
                                    
                                    // Parse current episode number to Float
                                    val currentNum = currentEpisodeNumber.toFloatOrNull() ?: 0f
                                    
                                    // Sort episodes by number and find next one
                                    val sortedEpisodes = episodes.sortedBy { it.number.toFloatOrNull() ?: 0f }
                                    val nextEpisode = sortedEpisodes.find { 
                                        val epNum = it.number.toFloatOrNull() ?: 0f
                                        epNum > currentNum
                                    }
                                    
                                    if (nextEpisode != null) {
                                        // Fetch stream URL for next episode
                                        val streamUrl = AppRepository.getStreamUrl(nextEpisode.id)
                                        
                                        val finalUrl = if (streamUrl.isNotEmpty()) {
                                            streamUrl
                                        } else {
                                            // Fallback to WebView URL pattern (same as DetailScreen)
                                            val movie = AppRepository.getMovieDetails(movieIdParam)
                                            if (movie != null) {
                                                val slug = movie.title.replace(" ", "-").replace(Regex("[^a-zA-Z0-9-]"), "")
                                                "https://kisskh.co/Drama/$slug/Episode-${nextEpisode.number}?id=${movieIdParam}&ep=${nextEpisode.id}&page=0&pageSize=100"
                                            } else {
                                                return@launch
                                            }
                                        }
                                        
                                        // Navigate to next episode (replace current player to avoid back stack issues)
                                        val encodedUrl = URLEncoder.encode(finalUrl, StandardCharsets.UTF_8.toString())
                                        val encodedTitle = URLEncoder.encode(nextEpisode.title, StandardCharsets.UTF_8.toString())
                                        val nextEpisodeRoute = "player/$encodedUrl/${movieIdParam}/${nextEpisode.id}/${nextEpisode.number}/$encodedTitle"
                                        
                                        // Pop current player screen and navigate to next episode
                                        navController.popBackStack()
                                        navController.navigate(nextEpisodeRoute)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Fail silently - user can manually navigate
                                }
                            }
                        }
                        
                        // Handler to pop back to last non-player screen
                        val onBack: () -> Unit = {
                            // Pop back until we reach a non-player screen (home or detail)
                            // This handles the case where multiple episodes were auto-played
                            while (true) {
                                val previousEntry = navController.previousBackStackEntry
                                if (previousEntry == null) {
                                    // No previous screen, just pop once and exit
                                    navController.popBackStack()
                                    break
                                }
                                
                                val previousRoute = previousEntry.destination.route
                                if (previousRoute != null && previousRoute.startsWith("player/")) {
                                    // Previous screen is also a player, pop it and continue
                                    navController.popBackStack()
                                } else {
                                    // Found a non-player screen (home or detail), pop to it and exit
                                    navController.popBackStack()
                                    break
                                }
                            }
                        }
                        
                        PlayerScreen(
                            videoUrl = videoUrl,
                            movieId = movieId,
                            episodeId = episodeId,
                            episodeNumber = episodeNum,
                            episodeTitle = episodeTitle,
                            onBack = onBack,
                            onNextEpisode = onNextEpisode
                        )
                    }
                }
            }
        }
    }
    
    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
