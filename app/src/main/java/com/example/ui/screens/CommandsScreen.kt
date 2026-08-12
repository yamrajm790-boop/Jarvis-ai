package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.components.CustomCommandDialog
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBackground
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisWarningRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class CommandItem(
    val title: String,
    val commandText: String,
    val icon: ImageVector,
    val category: String
)

@Composable
fun CommandsScreen(
    viewModel: JarvisViewModel,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit
) {
    val customCommands by viewModel.customCommands.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        CustomCommandDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { trigger, desc, tool, arg ->
                viewModel.addCustomCommand(trigger, desc, tool, arg)
            }
        )
    }

    val generalCommands = remember {
        listOf(
            CommandItem("Open YouTube", "Open YouTube", Icons.Outlined.PlayCircle, "GENERAL"),
            CommandItem("Open Google Maps", "Open Google Maps", Icons.Outlined.Map, "GENERAL"),
            CommandItem("Open WhatsApp", "Open WhatsApp", Icons.Outlined.SmartButton, "GENERAL"),
            CommandItem("Open Settings", "Open Settings", Icons.Outlined.Settings, "GENERAL"),
            CommandItem("Take Screenshot", "Take Screenshot", Icons.Outlined.Screenshot, "GENERAL")
        )
    }

    val mediaCommands = remember {
        listOf(
            CommandItem("Play Music", "Play music", Icons.Outlined.PlayArrow, "MEDIA"),
            CommandItem("Pause Music", "Pause music", Icons.Outlined.Pause, "MEDIA"),
            CommandItem("Next Track", "Next song", Icons.Outlined.SkipNext, "MEDIA"),
            CommandItem("Previous Track", "Previous song", Icons.Outlined.SkipPrevious, "MEDIA")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisDarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "COMMAND HUB",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Commands",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .background(JarvisCyan.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, JarvisCyan, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Command", tint = JarvisCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = JarvisSurfaceDark,
            contentColor = JarvisCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = JarvisCyan
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("All Commands", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Custom (${customCommands.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Shortcuts", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0, 2 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            CategoryHeader(title = "GENERAL")
                        }
                        items(generalCommands) { cmd ->
                            CommandRowItem(cmd = cmd, onClick = { viewModel.processUserVoiceText(cmd.commandText) })
                        }

                        item {
                            CategoryHeader(title = "MEDIA")
                        }
                        items(mediaCommands) { cmd ->
                            CommandRowItem(cmd = cmd, onClick = { viewModel.processUserVoiceText(cmd.commandText) })
                        }
                    }
                }
                1 -> {
                    if (customCommands.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No custom routines defined. Tap + to add one, sir.", color = TextSecondary, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(customCommands) { cmd ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.processUserVoiceText(cmd.triggerPhrase) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("\"${cmd.triggerPhrase}\"", fontWeight = FontWeight.Bold, color = JarvisCyan, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(cmd.description, fontSize = 12.sp, color = TextSecondary)
                                        }
                                        IconButton(onClick = { viewModel.deleteCustomCommand(cmd.id) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = JarvisWarningRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = TextSecondary,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun CommandRowItem(cmd: CommandItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = cmd.icon, contentDescription = cmd.title, tint = JarvisCyan, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = cmd.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Run", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
