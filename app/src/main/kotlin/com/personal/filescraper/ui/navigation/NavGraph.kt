package com.personal.filescraper.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.filescraper.di.ViewModelFactory
import com.personal.filescraper.ui.folders.FolderPickerScreen
import com.personal.filescraper.ui.gallery.GalleryScreen
import com.personal.filescraper.ui.home.HomeScreen
import com.personal.filescraper.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Monitor", Icons.Default.Home)
    data object Folders : Screen("folders", "Folders", Icons.Default.Folder)
    data object Gallery : Screen("gallery", "Archive", Icons.Default.PhotoLibrary)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

private val bottomBarScreens = listOf(Screen.Home, Screen.Folders, Screen.Gallery, Screen.Settings)

@Composable
fun AppNavHost(factory: ViewModelFactory) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                bottomBarScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel(factory = factory)) }
            composable(Screen.Folders.route) { FolderPickerScreen(viewModel(factory = factory)) }
            composable(Screen.Gallery.route) { GalleryScreen(viewModel(factory = factory)) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel(factory = factory)) }
        }
    }
}
