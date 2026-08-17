package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.NoteColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialColor: NoteColor = NoteColor.YELLOW,
    viewModel: NoteEditorViewModel = viewModel()
) {
    LaunchedEffect(noteId) {
        viewModel.initialize(noteId, initialColor)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val animatedBgColor by animateColorAsState(
        targetValue = uiState.color.bgColor,
        animationSpec = tween(durationMillis = 200),
        label = "bg_color_anim"
    )

    var showPaletteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Intercept hardware and system back gesture to auto-save
    BackHandler {
        viewModel.saveNote()
        onNavigateBack()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        ),
                        cursorBrush = SolidColor(uiState.color.stripeColor),
                        decorationBox = { innerTextField ->
                            Box {
                                if (uiState.title.isEmpty()) {
                                    Text(
                                        text = "Note Title...",
                                        style = TextStyle(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B).copy(alpha = 0.6f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveNote()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Save and Back",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                actions = {
                    // Pinned status toggle
                    IconButton(
                        onClick = { viewModel.togglePinned() },
                        modifier = Modifier.testTag("editor_pin_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (uiState.isPinned) "Unpin Note" else "Pin Note",
                            tint = if (uiState.isPinned) uiState.color.stripeColor else Color(0xFF475569)
                        )
                    }

                    // Color palette trigger
                    IconButton(
                        onClick = { showPaletteDialog = true },
                        modifier = Modifier.testTag("editor_color_palette_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = "Change Color",
                            tint = uiState.color.stripeColor
                        )
                    }

                    // Explicit Save Checkmark
                    IconButton(
                        onClick = {
                            viewModel.saveNote()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Note",
                            tint = Color(0xFF1E293B)
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Editor Options",
                                tint = Color(0xFF334155)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Note") },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935))
                                },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = animatedBgColor
                )
            )
        },
        bottomBar = {
            EditorBottomBar(
                uiState = uiState,
                backgroundColor = animatedBgColor
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(animatedBgColor)
        ) {
            TextEditorView(
                content = uiState.content,
                stripeColor = uiState.color.stripeColor,
                onContentChange = { viewModel.onContentChanged(it) }
            )
        }
    }

    // Color Palette Selection Dialog
    if (showPaletteDialog) {
        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            title = { Text("Choose Note Color") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NoteColor.entries.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(color.bgColor, CircleShape)
                                .clickable {
                                    viewModel.onColorSelected(color)
                                    showPaletteDialog = false
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(color.stripeColor, CircleShape)
                            )
                            if (uiState.color == color) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = color.stripeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaletteDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete this note?") },
            text = { Text("This will permanently remove the note from the Room SQLite database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteCurrentNote()
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TextEditorView(
    content: String,
    stripeColor: Color,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = Color(0xFF000000).copy(alpha = 0.05f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Subtle notebook lined paper background
                val lineHeight = 32.dp.toPx()
                var y = lineHeight
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += lineHeight
                }
            }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            textStyle = TextStyle(
                fontSize = 17.sp,
                lineHeight = 32.sp,
                color = Color(0xFF1E293B)
            ),
            cursorBrush = SolidColor(stripeColor),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (content.isEmpty()) {
                        Text(
                            text = "Tap here to start writing your note...",
                            style = TextStyle(
                                fontSize = 17.sp,
                                lineHeight = 32.sp,
                                color = Color(0xFF64748B).copy(alpha = 0.6f)
                            )
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("editor_body_input")
        )
    }
}

@Composable
fun EditorBottomBar(
    uiState: NoteEditorUiState,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(uiState.updatedAt) {
        val date = Date(uiState.updatedAt)
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
    }

    val words = if (uiState.content.isBlank()) 0 else uiState.content.trim().split("\\s+".toRegex()).size
    val chars = uiState.content.length
    val statusText = "$chars chars  |  $words words"

    Surface(
        color = backgroundColor,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Modified: $formattedTime",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
            )
        }
    }
}
