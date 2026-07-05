package com.example.preader.ui.screens.picker

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePickerScreen(
    onFilePicked: (File) -> Unit,
    onFolderPicked: (File) -> Unit,
    onNavigateBack: () -> Unit
) {
    val initialDir = Environment.getExternalStorageDirectory()
    var currentDir by remember { mutableStateOf(initialDir) }
    
    val filesAndDirs = remember(currentDir) {
        val all = currentDir.listFiles()?.toList() ?: emptyList()
        val dirs = all.filter { it.isDirectory && !it.isHidden }.sortedBy { it.name.lowercase() }
        val htmlFiles = all.filter { it.isFile && !it.isHidden && (it.name.endsWith(".html", ignoreCase = true) || it.name.endsWith(".htm", ignoreCase = true)) }.sortedBy { it.name.lowercase() }
        dirs + htmlFiles
    }

    val goBack = {
        val parent = currentDir.parentFile
        if (parent != null && parent.absolutePath.startsWith(initialDir.parent ?: "")) {
            currentDir = parent
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = goBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = currentDir.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.preader.R.drawable.back),
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onFolderPicked(currentDir) },
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбрать текущую папку")
                }
            }
        }
    ) { padding ->
        if (filesAndDirs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Папка пуста или нет прав доступа")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(filesAndDirs) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (file.isDirectory) {
                                    currentDir = file
                                } else {
                                    onFilePicked(file)
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
