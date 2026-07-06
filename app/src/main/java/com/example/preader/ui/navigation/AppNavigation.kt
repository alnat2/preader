package com.example.preader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.preader.data.SettingsRepository
import com.example.preader.ui.screens.home.HomeScreen
import com.example.preader.ui.screens.reader.ReaderScreen

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
