package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.accessibility.JarvisAccessibilityService
import com.example.service.JarvisForegroundService
import com.example.service.JarvisNotificationListenerService
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisSurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var micGranted by remember { mutableStateOf(checkPermission(context, Manifest.permission.RECORD_AUDIO)) }
    var phoneGranted by remember { mutableStateOf(checkPermission(context, Manifest.permission.CALL_PHONE)) }
    var contactsGranted by remember { mutableStateOf(checkPermission(context, Manifest.permission.READ_CONTACTS)) }
    var locationGranted by remember { mutableStateOf(checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var bluetoothGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            } else true
        )
    }
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            } else true
        )
    }
    var accessibilityEnabled by remember { mutableStateOf(JarvisAccessibilityService.isServiceAvailable()) }
    var notificationAccessEnabled by remember { mutableStateOf(JarvisNotificationListenerService.isServiceAvailable()) }
    var batteryOptDisabled by remember { mutableStateOf(isBatteryOptDisabled(context)) }
    var backgroundServiceRunning by remember { mutableStateOf(JarvisForegroundService.isServiceRunning) }

    fun refreshStatus() {
        micGranted = checkPermission(context, Manifest.permission.RECORD_AUDIO)
        phoneGranted = checkPermission(context, Manifest.permission.CALL_PHONE)
        contactsGranted = checkPermission(context, Manifest.permission.READ_CONTACTS)
        locationGranted = checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothGranted = checkPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted = checkPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        }
        accessibilityEnabled = JarvisAccessibilityService.isServiceAvailable()
        notificationAccessEnabled = JarvisNotificationListenerService.isServiceAvailable()
        batteryOptDisabled = isBatteryOptDisabled(context)
        backgroundServiceRunning = JarvisForegroundService.isServiceRunning
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshStatus()
    }

    Scaffold(
        containerColor = JarvisDarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("JARVIS Permissions Center", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = JarvisCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "System Access & Permissions Status",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "JARVIS uses user-granted permissions and Android system services to provide genuine hands-free device control.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            item {
                PermissionItemCard(
                    title = "Microphone",
                    description = "Required for voice commands & wake word",
                    isGranted = micGranted,
                    onGrant = {
                        requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Accessibility Service",
                    description = "Required for UI actions (home, back, recent apps, screenshot, scroll)",
                    isGranted = accessibilityEnabled,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Notifications",
                    description = "Required for background service notification",
                    isGranted = notificationsGranted,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Notification Access",
                    description = "Required for reading/summarizing incoming device notifications",
                    isGranted = notificationAccessEnabled,
                    onGrant = {
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Phone & Calling",
                    description = "Required to initiate phone calls upon voice command",
                    isGranted = phoneGranted,
                    onGrant = {
                        requestPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Contacts",
                    description = "Required to resolve contact names for calling and messaging",
                    isGranted = contactsGranted,
                    onGrant = {
                        requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Bluetooth",
                    description = "Required for Bluetooth audio headset integration",
                    isGranted = bluetoothGranted,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            requestPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Location",
                    description = "Required for location-contextual assistant requests",
                    isGranted = locationGranted,
                    onGrant = {
                        requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Battery Optimization",
                    description = "Disabling battery optimization allows JARVIS to remain active in background",
                    isGranted = batteryOptDisabled,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(settingsIntent)
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Background Service",
                    description = "Persistent Foreground Service monitoring assistant requests",
                    isGranted = backgroundServiceRunning,
                    onGrant = {
                        if (backgroundServiceRunning) {
                            JarvisForegroundService.stopService(context)
                        } else {
                            JarvisForegroundService.startService(context)
                        }
                        refreshStatus()
                    },
                    buttonLabel = if (backgroundServiceRunning) "Stop Service" else "Start Service"
                )
            }
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit,
    buttonLabel: String = "Configure"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisSurfaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, if (isGranted) JarvisCyan.copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isGranted) JarvisCyan else Color.Red,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGranted) "✓ Active" else "✗ Needed",
                        color = if (isGranted) JarvisCyan else Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isGranted || buttonLabel == "Stop Service") {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGranted) Color.Red.copy(alpha = 0.8f) else JarvisCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isGranted) buttonLabel else "Grant",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onGrant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f))
                ) {
                    Text("Settings", color = JarvisCyan, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun isBatteryOptDisabled(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
