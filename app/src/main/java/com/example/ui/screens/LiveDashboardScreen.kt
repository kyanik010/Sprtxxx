package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.player.MatchNightPlayerEngine
import com.example.ui.theme.*

@Composable
fun LiveDashboardScreen(
    categories: List<Category>,
    channels: List<Channel>,
    favorites: Set<String>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenAccount: () -> Unit,
    onBackHome: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    playerEngine: MatchNightPlayerEngine,
    modifier: Modifier = Modifier
) {
    var categoryQuery by remember { mutableStateOf("") }
    var channelQuery by remember { mutableStateOf("") }
    val currentChannel by playerEngine.currentChannel.collectAsState()
    val isBuffering by playerEngine.isBuffering.collectAsState()
    val filteredCategories = remember(categories, categoryQuery) {
        categories.filter { it.name.contains(categoryQuery.trim(), true) }
    }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val visibleChannels = remember(channels, selectedCategoryId, channelQuery) {
        channels.filter { channel ->
            (selectedCategoryId.isBlank() || channel.categoryId == selectedCategoryId) &&
                (channelQuery.isBlank() || channel.name.contains(channelQuery.trim(), true) ||
                    channel.channelNumber.toString().contains(channelQuery.trim()))
        }
    }
    LaunchedEffect(categories) {
        if (selectedCategoryId.isBlank() && categories.isNotEmpty()) onCategorySelected(categories.first().id)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier.fillMaxSize().background(Color(0xFF0A0F14)).padding(7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // تم التعديل: شريط تنقل جانبي نحيف وثابت في أقصى اليمين.
            Column(
                modifier = Modifier.weight(0.04f).fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF111820))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IPTV", color = Color(0xFFD6DEE8), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Text("LIVE", color = Color(0xFF8FB8E8), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                RailItem(Icons.Default.Tv, "مباشر", true) {}
                RailItem(Icons.Default.Movie, "أفلام", false, onOpenMovies)
                RailItem(Icons.Default.Home, "مسلسلات", false, onOpenSeries)
                RailItem(Icons.Default.History, "السجل", false, onOpenPlayer)
                Spacer(Modifier.weight(1f))
                RailItem(Icons.Default.Settings, "الإعدادات", false, onOpenAccount)
            }

            // تم التعديل: عمود التصنيفات بنسبة تقارب 22%.
            Column(
                modifier = Modifier.weight(0.22f).fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF111820))
                    .padding(10.dp)
            ) {
                Text("التصنيفات", color = SnowWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = categoryQuery, onValueChange = { categoryQuery = it },
                    modifier = Modifier.fillMaxWidth().height(51.dp).testTag("category_search"),
                    singleLine = true, placeholder = { Text("البحث في الفئات", fontSize = 11.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFFE0A526), modifier = Modifier.size(17.dp)) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SnowWhite, unfocusedTextColor = SnowWhite,
                        focusedBorderColor = Color(0xFF8FB8E8), unfocusedBorderColor = Color(0xFF263442),
                        focusedContainerColor = Color(0xFF0D141B), unfocusedContainerColor = Color(0xFF0D141B)
                    )
                )
                Spacer(Modifier.height(7.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredCategories, key = { it.id }) { category ->
                        val active = category.id == selectedCategoryId
                        FocusRow(
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("live_category_${category.id}"),
                            selected = active,
                            onClick = { onCategorySelected(category.id) }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(category.channelCount.toString(), color = TextMuted, fontSize = 10.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(category.name, color = if (active) SnowWhite else Color(0xFFB8C2CC), fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // تم التعديل: عمود القنوات بنسبة تقارب 28% مع أرقام وشعارات القنوات.
            Column(
                modifier = Modifier.weight(0.28f).fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF111820))
                    .padding(10.dp)
            ) {
                Text(selectedCategory?.name ?: "القنوات", color = SnowWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = channelQuery, onValueChange = { channelQuery = it },
                    modifier = Modifier.fillMaxWidth().height(51.dp).testTag("channel_search"),
                    singleLine = true, placeholder = { Text("البحث في القنوات", fontSize = 11.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFFE0A526), modifier = Modifier.size(17.dp)) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SnowWhite, unfocusedTextColor = SnowWhite,
                        focusedBorderColor = Color(0xFF8FB8E8), unfocusedBorderColor = Color(0xFF263442),
                        focusedContainerColor = Color(0xFF0D141B), unfocusedContainerColor = Color(0xFF0D141B)
                    )
                )
                Spacer(Modifier.height(7.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxSize()) {
                    items(visibleChannels, key = { it.id }) { channel ->
                        val active = currentChannel?.id == channel.id
                        FocusRow(
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("live_channel_${channel.id}"),
                            selected = active,
                            onClick = { onSelectChannel(channel) }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(channel.channelNumber.toString(), color = Color(0xFFB8C2CC), fontSize = 10.sp, modifier = Modifier.width(27.dp))
                                AsyncImage(
                                    model = channel.logoUrl.takeIf { it.isNotBlank() },
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF0A0F14))
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(channel.name, color = if (active) SnowWhite else Color(0xFFB8C2CC),
                                    fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (favorites.contains(channel.id)) {
                                    Icon(Icons.Default.Favorite, null, tint = Color(0xFFE0A526), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // تم التعديل: مساحة المشغل بنسبة تقارب 46% مع البحث ومعلومات القناة.
            Column(
                modifier = Modifier.weight(0.46f).fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF111820)).padding(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("مباشر", color = SnowWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = channelQuery, onValueChange = { channelQuery = it },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("live_top_search"),
                        singleLine = true, placeholder = { Text("البحث في القنوات", fontSize = 11.sp, color = TextMuted) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SnowWhite, unfocusedTextColor = SnowWhite,
                            focusedBorderColor = Color(0xFF8FB8E8), unfocusedBorderColor = Color(0xFF263442),
                            focusedContainerColor = Color(0xFF0D141B), unfocusedContainerColor = Color(0xFF0D141B)
                        )
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(Color.Black).clickable(onClick = onOpenPlayer).testTag("live_video_preview"),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                useController = false
                                player = playerEngine.primaryPlayer
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { it.player = playerEngine.primaryPlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (currentChannel == null) {
                        Text("اختر قناة لبدء المشاهدة", color = Color(0xFFB8C2CC), fontSize = 14.sp)
                    } else if (isBuffering) {
                        CircularProgressIndicator(color = Color(0xFFE0A526), modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(currentChannel?.name ?: "لم يتم اختيار قناة", color = SnowWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "أنت تشاهد...  ${selectedCategory?.name ?: "مباشر"}  >>  ${currentChannel?.name ?: "—"}",
                            color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { currentChannel?.let { onToggleFavorite(it.id) } }, enabled = currentChannel != null) {
                        Icon(Icons.Default.Favorite, contentDescription = "المفضلة",
                            tint = if (currentChannel?.let { favorites.contains(it.id) } == true) Color(0xFFE0A526) else TextMuted)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenPlayer, modifier = Modifier.weight(1f).height(43.dp),
                        border = BorderStroke(1.dp, Color(0xFF657789)), shape = RoundedCornerShape(10.dp)) {
                        Text("epg", color = SnowWhite, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { currentChannel?.let { onToggleFavorite(it.id) } },
                        enabled = currentChannel != null,
                        modifier = Modifier.weight(1f).height(43.dp),
                        border = BorderStroke(1.dp, Color(0xFF657789)), shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إضافة إلى المفضلة", color = SnowWhite, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp).scale(if (focused) 1.04f else 1f)
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick),
        color = if (selected) Color(0xFF1C2A38) else Color.Transparent,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 1.5.dp else if (focused) 2.dp else 0.dp,
            if (selected) Color(0xFFE0A526) else Color(0xFF8FB8E8))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = if (selected) Color(0xFFE0A526) else TextMuted, modifier = Modifier.size(19.dp))
            Text(label, color = TextMuted, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun FocusRow(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.scale(if (focused) 1.04f else 1f)
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick),
        color = if (selected) Color(0xFF1C2A38) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else if (selected) 1.dp else 0.dp,
            if (focused) Color(0xFF8FB8E8) else Color.White.copy(alpha = 0.14f))
    ) { content() }
}


@Composable
fun PlaceholderSectionScreen(
    section: String,
    onBackHome: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenAccount: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(Modifier.fillMaxSize().background(Color(0xFF0A0F14)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.width(68.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111820)).padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("IPTV", color = SnowWhite, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text("LIVE", color = Color(0xFF8FB8E8), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                RailItem(Icons.Default.Tv, "مباشر", false, onOpenChannels)
                RailItem(Icons.Default.Movie, "أفلام", section == "أفلام", onOpenMovies)
                RailItem(Icons.Default.Home, "مسلسلات", section == "مسلسلات", onOpenSeries)
                Spacer(Modifier.weight(1f))
                RailItem(Icons.Default.Settings, "الإعدادات", false, onOpenAccount)
                RailItem(Icons.Default.Home, "الرئيسية", false, onBackHome)
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111820)), contentAlignment = Alignment.Center) {
                Text(section, color = SnowWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
