package com.example.kisskh.ui.screens

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.content.res.Configuration
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*

@Composable
fun PlayerScreen(
    videoUrl: String,
    movieId: String,
    episodeId: String,
    episodeNumber: String,
    episodeTitle: String,
    onBack: () -> Unit,
    onNextEpisode: ((String, String) -> Unit)? = null // movieId, currentEpisodeNumber
) {
    val context = LocalContext.current
    val isWeb = videoUrl.contains("kisskh.co")

    // --- State ---
    // State for initial seek (Resume)
    var initialSeekTime by remember { mutableStateOf(0f) }
    var hasPerformedInitialSeek by remember { mutableStateOf(false) }

    // Video State
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(initialSeekTime) }
    var duration by remember { mutableStateOf(1f) } // Avoid div by zero
    var isBuffering by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    
    // Episode end detection
    var hasEpisodeEnded by remember { mutableStateOf(false) }
    
    // Movie title for display
    var movieTitle by remember { mutableStateOf<String?>(null) }
    
    // Reset episode end state when episode changes
    LaunchedEffect(episodeId) {
        hasEpisodeEnded = false
    }
    
    // Fetch movie title
    LaunchedEffect(movieId) {
        if (movieId.isNotEmpty()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val movie = com.example.kisskh.data.AppRepository.getMovieDetails(movieId)
                movieTitle = movie?.title
            }
        }
    }

    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }
    
    // WebView Reference
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Save to History on Load / Restore Position
    LaunchedEffect(Unit) {
        if (movieId.isNotEmpty() && episodeId.isNotEmpty()) {
            val history = com.example.kisskh.data.LocalStorage.getHistory()
            val existingItem = history.find { it.id == episodeId }
            
            val resumeTime = existingItem?.timestamp ?: 0L
            if (resumeTime > 0) {
               val timeInSeconds = (resumeTime / 1000).toFloat()
               // We are storing in milliseconds (Long), but Player logic roughly uses seconds (Float)
               // wait, we decided earlier logic: 
               // "initialSeekTime = (resumeTime / 1000).toFloat()" on line 69
               // AND "initialSeekTime = resumeTime.toFloat()" on line 77 ???
               // Line 77 overrides Line 69!?
               // Let's FIX this logic while we are here. 
               // HTML Video currentTime is in SECONDS.
               // We saved "currentTime.toLong()" (Seconds -> Long)
               // So existingItem.timestamp is SECONDS.
               
               initialSeekTime = resumeTime.toFloat()
               currentTime = resumeTime.toFloat()
            }

            // Still add to top/refresh timestamp
            com.example.kisskh.data.LocalStorage.addToHistory(
                com.example.kisskh.data.model.Episode(
                    id = episodeId,
                    movieId = movieId,
                    number = episodeNumber,
                    title = episodeTitle,
                    videoUrl = videoUrl,
                    thumbnailUrl = null,
                    timestamp = resumeTime,
                    movieTitle = movieTitle
                )
            )
        }
    }

    // Save Progress on Exit
    DisposableEffect(episodeId) {
        onDispose {
            // Don't save progress if episode has ended (we've already navigated to next episode)
            if (currentTime > 0 && !hasEpisodeEnded) {
                val episode = com.example.kisskh.data.model.Episode(
                    id = episodeId,
                    movieId = movieId,
                    number = episodeNumber,
                    title = episodeTitle,
                    videoUrl = videoUrl,
                    thumbnailUrl = null,
                    timestamp = currentTime.toLong(),
                    duration = duration.toLong(),
                    movieTitle = movieTitle
                )
                com.example.kisskh.data.LocalStorage.updateHistoryProgress(episodeId, currentTime.toLong(), duration.toLong(), episode)
            }
        }
    }
    
    // Immersive Mode Logic (Hide Status Bar in Landscape)
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    
    DisposableEffect(configuration.orientation) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Hide System Bars in Landscape
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                // Show System Bars in Portrait
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        
        onDispose {
            // Restore System Bars when leaving or changing state significantly
            val window = (context as? Activity)?.window
            if (window != null) {
                 val insetsController = WindowCompat.getInsetsController(window, view)
                 insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    // WebView Reference
    // webViewRef is defined at top of function
    
    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Polling Loop for WebView Status
    if (isWeb && webViewRef != null) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                webViewRef?.evaluateJavascript(
                    "(function() { var v = document.querySelector('video'); return v ? JSON.stringify({paused: v.paused, currentTime: v.currentTime, duration: v.duration, readyState: v.readyState}) : null; })();"
                ) { result ->
                    if (result != "null") {
                        try {
                            // Result is like: "{\"paused\":false,\"currentTime\":12.5,\"duration\":120.0, \"readyState\": 4}"
                            // Needs unescaping if wrapped in quotes, usually returns stringified JSON
                            val json = result.replace("^\"|\"$".toRegex(), "").replace("\\\"", "\"")
                            val jsonObj = org.json.JSONObject(json)
                            isPlaying = !jsonObj.getBoolean("paused")
                            
                            val webTime = jsonObj.getDouble("currentTime").toFloat()
                            val d = jsonObj.getDouble("duration").toFloat()
                            if (d > 0) duration = d
                            
                             // Initial Seek Logic
                             if (!hasPerformedInitialSeek && initialSeekTime > 0) {
                                 // Execute seek
                                 webViewRef?.evaluateJavascript("document.querySelector('video').currentTime = ${initialSeekTime}", null)
                                 hasPerformedInitialSeek = true
                                 currentTime = initialSeekTime
                             } else {
                                 currentTime = webTime
                             }

                            isBuffering = jsonObj.getInt("readyState") < 3
                            
                            // Detect episode end: currentTime >= duration - 1.0 (1 second threshold)
                            if (!hasEpisodeEnded && duration > 1f && currentTime >= duration - 1.0f) {
                                hasEpisodeEnded = true
                                onNextEpisode?.invoke(movieId, episodeNumber)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    // Handle Back Press for Fullscreen
    androidx.activity.compose.BackHandler(enabled = customView != null) {
        customViewCallback?.onCustomViewHidden()
        customView = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (customView != null) {
            // FULLSCREEN MODE
            // Note: Standard WebView full-screen triggers this.
            // We just render the view provided by WebChromeClient.
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // NORMAL MODE
            Box(Modifier.fillMaxSize()) {
                if (isWeb) {
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                
                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                        customView = view
                                        customViewCallback = callback
                                    }
                                    override fun onHideCustomView() {
                                        customView = null
                                        customViewCallback = null
                                    }
                                }

                                webViewClient = object : android.webkit.WebViewClient() {
                                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        webViewRef = view
                                        
                                        // "Nuclear Option" Cleanup & Setup
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                // 1. Basic Site Cleanup (Global Headers/Footers)
                                                var css = '.header, app-header, mat-toolbar, .mat-toolbar, nav, footer, #disqus_thread, .col-xl, .col-12.col-xl { display: none !important; }';
                                                
                                                // Hide native web controls everywhere
                                                css += 'video::-webkit-media-controls, video::-webkit-media-controls-enclosure { display: none !important; }';

                                                var style = document.createElement('style');
                                                style.type = 'text/css';
                                                style.appendChild(document.createTextNode(css));
                                                document.head.appendChild(style);
                                                
                                                // 2. Persistent Loop for Video Player Cleanup
                                                setInterval(function() {
                                                    var v = document.querySelector('video');
                                                    if (!v) return;
                                                    
                                                    // A. Setup Video Element
                                                    v.controls = false;
                                                    v.style.width = '100vw'; // Use vw/vh for the video itself to force full viewport
                                                    v.style.height = '100vh'; 
                                                    v.style.objectFit = 'contain'; // Maintain aspect ratio, black bars if needed
                                                    v.style.position = 'fixed';
                                                    v.style.top = '0';
                                                    v.style.left = '0';
                                                    v.style.zIndex = '0';
                                                    v.style.background = 'black';
                                                    
                                                    // B. Nuclear Option: Hide ALL siblings of the video (posters, controls, overlays)
                                                    var parent = v.parentElement;
                                                    if (parent) {
                                                        // Fix parent container layout: Flex Center
                                                        parent.style.background = 'black';
                                                        parent.style.margin = '0';
                                                        parent.style.padding = '0';
                                                        parent.style.border = 'none';
                                                        parent.style.display = 'flex';
                                                        parent.style.justifyContent = 'center';
                                                        parent.style.alignItems = 'center';
                                                        
                                                        // Iterate and Hide Siblings
                                                        for (var i = 0; i < parent.children.length; i++) {
                                                            var child = parent.children[i];
                                                            if (child !== v && child.tagName !== 'STYLE') { // Don't hide our own style if we injected one there
                                                                child.style.display = 'none';
                                                                child.style.visibility = 'hidden';
                                                                child.style.opacity = '0';
                                                            }
                                                        }
                                                    }
                                                    
                                                    // C. Re-hide specific global headers if they reappear
                                                    var headers = document.querySelectorAll('.header, app-header, mat-toolbar, nav');
                                                    headers.forEach(h => h.style.display = 'none');
                                                    
                                                }, 250); // Aggressive check every 250ms

                                                // 3. Force parent column cleanup to prevent layout restrictions
                                                var parentCol = document.querySelector('.col-12.col-xl-7');
                                                if (parentCol) {
                                                    parentCol.style.width = '100%';
                                                    parentCol.style.maxWidth = '100%';
                                                    parentCol.style.padding = '0';
                                                    parentCol.style.margin = '0';
                                                    parentCol.className = '';
                                                }
                                            })();
                                            """.trimIndent(),
                                            null
                                        )
                                    }
                                }
                                loadUrl(videoUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // ExoPlayer fallback (Native)
                    VideoPlayerNative(
                        videoUrl = videoUrl,
                        context = context,
                        movieId = movieId,
                        episodeNumber = episodeNumber,
                        onNextEpisode = onNextEpisode,
                        onTimeUpdate = { time, dur ->
                            currentTime = time
                            duration = dur
                            // Detect episode end: currentTime >= duration - 1.0 (1 second threshold)
                            if (!hasEpisodeEnded && dur > 1f && time >= dur - 1.0f) {
                                hasEpisodeEnded = true
                                onNextEpisode?.invoke(movieId, episodeNumber)
                            }
                        }
                    )
                }
                
                // --- CUSTOM OVERLAY ---
                if (isWeb) { // Only show custom overlay for Web mode
                    VideoControlsOverlay(
                        isVisible = controlsVisible,
                        isPlaying = isPlaying,
                        currentTime = currentTime,
                        duration = duration,
                        showTitle = movieTitle,
                        episodeNumber = episodeNumber,
                        onTogglePlay = {
                            if (isPlaying) {
                                webViewRef?.evaluateJavascript("document.querySelector('video').pause()", null)
                                isPlaying = false
                            } else {
                                webViewRef?.evaluateJavascript("document.querySelector('video').play()", null)
                                isPlaying = true
                            }
                        },
                        onSeek = { newTime ->
                            webViewRef?.evaluateJavascript("document.querySelector('video').currentTime = $newTime", null)
                            currentTime = newTime
                        },
                        onSeekForward = {
                             webViewRef?.evaluateJavascript("document.querySelector('video').currentTime += 10", null)
                        },
                        onSeekBackward = {
                             webViewRef?.evaluateJavascript("document.querySelector('video').currentTime -= 10", null)
                        },
                        onToggleControls = { controlsVisible = !controlsVisible },
                        onClose = onBack
                    )
                } else {
                   // Native ExoPlayer overlay
                   if (controlsVisible) {
                       Box(
                           modifier = Modifier
                               .fillMaxSize()
                               .background(Color.Black.copy(alpha = 0.4f))
                       ) {
                           // Show title and episode number in top left
                           if (movieTitle != null || episodeNumber.isNotEmpty()) {
                               Column(
                                   modifier = Modifier
                                       .statusBarsPadding()
                                       .padding(16.dp)
                                       .align(Alignment.TopStart)
                               ) {
                                   if (movieTitle != null) {
                                       Text(
                                           text = movieTitle!!,
                                           color = Color.White,
                                           fontSize = 16.sp,
                                           fontWeight = FontWeight.Bold,
                                           maxLines = 1,
                                           overflow = TextOverflow.Ellipsis
                                       )
                                   }
                                   if (episodeNumber.isNotEmpty()) {
                                       Text(
                                           text = "Episode $episodeNumber",
                                           color = Color.White.copy(alpha = 0.8f),
                                           fontSize = 14.sp,
                                           maxLines = 1
                                       )
                                   }
                               }
                           }
                           
                           // Close button in top right
                           IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(Icons.Filled.Close, "Close", tint = Color.White)
                            }
                       }
                   }
                }
            }
        }
    }
}

@Composable
fun VideoControlsOverlay(
    isVisible: Boolean,
    isPlaying: Boolean,
    currentTime: Float,
    duration: Float,
    showTitle: String?,
    episodeNumber: String,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onToggleControls: () -> Unit,
    onClose: () -> Unit
) {
    val overlayRequester = remember { FocusRequester() }
    val sliderRequester = remember { FocusRequester() }
    var isSliderFocused by remember { mutableStateOf(false) }

    // Grab focus when visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            overlayRequester.requestFocus()
        }
    }

    // Gesture Detector Layer & Key Handler
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(overlayRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    // Reset auto-hide timer mechanism (handled by parent state usually, 
                    // but here we just ensure we interact. 
                    // Ideally we should callback onInteraction)
                    
                    if (isSliderFocused) {
                        // SLIDER MODE
                        when (event.key) {
                            Key.DirectionUp, Key.Back -> {
                                overlayRequester.requestFocus()
                                true
                            }
                            else -> false // Let Slider handle Left/Right
                        }
                    } else {
                        // DEFAULT MODE
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause -> {
                                onTogglePlay()
                                true
                            }
                            Key.DirectionLeft -> {
                                onSeekBackward()
                                true
                            }
                            Key.DirectionRight -> {
                                onSeekForward()
                                true
                            }
                            Key.DirectionDown -> {
                                if (isVisible) {
                                    try {
                                        sliderRequester.requestFocus()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    onToggleControls()
                                }
                                true
                            }
                            Key.Back -> {
                                // Default back behavior handled by system/parent
                                false 
                            }
                            else -> false
                        }
                    }
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) {
                            onSeekBackward()
                        } else {
                            onSeekForward()
                        }
                    }
                )
            }
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))) {
                
                // Show title and episode number in top left
                if (showTitle != null || episodeNumber.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                    ) {
                        if (showTitle != null) {
                            Text(
                                text = showTitle,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (episodeNumber.isNotEmpty()) {
                            Text(
                                text = "Episode $episodeNumber",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // Close button in top right
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSeekBackward) {
                        Icon(Icons.Filled.FastRewind, "-10s", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    
                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    
                    IconButton(onClick = onSeekForward) {
                        Icon(Icons.Filled.FastForward, "+10s", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "${formatTime(currentTime)} / ${formatTime(duration)}",
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Slider(
                        value = currentTime,
                        onValueChange = { onSeek(it) }, // Immediate seek for visual feedback
                        onValueChangeFinished = { 
                            // Return focus to overlay after seeking? 
                            // User might want to seek more. Keep focus.
                        },
                        valueRange = 0f..duration.coerceAtLeast(0.1f),
                        colors = SliderDefaults.colors(
                            thumbColor = if (isSliderFocused) Color.Red else Color.White,
                            activeTrackColor = Color.Red,
                            inactiveTrackColor = Color.Gray
                        ),
                        modifier = Modifier
                            .focusRequester(sliderRequester)
                            .onFocusChanged { isSliderFocused = it.isFocused }
                            .focusable()
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayerNative(
    videoUrl: String,
    context: android.content.Context,
    movieId: String,
    episodeNumber: String,
    onNextEpisode: ((String, String) -> Unit)?,
    onTimeUpdate: (Float, Float) -> Unit
) {
    val exoPlayer = remember {
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
        
        // Add listener to track playback position and detect end
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    val currentPos = player.currentPosition / 1000f // Convert ms to seconds
                    val dur = player.duration / 1000f // Convert ms to seconds
                    if (dur > 0) {
                        onTimeUpdate(currentPos, dur)
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && player.playbackState == androidx.media3.common.Player.STATE_READY) {
                    val currentPos = player.currentPosition / 1000f
                    val dur = player.duration / 1000f
                    if (dur > 0) {
                        onTimeUpdate(currentPos, dur)
                    }
                }
            }
        })
        
        player
    }
    
    // Poll for position updates
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_READY) {
                val currentPos = exoPlayer.currentPosition / 1000f
                val dur = exoPlayer.duration / 1000f
                if (dur > 0) {
                    onTimeUpdate(currentPos, dur)
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
    
    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
