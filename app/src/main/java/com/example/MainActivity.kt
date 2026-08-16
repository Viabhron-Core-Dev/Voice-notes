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
import com.example.ui.logkeeper.LogKeeperScreen
import com.example.ui.main.MainShellScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    MAIN,
    LOG_KEEPER
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
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }

    BackHandler(enabled = currentScreen != Screen.MAIN) {
        LogKeeperManager.log(LogTag.Navigation, "Back pressed: returned to main")
        currentScreen = Screen.MAIN
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.MAIN -> {
                MainShellScreen(
                    onOpenLogKeeper = {
                        currentScreen = Screen.LOG_KEEPER
                    }
                )
            }
            Screen.LOG_KEEPER -> {
                LogKeeperScreen(
                    onNavigateBack = {
                        LogKeeperManager.log(LogTag.Navigation, "Navigated back from LogKeeper to main")
                        currentScreen = Screen.MAIN
                    }
                )
            }
        }
    }
}
