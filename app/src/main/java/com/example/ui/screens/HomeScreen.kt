package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                .replace("AM", "ص").replace("PM", "م")
                .mapDigitsToArabic()
            delay(30_000)
        }
    }
    val searchResults = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else channels.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }.take(8)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF0B1118), Color(0xFF1A2430)))
        ).padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        // تم التعديل: تخطيط الشاشة الرئيسية الأفقي وشريط الأدوات العلوي.
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                HomeToolButton("إغلاق", Icons.Default.PowerSettingsNew, Color(0xFFB85D45), onExit)
                HomeToolButton("تحديث", Icons.Default.Refresh, SnowWhite, onRefresh)
                HomeToolButton("الملف الشخصي", Icons.Default.Person, SnowWhite, onOpenAccount)
                HomeToolButton("الإعدادات", Icons.Default.Settings, SnowWhite, onOpenAccount)
                HomeToolButton("بحث", Icons.Default.Search, SnowWhite) { searchOpen = !searchOpen }
            }
            Text(clock, color = SnowWhite, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("IPTV", color = Color(0xFFD6DEE8), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("LIVE", color = Color(0xFF8FB8E8), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("S T R E M", color = TextMuted, fontSize = 9.sp, letterSpacing = 4.sp)
            }
        }
        }

        // تم التعديل: لوحة زجاجية مركزية بثلاث بطاقات متساوية.
        Surface(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.80f).fillMaxHeight(0.58f),
            color = Color(0xB51C2A38),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            shadowElevation = 16.dp
        ) {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeSectionCard("أفلام", Icons.Default.Movie, Color(0xFFB87333), onClick = onOpenMovies, modifier = Modifier.weight(1f))
                    HomeSectionCard("مسلسلات", Icons.Default.LiveTv, Color(0xFFB87333), onClick = onOpenSeries, modifier = Modifier.weight(1f))
                    HomeSectionCard("قنوات", Icons.Default.Tv, Color(0xFF8FB8E8), onClick = onOpenChannels, modifier = Modifier.weight(1f))
                }
            }
        }

        // تم التعديل: بطاقة بيانات الاشتراك أسفل اليسار مع إبقاء الأرقام اللاتينية.
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).widthIn(min = 230.dp, max = 330.dp),
            color = Color(0xCC111820),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("اسم المستخدم", color = TextMuted, fontSize = 11.sp)
                Text(subscription.username.ifBlank { subscription.name }, color = SnowWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("رقم المشترك", color = TextMuted, fontSize = 11.sp)
                Text(subscription.deviceId.ifBlank { subscription.username.ifBlank { "—" } }, color = SnowWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("تاريخ انتهاء الصلاحية", color = TextMuted, fontSize = 11.sp)
                Text(subscription.expiryDate.ifBlank { "—" }.mapDigitsToArabic(), color = SnowWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (searchOpen) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp).fillMaxWidth(0.48f),
                color = Color(0xF0111820), shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("home_channel_search"),
                        singleLine = true, placeholder = { Text("البحث في القنوات", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SnowWhite, unfocusedTextColor = SnowWhite,
                            focusedBorderColor = Color(0xFFE0A526), unfocusedBorderColor = CardBorder,
                            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground
                        )
                    )
                    searchResults.forEach { channel ->
                        Text(
                            channel.name, color = SnowWhite, modifier = Modifier.fillMaxWidth()
                                .clickable { onSelectChannel(channel) }.padding(10.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
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
        modifier = Modifier.size(46.dp).scale(if (focused) 1.04f else 1f)
            .onFocusChanged { focused = it.isFocused }.focusable()
            .clickable(onClick = onClick).testTag("home_tool_$label"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xAA111820),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFF8FB8E8) else Color.White.copy(alpha = 0.14f))
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
        modifier = modifier.fillMaxHeight().scale(if (focused) 1.04f else 1f)
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
            .testTag("home_section_$title"),
        color = Color(0x66101820),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFF8FB8E8) else Color.White.copy(alpha = 0.15f))
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(
                modifier = Modifier.size(94.dp),
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(54.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = SnowWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
