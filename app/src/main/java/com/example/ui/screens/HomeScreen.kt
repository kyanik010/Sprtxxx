package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.SubscriptionProfile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val SonixGold = Color(0xFFE0A526)
private val HomeBackground = Color(0xFF0B1118)
private val HomePanel = Color(0xFF111820)
private val HomeCard = Color(0xFF1C2A38)

@Composable
fun HomeScreen(
    channels: List<Channel>,
    subscription: SubscriptionProfile,
    onOpenChannels: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenAccount: () -> Unit,
    onExit: () -> Unit,
    onRefresh: () -> Unit,
    onSelectChannel: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var clock by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            clock = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
                .replace("AM", "ص").replace("PM", "م").mapDigitsToArabic()
            delay(30_000)
        }
    }

    val searchResults = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else channels.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }.take(8)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(HomeBackground, Color(0xFF17212B)))
        ).padding(horizontal = 26.dp, vertical = 18.dp)
    ) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    HomeToolButton("إيقاف", Icons.Default.PowerSettingsNew, Color(0xFFD65B4B), onExit)
                    HomeToolButton("تحديث", Icons.Default.Refresh, SnowWhite, onRefresh)
                    HomeToolButton("الملف الشخصي", Icons.Default.Person, SnowWhite, onOpenAccount)
                    HomeToolButton("الإعدادات", Icons.Default.Settings, SnowWhite, onOpenAccount)
                    HomeToolButton("بحث", Icons.Default.Search, SnowWhite) { searchOpen = !searchOpen }
                }
                Text(clock, color = Color(0xFFE8EDF3), fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Column(horizontalAlignment = Alignment.End) {
                    Text("SONIX TV", color = SnowWhite, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("PREMIUM IPTV", color = SonixGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.82f).fillMaxHeight(0.54f),
            color = Color(0xB5111820),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            shadowElevation = 18.dp
        ) {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeSectionCard("أفلام", Icons.Default.Movie, SonixGold, onOpenMovies, Modifier.weight(1f))
                    HomeSectionCard("مسلسلات", Icons.Default.LiveTv, SonixGold, onOpenSeries, Modifier.weight(1f))
                    HomeSectionCard("قنوات", Icons.Default.Tv, SonixGold, onOpenChannels, Modifier.weight(1f))
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).widthIn(min = 300.dp, max = 520.dp),
            color = Color(0xCC111820),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            shadowElevation = 8.dp
        ) {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeSubscriptionValue("اسم المستخدم", subscription.username.ifBlank { subscription.name })
                    HomeInfoDivider()
                    HomeSubscriptionValue("رقم المشترك", subscription.deviceId.ifBlank { subscription.username.ifBlank { "—" } })
                    HomeInfoDivider()
                    HomeSubscriptionValue("انتهاء الصلاحية", subscription.expiryDate.ifBlank { "—" }.mapDigitsToArabic())
                }
            }
        }

        if (searchOpen) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 62.dp).fillMaxWidth(0.48f),
                color = Color(0xF0111820),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF3A4652)),
                shadowElevation = 12.dp
            ) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("home_channel_search"),
                        singleLine = true,
                        placeholder = { Text("البحث في القنوات", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SnowWhite,
                            unfocusedTextColor = SnowWhite,
                            focusedBorderColor = SonixGold,
                            unfocusedBorderColor = Color(0xFF34404C),
                            focusedContainerColor = HomePanel,
                            unfocusedContainerColor = HomePanel
                        )
                    )
                    searchResults.forEach { channel ->
                        Text(
                            channel.name,
                            color = SnowWhite,
                            modifier = Modifier.fillMaxWidth().clickable { onSelectChannel(channel) }.padding(10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSubscriptionValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TextMuted, fontSize = 10.sp)
        Text(value, color = SnowWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeInfoDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.14f)))
}

private fun String.mapDigitsToArabic(): String = map { c ->
    if (c in '0'..'9') ('٠'.code + (c - '0')).toChar() else c
}.joinToString("")

@Composable
private fun HomeToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.size(46.dp).scale(if (focused) 1.05f else 1f)
            .onFocusChanged { focused = it.isFocused }.focusable()
            .clickable(onClick = onClick).testTag("home_tool_$label"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xAA111820),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) SonixGold else Color.White.copy(alpha = 0.12f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun HomeSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxHeight().padding(horizontal = 7.dp)
            .scale(if (focused) 1.035f else 1f)
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
            .testTag("home_section_$title"),
        color = Color(0x99101820),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) SonixGold else Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(15.dp))
            Text(title, color = SnowWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
    }
}
