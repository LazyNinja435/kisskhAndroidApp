package com.example.kisskh.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.kisskh.data.UpdateInfo
import com.example.kisskh.data.UpdateRepository
import com.example.kisskh.ui.components.FocusableWrapper
import com.example.kisskh.ui.theme.BackgroundColor
import com.example.kisskh.ui.theme.White
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentVersion by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(true) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load current version
    LaunchedEffect(Unit) {
        currentVersion = UpdateRepository.getCurrentVersionName(context)
        
        // Check for updates
        scope.launch {
            try {
                updateInfo = UpdateRepository.checkForUpdates(context)
            } catch (e: Exception) {
                errorMessage = "Failed to check for updates: ${e.message}"
            } finally {
                isChecking = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "App Updates",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        // Current Version
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Version",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentVersion,
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isChecking -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = White)
                        Text(
                            text = "Checking for updates...",
                            color = Color.Gray
                        )
                    }
                }
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        FocusableWrapper(
                            onClick = {
                                isChecking = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        updateInfo = UpdateRepository.checkForUpdates(context)
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to check for updates: ${e.message}"
                                    } finally {
                                        isChecking = false
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Button(
                                onClick = { /* Handled by FocusableWrapper */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("Retry", color = White)
                            }
                        }
                    }
                }
                updateInfo?.isUpdateAvailable == true -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Update Available",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Update Available!",
                            color = White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version ${updateInfo?.latestVersionName} is available",
                            color = Color.Gray
                        )
                        
                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(
                                color = White,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Downloading update...",
                                color = Color.Gray
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            FocusableWrapper(
                                onClick = {
                                    showUpdateDialog = true
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Button(
                                    onClick = { /* Handled by FocusableWrapper */ },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Download & Install Update", color = White)
                                }
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Up to Date",
                            tint = Color.Green,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "App is up to date",
                            color = White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "You have the latest version installed",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
    
    // Update Dialog
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(
                    text = "Update Available",
                    color = White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Version ${updateInfo?.latestVersionName} is available.",
                        color = White
                    )
                    if (!updateInfo?.releaseNotes.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = updateInfo?.releaseNotes ?: "",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                FocusableWrapper(
                    onClick = {
                        showUpdateDialog = false
                        isDownloading = true
                        scope.launch {
                            try {
                                val downloadUrl = updateInfo?.downloadUrl
                                if (downloadUrl != null) {
                                    val apkFile = UpdateRepository.downloadApk(context, downloadUrl)
                                    if (apkFile != null) {
                                        installApk(context, apkFile)
                                    } else {
                                        errorMessage = "Failed to download update"
                                        isDownloading = false
                                    }
                                } else {
                                    errorMessage = "Download URL not available"
                                    isDownloading = false
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                                isDownloading = false
                            }
                        }
                    }
                ) {
                    Button(
                        onClick = { /* Handled by FocusableWrapper */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Update", color = White)
                    }
                }
            },
            dismissButton = {
                FocusableWrapper(
                    onClick = { showUpdateDialog = false }
                ) {
                    TextButton(
                        onClick = { /* Handled by FocusableWrapper */ }
                    ) {
                        Text("Cancel", color = White)
                    }
                }
            },
            containerColor = BackgroundColor,
            titleContentColor = White,
            textContentColor = White
        )
    }
}

private fun installApk(context: android.content.Context, apkFile: File) {
    try {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            @Suppress("DEPRECATION")
            Uri.fromFile(apkFile)
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
