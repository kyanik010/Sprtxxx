package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.ui.theme.*

@Composable
fun CategoryBrowserScreen(
    categories: List<Category>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories else categories.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true)
        }
    }
    Column(modifier.fillMaxSize().background(NightNavy).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tv, null, tint = CalmGold, modifier = Modifier.size(27.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("تصنيفات القنوات", color = SnowWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${filtered.size} تصنيف", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
        BrowserSearchField(searchQuery, onSearchQueryChanged, "ابحث عن تصنيف...")
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            EmptyBrowserMessage("لا توجد تصنيفات مطابقة")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = Modifier.fillMaxSize().testTag("category_grid"),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                gridItems(filtered, key = { it.id }) { category ->
                    CategoryCard(category = category, onClick = { onCategorySelected(category.id) })
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().heightIn(min = 142.dp)
            .testTag("category_${category.id}").clickable(onClick = onClick).focusable()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            var logoFailed by remember(category.logoUrl) { mutableStateOf(category.logoUrl.isBlank()) }
            if (!logoFailed) {
                AsyncImage(
                    model = category.logoUrl,
                    contentDescription = "شعار ${category.name}",
                    modifier = Modifier.size(62.dp).clip(RoundedCornerShape(12.dp)),
                    onError = { logoFailed = true }
                )
            }
            if (logoFailed) {
                Surface(
                    color = DarkSportsBlue,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tv, null, tint = CalmGold, modifier = Modifier.size(31.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                category.name,
                color = SnowWhite,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 3
            )
            Spacer(Modifier.height(5.dp))
            Surface(color = CalmGold.copy(alpha = 0.13f), shape = RoundedCornerShape(20.dp)) {
                Text(
                    "${category.channelCount} قناة",
                    color = CalmGold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryChannelsScreen(
    category: Category?,
    channels: List<Channel>,
    favorites: Set<String>,
    currentChannelId: String?,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleChannels = remember(channels, category?.id, searchQuery) {
        channels.filter { channel ->
            (category == null || channel.categoryId == category.id) &&
                (searchQuery.isBlank() || channel.name.contains(searchQuery.trim(), true) ||
                    channel.channelNumber.toString().contains(searchQuery.trim()))
        }
    }
    Column(modifier.fillMaxSize().background(NightNavy).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_to_categories")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "العودة إلى التصنيفات", tint = CalmGold)
            }
            if (category?.logoUrl?.isNotBlank() == true) {
                AsyncImage(category.logoUrl, contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)))
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(category?.name ?: "قنوات التصنيف", color = SnowWhite,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2)
                Text("${visibleChannels.size} قناة", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
        BrowserSearchField(searchQuery, onSearchQueryChanged, "ابحث داخل هذا التصنيف...")
        Spacer(Modifier.height(12.dp))
        if (visibleChannels.isEmpty()) {
            EmptyBrowserMessage("لا توجد قنوات في هذا التصنيف")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("category_channel_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(visibleChannels, key = { it.id }) { channel ->
                    ChannelBrowserRow(
                        channel = channel,
                        isCurrent = currentChannelId == channel.id,
                        isFavorite = favorites.contains(channel.id) || channel.isFavorite,
                        onClick = { onSelectChannel(channel) },
                        onToggleFavorite = { onToggleFavorite(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserSearchField(value: String, onValueChange: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().testTag("channel_search_input"),
        placeholder = { Text(hint, color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = CalmGold) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "مسح البحث", tint = TextMuted)
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSportsBlue,
            unfocusedContainerColor = CardBackground,
            focusedBorderColor = CalmGold,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = SnowWhite,
            unfocusedTextColor = SnowWhite
        )
    )
}

@Composable
private fun ChannelBrowserRow(
    channel: Channel,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var logoFailed by remember(channel.logoUrl) { mutableStateOf(channel.logoUrl.isBlank()) }
    Surface(
        color = if (isCurrent) CalmGold.copy(alpha = 0.15f) else CardBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) CalmGold else CardBorder),
        modifier = Modifier.fillMaxWidth().testTag("channel_item_${channel.id}")
            .clickable(onClick = onClick).focusable()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(DarkSportsBlue),
                contentAlignment = Alignment.Center
            ) {
                if (!logoFailed) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = "شعار ${channel.name}",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                        onError = { logoFailed = true }
                    )
                } else {
                    Text(channel.name.trim().firstOrNull()?.toString() ?: "؟", color = CalmGold,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(channel.name, color = if (isCurrent) CalmGold else SnowWhite,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                if (isCurrent) Text("القناة الحالية", color = CalmGold, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(channel.channelNumber.toString(), color = CalmGold, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Star, contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                        tint = if (isFavorite) CalmGold else TextMuted)
                }
            }
        }
    }
}

@Composable
private fun EmptyBrowserMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}
