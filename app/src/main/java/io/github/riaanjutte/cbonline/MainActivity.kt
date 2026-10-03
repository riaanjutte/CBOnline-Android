package io.github.riaanjutte.cbonline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.riaanjutte.cbonline.ui.RosterScreen
import io.github.riaanjutte.cbonline.ui.RosterViewModel
import io.github.riaanjutte.cbonline.ui.theme.CbOnlineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so system bar icons must be light even when the phone is in light mode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val container = (application as CbOnlineApp).container
        setContent {
            CbOnlineTheme {
                val vm: RosterViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            RosterViewModel(
                                container.playersApi,
                                container.missionApi,
                                container.friendsStore,
                                container.updateChecker,
                                BuildConfig.VERSION_NAME
                            )
                        }
                    }
                )
                val state by vm.state.collectAsStateWithLifecycle()
                RosterScreen(
                    state = state,
                    versionName = BuildConfig.VERSION_NAME,
                    onRefresh = vm::refresh,
                    onToggleFriend = vm::toggleFriend,
                    onDismissUpdate = vm::dismissUpdate,
                    // No browser installed is the only failure; nothing useful to show for it
                    onOpenUrl = { runCatching { startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) } }
                )
            }
        }
    }
}
