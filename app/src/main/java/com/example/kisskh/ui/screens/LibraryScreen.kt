package com.example.kisskh.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.kisskh.data.LocalStorage
import com.example.kisskh.data.model.Episode
import com.example.kisskh.data.model.Movie
import com.example.kisskh.ui.components.FocusableWrapper
import com.example.kisskh.ui.theme.BackgroundColor
import com.example.kisskh.ui.theme.White

@Composable
fun LibraryScreen(
    onMovieClick: (String) -> Unit,
    onEpisodeClick: (Episode) -> Unit // Navigates to Player
) {
    var selectedTab by remember { mutableStateOf(1) } // Default to History tab
    val tabs = listOf("Watchlist", "History")

    // Loads data every time screen is composed/tab changes
    var watchlist by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var history by remember { mutableStateOf<List<Episode>>(emptyList()) }

    LaunchedEffect(Unit) {
        watchlist = LocalStorage.getWatchlist()
        history = LocalStorage.getHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
    ) {
        // --- HEADER ---
        Text(
            "Library",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // --- TABS ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BackgroundColor,
            contentColor = White,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color.Red
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) White else Color.Gray) }
                )
            }
        }

        // --- CONTENT ---
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedTab == 0) {
                // WATCHLIST
                if (watchlist.isEmpty()) {
                    EmptyState("No show is watchlisted yet. Start adding shows to your watchlist!")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(watchlist) { movie ->
                            MovieThumbnail(movie = movie, onClick = { onMovieClick(movie.id) })
                        }
                    }
                }
            } else {
                // HISTORY
                if (history.isEmpty()) {
                    EmptyState("Start watching a show to see your history here.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { episode ->
                            HistoryItem(episode = episode, onClick = { 
                                onEpisodeClick(episode)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray)
    }
}

@Composable
fun HistoryItem(episode: Episode, onClick: () -> Unit) {
    FocusableWrapper(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.movieTitle ?: episode.title,
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Episode ${episode.number}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = White
                )
            }
            
            // Progress Bar
            if (episode.duration > 0 && episode.timestamp > 0) {
                val progress = (episode.timestamp.toFloat() / episode.duration.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .fillMaxHeight()
                            .background(Color.Red)
                    )
                }
            }
        }
    }
}
