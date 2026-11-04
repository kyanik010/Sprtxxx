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

private val SonixGold = Color(0xFFE0A526)
private val LiveBackground = Color(0xFF0A0F14)
private val LivePanel = Color(0xFF0E151E)
private val LiveInput = Color(0xFF090E14)
private val LiveSelected = Color(0xFF20232A)
private val LiveBorder = Color(0xFF303943)

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
        Column(
            modifier.fillMaxSize().background(LiveBackground).padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(0.09f).clip(RoundedCornerShape(10.dp))
                    .background(LivePanel).padding(horizontal = 18.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("مباشر", color = SnowWhite, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                OutlinedTextField(
                    value = channelQuery,
                    onValueChange = { channelQuery = it },
                    modifier = Modifier.weight(0.40f).fillMaxHeight().testTag("live_top_search"),
                    singleLine = true,
                    placeholder = { Text("البحث في القنوات...", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = SonixGold, modifier = Modifier.size(19.dp)) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SnowWhite, unfocusedTextColor = SnowWhite,
                        focusedBorderColor = SonixGold, unfocusedBorderColor = LiveBorder,
                        focusedContainerColor = LiveInput, unfocusedContainerColor = LiveInput
                    )
                )
                Column(Modifier.weight(0.32f), horizontalAlignment = Alignment.Start) {
                    Text("أنت تشاهد...", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "${selectedCategory?.name ?: "مباشر"}  >>  ${currentChannel?.name ?: "لم يتم اختيار قناة"}",
                        color = SnowWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().weight(0.91f),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.04f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp)).background(LivePanel)
                        .padding(vertical = 9.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Text("SX", color = SonixGold, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    IconRailItem(Icons.Default.Tv, "مباشر", true) {}
                    IconRailItem(Icons.Default.Movie, "أفلام", false, onOpenMovies)
                    IconRailItem(Icons.Default.Home, "مسلسلات", false, onOpenSeries)
                    IconRailItem(Icons.Default.History, "السجل", false, onOpenPlayer)
                    Spacer(Modifier.weight(1f))
                    IconRailItem(Icons.Default.Settings, "الإعدادات", false, onOpenAccount)
                    IconRailItem(Icons.Default.Home, "الرئيسية", false, onBackHome)
                }

                Column(
                    modifier = Modifier.weight(0.22f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp)).background(LivePanel)
                        .padding(horizontal = 10.dp, vertical = 11.dp)
                ) {
                    Text("التصنيفات", color = SnowWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = categoryQuery, onValueChange = { categoryQuery = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("category_search"),
                        singleLine = true,
                        placeholder = { Text("البحث في التصنيفات", fontSize = 11.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = SonixGold, modifier = Modifier.size(17.dp)) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SnowWhite, unfocusedTextColor = SnowWhite,
                            focusedBorderColor = SonixGold, unfocusedBorderColor = LiveBorder,
                            focusedContainerColor = LiveInput, unfocusedContainerColor = LiveInput
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize().testTag("live_category_list")
                    ) {
                        items(filteredCategories, key = { it.id }) { category ->
                            val active = category.id == selectedCategoryId
                            FocusRow(
                                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("live_category_${category.id}"),
                                selected = active,
                                onClick = { onCategorySelected(category.id) }
                            ) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        category.name, color = if (active) SnowWhite else Color(0xFFB8C0C9),
                                        fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                    )
                                    Text(category.channelCount.toString(), color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.28f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp)).background(Color(0xFF0E151E))
                        .padding(horizontal = 10.dp, vertical = 11.dp)
                ) {
                    Text(
                        "### ${selectedCategory?.name ?: "القنوات"} ###",
                        color = SonixGold, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).testTag("channel_category_banner")
                    )
                    Spacer(Modifier.height(5.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxSize().testTag("live_channel_list")
                    ) {
                        items(visibleChannels, key = { it.id }) { channel ->
                            val active = currentChannel?.id == channel.id
                            FocusRow(
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("live_channel_${channel.id}"),
                                selected = active,
                                onClick = { onSelectChannel(channel) }
                            ) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        channel.channelNumber.toString(),
                                        color = if (active) SonixGold else TextMuted,
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(29.dp)
                                    )
                                    AsyncImage(
                                        model = channel.logoUrl.takeIf { it.isNotBlank() },
                                        contentDescription = null,
                                        modifier = Modifier.size(31.dp).clip(RoundedCornerShape(6.dp)).background(LiveInput)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        channel.name, color = if (active) SnowWhite else Color(0xFFD1D6DC),
                                        fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                    )
                                    if (favorites.contains(channel.id)) {
                                        Icon(Icons.Default.Favorite, null, tint = SonixGold, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.46f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp)).background(LiveBackground)
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(13.dp)).background(Color.Black)
                                .clickable(onClick = onOpenPlayer).testTag("live_video_preview"),
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
                                Text("اختر قناة لبدء المشاهدة", color = Color(0xFFB8C0C9), fontSize = 14.sp)
                            } else if (isBuffering) {
                                CircularProgressIndicator(color = SonixGold, modifier = Modifier.size(30.dp))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    currentChannel?.name ?: "لم يتم اختيار قناة", color = SnowWhite,
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "أنت تشاهد...  ${selectedCategory?.name ?: "مباشر"}  >>  ${currentChannel?.name ?: "—"}",
                                    color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { currentChannel?.let { onToggleFavorite(it.id) } },
                                enabled = currentChannel != null
                            ) {
                                Icon(
                                    Icons.Default.Favorite, contentDescription = "المفضلة",
                                    tint = if (currentChannel?.let { favorites.contains(it.id) } == true) SonixGold else TextMuted
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onOpenPlayer, modifier = Modifier.weight(1f).height(43.dp),
                            border = BorderStroke(1.dp, LiveBorder), shape = RoundedCornerShape(10.dp)
                        ) { Text("epg", color = SnowWhite, fontSize = 12.sp) }
                        OutlinedButton(
                            onClick = { currentChannel?.let { onToggleFavorite(it.id) } },
                            enabled = currentChannel != null,
                            modifier = Modifier.weight(1f).height(43.dp),
                            border = BorderStroke(1.dp, LiveBorder), shape = RoundedCornerShape(10.dp)
                        ) { Text("إضافة إلى المفضلة", color = SnowWhite, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconRailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.size(42.dp).scale(if (focused) 1.05f else 1f)
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
            .testTag("rail_$label"),
        color = if (selected) SonixGold.copy(alpha = 0.12f) else Color(0x331C2A38),
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(if (focused || selected) 1.5.dp else 1.dp, if (focused || selected) SonixGold else Color.Transparent)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = if (selected || focused) SonixGold else TextMuted, modifier = Modifier.size(21.dp))
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
        modifier = modifier.scale(if (focused) 1.025f else 1f)
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick),
        color = if (selected) LiveSelected else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            if (focused) 2.dp else if (selected) 1.dp else 0.dp,
            if (focused || selected) SonixGold.copy(alpha = if (focused) 1f else 0.7f) else Color.Transparent
        )
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
        Row(
            Modifier.fillMaxSize().background(LiveBackground).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                Modifier.width(68.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                    .background(LivePanel).padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("SX", color = SonixGold, fontSize = 17.sp, fontWeight = FontWeight.Black)
                RailItem(Icons.Default.Tv, "مباشر", false, onOpenChannels)
                RailItem(Icons.Default.Movie, "أفلام", section == "أفلام", onOpenMovies)
                RailItem(Icons.Default.Home, "مسلسلات", section == "مسلسلات", onOpenSeries)
                Spacer(Modifier.weight(1f))
                RailItem(Icons.Default.Settings, "الإعدادات", false, onOpenAccount)
                RailItem(Icons.Default.Home, "الرئيسية", false, onBackHome)
            }
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(LivePanel),
                contentAlignment = Alignment.Center
            ) {
                Text(section, color = SnowWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
        color = if (selected) LiveSelected else Color.Transparent,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected || focused) 1.5.dp else 0.dp, if (selected || focused) SonixGold else Color.Transparent)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = if (selected || focused) SonixGold else TextMuted, modifier = Modifier.size(19.dp))
            Text(label, color = TextMuted, fontSize = 8.sp, maxLines = 1)
        }
    }
}
