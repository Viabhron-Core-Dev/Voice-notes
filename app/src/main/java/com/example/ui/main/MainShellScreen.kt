package com.example.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.NoteEntity
import com.example.data.logkeeper.LogKeeperManager
import com.example.data.logkeeper.LogTag
import com.example.data.model.NoteColor
import com.example.ui.main.views.ArchiveNotesView
import com.example.ui.main.views.CalendarNotesView
import com.example.ui.main.views.FoldersView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    onOpenLogKeeper: () -> Unit,
    onOpenNoteEditor: (noteId: Long?, initialColor: NoteColor) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val allActiveNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedColorFilter by viewModel.selectedColorFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var noteToEditColor by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToArchive by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search title or content...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_input")
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                viewModel.onSearchQueryChanged("")
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Close Search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            } else {
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
                            if (selectedTab != 0) {
                                Text(
                                    text = " • " + when (selectedTab) {
                                        1 -> "Calendar"
                                        2 -> "Archive"
                                        3 -> "Folders"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            IconButton(
                                onClick = {
                                    isSearchActive = true
                                    LogKeeperManager.log(LogTag.UI_Editor, "Search mode opened")
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
                                    viewModel.cycleSortOrder()
                                },
                                modifier = Modifier.testTag("main_view_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Toggle Grid or Sort View"
                                )
                            }
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
                                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
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
            }
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
                    icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes") },
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
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = "Open Log Keeper Console",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Primary Add Note FAB
                FloatingActionButton(
                    onClick = {
                        LogKeeperManager.log(LogTag.UI_Editor, "Creating new note")
                        onOpenNoteEditor(null, selectedColorFilter ?: NoteColor.YELLOW)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // TAB 0: Main Notes List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Sort & Filter Header Bar
                            item(key = "header_sort", contentType = "header") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.cycleSortOrder() }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${sortOrder.displayName} ▼",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        )
                                    }

                                    // Color Filter Horizontal Selector
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 6.dp)
                                    ) {
                                        item(key = "color_all") {
                                            FilterChip(
                                                selected = selectedColorFilter == null,
                                                onClick = { viewModel.onColorFilterSelected(null) },
                                                label = { Text("All (${notes.size})") },
                                                colors = FilterChipDefaults.filterChipColors()
                                            )
                                        }

                                        items(NoteColor.entries.toTypedArray(), key = { it.name }) { color ->
                                            FilterChip(
                                                selected = selectedColorFilter == color,
                                                onClick = {
                                                    if (selectedColorFilter == color) {
                                                        viewModel.onColorFilterSelected(null)
                                                    } else {
                                                        viewModel.onColorFilterSelected(color)
                                                    }
                                                },
                                                label = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .background(color.stripeColor, CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(color.displayName)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Live Room Database Note Cards
                            items(
                                items = notes,
                                key = { it.id },
                                contentType = { "note_card" }
                            ) { note ->
                                val noteColor = remember(note.colorTheme) { NoteColor.fromName(note.colorTheme) }
                                val formattedTime = remember(note.updatedAt) {
                                    val date = Date(note.updatedAt)
                                    val now = System.currentTimeMillis()
                                    if (now - note.updatedAt < 86400000) {
                                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
                                    } else {
                                        SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
                                    }
                                }

                                ColorNoteCardItem(
                                    note = note,
                                    noteColor = noteColor,
                                    formattedTime = formattedTime,
                                    onClick = {
                                        onOpenNoteEditor(note.id, noteColor)
                                    },
                                    onTogglePin = { viewModel.togglePin(note) },
                                    onArchive = {
                                        LogKeeperManager.log(LogTag.Storage, "Archiving note #${note.id}")
                                        viewModel.toggleArchive(note)
                                    },
                                    onDelete = { viewModel.deleteNote(note) },
                                    onChangeColor = { noteToEditColor = note }
                                )
                            }

                            item(key = "footer_spacer", contentType = "spacer") {
                                Spacer(modifier = Modifier.height(30.dp))
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Calendar View
                        CalendarNotesView(
                            notes = allActiveNotes,
                            onOpenNoteEditor = onOpenNoteEditor,
                            onTogglePin = { viewModel.togglePin(it) },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onChangeColor = { noteToEditColor = it }
                        )
                    }

                    2 -> {
                        // TAB 2: Archive View
                        ArchiveNotesView(
                            archivedNotes = archivedNotes,
                            onOpenNoteEditor = onOpenNoteEditor,
                            onRestoreNote = { viewModel.toggleArchive(it) },
                            onPermanentDelete = { viewModel.deleteNote(it) }
                        )
                    }

                    3 -> {
                        // TAB 3: Folders View
                        FoldersView(
                            notes = allActiveNotes,
                            onOpenNoteEditor = onOpenNoteEditor,
                            onTogglePin = { viewModel.togglePin(it) },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onChangeColor = { noteToEditColor = it }
                        )
                    }
                }
            }
        }
    }

    // Color Picker Dialog
    noteToEditColor?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToEditColor = null },
            title = { Text("Change Note Color") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NoteColor.entries.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color.bgColor, CircleShape)
                                .clickable {
                                    viewModel.updateNoteColor(note, color)
                                    noteToEditColor = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(color.stripeColor, CircleShape)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { noteToEditColor = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun ColorNoteCardItem(
    note: NoteEntity,
    noteColor: NoteColor,
    formattedTime: String,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onChangeColor: () -> Unit,
    modifier: Modifier = Modifier,
    onArchive: (() -> Unit)? = null
) {
    var cardMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = noteColor.bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Colored Accent Stripe (Tap to change color)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxSize()
                    .background(noteColor.stripeColor)
                    .clickable { onChangeColor() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Content preview
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1E293B)
                    ),
                    maxLines = 1
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF475569),
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            // Pin Button
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                    tint = if (note.isPinned) noteColor.stripeColor else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Timestamp
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF334155),
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(end = 4.dp)
            )

            // Card Options Menu (Archive, Change Color, Delete)
            Box {
                IconButton(
                    onClick = { cardMenuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Note Actions",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = cardMenuExpanded,
                    onDismissRequest = { cardMenuExpanded = false }
                ) {
                    if (onArchive != null) {
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                cardMenuExpanded = false
                                onArchive()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Change Color") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(noteColor.stripeColor, CircleShape)
                            )
                        },
                        onClick = {
                            cardMenuExpanded = false
                            onChangeColor()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFE53935)) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935))
                        },
                        onClick = {
                            cardMenuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
