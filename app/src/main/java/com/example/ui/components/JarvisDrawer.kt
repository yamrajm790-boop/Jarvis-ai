package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisWarningRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun JarvisDrawerContent(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onStopJarvis: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(290.dp)
            .background(JarvisDarkBackground)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JÁRVIS",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                letterSpacing = 2.sp
            )

            IconButton(onClick = onCloseDrawer) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Avatar Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(JarvisSurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan.copy(alpha = 0.2f))
                    .border(1.dp, JarvisCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Jarvis AI",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Personal Assistant",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(JarvisSuccessGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ONLINE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisSuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Items
        DrawerMenuItem(
            icon = Icons.Outlined.Home,
            label = "Home",
            isSelected = currentRoute == "home",
            onClick = {
                onNavigate("home")
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.History,
            label = "History",
            isSelected = currentRoute == "history",
            onClick = {
                onNavigate("history")
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.SmartButton,
            label = "Commands",
            isSelected = currentRoute == "commands",
            onClick = {
                onNavigate("commands")
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.Code,
            label = "Custom Commands",
            isSelected = currentRoute == "commands?tab=1",
            onClick = {
                onNavigate("commands?tab=1")
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            isSelected = currentRoute == "settings",
            onClick = {
                onNavigate("settings")
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.Mic,
            label = "Voice & Wake Word",
            isSelected = false,
            onClick = {
                onNavigate("settings")
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.Security,
            label = "Permissions",
            isSelected = false,
            onClick = {
                onNavigate("settings")
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.PhoneAndroid,
            label = "Device Info",
            isSelected = false,
            onClick = {
                onNavigate("settings")
                onCloseDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.Info,
            label = "About Jarvis",
            isSelected = false,
            onClick = {
                onNavigate("settings")
                onCloseDrawer()
            }
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(16.dp))

        // Stop Jarvis Warning Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onStopJarvis() }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PowerSettingsNew,
                contentDescription = "Stop Jarvis",
                tint = JarvisWarningRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Stop Jarvis",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = JarvisWarningRed
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) JarvisCyan.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) JarvisCyan else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) JarvisCyan else TextPrimary
        )
    }
}
