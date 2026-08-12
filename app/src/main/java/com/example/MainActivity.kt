package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.service.JarvisForegroundService
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisBottomNav
import com.example.ui.components.JarvisDrawerContent
import com.example.ui.screens.BackgroundSetupScreen
import com.example.ui.screens.CommandsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()

        if (viewModel.preferences.isBackgroundAssistantEnabled && !JarvisForegroundService.isServiceRunning) {
            JarvisForegroundService.startService(this)
        }

        if (intent?.getBooleanExtra("START_VOICE_NOW", false) == true) {
            viewModel.startVoiceInput()
        }

        setContent {
            JarvisTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        JarvisDrawerContent(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onCloseDrawer = {
                                scope.launch { drawerState.close() }
                            },
                            onStopJarvis = {
                                viewModel.stopVoiceInput()
                                viewModel.ttsManager.stop()
                                JarvisForegroundService.stopService(this@MainActivity)
                                scope.launch { drawerState.close() }
                                finish()
                            }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = JarvisDarkBackground,
                        bottomBar = {
                            JarvisBottomNav(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onOrbClick = {
                                    if (currentRoute != "home") {
                                        navController.navigate("home")
                                    }
                                    viewModel.startVoiceInput()
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "home"
                            ) {
                                composable("home") {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onOpenDrawer = {
                                            scope.launch { drawerState.open() }
                                        },
                                        onNavigateHistory = { navController.navigate("history") }
                                    )
                                }

                                composable("permissions") {
                                    PermissionsScreen(
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable("background-setup") {
                                    BackgroundSetupScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable("history") {
                                    HistoryScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable("commands") {
                                    CommandsScreen(
                                        viewModel = viewModel,
                                        initialTab = 0,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable("commands?tab=1") {
                                    CommandsScreen(
                                        viewModel = viewModel,
                                        initialTab = 1,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable("settings") {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigatePermissions = { navController.navigate("permissions") },
                                        onNavigateBackgroundSetup = { navController.navigate("background-setup") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
