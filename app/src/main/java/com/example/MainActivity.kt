package com.example

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Channel
import com.example.data.model.Match
import com.example.data.repository.IptvRepository
import com.example.player.MatchNightPlayerEngine
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class AppScreen {
    HOME,
    MATCHES,
    CHANNELS,
    ACCOUNT,
    PLAYER
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: IptvRepository
    private lateinit var playerEngine: MatchNightPlayerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during sports match playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        repository = IptvRepository(applicationContext)
        playerEngine = MatchNightPlayerEngine(applicationContext)

        setContent {
            MatchNightTheme {
                val channels by repository.channels.collectAsState()
                val categories by repository.categories.collectAsState()
                val matches by repository.matches.collectAsState()
                val favorites by repository.favorites.collectAsState()
                val history by repository.history.collectAsState()
                val subscription by repository.currentSubscription.collectAsState()

                // Keep player engine channels updated for previous/next channel actions
                LaunchedEffect(channels) {
                    playerEngine.setChannelsList(channels)
                }

                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                var previousScreen by remember { mutableStateOf(AppScreen.HOME) }

                fun openChannel(channel: Channel) {
                    repository.addToHistory(channel.id)
                    playerEngine.playChannel(channel)
                    previousScreen = currentScreen
                    currentScreen = AppScreen.PLAYER
                }

                fun openMatch(match: Match) {
                    val ch = channels.firstOrNull { it.id == match.primaryChannelId }
                        ?: channels.firstOrNull()
                    if (ch != null) {
                        openChannel(ch)
                    }
                }

                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                Scaffold(
                    bottomBar = {
                        // Hide bottom bar when inside fullscreen video player or landscape video
                        if (currentScreen != AppScreen.PLAYER) {
                            NavigationBar(
                                containerColor = DarkSportsBlue,
                                contentColor = SnowWhite,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .testTag("main_bottom_nav")
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.HOME,
                                    onClick = { currentScreen = AppScreen.HOME },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                                    label = { Text("الرئيسية", fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NightNavy,
                                        selectedTextColor = CalmGold,
                                        indicatorColor = CalmGold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_home")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.MATCHES,
                                    onClick = { currentScreen = AppScreen.MATCHES },
                                    icon = { Icon(Icons.Default.SportsSoccer, contentDescription = "المباريات") },
                                    label = { Text("المباريات", fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NightNavy,
                                        selectedTextColor = CalmGold,
                                        indicatorColor = CalmGold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_matches")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.CHANNELS,
                                    onClick = { currentScreen = AppScreen.CHANNELS },
                                    icon = { Icon(Icons.Default.Tv, contentDescription = "القنوات") },
                                    label = { Text("القنوات", fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NightNavy,
                                        selectedTextColor = CalmGold,
                                        indicatorColor = CalmGold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_channels")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.ACCOUNT,
                                    onClick = { currentScreen = AppScreen.ACCOUNT },
                                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "الاشتراك") },
                                    label = { Text("الاشتراك", fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NightNavy,
                                        selectedTextColor = CalmGold,
                                        indicatorColor = CalmGold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_account")
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NightNavy)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                if (currentScreen == AppScreen.PLAYER) PaddingValues(0.dp)
                                else innerPadding
                            )
                    ) {
                        when (currentScreen) {
                            AppScreen.HOME -> {
                                HomeScreen(
                                    matches = matches,
                                    channels = channels,
                                    favorites = favorites,
                                    history = history,
                                    onSelectMatch = { openMatch(it) },
                                    onSelectChannel = { openChannel(it) },
                                    onToggleFavorite = { repository.toggleFavorite(it) }
                                )
                            }
                            AppScreen.MATCHES -> {
                                MatchesScreen(
                                    matches = matches,
                                    onSelectMatch = { openMatch(it) }
                                )
                            }
                            AppScreen.CHANNELS -> {
                                ChannelsScreen(
                                    channels = channels,
                                    categories = categories,
                                    favorites = favorites,
                                    onSelectChannel = { openChannel(it) },
                                    onToggleFavorite = { repository.toggleFavorite(it) }
                                )
                            }
                            AppScreen.ACCOUNT -> {
                                AccountScreen(
                                    subscription = subscription,
                                    onSaveSubscription = { type, s, u, p, m ->
                                        repository.saveSubscription(type, s, u, p, m)
                                    }
                                )
                            }
                            AppScreen.PLAYER -> {
                                PlayerScreen(
                                    playerEngine = playerEngine,
                                    onBack = {
                                        currentScreen = previousScreen
                                    },
                                    onToggleFavorite = { repository.toggleFavorite(it) }
                                )
                            }
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
