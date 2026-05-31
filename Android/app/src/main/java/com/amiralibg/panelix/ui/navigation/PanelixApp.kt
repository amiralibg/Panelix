package com.amiralibg.panelix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amiralibg.panelix.repository.LibraryRepository
import com.amiralibg.panelix.ui.library.LibraryScreen
import com.amiralibg.panelix.ui.library.LibraryViewModel
import com.amiralibg.panelix.ui.onboarding.OnboardingScreen
import com.amiralibg.panelix.ui.reader.ReaderScreen
import com.amiralibg.panelix.ui.reader.ReaderViewModel
import com.amiralibg.panelix.ui.settings.SettingsScreen
import com.amiralibg.panelix.ui.settings.SettingsViewModel

private object Routes {
    const val Onboarding = "onboarding"
    const val Library = "library"
    const val Settings = "settings"
    const val Reader = "reader/{comicId}"
    fun reader(comicId: String) = "reader/$comicId"
}

@Composable
fun PanelixApp(repository: LibraryRepository) {
    val navController = rememberNavController()
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModelFactory(repository))
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val start = if (libraryState.preferences.hasCompletedOnboarding) Routes.Library else Routes.Onboarding

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.Onboarding) {
            OnboardingScreen { uri, flags ->
                libraryViewModel.addFolder(uri, flags)
                navController.navigate(Routes.Library) { popUpTo(Routes.Onboarding) { inclusive = true } }
            }
        }
        composable(Routes.Library) {
            LibraryScreen(
                state = libraryState,
                onAddFolder = libraryViewModel::addFolder,
                onRescan = libraryViewModel::rescan,
                onQuery = libraryViewModel::setQuery,
                onSort = libraryViewModel::setSort,
                onViewMode = libraryViewModel::setViewMode,
                onFilter = libraryViewModel::setFilter,
                onOpenComic = { navController.navigate(Routes.reader(it)) },
                onSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(Routes.Settings) {
            val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))
            val state by viewModel.state.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onAddFolder = viewModel::addFolder,
                onRemoveFolder = viewModel::removeFolder,
                onRescan = viewModel::rescan,
                onTheme = viewModel::setTheme,
                onReaderMode = viewModel::setReaderMode,
                onDirection = viewModel::setDirection,
            )
        }
        composable(
            route = Routes.Reader,
            arguments = listOf(navArgument("comicId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val comicId = requireNotNull(backStackEntry.arguments?.getString("comicId"))
            val viewModel: ReaderViewModel = viewModel(key = comicId, factory = ReaderViewModelFactory(comicId, repository))
            val state by viewModel.state.collectAsStateWithLifecycle()
            ReaderScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onPage = viewModel::setCurrentPage,
                onBookmark = viewModel::toggleBookmark,
                onPrefs = viewModel::saveReaderPrefs,
            )
        }
    }
}
