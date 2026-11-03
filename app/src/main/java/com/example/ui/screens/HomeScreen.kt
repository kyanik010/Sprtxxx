package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
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
    val channelMap = remember(channels) { channels.associateBy { it.id } }
    val recent = remember(channels, history) { history.mapNotNull { channelMap[it] } }
    val favoriteChannels = remember(channels, favorites) { channels.filter { favorites.contains(it.id) } }
    val results = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else channels.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }.take(30)
    }
    val lastWatched = recent.firstOrNull()
    val featured = lastWatched ?: channels.firstOrNull()

    BoxWithConstraints(modifier.fillMaxSize().background(NightNavy)) {
        val wide = maxWidth >= 700.dp || maxWidth > maxHeight
        val horizontal = if (wide) 30.dp else 18.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = horizontal).widthIn(max = 1120.dp),
            contentPadding = PaddingValues(top = if (wide) 24.dp else 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item("sonix_header") {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("SONIX", fontSize = if (wide) 38.sp else 34.sp, lineHeight = 38.sp,
                            fontWeight = FontWeight.Black, color = CalmGold, letterSpacing = (-1.5).sp)
                        Text("T V   •   PREMIUM", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = CalmGoldVariant, letterSpacing = 2.4.sp)
                    }
                    Surface(shape = CircleShape, color = CardBackground,
                        border = BorderStroke(1.dp, CalmGold.copy(alpha = .65f))) {
                        Icon(Icons.Default.Tv, "Sonix TV", tint = CalmGold,
                            modifier = Modifier.padding(12.dp).size(25.dp))
                    }
                }
            }
            item("home_search") {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("home_channel_search"),
                    singleLine = true,
                    placeholder = { Text("ابحث عن قناة في اشتراكك", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, "بحث", tint = CalmGold) },
                    trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, "مسح", tint = TextMuted)
                    } },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SnowWhite, unfocusedTextColor = SnowWhite,
                        focusedBorderColor = CalmGold, unfocusedBorderColor = CardBorder,
                        cursorColor = CalmGold, focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    )
                )
            }
            if (searchQuery.isNotBlank()) {
                item("search_results_title") { SectionHeader("نتائج البحث", "${results.size}") }
                if (results.isEmpty()) item("search_empty") { HomeEmptyMessage("لا توجد قناة بهذا الاسم") }
                else items(results, key = { "search_${it.id}" }) { channel ->
                    ChannelItemCard(channel, { onSelectChannel(channel) },
                        { onToggleFavorite(channel.id) }, Modifier.padding(vertical = 3.dp))
                }
            } else {
                item("sonix_hero") {
                    SonixHeroBanner(
                        wide = wide,
                        onClick = { featured?.let(onSelectChannel) },
                        channel = featured,
                        channelCount = channels.size
                    )
                }
                if (featured != null) {
                    item("featured_live") {
                        SectionHeader("البث المباشر من اشتراكك")
                        LiveChannelCard(featured, onClick = { onSelectChannel(featured) })
                    }
                } else {
                    item("no_channels") { HomeEmptyMessage("أضف اشتراك IPTV لعرض قنواتك هنا") }
                }
                if (lastWatched != null) {
                    item("continue_title") { SectionHeader("متابعة المشاهدة") }
                    item("continue_card") { ContinueWatchingCard(lastWatched) { onSelectChannel(lastWatched) } }
                }
                if (categories.isNotEmpty()) {
                    item("categories_title") { SectionHeader("تصنيفات اشتراكك", "${categories.size}") }
                    item("categories_row") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(categories, key = { it.id }) { category ->
                                CategoryShortcut(category, onOpenChannels)
                            }
                        }
                    }
                }
                if (favoriteChannels.isNotEmpty()) {
                    item("favorites_title") { SectionHeader("قنواتك المفضلة", "${favoriteChannels.size}") }
                    item("favorites_row") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(favoriteChannels, key = { it.id }) { channel ->
                                QuickChannelCard(channel) { onSelectChannel(channel) }
                            }
                        }
                    }
                }
                if (recent.size > 1) {
                    item("recent_title") { SectionHeader("آخر المشاهدات", "${recent.size}") }
                    items(recent.drop(1).take(6), key = { "recent_${it.id}" }) { channel ->
                        ChannelItemCard(channel, { onSelectChannel(channel) },
                            { onToggleFavorite(channel.id) }, Modifier.padding(vertical = 3.dp))
                    }
                }
                item("all_channels") {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onOpenChannels, modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CalmGold, contentColor = NightNavy)
                    ) {
                        Icon(Icons.Default.Tv, null)
                        Spacer(Modifier.width(9.dp))
                        Text("استعراض جميع القنوات", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SonixHeroBanner(wide: Boolean, onClick: () -> Unit, channel: Channel?, channelCount: Int) {
    val height = if (wide) 250.dp else 210.dp
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp).height(height)
            .clickable(enabled = channel != null, onClick = onClick).testTag("home_live_hero"),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CalmGold.copy(alpha = .45f))
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.img_match_night_stadium),
                contentDescription = "Sonix TV sports stadium",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xEE050505), Color(0x99050505), Color(0x22050505)))
            ))
            Column(
                Modifier.align(Alignment.CenterStart).fillMaxWidth(.82f).padding(if (wide) 26.dp else 19.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = CalmGold, shape = RoundedCornerShape(7.dp)) {
                    Text("SONIX TV  •  PREMIUM", color = NightNavy, fontSize = 10.sp,
                        fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
                Text("ترفيهك… بصورة مختلفة", color = SnowWhite,
                    fontSize = if (wide) 29.sp else 24.sp, fontWeight = FontWeight.Black)
                Text(if (channelCount > 0) "كل قنوات اشتراكك في مكان واحد" else "أضف اشتراكك وابدأ المشاهدة",
                    color = SnowWhite.copy(alpha = .85f), fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Default.PlayCircleFilled, null, tint = CalmGold, modifier = Modifier.size(21.dp))
                    Text(if (channelCount > 0) "${channelCount} قناة متاحة" else "تجربة مشاهدة مميزة",
                        color = CalmGoldVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LiveChannelCard(channel: Channel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("home_featured_channel"),
        shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(channel.logoUrl.takeIf { it.isNotBlank() }, channel.name,
                Modifier.size(68.dp).clip(RoundedCornerShape(13.dp)).background(DarkSportsBlue).padding(7.dp),
                contentScale = ContentScale.Fit)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(LiveRed))
                    Text("متاحة للمشاهدة", color = CalmGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(channel.name, color = SnowWhite, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(channel.currentProgram.ifBlank { "اضغط للمشاهدة الآن" }, color = TextMuted,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(shape = CircleShape, color = CalmGold) {
                Icon(Icons.Default.PlayArrow, "شاهد الآن", tint = NightNavy, modifier = Modifier.padding(10.dp).size(23.dp))
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(channel: Channel, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("home_continue_watching"),
        shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CalmGold.copy(alpha = .45f))) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(channel.logoUrl.takeIf { it.isNotBlank() }, channel.name,
                Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(DarkSportsBlue).padding(6.dp),
                contentScale = ContentScale.Fit)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("آخر قناة شاهدتها", color = CalmGold, style = MaterialTheme.typography.labelSmall)
                Text(channel.name, color = SnowWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("القناة رقم ${channel.channelNumber}", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Default.PlayCircleFilled, null, tint = CalmGold, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun CategoryShortcut(category: Category, onClick: () -> Unit) {
    Surface(Modifier.width(150.dp).height(105.dp).clickable(onClick = onClick).testTag("home_category_${category.id}"),
        shape = RoundedCornerShape(15.dp), color = CardBackground, border = BorderStroke(1.dp, CardBorder)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            AsyncImage(category.logoUrl.takeIf { it.isNotBlank() }, category.name,
                Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(DarkSportsBlue).padding(3.dp),
                contentScale = ContentScale.Fit)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(category.name, color = SnowWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${category.channelCount} قناة", color = CalmGold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HomeEmptyMessage(message: String) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 10.dp), color = CardBackground,
        shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, CardBorder)) {
        Text(message, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun QuickChannelCard(channel: Channel, onClick: () -> Unit) {
    Surface(Modifier.width(138.dp).clickable(onClick = onClick).testTag("quick_channel_${channel.id}"),
        shape = RoundedCornerShape(15.dp), color = CardBackground, border = BorderStroke(1.dp, CardBorder)) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(channel.logoUrl.takeIf { it.isNotBlank() }, channel.name,
                Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(DarkSportsBlue).padding(5.dp),
                contentScale = ContentScale.Fit)
            Spacer(Modifier.height(7.dp))
            Text(channel.name, color = SnowWhite, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium)
        }
    }
}
