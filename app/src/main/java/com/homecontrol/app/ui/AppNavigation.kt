package com.homecontrol.app.ui

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.homecontrol.app.AppContainer
import com.homecontrol.app.data.AppSettings
import com.homecontrol.app.data.ThemeMode
import com.homecontrol.app.ui.devices.DevicesScreen
import com.homecontrol.app.ui.devices.DevicesViewModel
import com.homecontrol.app.ui.devices.EditorScreen
import com.homecontrol.app.ui.devices.EditorViewModel
import com.homecontrol.app.ui.history.HistoryScreen
import com.homecontrol.app.ui.history.HistoryViewModel
import com.homecontrol.app.ui.home.HomeScreen
import com.homecontrol.app.ui.home.HomeViewModel
import com.homecontrol.app.ui.settings.SettingsScreen
import com.homecontrol.app.ui.settings.SettingsViewModel
import com.homecontrol.app.ui.theme.HomeControlTheme

object Routes {
    const val HOME = "home"
    const val DEVICES = "devices"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{id}"
    fun editor(id: Long) = "editor/$id"
}

private val TOP_ROUTES = listOf(Routes.HOME, Routes.DEVICES, Routes.HISTORY, Routes.SETTINGS)

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun HomeControlRoot(container: AppContainer) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val dark = when (settings.theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(dark) {
        val transparent = AndroidColor.TRANSPARENT
        val style = if (dark) {
            SystemBarStyle.dark(transparent)
        } else {
            SystemBarStyle.light(transparent, transparent)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { }
    }

    HomeControlTheme(dark = dark) {
        AppScaffold(container)
    }
}

@Composable
private fun AppScaffold(container: AppContainer) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.background, scheme.surface)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if (route in TOP_ROUTES) BottomBar(nav, route) }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 24 } },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(150)) }
            ) {
                composable(Routes.HOME) {
                    val vm: HomeViewModel = viewModel(factory = viewModelFactory {
                        initializer { HomeViewModel(container) }
                    })
                    HomeScreen(
                        vm = vm,
                        snackbar = snackbar,
                        onOpenSettings = { nav.navigateTop(Routes.SETTINGS) },
                        onOpenDevices = { nav.navigateTop(Routes.DEVICES) }
                    )
                }
                composable(Routes.DEVICES) {
                    val vm: DevicesViewModel = viewModel(factory = viewModelFactory {
                        initializer { DevicesViewModel(container) }
                    })
                    DevicesScreen(
                        vm = vm,
                        onAdd = { nav.navigate(Routes.editor(0L)) },
                        onEdit = { id -> nav.navigate(Routes.editor(id)) }
                    )
                }
                composable(Routes.HISTORY) {
                    val vm: HistoryViewModel = viewModel(factory = viewModelFactory {
                        initializer { HistoryViewModel(container) }
                    })
                    HistoryScreen(vm)
                }
                composable(Routes.SETTINGS) {
                    val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                        initializer { SettingsViewModel(container) }
                    })
                    SettingsScreen(
                        vm = vm,
                        snackbar = snackbar,
                        onOpenHistory = { nav.navigateTop(Routes.HISTORY) }
                    )
                }
                composable(
                    route = Routes.EDITOR,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("id") ?: 0L
                    val vm: EditorViewModel = viewModel(
                        key = "editor-$id",
                        factory = viewModelFactory { initializer { EditorViewModel(container, id) } }
                    )
                    EditorScreen(vm = vm, onClose = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    val items = listOf(
        Triple(Routes.HOME, "خانه", Icons.Filled.Home),
        Triple(Routes.DEVICES, "وسایل", Icons.Filled.Dashboard),
        Triple(Routes.HISTORY, "تاریخچه", Icons.Filled.History),
        Triple(Routes.SETTINGS, "تنظیمات", Icons.Filled.Settings)
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        items.forEach { (r, label, icon) ->
            NavigationBarItem(
                selected = route == r,
                onClick = { nav.navigateTop(r) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
