package com.example.kisskh.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.example.kisskh.ui.theme.BackgroundColor
import com.example.kisskh.ui.theme.White

@Composable
fun MainScreen(
    onMovieClick: (String) -> Unit,
    onEpisodeClick: (com.example.kisskh.data.model.Episode) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Library", "Browse", "Update")
    val icons = listOf(Icons.Filled.VideoLibrary, Icons.Filled.Search, Icons.Filled.SystemUpdate)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = BackgroundColor,
                contentColor = White
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = White,
                            selectedTextColor = White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Red
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> LibraryScreen(
                    onMovieClick = onMovieClick,
                    onEpisodeClick = onEpisodeClick
                )
                1 -> HomeScreen(onMovieClick = onMovieClick)
                2 -> UpdateScreen()
            }
        }
    }
}
