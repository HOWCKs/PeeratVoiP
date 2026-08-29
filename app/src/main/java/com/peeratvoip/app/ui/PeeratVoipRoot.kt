package com.peeratvoip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.peeratvoip.app.ui.components.NavItem
import com.peeratvoip.app.ui.components.NeuBottomNavBar
import com.peeratvoip.app.ui.screens.live.LiveScreen
import com.peeratvoip.app.ui.screens.presets.PresetsScreen
import com.peeratvoip.app.ui.screens.recordings.RecordingsScreen
import com.peeratvoip.app.ui.screens.settings.SettingsScreen
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.PeeratVoipTheme

private object Routes {
    const val LIVE = "live"
    const val PRESETS = "presets"
    const val RECORDINGS = "recordings"
    const val SETTINGS = "settings"
}

private val navItems = listOf(
    NavItem(Routes.LIVE, "Ao vivo", Icons.Filled.Mic),
    NavItem(Routes.PRESETS, "Vozes", Icons.Filled.Widgets),
    NavItem(Routes.RECORDINGS, "Gravações", Icons.Filled.PlayArrow),
    NavItem(Routes.SETTINGS, "Ajustes", Icons.Filled.Settings),
)

@Composable
fun PeeratVoipApp(
    hasMicPermission: () -> Boolean,
    onRequestMicPermission: () -> Unit,
) {
    PeeratVoipTheme {
        val palette = LocalNeuPalette.current
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    NavHost(navController = navController, startDestination = Routes.LIVE) {
                        composable(Routes.LIVE) {
                            LiveScreen(
                                hasMicPermission = hasMicPermission,
                                onRequestMicPermission = onRequestMicPermission,
                                onOpenPresets = { navController.navigate(Routes.PRESETS) },
                            )
                        }
                        composable(Routes.PRESETS) {
                            PresetsScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.RECORDINGS) {
                            RecordingsScreen()
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen()
                        }
                    }
                }
                NeuBottomNavBar(
                    items = navItems,
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun PeeratVoipAppPreview() {
    PeeratVoipApp(hasMicPermission = { true }, onRequestMicPermission = {})
}
