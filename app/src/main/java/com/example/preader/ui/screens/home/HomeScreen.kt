package com.example.preader.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.preader.data.SettingsRepository
import com.example.preader.domain.ReadingPage
import com.example.preader.domain.SourceType
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settingsRepository: SettingsRepository,
    deletedPageIdFromReader: String? = null,
    onDeletedPageHandled: () -> Unit = {},
    onNavigateToReader: (String) -> Unit,
    onNavigateToPicker: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pages by settingsRepository.pagesFlow.collectAsState(initial = emptyList())
    var errorDialogPageId by remember { mutableStateOf<String?>(null) }
    
    val pendingDeletions = remember { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deletedPageIdFromReader) {
        deletedPageIdFromReader?.let { pageId ->
            pendingDeletions.add(pageId)
            onDeletedPageHandled()
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    pendingDeletions.remove(pageId)
                } else {
                    if (pendingDeletions.contains(pageId)) {
                        settingsRepository.deletePage(pageId)
                        pendingDeletions.remove(pageId)
                    }
                }
            }
        }
    }

    fun triggerDelete(pageId: String) {
        pendingDeletions.add(pageId)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                pendingDeletions.remove(pageId)
            } else {
                if (pendingDeletions.contains(pageId)) {
                    settingsRepository.deletePage(pageId)
                    pendingDeletions.remove(pageId)
                }
            }
        }
    }

    val visiblePages = pages.filter { it.id !in pendingDeletions }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            onNavigateToPicker()
        }
    }

    fun handleAddClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onNavigateToPicker()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                permissionLauncher.launch(intent)
            }
        } else {
            // For Android < 11, we would need standard READ_EXTERNAL_STORAGE request.
            // For simplicity in MVP assuming Android 11+
            onNavigateToPicker()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (pages.isEmpty()) "Progress Reader" else "Reading files", 
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(
                        onClick = { handleAddClick() },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.open),
                            contentDescription = "Открыть",
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (visiblePages.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface, 
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.open),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "The story is currently unwritten.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Open the saved HTML file or folder with the page to continue reading here.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { handleAddClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.open),
                        contentDescription = "Open",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Supported formats include .html, .htm, and folders of saved pages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visiblePages, key = { it.id }) { page ->
                    PageItem(
                        page = page,
                        onClick = {
                            val file = File(page.sourcePath)
                            var isValid = file.exists() && file.canRead()
                            if (isValid && page.sourceType == SourceType.Folder) {
                                val htmlFiles = file.listFiles()?.filter { 
                                    it.isFile && (it.name.endsWith(".html", ignoreCase = true) || it.name.endsWith(".htm", ignoreCase = true)) 
                                }
                                if (htmlFiles.isNullOrEmpty()) {
                                    isValid = false
                                }
                            }

                            if (!isValid) {
                                errorDialogPageId = page.id
                            } else {
                                coroutineScope.launch {
                                    settingsRepository.addOrUpdatePage(page.copy(firstOpenedAt = page.firstOpenedAt))
                                    onNavigateToReader(page.id)
                                }
                            }
                        },
                        onReset = {
                            coroutineScope.launch {
                                settingsRepository.resetProgress(page.id)
                            }
                        },
                        onDelete = {
                            triggerDelete(page.id)
                        }
                    )
                }
            }
        }

        if (errorDialogPageId != null) {
            AlertDialog(
                onDismissRequest = { errorDialogPageId = null },
                containerColor = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        "Can not open file",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                },
                text = {
                    Text(
                        "The file has been moved, the folder does not contain HTML, or the application has lost access to the selected page.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { 
                        errorDialogPageId = null
                        handleAddClick() 
                    }) {
                        Text("Open", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        val idToDelete = errorDialogPageId
                        errorDialogPageId = null 
                        if (idToDelete != null) {
                            triggerDelete(idToDelete)
                        }
                    }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        ) { data ->
            var localCountdown by remember(data) { mutableIntStateOf(10) }
            
            LaunchedEffect(data) {
                localCountdown = 10
                while(localCountdown > 0) {
                    kotlinx.coroutines.delay(1000)
                    localCountdown--
                }
                data.dismiss()
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page will be deleted in $localCountdown sec",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = data.visuals.actionLabel ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { data.performAction() }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
}

// In a real app, observing savedStateHandle is better done from the NavHost, but for MVP we can do it here if we pass navController.
// Wait, I passed onNavigateToPicker instead of navController, so HomeScreen doesn't see savedStateHandle!
// I'll update AppNavigation to handle the picked results.


@Composable
fun PageItem(
    page: ReadingPage,
    onClick: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = page.displayName, 
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress: ${page.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Reset Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .clickable(onClick = onReset),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.reset),
                            contentDescription = "Сбросить",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Delete/Close button (mocked)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp))
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.delete),
                            contentDescription = "Удалить",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
