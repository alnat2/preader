package com.example.preader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.preader.data.SettingsRepository
import com.example.preader.ui.screens.home.HomeScreen
import com.example.preader.ui.screens.reader.ReaderScreen

import com.example.preader.ui.screens.picker.FilePickerScreen
import java.io.File

@Composable
fun AppNavigation(settingsRepository: SettingsRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { backStackEntry ->
            val deletedPageId = backStackEntry.savedStateHandle.get<String>("deletedPageId")
            HomeScreen(
                settingsRepository = settingsRepository,
                deletedPageIdFromReader = deletedPageId,
                onDeletedPageHandled = {
                    backStackEntry.savedStateHandle.remove<String>("deletedPageId")
                },
                onNavigateToReader = { pageId ->
                    navController.navigate("reader/$pageId")
                },
                onNavigateToPicker = {
                    navController.navigate("picker")
                }
            )
        }
        
        composable("picker") {
            val coroutineScope = rememberCoroutineScope()
            FilePickerScreen(
                onFilePicked = { file ->
                    val pageId = java.util.UUID.randomUUID().toString()
                    val newPage = com.example.preader.domain.ReadingPage(
                        id = pageId,
                        displayName = file.name,
                        sourcePath = file.absolutePath,
                        sourceType = com.example.preader.domain.SourceType.HtmlFile,
                        firstOpenedAt = System.currentTimeMillis(),
                        positionRatio = 0.0,
                        progressPercent = 0
                    )
                    coroutineScope.launch {
                        settingsRepository.addOrUpdatePage(newPage)
                        navController.popBackStack()
                        navController.navigate("reader/$pageId")
                    }
                },
                onFolderPicked = { folder ->
                    val pageId = java.util.UUID.randomUUID().toString()
                    val newPage = com.example.preader.domain.ReadingPage(
                        id = pageId,
                        displayName = folder.name,
                        sourcePath = folder.absolutePath,
                        sourceType = com.example.preader.domain.SourceType.Folder,
                        firstOpenedAt = System.currentTimeMillis(),
                        positionRatio = 0.0,
                        progressPercent = 0
                    )
                    coroutineScope.launch {
                        settingsRepository.addOrUpdatePage(newPage)
                        navController.popBackStack()
                        navController.navigate("reader/$pageId")
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = "reader/{pageId}",
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: return@composable
            ReaderScreen(
                pageId = pageId,
                settingsRepository = settingsRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDeletePage = { id ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("deletedPageId", id)
                    navController.popBackStack()
                }
            )
        }
    }
}
