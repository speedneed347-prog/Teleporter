package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.ui.screens.CameraTeleprompterScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ImportDocumentScreen
import com.example.ui.screens.MyScriptsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ScriptEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TeleprompterReaderScreen
import com.example.ui.screens.VideoGalleryScreen
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.ImportViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.VideoGalleryViewModel

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun TeleprompterAppShell(
    container: AppContainer,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val recentScripts by mainViewModel.recentScripts.collectAsStateWithLifecycle()
    val allScripts by mainViewModel.allScripts.collectAsStateWithLifecycle()
    val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by mainViewModel.sortOrder.collectAsStateWithLifecycle()
    val quickScriptText by mainViewModel.quickScriptText.collectAsStateWithLifecycle()

    val videoGalleryViewModel: VideoGalleryViewModel = viewModel(
        factory = VideoGalleryViewModel.factory(container)
    )

    val topDestinations = listOf(
        TopLevelDestination(NavRoutes.HOME, "Studio", Icons.Filled.Home, Icons.Outlined.Home, "nav_studio"),
        TopLevelDestination(NavRoutes.MY_SCRIPTS, "Scripts", Icons.Filled.Description, Icons.Outlined.Description, "nav_scripts"),
        TopLevelDestination(NavRoutes.CAMERA_TELEPROMPTER_PATTERN, "Camera", Icons.Filled.Videocam, Icons.Outlined.Videocam, "nav_camera"),
        TopLevelDestination(NavRoutes.VIDEOS, "Videos", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_videos"),
        TopLevelDestination(NavRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showChrome = currentRoute in setOf(
        NavRoutes.HOME,
        NavRoutes.MY_SCRIPTS,
        NavRoutes.VIDEOS,
        NavRoutes.SETTINGS
    )

    val navigateToTopLevel: (String) -> Unit = { route ->
        if (route == NavRoutes.CAMERA_TELEPROMPTER_PATTERN) {
            navController.navigate(NavRoutes.cameraTeleprompter(recentScripts.firstOrNull()?.id ?: -1L))
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = if (showChrome) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showChrome && !useNavigationRail) {
                    NavigationBar {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useNavigationRail) {
                    NavigationRail {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.SPLASH
                    ) {
                        // 1. Splash Screen
                        composable(NavRoutes.SPLASH) {
                            SplashScreen(
                                onboardingCompleted = settings.onboardingCompleted,
                                onFinished = { goToOnboarding ->
                                    val target = if (goToOnboarding) NavRoutes.ONBOARDING else NavRoutes.HOME
                                    navController.navigate(target) {
                                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Onboarding Screen
                        composable(NavRoutes.ONBOARDING) {
                            OnboardingScreen(
                                onComplete = {
                                    mainViewModel.completeOnboarding()
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 3. Home Dashboard
                        composable(NavRoutes.HOME) {
                            HomeDashboardScreen(
                                recentScripts = recentScripts,
                                totalScriptsCount = allScripts.size,
                                quickScriptText = quickScriptText,
                                onUpdateQuickScript = mainViewModel::updateQuickScriptText,
                                onCreateNewScript = { navController.navigate(NavRoutes.scriptEditor(0L)) },
                                onImportDocument = { navController.navigate(NavRoutes.IMPORT_DOCUMENT) },
                                onOpenMyScripts = { navigateToTopLevel(NavRoutes.MY_SCRIPTS) },
                                onOpenScriptEditor = { id -> navController.navigate(NavRoutes.scriptEditor(id)) },
                                onStartTeleprompter = { id -> navController.navigate(NavRoutes.teleprompterReader(id)) },
                                onStartQuickTeleprompter = { navController.navigate(NavRoutes.teleprompterReader(-1L)) },
                                onSaveQuickAsScript = { title, content ->
                                    mainViewModel.createNewScript(title, content) { newId ->
                                        navController.navigate(NavRoutes.teleprompterReader(newId))
                                    }
                                },
                                onOpenCameraTeleprompter = { id -> navController.navigate(NavRoutes.cameraTeleprompter(id)) },
                                onOpenVideoGallery = { navigateToTopLevel(NavRoutes.VIDEOS) },
                                onOpenSettings = { navigateToTopLevel(NavRoutes.SETTINGS) }
                            )
                        }

                        // 4. Script Editor
                        composable(
                            route = NavRoutes.SCRIPT_EDITOR_PATTERN,
                            arguments = listOf(navArgument("scriptId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val scriptId = backStackEntry.arguments?.getLong("scriptId") ?: 0L
                            val editorViewModel: EditorViewModel = viewModel(
                                factory = EditorViewModel.factory(container)
                            )
                            ScriptEditorScreen(
                                scriptId = scriptId,
                                settings = settings,
                                editorViewModel = editorViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenImportDocument = { navController.navigate(NavRoutes.IMPORT_DOCUMENT) },
                                onStartTeleprompter = { savedId ->
                                    navController.navigate(NavRoutes.teleprompterReader(savedId))
                                },
                                onStartCameraTeleprompter = { savedId ->
                                    navController.navigate(NavRoutes.cameraTeleprompter(savedId))
                                }
                            )
                        }

                        // 5. Import Document Screen
                        composable(NavRoutes.IMPORT_DOCUMENT) {
                            val importViewModel: ImportViewModel = viewModel(
                                factory = ImportViewModel.factory(container)
                            )
                            ImportDocumentScreen(
                                settings = settings,
                                importViewModel = importViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSavedToEditor = { newId ->
                                    navController.navigate(NavRoutes.scriptEditor(newId)) {
                                        popUpTo(NavRoutes.IMPORT_DOCUMENT) { inclusive = true }
                                    }
                                },
                                onSavedToTeleprompter = { newId ->
                                    navController.navigate(NavRoutes.teleprompterReader(newId)) {
                                        popUpTo(NavRoutes.IMPORT_DOCUMENT) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 6. My Scripts Screen
                        composable(NavRoutes.MY_SCRIPTS) {
                            MyScriptsScreen(
                                scripts = allScripts,
                                searchQuery = searchQuery,
                                sortOrder = sortOrder,
                                onSearchQueryChange = mainViewModel::updateSearchQuery,
                                onSortOrderChange = mainViewModel::updateSortOrder,
                                onCreateNewScript = { navController.navigate(NavRoutes.scriptEditor(0L)) },
                                onImportDocument = { navController.navigate(NavRoutes.IMPORT_DOCUMENT) },
                                onEditScript = { id -> navController.navigate(NavRoutes.scriptEditor(id)) },
                                onStartTeleprompter = { id -> navController.navigate(NavRoutes.teleprompterReader(id)) },
                                onStartCameraTeleprompter = { id -> navController.navigate(NavRoutes.cameraTeleprompter(id)) },
                                onRenameScript = mainViewModel::renameScript,
                                onDuplicateScript = { mainViewModel.duplicateScript(it) },
                                onDeleteScript = mainViewModel::deleteScript
                            )
                        }

                        // 7. Teleprompter Reading Screen
                        composable(
                            route = NavRoutes.TELEPROMPTER_READER_PATTERN,
                            arguments = listOf(navArgument("scriptId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val scriptId = backStackEntry.arguments?.getLong("scriptId") ?: -1L
                            TeleprompterReaderScreen(
                                scriptId = scriptId,
                                settings = settings,
                                mainViewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onEditScript = { id -> navController.navigate(NavRoutes.scriptEditor(id)) },
                                onOpenCamera = { id ->
                                    navController.navigate(NavRoutes.cameraTeleprompter(id))
                                }
                            )
                        }

                        // 8. Camera Teleprompter Screen
                        composable(
                            route = NavRoutes.CAMERA_TELEPROMPTER_PATTERN,
                            arguments = listOf(navArgument("scriptId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val scriptId = backStackEntry.arguments?.getLong("scriptId") ?: -1L
                            CameraTeleprompterScreen(
                                initialScriptId = scriptId,
                                settings = settings,
                                allScripts = allScripts,
                                videoRepository = container.videoRepository,
                                mainViewModel = mainViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenVideoGallery = {
                                    navController.navigate(NavRoutes.VIDEOS) {
                                        popUpTo(NavRoutes.CAMERA_TELEPROMPTER_PATTERN) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 9. Video Gallery
                        composable(NavRoutes.VIDEOS) {
                            VideoGalleryScreen(
                                viewModel = videoGalleryViewModel,
                                onOpenCamera = {
                                    navController.navigate(
                                        NavRoutes.cameraTeleprompter(recentScripts.firstOrNull()?.id ?: -1L)
                                    )
                                }
                            )
                        }

                        // 10. Settings Screen
                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                mainViewModel = mainViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
