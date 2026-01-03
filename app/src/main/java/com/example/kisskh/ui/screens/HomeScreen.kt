package com.example.kisskh.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.kisskh.data.AppRepository
import com.example.kisskh.data.model.Movie
import com.example.kisskh.ui.theme.BackgroundColor
import com.example.kisskh.ui.theme.White
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.kisskh.ui.components.FocusableWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit
) {
    // State
    var featuredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var actionMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    
    // Search State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val heroRequester = remember { FocusRequester() }

    // Initial Load
    LaunchedEffect(Unit) {
        scope.launch {
            featuredMovies = AppRepository.searchMovies("Love")
            actionMovies = AppRepository.searchMovies("Action")
        }
    }

    // Search Logic with Debounce
    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        if (searchQuery.isNotEmpty()) {
            searchJob = scope.launch {
                delay(500) // Debounce
                isSearching = true
                searchResults = AppRepository.searchMovies(searchQuery)
                isSearching = false
            }
        } else {
            searchResults = emptyList()
            // Make sure we are not in searching state
            isSearching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // --- BRANDING ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(com.example.kisskh.R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "KissKH",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // --- SEARCH BAR ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onPreviewKeyEvent {
                    if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) {
                         if (searchQuery.isEmpty()) {
                             // Explicitly jump to Hero content in Home mode
                             heroRequester.requestFocus()
                         } else {
                             focusManager.moveFocus(FocusDirection.Down)
                         }
                        true
                    } else {
                        false
                    }
                },
             // Removed statusBarsPadding here as it's handled by Branding Row
            placeholder = { Text("Search dramas...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                containerColor = Color.DarkGray.copy(alpha = 0.3f),
                cursorColor = White,
                focusedBorderColor = White,
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )

        // --- CONTENT ---
        Box(modifier = Modifier.fillMaxSize()) {
            if (searchQuery.isNotEmpty()) {
                // SEARCH RESULTS VIEW
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = White
                    )
                } else if (searchResults.isEmpty()) {
                    Text(
                        "No results found.",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(searchResults) { movie ->
                            MovieThumbnail(movie = movie, onClick = { onMovieClick(movie.id) })
                        }
                    }
                }
            } else {
                // DEFAULT HOME VIEW
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Featured Carousel (Hero)
                    item {
                        if (featuredMovies.isNotEmpty()) {
                            val heroMovie = featuredMovies.first()
                            FeaturedHero(
                                movie = heroMovie,
                                modifier = Modifier.focusRequester(heroRequester),
                                onClick = { onMovieClick(heroMovie.id) }
                            )
                        }
                    }

                    // Categories
                    item {
                        CategoryRow(title = "Romance & Drama", movies = featuredMovies, onMovieClick = onMovieClick)
                    }
                    item {
                        CategoryRow(title = "Action", movies = actionMovies, onMovieClick = onMovieClick)
                    }
                    
                    // Spacer
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun FeaturedHero(movie: Movie, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusableWrapper(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp) // Slightly reduced height to accommodate search bar
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(movie.bannerUrl),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, BackgroundColor),
                            startY = 200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = movie.title,
                    color = White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoryRow(title: String, movies: List<Movie>, onMovieClick: (String) -> Unit) {
    if (movies.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(movies) { movie ->
                MovieThumbnail(movie = movie, onClick = { onMovieClick(movie.id) })
            }
        }
    }
}

@Composable
fun MovieThumbnail(movie: Movie, onClick: () -> Unit) {
    FocusableWrapper(
        onClick = onClick,
        modifier = Modifier.width(110.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(movie.posterUrl),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth()
                    .background(Color.DarkGray) // Placeholder color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = movie.title,
                color = White,
                fontSize = 12.sp,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
