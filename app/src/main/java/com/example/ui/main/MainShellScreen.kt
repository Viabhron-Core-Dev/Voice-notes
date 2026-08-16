package com.example.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.logkeeper.LogKeeperManager
import com.example.data.logkeeper.LogTag
import com.example.ui.theme.NoteBlue
import com.example.ui.theme.NoteBlueStripe
import com.example.ui.theme.NoteGreen
import com.example.ui.theme.NoteGreenStripe
import com.example.ui.theme.NotePeach
import com.example.ui.theme.NotePeachStripe
import com.example.ui.theme.NotePink
import com.example.ui.theme.NotePinkStripe
import com.example.ui.theme.NoteYellow
import com.example.ui.theme.NoteYellowStripe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    onOpenLogKeeper: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Color",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Note",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            LogKeeperManager.log(LogTag.UI_Editor, "Search button clicked")
                        },
                        modifier = Modifier.testTag("main_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Notes"
                        )
                    }

                    IconButton(
                        onClick = {
                            LogKeeperManager.log(LogTag.UI_Editor, "View mode toggled")
                        },
                        modifier = Modifier.testTag("main_view_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Toggle Grid or List View"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("main_overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Log Keeper") },
                                leadingIcon = {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    LogKeeperManager.log(LogTag.Navigation, "Opened LogKeeper via top menu")
                                    onOpenLogKeeper()
                                },
                                modifier = Modifier.testTag("menu_item_logkeeper")
                            )
                            DropdownMenuItem(
                                text = { Text("Import Whisper Model") },
                                leadingIcon = {
                                    Icon(Icons.Default.Tune, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    LogKeeperManager.log(LogTag.Navigation, "Opened Model Manager (Mini-Phase 6 ready)")
                                },
                                modifier = Modifier.testTag("menu_item_import_model")
                            )
                            DropdownMenuItem(
                                text = { Text("Backup & Restore") },
                                leadingIcon = {
                                    Icon(Icons.Default.Security, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    LogKeeperManager.log(LogTag.Navigation, "Opened Backup & Restore (Mini-Phase 9 ready)")
                                },
                                modifier = Modifier.testTag("menu_item_backup")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        LogKeeperManager.log(LogTag.Navigation, "Switched tab: Notes List")
                    },
                    icon = { Icon(Icons.Default.Notes, contentDescription = "Notes") },
                    label = { Text("Notes") },
                    modifier = Modifier.testTag("bottom_tab_notes")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        LogKeeperManager.log(LogTag.Navigation, "Switched tab: Calendar")
                    },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    modifier = Modifier.testTag("bottom_tab_calendar")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        LogKeeperManager.log(LogTag.Navigation, "Switched tab: Archive")
                    },
                    icon = { Icon(Icons.Default.Archive, contentDescription = "Archive") },
                    label = { Text("Archive") },
                    modifier = Modifier.testTag("bottom_tab_archive")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        LogKeeperManager.log(LogTag.Navigation, "Switched tab: Folders")
                    },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Folders") },
                    label = { Text("Folders") },
                    modifier = Modifier.testTag("bottom_tab_folders")
                )
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Floating LogKeeper FAB
                FloatingActionButton(
                    onClick = {
                        LogKeeperManager.log(LogTag.Navigation, "Opened LogKeeper via global FAB")
                        onOpenLogKeeper()
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fab_open_logkeeper")
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Open Log Keeper Console",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Primary Add Note FAB
                FloatingActionButton(
                    onClick = {
                        LogKeeperManager.log(LogTag.UI_Editor, "Create note triggered (Ready for Mini-Phase 5)")
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF00897B),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("fab_add_note")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create New Note",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sort Header Bar
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort by color ▼",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            // Milestone Banner: Mini-Phase 1 Ready Check
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Mini-Phase 1: Shell & LogKeeper Active",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• System insets are fully active (Status bar & Navigation bar safe)\n• LogKeeper is live and listening across all components\n• Tap the dark button on bottom-right to open LogKeeper",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // Sample ColorNote Cards
            item {
                SampleColorNoteCard(
                    title = "To do",
                    time = "3:11 pm",
                    bgColor = NotePink,
                    stripeColor = NotePinkStripe,
                    hasCheckmark = true
                )
            }
            item {
                SampleColorNoteCard(
                    title = "Record of things bought",
                    time = "14 Aug",
                    bgColor = NotePink,
                    stripeColor = NotePinkStripe
                )
            }
            item {
                SampleColorNoteCard(
                    title = "Good prompts",
                    time = "12:49 pm",
                    bgColor = NotePeach,
                    stripeColor = NotePeachStripe,
                    hasCheckmark = true
                )
            }
            item {
                SampleColorNoteCard(
                    title = "Omnivian",
                    time = "4:39 pm",
                    bgColor = NoteYellow,
                    stripeColor = NoteYellowStripe
                )
            }
            item {
                SampleColorNoteCard(
                    title = "A web novel ai writer.\nThis is not written by...",
                    time = "15 Aug",
                    bgColor = NoteYellow,
                    stripeColor = NoteYellowStripe
                )
            }
            item {
                SampleColorNoteCard(
                    title = "Voice Notes offline engine",
                    time = "Just now",
                    bgColor = NoteBlue,
                    stripeColor = NoteBlueStripe
                )
            }
            item {
                SampleColorNoteCard(
                    title = "Meeting agenda - Board",
                    time = "Today",
                    bgColor = NoteGreen,
                    stripeColor = NoteGreenStripe
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SampleColorNoteCard(
    title: String,
    time: String,
    bgColor: Color,
    stripeColor: Color,
    hasCheckmark: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Colored Accent Stripe
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxSize()
                    .background(stripeColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF1E293B)
                ),
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )

            // Right Metadata: Checkmark + Timestamp
            Row(
                modifier = Modifier.padding(end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasCheckmark) {
                    Text(
                        text = "✓ ",
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF334155),
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
