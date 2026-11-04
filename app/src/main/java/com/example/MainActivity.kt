package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.Channel
import com.example.data.repository.IptvRepository
import com.example.player.MatchNightPlayerEngine
import com.example.ui.screens.*
import com.example.ui.theme.MatchNightTheme

enum class AppScreen { HOME, CHANNELS, ACCOUNT, PLAYER, SETUP }

class MainActivity : ComponentActivity() {
    private lateinit var repository: IptvRepository
    private lateinit var playerEngine: MatchNightPlayerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        repository = IptvRepository(applicationContext)
        playerEngine = MatchNightPlayerEngine(applicationContext)

        setContent {
            MatchNightTheme {
                val channels by repository.channels.collectAsState()
                val categories by repository.categories.collectAsState()
                val audioSources by repository.audioSources.collectAsState()
                val favorites by repository.favorites.collectAsState()
                val subscription by repository.currentSubscription.collectAsState()
                val playingChannel by playerEngine.currentChannel.collectAsState()

                LaunchedEffect(channels) { playerEngine.setChannelsList(channels) }

                var currentScreen by remember { mutableStateOf(if (subscription.isActive) AppScreen.HOME else AppScreen.SETUP) }
                var selectedCategoryId by rememberSaveable { mutableStateOf("") }
                var previousScreen by remember { mutableStateOf(AppScreen.HOME) }

                LaunchedEffect(playingChannel?.id) {
                    playingChannel?.let { repository.addToHistory(it.id) }
                }
                LaunchedEffect(subscription.isActive) {
                    if (subscription.isActive && currentScreen == AppScreen.SETUP) currentScreen = AppScreen.HOME
                }

                fun openChannel(channel: Channel) {
                    repository.addToHistory(channel.id)
                    playerEngine.playChannel(channel)
                    previousScreen = currentScreen
                    currentScreen = AppScreen.PLAYER
                }

                BackHandler(enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.SETUP) {
                    currentScreen = when (currentScreen) {
                        AppScreen.PLAYER -> previousScreen
                        AppScreen.ACCOUNT, AppScreen.CHANNELS -> AppScreen.HOME
                        else -> AppScreen.HOME
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.fillMaxSize().background(Color(0xFF0A0F14))) {
                        when (currentScreen) {
                            AppScreen.SETUP -> SubscriptionSetupScreen(
                                onConnect = { type, server, user, pass, m3u ->
                                    val result = repository.saveSubscription(type, server, user, pass, m3u)
                                    if (result.isSuccess) currentScreen = AppScreen.HOME
                                    result
                                }
                            )
                            AppScreen.HOME -> HomeScreen(
                                channels = channels,
                                subscription = subscription,
                                onOpenChannels = { currentScreen = AppScreen.CHANNELS },
                                onOpenAccount = { currentScreen = AppScreen.ACCOUNT },
                                onExit = { finish() },
                                onRefresh = { playerEngine.retryPlayback() },
                                onSelectChannel = { openChannel(it) }
                            )
                            AppScreen.CHANNELS -> LiveDashboardScreen(
                                categories = categories,
                                channels = channels,
                                favorites = favorites,
                                selectedCategoryId = selectedCategoryId,
                                onCategorySelected = { selectedCategoryId = it },
                                onSelectChannel = {
                                    repository.addToHistory(it.id)
                                    playerEngine.playChannel(it)
                                },
                                onToggleFavorite = { repository.toggleFavorite(it) },
                                onOpenPlayer = {
                                    previousScreen = AppScreen.CHANNELS
                                    currentScreen = AppScreen.PLAYER
                                },
                                onOpenAccount = { currentScreen = AppScreen.ACCOUNT },
                                onBackHome = { currentScreen = AppScreen.HOME },
                                playerEngine = playerEngine
                            )
                            AppScreen.ACCOUNT -> AccountScreen(
                                subscription = subscription,
                                onSaveSubscription = { type, server, user, pass, m3u ->
                                    repository.saveSubscription(type, server, user, pass, m3u)
                                }
                            )
                            AppScreen.PLAYER -> PlayerScreen(
                                playerEngine = playerEngine,
                                audioSources = audioSources,
                                onBack = { currentScreen = previousScreen },
                                onToggleFavorite = { repository.toggleFavorite(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerEngine.release()
    }
}
