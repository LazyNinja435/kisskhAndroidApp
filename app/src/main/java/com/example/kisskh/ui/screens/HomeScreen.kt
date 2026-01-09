package com.example.kisskh.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.kisskh.ui.components.FocusableWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit
) {
    // Search State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var searchJob by remember { mutableStateOf<Job?>(null) }

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
            .statusBarsPadding()
    ) {
        // --- SEARCH BAR ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .onPreviewKeyEvent {
                    if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) {
                        if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                        true
                    } else {
                        false
                    }
                },
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
            if (searchQuery.isEmpty()) {
                // Empty state when no search query
                Text(
                    "Start typing to search for dramas...",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (isSearching) {
                // SEARCHING
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = White
                )
            } else if (searchResults.isEmpty()) {
                // NO RESULTS
                Text(
                    "No results found.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // SEARCH RESULTS
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
