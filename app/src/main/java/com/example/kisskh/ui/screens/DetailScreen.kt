package com.example.kisskh.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.kisskh.data.AppRepository
import com.example.kisskh.data.model.Episode
import com.example.kisskh.data.model.Movie
import com.example.kisskh.ui.theme.BackgroundColor
import com.example.kisskh.ui.theme.Red
import com.example.kisskh.ui.theme.White
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.*
import com.example.kisskh.ui.components.FocusableWrapper

@Composable
fun DetailScreen(
    movieId: String,
    onEpisodeClick: (String, String) -> Unit, // NOT USED DIRECTLY ANYMORE for URL
    onVideoReady: (String, String, String, String) -> Unit, // Updated callback: url, epId, epNum, epTitle
    onBack: () -> Unit
) {
    var movie by remember { mutableStateOf<Movie?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var isWatchlisted by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current



    // Navigation Targets
    val watchlistButtonRequester = remember { FocusRequester() }
    val episodeFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Find Target Episode (Last Viewed OR First Episode i.e. Bottom of list)
    val history = remember { mutableStateListOf<com.example.kisskh.data.model.Episode>() }
    
    LaunchedEffect(movieId) {
        scope.launch {
            movie = AppRepository.getMovieDetails(movieId)
            episodes = AppRepository.getEpisodes(movieId)
            isWatchlisted = com.example.kisskh.data.LocalStorage.isWatchlisted(movieId)
            
            // Load History
            val allHistory = com.example.kisskh.data.LocalStorage.getHistory()
            history.clear()
            history.addAll(allHistory.filter { it.movieId == movieId })
        }
    }

    fun navigateToTargetEpisode() {
        if (episodes.isEmpty()) return
        
        // Target: Latest Watched OR "First Episode" (which is at the bottom/end of the list)
        val targetEpisode = history.firstOrNull() ?: episodes.last()
        val index = episodes.indexOfFirst { it.id == targetEpisode.id }
        
        if (index != -1) {
            scope.launch {
                // Scroll to the item + offset for header (header is 3 items: Backdrop, Title, HeaderText)
                // Wait, index in "items(episodes)" corresponds to index+3 in LazyColumn items
                // LazyColumn items: 0=Backdrop, 1=Info(Watchlist), 2=HeaderText("Episodes"), 3...=Episodes
                val listIndex = index + 3
                listState.animateScrollToItem(listIndex)
                delay(100)
                episodeFocusRequesters[targetEpisode.id]?.requestFocus()
            }
        }
    }

    if (movie == null) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundColor), contentAlignment = Alignment.Center) {
            Text("Loading...", color = White)
        }
        return
    }

    // Root Container for Floating UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            // Backdrop (Image only now)
            item {
                Box(modifier = Modifier.height(250.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(movie!!.bannerUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Title & Info
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(movie!!.title, color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        
                        // WATCHLIST BUTTON
                        FocusableWrapper(
                            onClick = {
                                if (isWatchlisted) {
                                    com.example.kisskh.data.LocalStorage.removeFromWatchlist(movie!!.id)
                                    isWatchlisted = false
                                } else {
                                    com.example.kisskh.data.LocalStorage.saveToWatchlist(movie!!)
                                    isWatchlisted = true
                                }
                            },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier
                                .focusRequester(watchlistButtonRequester)
                                .onPreviewKeyEvent {
                                    if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) {
                                        navigateToTargetEpisode()
                                        true
                                    } else {
                                        false
                                    }
                                }
                        ) {
                             Icon(
                                imageVector = if (isWatchlisted) androidx.compose.material.icons.Icons.Default.Favorite else androidx.compose.material.icons.Icons.Default.FavoriteBorder,
                                contentDescription = "Watchlist",
                                tint = if (isWatchlisted) Red else White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${movie!!.country}  |  ${movie!!.status}", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(movie!!.description, color = White.copy(alpha = 0.9f), lineHeight = 20.sp)
                }
            }

            // Episode List
            item {
                Text(
                    "Episodes",
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(episodes) { episode ->
                val requester = remember { FocusRequester() }
                DisposableEffect(episode.id) {
                    episodeFocusRequesters[episode.id] = requester
                    onDispose { episodeFocusRequesters.remove(episode.id) }
                }

                EpisodeItemView(
                    episode = episode,
                    modifier = Modifier.focusRequester(requester),
                    onClick = {
                        // Fetch real stream URL
                        scope.launch {
                             val streamUrl = AppRepository.getStreamUrl(episode.id)
                             if (streamUrl.isNotEmpty()) {
                                 onVideoReady(streamUrl, episode.id, episode.number, episode.title)
                             } else {
                                 // Fallback to WebView
                                 val slug = movie!!.title.replace(" ", "-").replace(Regex("[^a-zA-Z0-9-]"), "")
                                 val webUrl = "https://kisskh.co/Drama/$slug/Episode-${episode.number}?id=${movie!!.id}&ep=${episode.id}&page=0&pageSize=100"
                                 onVideoReady(webUrl, episode.id, episode.number, episode.title)
                             }
                        }
                    }
                )
            }
        }
        
        // Floating Back Button (Persistent)
        FocusableWrapper(
            onClick = onBack,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
                .onPreviewKeyEvent {
                    if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) {
                        watchlistButtonRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                }
        ) {
             Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = White
                )
            }
        }
    }
}

@Composable
fun EpisodeItemView(episode: Episode, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusableWrapper(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(60.dp)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(episode.number.toString(), color = Color.Gray)
                }
                
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(episode.title, color = White, fontWeight = FontWeight.Bold)
                }
            }
            Divider(color = Color.DarkGray, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
