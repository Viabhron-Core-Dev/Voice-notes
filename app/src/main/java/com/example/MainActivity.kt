package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.logkeeper.LogKeeperManager
import com.example.data.logkeeper.LogTag
import com.example.data.model.NoteColor
import com.example.ui.editor.NoteEditorScreen
import com.example.ui.logkeeper.LogKeeperScreen
import com.example.ui.main.MainShellScreen
import com.example.ui.theme.MyApplicationTheme

sealed interface AppScreen {
    data object Main : AppScreen
    data object LogKeeper : AppScreen
    data class Editor(
        val noteId: Long? = null,
        val initialColor: NoteColor = NoteColor.YELLOW
    ) : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigationRoot()
                }
            }
        }
    }
}

@Composable
fun AppNavigationRoot() {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Main) }

    BackHandler(enabled = currentScreen !is AppScreen.Main) {
        LogKeeperManager.log(LogTag.Navigation, "Back pressed: returned to main")
        currentScreen = AppScreen.Main
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is AppScreen.Main -> {
                MainShellScreen(
                    onOpenLogKeeper = {
                        currentScreen = AppScreen.LogKeeper
                    },
                    onOpenNoteEditor = { noteId, color ->
                        currentScreen = AppScreen.Editor(noteId, color)
                    }
                )
            }
            is AppScreen.LogKeeper -> {
                LogKeeperScreen(
                    onNavigateBack = {
                        LogKeeperManager.log(LogTag.Navigation, "Navigated back from LogKeeper to main")
                        currentScreen = AppScreen.Main
                    }
                )
            }
            is AppScreen.Editor -> {
                NoteEditorScreen(
                    noteId = screen.noteId,
                    initialColor = screen.initialColor,
                    onNavigateBack = {
                        LogKeeperManager.log(LogTag.Navigation, "Navigated back from Editor to main")
                        currentScreen = AppScreen.Main
                    }
                )
            }
        }
    }
}
