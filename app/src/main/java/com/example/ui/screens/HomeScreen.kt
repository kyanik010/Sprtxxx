package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.ui.components.ChannelItemCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    channels: List<Channel>,
    categories: List<Category>,
    favorites: Set<String>,
    history: List<String>,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenChannels: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val channelsById = remember(channels) { channels.associateBy { it.id } }
    val favoriteChannels = remember(channels, favorites) {
        channels.filter { favorites.contains(it.id) }
    }
    val historyChannels = remember(channels, history) {
        history.mapNotNull { channelsById[it] }
    }
    val searchResults = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else channels.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }.take(12)
    }
    val lastWatched = historyChannels.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NightNavy)
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "ماتش نايت",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SnowWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "تجربتك لمشاهدة قنواتك",
                        style = MaterialTheme.typography.labelMedium,
                        color = CalmGold
                    )
                }
                Surface(
                    color = DarkSportsBlue,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CalmGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = CalmGold, modifier = Modifier.size(17.dp))
                        Text("IPTV", color = SnowWhite, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item(key = "search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("home_channel_search"),
                singleLine = true,
                placeholder = { Text("ابحث عن قناة في اشتراكك", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = CalmGold) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        TextButton(onClick = { searchQuery = "" }) { Text("مسح", color = CalmGold) }
                    }
                },
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SnowWhite,
                    unfocusedTextColor = SnowWhite,
                    focusedBorderColor = CalmGold,
                    unfocusedBorderColor = CardBorder,
                    cursorColor = CalmGold,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )
            Spacer(Modifier.height(12.dp))
        }

        if (searchQuery.isNotBlank()) {
            item(key = "search_heading") {
                SectionHeader(title = "نتائج البحث", badgeText = "${searchResults.size}")
            }
            if (searchResults.isEmpty()) {
                item(key = "search_empty") { HomeEmptyMessage("لا توجد قناة بهذا الاسم") }
            } else {
                items(searchResults, key = { "search_${it.id}" }) { channel ->
                    ChannelItemCard(
                        channel = channel,
                        onClick = { onSelectChannel(channel) },
                        onToggleFavorite = { onToggleFavorite(channel.id) },
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        } else {
            if (lastWatched != null) {
                item(key = "continue_watching") {
                    SectionHeader(title = "متابعة المشاهدة")
                    ContinueWatchingCard(channel = lastWatched, onClick = { onSelectChannel(lastWatched) })
                }
            }

            if (categories.isNotEmpty()) {
                item(key = "categories_header") {
                    SectionHeader(title = "تصنيفات اشتراكك", badgeText = "${categories.size}")
                }
                item(key = "categories") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(categories, key = { it.id }) { category ->
                            CategoryHomeCard(category = category, onClick = onOpenChannels)
                        }
                    }
                }
            }

            if (favoriteChannels.isNotEmpty()) {
                item(key = "favorites_header") {
                    SectionHeader(title = "قنواتك المفضلة", badgeText = "${favoriteChannels.size}")
                }
                item(key = "favorites") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(favoriteChannels, key = { it.id }) { channel ->
                            QuickChannelCard(channel = channel, onClick = { onSelectChannel(channel) })
                        }
                    }
                }
            }

            if (historyChannels.isNotEmpty()) {
                item(key = "history_header") {
                    SectionHeader(title = "آخر المشاهدات", badgeText = "${historyChannels.size}")
                }
                items(historyChannels.take(8), key = { "history_${it.id}" }) { channel ->
                    ChannelItemCard(
                        channel = channel,
                        onClick = { onSelectChannel(channel) },
                        onToggleFavorite = { onToggleFavorite(channel.id) },
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }

            if (channels.isEmpty()) {
                item(key = "empty_subscription") {
                    HomeEmptyMessage("أضف اشتراك IPTV لعرض قنواتك وتصنيفاتك هنا")
                }
            } else if (favoriteChannels.isEmpty() && historyChannels.isEmpty() && categories.isEmpty()) {
                item(key = "getting_started") {
                    HomeEmptyMessage("ابدأ باختيار قناة من صفحة القنوات، وستظهر مشاهداتك ومفضلاتك هنا")
                }
            }

            item(key = "all_channels_action") {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenChannels,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CalmGold.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmGold)
                ) {
                    Icon(Icons.Default.GridView, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("استعراض جميع القنوات", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(channel: Channel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("home_continue_watching").clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CalmGold.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(DarkSportsBlue, CardBackground))
            ).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = channel.logoUrl.takeIf { it.isNotBlank() },
                contentDescription = channel.name,
                modifier = Modifier.size(62.dp).clip(RoundedCornerShape(12.dp)).background(DarkSportsBlue),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("آخر قناة شاهدتها", color = CalmGold, style = MaterialTheme.typography.labelSmall)
                Text(channel.name, color = SnowWhite, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(channel.categoryId, color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Surface(shape = CircleShape, color = CalmGold) {
                Icon(Icons.Default.PlayArrow, contentDescription = "متابعة المشاهدة", tint = NightNavy, modifier = Modifier.padding(9.dp).size(22.dp))
            }
        }
    }
}

@Composable
private fun CategoryHomeCard(category: Category, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(148.dp).height(94.dp).clickable(onClick = onClick).testTag("home_category_${category.id}"),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.GridView, contentDescription = null, tint = CalmGold, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(category.name, color = SnowWhite, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${category.channelCount} قناة", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HomeEmptyMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        color = CardBackground,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Text(message, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
    }
}

@Composable
fun QuickChannelCard(channel: Channel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = modifier.width(140.dp).testTag("quick_channel_${channel.id}").clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = channel.logoUrl.takeIf { it.isNotBlank() },
                contentDescription = channel.name,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(DarkSportsBlue),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
            Text(channel.name, style = MaterialTheme.typography.labelMedium, color = SnowWhite, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (channel.resolution.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(channel.resolution, color = CalmGold, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
        }
    }
}
