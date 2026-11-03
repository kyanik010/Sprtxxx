package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.SubscriptionType
import com.yahorzabotsin.openvpnclientgate.core.servers.Server
import com.yahorzabotsin.openvpnclientgate.vpn.ConnectionState
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SubscriptionSetupScreen(
    onConnect: suspend (SubscriptionType, String, String, String, String) -> Result<Int>,
    onLoadVpnServers: suspend () -> List<Server>,
    onConnectVpnServer: (Server) -> Unit,
    onDisconnectVpn: () -> Unit,
    vpnStatus: ConnectionState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(SubscriptionType.XTREAM_CODES) }
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var playlist by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showVpnDialog by remember { mutableStateOf(false) }
    var vpnLoading by remember { mutableStateOf(false) }
    var vpnError by remember { mutableStateOf<String?>(null) }
    var vpnServers by remember { mutableStateOf<List<Server>>(emptyList()) }

    Box(modifier = modifier.fillMaxSize().background(NightNavy)) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(shape = RoundedCornerShape(18.dp), color = CalmGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.55f))) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = CalmGold,
                        modifier = Modifier.padding(15.dp).size(38.dp))
                }
                Text("مرحباً بك في SONIX TV", style = MaterialTheme.typography.headlineMedium, color = SnowWhite, fontWeight = FontWeight.ExtraBold)
                Text(
                    "أضف اشتراك IPTV الخاص بك للبدء",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedType == SubscriptionType.XTREAM_CODES,
                        onClick = { selectedType = SubscriptionType.XTREAM_CODES; error = null },
                        label = { Text("Xtream Codes") },
                        leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmGold,
                            selectedLabelColor = NightNavy,
                            containerColor = DarkSportsBlue,
                            labelColor = SnowWhite
                        )
                    )
                    FilterChip(
                        selected = selectedType == SubscriptionType.M3U_PLAYLIST,
                        onClick = { selectedType = SubscriptionType.M3U_PLAYLIST; error = null },
                        label = { Text("رابط M3U") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmGold,
                            selectedLabelColor = NightNavy,
                            containerColor = DarkSportsBlue,
                            labelColor = SnowWhite
                        )
                    )
                }
                if (selectedType == SubscriptionType.XTREAM_CODES) {
                    OutlinedTextField(
                        value = server, onValueChange = { server = it },
                        label = { Text("عنوان السيرفر") },
                        placeholder = { Text("http://server:port") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("اسم المستخدم") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("كلمة المرور") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = playlist, onValueChange = { playlist = it },
                        label = { Text("رابط قائمة M3U / M3U8") },
                        placeholder = { Text("https://...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error != null) {
                    Text(error!!, color = LiveRed, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            val result = onConnect(selectedType, server, username, password, playlist)
                            busy = false
                            if (result.isSuccess) {
                                // The host screen switches to the player interface after a successful import.
                            } else {
                                error = result.exceptionOrNull()?.localizedMessage ?: "تعذر الاتصال أو تحميل قائمة القنوات"
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CalmGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (busy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NightNavy, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("جارٍ الاتصال وتحميل القنوات...", color = NightNavy, fontWeight = FontWeight.Bold)
                        }
                    } else Text("دخول", color = NightNavy, fontWeight = FontWeight.Bold)
                }
                Text(
                    "لن تظهر قنوات تجريبية. سيعرض التطبيق القنوات التي يوفرها اشتراكك فقط.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }


    Surface(
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.65f))
    ) {
        IconButton(
            onClick = {
                showVpnDialog = true
                vpnLoading = true
                vpnError = null
                scope.launch {
                    try {
                        vpnServers = onLoadVpnServers()
                        if (vpnServers.isEmpty()) vpnError = "لم يتم العثور على سيرفرات VPN متاحة حالياً"
                    } catch (e: Exception) {
                        vpnError = e.localizedMessage ?: "تعذر تحديث قائمة سيرفرات VPN"
                    } finally {
                        vpnLoading = false
                    }
                }
            },
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = if (vpnStatus == ConnectionState.CONNECTED) "VPN متصل" else "اتصال VPN",
                tint = if (vpnStatus == ConnectionState.CONNECTED) Color(0xFF4CAF50) else CalmGold,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showVpnDialog) {
        AlertDialog(
            onDismissRequest = { showVpnDialog = false },
            containerColor = CardBackground,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("أفضل 7 سيرفرات VPN", color = SnowWhite, fontWeight = FontWeight.Bold)
                    Text(
                        text = when (vpnStatus) {
                            ConnectionState.CONNECTED -> "الحالة: متصل"
                            ConnectionState.CONNECTING -> "الحالة: جارٍ الاتصال"
                            ConnectionState.DISCONNECTING -> "الحالة: جارٍ قطع الاتصال"
                            ConnectionState.PAUSED, ConnectionState.PAUSING -> "الحالة: متوقف مؤقتاً"
                            ConnectionState.DISCONNECTED -> "الحالة: غير متصل"
                        },
                        color = if (vpnStatus == ConnectionState.CONNECTED) Color(0xFF4CAF50) else TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "تنبيه: هذه خوادم تطوعية مجانية؛ تجنب إرسال معلومات حساسة عبرها.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (vpnLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = CalmGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("جارٍ تحديث السيرفرات وفحص الأفضلية...", color = SnowWhite)
                        }
                    }
                    vpnError?.let { Text(it, color = LiveRed, style = MaterialTheme.typography.bodySmall) }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vpnServers, key = { "${it.ip}-${it.id}" }) { server ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showVpnDialog = false
                                    onConnectVpnServer(server)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSportsBlue,
                                border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.22f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "${server.country.name} · ${server.name}",
                                            color = SnowWhite,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val speedMbps = server.speed.coerceAtLeast(0L) / 1_000_000.0
                                        Text(
                                            text = "Ping: ${if (server.ping > 0) "${server.ping} ms" else "غير معروف"} · ${"%.1f".format(java.util.Locale.US, speedMbps)} Mbps · Score: ${server.score}",
                                            color = TextMuted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = CalmGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVpnDialog = false }) { Text("إغلاق", color = CalmGold) }
            },
            dismissButton = {
                if (vpnStatus != ConnectionState.DISCONNECTED) {
                    TextButton(onClick = {
                        showVpnDialog = false
                        onDisconnectVpn()
                    }) { Text("إيقاف VPN", color = LiveRed) }
                }
            }
        )
    }

    }
}
