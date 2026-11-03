package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun ChannelsScreen(
    channels: List<Channel>,
    categories: List<Category>,
    favorites: Set<String>,
    selectedCategoryId: String,
    currentChannelId: String?,
    searchQuery: String,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories else categories.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.id.contains(searchQuery.trim(), ignoreCase = true)
        }
    }
    val visibleChannels = remember(channels, selectedCategoryId, searchQuery) {
        channels.filter { channel ->
            channel.categoryId.equals(selectedCategoryId, ignoreCase = true) &&
                (searchQuery.isBlank() || channel.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    channel.channelNumber.toString().contains(searchQuery.trim()))
        }
    }

    Column(modifier = modifier.fillMaxSize().background(NightNavy).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = CalmGold, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(8.dp))
            Text("تصفح القنوات", color = SnowWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = searchQuery, onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp).testTag("channel_search_input"),
            placeholder = { Text("ابحث عن تصنيف أو قناة...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = CalmGold) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchQueryChanged("") }) {
                Icon(Icons.Default.Close, contentDescription = "مسح البحث", tint = TextMuted)
            } },
            singleLine = true, shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSportsBlue, unfocusedContainerColor = CardBackground,
                focusedBorderColor = CalmGold, unfocusedBorderColor = CardBorder,
                focusedTextColor = SnowWhite, unfocusedTextColor = SnowWhite
            )
        )

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier.weight(0.36f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                    .background(CardBackground).padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Text("التصنيفات", color = CalmGold, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp))
                if (visibleCategories.isEmpty()) EmptyBrowserMessage("لا توجد تصنيفات مطابقة")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(visibleCategories, key = { it.id }) { category ->
                        val selected = selectedCategoryId == category.id
                        Surface(
                            color = if (selected) CalmGold.copy(alpha = 0.20f) else DarkSportsBlue,
                            shape = RoundedCornerShape(9.dp),
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) CalmGold else CardBorder),
                            modifier = Modifier.fillMaxWidth().testTag("category_" + category.id)
                                .clickable { onCategorySelected(category.id) }
                        ) {
                            Column(Modifier.padding(horizontal = 8.dp, vertical = 9.dp)) {
                                Text(category.name, color = if (selected) CalmGold else SnowWhite,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(3.dp))
                                Text("ID: " + category.id, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.64f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                    .background(CardBackground).padding(horizontal = 7.dp, vertical = 8.dp)
            ) {
                val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedCategory?.name ?: "القنوات", color = CalmGold,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(visibleChannels.size.toString(), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                }
                if (visibleChannels.isEmpty()) EmptyBrowserMessage(
                    if (selectedCategory == null) "اختر تصنيفاً لعرض قنواته" else "لا توجد قنوات في هذا التصنيف"
                ) else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(visibleChannels, key = { it.id }) { channel ->
                        ChannelBrowserRow(
                            channel = channel, isCurrent = currentChannelId == channel.id,
                            isFavorite = favorites.contains(channel.id) || channel.isFavorite,
                            onClick = { onSelectChannel(channel) },
                            onToggleFavorite = { onToggleFavorite(channel.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelBrowserRow(
    channel: Channel, isCurrent: Boolean, isFavorite: Boolean,
    onClick: () -> Unit, onToggleFavorite: () -> Unit
) {
    var logoFailed by remember(channel.logoUrl) { mutableStateOf(channel.logoUrl.isBlank()) }
    Surface(
        color = if (isCurrent) CalmGold.copy(alpha = 0.16f) else DarkSportsBlue,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (isCurrent) 1.5.dp else 1.dp, if (isCurrent) CalmGold else CardBorder),
        modifier = Modifier.fillMaxWidth().testTag("channel_item_" + channel.id).clickable(onClick = onClick)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(CardBorder), contentAlignment = Alignment.Center) {
                if (!logoFailed) AsyncImage(
                    model = channel.logoUrl, contentDescription = "شعار " + channel.name,
                    modifier = Modifier.fillMaxSize().clip(CircleShape), onError = { logoFailed = true }
                ) else Text(channel.name.trim().firstOrNull()?.toString() ?: "؟", color = CalmGold,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(channel.name, color = if (isCurrent) CalmGold else SnowWhite,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                if (isCurrent) Text("القناة الحالية", color = CalmGold, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(channel.channelNumber.toString(), color = CalmGold, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Star, contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                        tint = if (isFavorite) CalmGold else TextMuted, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyBrowserMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
        Text(message, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}
