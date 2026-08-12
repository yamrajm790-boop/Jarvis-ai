package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SpeakerNotes
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.JarvisAccessibilityService
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisWarningRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    onNavigateBack: () -> Unit,
    onNavigatePermissions: () -> Unit = {},
    onNavigateBackgroundSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = viewModel.preferences
    val backendStatus by viewModel.backendStatus.collectAsState()

    var isWakeWordEnabled by remember { mutableStateOf(prefs.isWakeWordEnabled) }
    var wakePhrase by remember { mutableStateOf(prefs.wakeWordPhrase) }

    var autoExecute by remember { mutableStateOf(prefs.isAutoExecuteEnabled) }
    var confirmationMode by remember { mutableStateOf(prefs.isConfirmationModeEnabled) }

    val connectionStatusText = when (backendStatus) {
        is com.example.ai.BackendStatus.Connected -> "● Connected"
        is com.example.ai.BackendStatus.Connecting -> "● Connecting..."
        else -> "● Offline"
    }

    val isAccessibilityActive = JarvisAccessibilityService.isServiceAvailable()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisDarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Personal Assistant Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Top Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan.copy(alpha = 0.2f))
                        .border(1.5.dp, JarvisCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = "JARVIS Operating System", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Personal Device Control Assistant", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SYSTEM ACCESS & PERMISSIONS
        SectionHeader("SYSTEM ACCESS & CONTROL")
        SettingsRowItem(
            icon = Icons.Outlined.Shield,
            title = "Permissions Center",
            value = "View & Grant All",
            onClick = onNavigatePermissions
        )
        SettingsRowItem(
            icon = Icons.Outlined.BatteryChargingFull,
            title = "Background Operation",
            value = "Battery Opt & Rules",
            onClick = onNavigateBackgroundSetup
        )

        Spacer(modifier = Modifier.height(20.dp))

        // VOICE & WAKE WORD SECTION
        SectionHeader("VOICE & WAKE WORD")
        SettingsRowItem(icon = Icons.Outlined.RecordVoiceOver, title = "Voice & Speech", value = "US Male 1")
        SettingsRowItem(icon = Icons.Outlined.Mic, title = "Wake Word", value = if (isWakeWordEnabled) wakePhrase else "Off", onClick = {
            isWakeWordEnabled = !isWakeWordEnabled
            prefs.isWakeWordEnabled = isWakeWordEnabled
        })

        Spacer(modifier = Modifier.height(20.dp))

        // PERSONAL DEVICE MODE & EXECUTION RULES
        SectionHeader("PERSONAL DEVICE MODE")
        SettingsToggleRow(
            icon = Icons.Outlined.Security,
            title = "Auto Execute Actions",
            subtitle = "Execute safe commands without voice confirmation",
            checked = autoExecute,
            onCheckedChange = {
                autoExecute = it
                prefs.isAutoExecuteEnabled = it
            }
        )
        SettingsToggleRow(
            icon = Icons.Outlined.Security,
            title = "Confirmation Mode",
            subtitle = "Require confirmation before sensitive phone calls or SMS",
            checked = confirmationMode,
            onCheckedChange = {
                confirmationMode = it
                prefs.isConfirmationModeEnabled = it
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ABOUT SECTION
        SectionHeader("SYSTEM STATUS")
        SettingsRowItem(icon = Icons.Outlined.Security, title = "Version", value = "v3.0 Maximum Control")
        SettingsRowItem(icon = Icons.Outlined.Security, title = "Backend Connection", value = connectionStatusText)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.clearAllMemoryAndHistory() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JarvisWarningRed.copy(alpha = 0.2f))
        ) {
            Icon(imageVector = Icons.Outlined.Delete, contentDescription = null, tint = JarvisWarningRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear History & Memory", color = JarvisWarningRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = TextSecondary,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = JarvisCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(text = value, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = JarvisCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
            )
        }
    }
}
