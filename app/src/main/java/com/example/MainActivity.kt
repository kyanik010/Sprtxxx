package com.example

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.data.model.Channel
import com.example.data.repository.IptvRepository
import com.example.player.MatchNightPlayerEngine
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.yahorzabotsin.openvpnclientgate.core.servers.Server
import com.yahorzabotsin.openvpnclientgate.core.servers.ServerRepository
import com.yahorzabotsin.openvpnclientgate.core.settings.ServerSource
import com.yahorzabotsin.openvpnclientgate.core.settings.UserSettingsStore
import com.yahorzabotsin.openvpnclientgate.vpn.ConnectionStateManager
import com.yahorzabotsin.openvpnclientgate.vpn.VpnManager
import org.koin.core.context.GlobalContext

enum class AppScreen {
    HOME,
    CHANNELS,
    CATEGORY_CHANNELS,
    ACCOUNT,
    PLAYER,
    SETUP
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: IptvRepository
    private lateinit var playerEngine: MatchNightPlayerEngine
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>
    private var pendingVpnServer: Server? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val selectedServer = pendingVpnServer
            pendingVpnServer = null
            if (result.resultCode == Activity.RESULT_OK && selectedServer != null) {
                connectVpnServer(selectedServer)
            } else if (selectedServer != null) {
                Toast.makeText(this, "تم إلغاء إذن اتصال VPN", Toast.LENGTH_SHORT).show()
            }
        }
        enableEdgeToEdge()

        // Keep screen on during media playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        repository = IptvRepository(applicationContext)
        playerEngine = MatchNightPlayerEngine(applicationContext)

        setContent {
            SonixTheme {
                val channels by repository.channels.collectAsState()
                val categories by repository.categories.collectAsState()
                val audioSources by repository.audioSources.collectAsState()
                val favorites by repository.favorites.collectAsState()
                val history by repository.history.collectAsState()
                val subscription by repository.currentSubscription.collectAsState()
                val playingChannel by playerEngine.currentChannel.collectAsState()
                val vpnStatus by ConnectionStateManager.state.collectAsState()

                // Keep player engine channels updated for previous/next channel actions
                LaunchedEffect(channels) {
                    playerEngine.setChannelsList(channels)
                }

                var currentScreen by remember { mutableStateOf(if (subscription.isActive) AppScreen.HOME else AppScreen.SETUP) }
                var showWelcome by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(4500)
                    showWelcome = false
                }
                var selectedCategoryId by rememberSaveable { mutableStateOf("") }
                var currentChannelId by rememberSaveable { mutableStateOf<String?>(null) }
                var categorySearchQuery by rememberSaveable { mutableStateOf("") }
                var channelSearchQuery by rememberSaveable { mutableStateOf("") }

                LaunchedEffect(categories) {
                    if (selectedCategoryId.isNotBlank() && categories.none { it.id == selectedCategoryId }) {
                        selectedCategoryId = ""
                    }
                }
                BackHandler(enabled = currentScreen == AppScreen.CATEGORY_CHANNELS) {
                    currentScreen = AppScreen.CHANNELS
                    channelSearchQuery = ""
                }
                LaunchedEffect(playingChannel?.id) {
                    playingChannel?.let { currentChannelId = it.id }
                }
                LaunchedEffect(subscription.isActive) {
                    if (subscription.isActive && currentScreen == AppScreen.SETUP) currentScreen = AppScreen.HOME
                }
                var previousScreen by remember { mutableStateOf(AppScreen.HOME) }

                fun openChannel(channel: Channel) {
                    currentChannelId = channel.id
                    repository.addToHistory(channel.id)
                    playerEngine.playChannel(channel)
                    previousScreen = currentScreen
                    currentScreen = AppScreen.PLAYER
                }


                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                Scaffold(
                    bottomBar = {
                        // Hide bottom bar when inside fullscreen video player or landscape video
                        if (!showWelcome && currentScreen != AppScreen.PLAYER && currentScreen != AppScreen.SETUP) {
                            NavigationBar(
                                containerColor = NightNavy,
                                contentColor = SnowWhite,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .height(if (isLandscape) 62.dp else 78.dp)
                                    .testTag("main_bottom_nav")
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.HOME,
                                    onClick = { currentScreen = AppScreen.HOME },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                                    label = if (isLandscape) null else ({ Text("الرئيسية", fontWeight = FontWeight.SemiBold) }),
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
                                    selected = currentScreen == AppScreen.CHANNELS || currentScreen == AppScreen.CATEGORY_CHANNELS,
                                    onClick = { currentScreen = AppScreen.CHANNELS },
                                    icon = { Icon(Icons.Default.Tv, contentDescription = "القنوات") },
                                    label = if (isLandscape) null else ({ Text("القنوات", fontWeight = FontWeight.SemiBold) }),
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
                                    label = if (isLandscape) null else ({ Text("الاشتراك", fontWeight = FontWeight.SemiBold) }),
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
                        if (showWelcome) {
                            SonixWelcomeScreen(modifier = Modifier.fillMaxSize())
                        } else when (currentScreen) {
                            AppScreen.SETUP -> {
                                SubscriptionSetupScreen(
                                    onLoadVpnServers = { loadBestVpnServers() },
                                    onConnectVpnServer = { requestVpnConnection(it) },
                                    onDisconnectVpn = {
                                        if (!VpnManager.stopVpn(this@MainActivity)) {
                                            Toast.makeText(this@MainActivity, "تعذر إرسال طلب إيقاف VPN", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    vpnStatus = vpnStatus,
                                    onConnect = { type, server, user, pass, m3u ->
                                        val result = repository.saveSubscription(type, server, user, pass, m3u)
                                        if (result.isSuccess) currentScreen = AppScreen.HOME
                                        result
                                    }
                                )
                            }
                            AppScreen.HOME -> {
                                HomeScreen(
                                    channels = channels,
                                    categories = categories,
                                    favorites = favorites,
                                    history = history,
                                    onSelectChannel = { openChannel(it) },
                                    onToggleFavorite = { repository.toggleFavorite(it) },
                                    onOpenChannels = { currentScreen = AppScreen.CHANNELS }
                                )
                            }
                            AppScreen.CHANNELS -> {
                                CategoryBrowserScreen(
                                    categories = categories,
                                    searchQuery = categorySearchQuery,
                                    onSearchQueryChanged = { categorySearchQuery = it },
                                    onCategorySelected = {
                                        selectedCategoryId = it
                                        channelSearchQuery = ""
                                        currentScreen = AppScreen.CATEGORY_CHANNELS
                                    }
                                )
                            }
                            AppScreen.CATEGORY_CHANNELS -> {
                                CategoryChannelsScreen(
                                    category = categories.firstOrNull { it.id == selectedCategoryId },
                                    channels = channels,
                                    favorites = favorites,
                                    currentChannelId = currentChannelId,
                                    searchQuery = channelSearchQuery,
                                    onSearchQueryChanged = { channelSearchQuery = it },
                                    onBack = {
                                        currentScreen = AppScreen.CHANNELS
                                        channelSearchQuery = ""
                                    },
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
                                    audioSources = audioSources,
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


    private suspend fun loadBestVpnServers(): List<Server> {
        val serverRepository = GlobalContext.get().get<ServerRepository>()
        val vpnGateSettings = UserSettingsStore.load(applicationContext).copy(serverSource = ServerSource.VPNGATE)
        val servers = serverRepository.getServers(
            context = applicationContext,
            forceRefresh = true,
            settingsOverride = vpnGateSettings
        )
        return servers.asSequence()
            .filter { it.configData.isNotBlank() && it.ip.isNotBlank() }
            .distinctBy { it.ip }
            .sortedWith(
                compareByDescending<Server> { it.score }
                    .thenBy { if (it.ping > 0) it.ping else Int.MAX_VALUE }
                    .thenByDescending { it.speed }
                    .thenBy { it.numVpnSessions }
            )
            .take(7)
            .toList()
    }

    private fun requestVpnConnection(server: Server) {
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent != null) {
            pendingVpnServer = server
            vpnPermissionLauncher.launch(permissionIntent)
        } else {
            connectVpnServer(server)
        }
    }

    private fun connectVpnServer(server: Server) {
        val dispatched = runCatching {
            VpnManager.startVpn(
                context = this,
                base64Config = server.configData,
                displayName = "${server.country.name} · ${server.name}",
                ip = server.ip
            )
        }.getOrDefault(false)

        Toast.makeText(
            this,
            if (dispatched) "تم إرسال طلب الاتصال بسيرفر VPN؛ جارٍ التحقق من الاتصال" else "تعذر بدء اتصال VPN",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerEngine.release()
    }
}
