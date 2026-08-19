package com.arielfaridja.ezrahi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
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
import com.arielfaridja.ezrahi.app.ui.auth.SignUpScreen
import com.arielfaridja.ezrahi.app.ui.dial.QuickDialScreen
import com.arielfaridja.ezrahi.app.ui.events.EventPickerScreen
import com.arielfaridja.ezrahi.app.ui.map.MapScreen
import com.arielfaridja.ezrahi.app.util.EventPrefs
import com.arielfaridja.ezrahi.app.ui.theme.EzrahiTheme
import com.google.firebase.auth.FirebaseAuth
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
    DrawerDestination("Map", Icons.Default.Place, "map"),
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

    val auth = FirebaseAuth.getInstance()
    var isSignedIn by remember { mutableStateOf(auth.currentUser != null) }
    var currentEventId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            isSignedIn = it.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            val current = navController.currentDestination?.route
            if (current == "auth" || current == "signup") {
                navController.navigate("events") {
                    popUpTo("auth") { inclusive = true }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen || drawerState.targetValue == DrawerValue.Open,
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
                            if (destination.route == "map") {
                                if (currentEventId != null) {
                                    navController.navigate("map/$currentEventId") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate("events") {
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate("events") {
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate("signup")
                    }
                )
            }
            composable("signup") {
                SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate("events") {
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable("events") {
                val context = LocalContext.current
                EventPickerScreen(
                    onSelectEvent = { eventId ->
                        currentEventId = eventId
                        EventPrefs.saveLastEventId(context, eventId)
                        navController.navigate("map/$eventId") {
                            popUpTo("events") { inclusive = true }
                        }
                    }
                )
            }
            composable("map/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                MapScreen(
                    eventId = eventId,
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
                QuickDialScreen(
                    eventId = currentEventId,
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

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
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