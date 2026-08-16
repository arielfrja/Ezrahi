package com.arielfaridja.ezrahi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arielfaridja.ezrahi.app.ui.auth.AuthScreen
import com.arielfaridja.ezrahi.app.ui.map.MapScreen
import com.arielfaridja.ezrahi.app.ui.theme.EzrahiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EzrahiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EzrahiNavApp()
                }
            }
        }
    }
}

private data class DrawerDestination(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val drawerDestinations = listOf(
    DrawerDestination("Map", Icons.Default.Map, "map"),
    DrawerDestination("Chat", Icons.AutoMirrored.Filled.Chat, "messages"),
    DrawerDestination("Speed Dial", Icons.Default.Phone, "speed_dial"),
    DrawerDestination("Activity Overview", Icons.AutoMirrored.Filled.List, "activity_overview"),
    DrawerDestination("Settings", Icons.Default.Settings, "settings")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EzrahiNavApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ezrahi",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }
                drawerDestinations.forEach { destination ->
                    val isSelected = currentRoute?.startsWith(destination.route) == true
                    NavigationDrawerItem(
                        label = { Text(destination.label) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "auth") {
            composable("auth") {
                AuthScreen(onAuthSuccess = {
                    navController.navigate("map/demo_event_123") {
                        popUpTo("auth") { inclusive = true }
                    }
                })
            }
            composable("map/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                MapScreen(
                    eventId = eventId,
                    onNavigateToMessages = { navController.navigate("messages/$eventId") },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("messages/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                PlaceholderScreen(
                    title = "Messages",
                    subtitle = "Messages for event: $eventId (Work in Progress)",
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("speed_dial") {
                PlaceholderScreen(
                    title = "Speed Dial",
                    subtitle = "Speed dial (Work in Progress)",
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("activity_overview") {
                PlaceholderScreen(
                    title = "Activity Overview",
                    subtitle = "Activity overview (Work in Progress)",
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("settings") {
                PlaceholderScreen(
                    title = "Settings",
                    subtitle = "Settings (Work in Progress)",
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    onOpenDrawer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(subtitle)
        }
    }
}