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
import com.example.kisskh.ui.screens.DetailScreen
import com.example.kisskh.ui.screens.HomeScreen
import com.example.kisskh.ui.screens.PlayerScreen
import com.example.kisskh.ui.theme.KissKhTheme
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
                        
                        PlayerScreen(
                            videoUrl = videoUrl,
                            movieId = movieId,
                            episodeId = episodeId,
                            episodeNumber = episodeNum,
                            episodeTitle = episodeTitle,
                            onBack = { navController.popBackStack() }
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
