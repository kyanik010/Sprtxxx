package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.ui.components.ChannelItemCard
import com.example.ui.theme.*

@Composable
fun ChannelsScreen(
    channels: List<Channel>,
    categories: List<Category>,
    favorites: Set<String>,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("all") }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val filteredChannels = remember(channels, searchQuery, selectedCategoryId, showOnlyFavorites, favorites) {
        channels.filter { channel ->
            val matchesCategory = when {
                showOnlyFavorites -> favorites.contains(channel.id)
                selectedCategoryId == "all" -> true
                else -> channel.categoryId.equals(selectedCategoryId, ignoreCase = true)
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                channel.name.contains(searchQuery, ignoreCase = true) ||
                channel.channelNumber.toString().contains(searchQuery.trim()) ||
                channel.currentProgram.contains(searchQuery, ignoreCase = true)
            }

            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NightNavy)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = CalmGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "دليل القنوات الرياضية",
                style = MaterialTheme.typography.headlineMedium,
                color = SnowWhite,
                fontWeight = FontWeight.Bold
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث باسم القناة، رقمها (1-99)، أو البرنامج...", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "بحث", tint = CalmGold)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح البحث", tint = TextMuted)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSportsBlue,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = CalmGold,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = SnowWhite,
                unfocusedTextColor = SnowWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("channel_search_input")
        )

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            // All channels
            item {
                FilterChip(
                    selected = !showOnlyFavorites && selectedCategoryId == "all",
                    onClick = { showOnlyFavorites = false; selectedCategoryId = "all" },
                    label = { Text("كل القنوات") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CalmGold, selectedLabelColor = NightNavy,
                        containerColor = DarkSportsBlue, labelColor = SnowWhite
                    ),
                    modifier = Modifier.testTag("filter_all_channels")
                )
            }

            // Favorites Quick Chip
            item {
                FilterChip(
                    selected = showOnlyFavorites,
                    onClick = {
                        showOnlyFavorites = !showOnlyFavorites
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (showOnlyFavorites) NightNavy else CalmGold,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("المفضلة (${favorites.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CalmGold,
                        selectedLabelColor = NightNavy,
                        containerColor = DarkSportsBlue,
                        labelColor = SnowWhite
                    ),
                    modifier = Modifier.testTag("filter_favorites")
                )
            }

            // Categories
            items(categories) { category ->
                val isSelected = !showOnlyFavorites && selectedCategoryId == category.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        showOnlyFavorites = false
                        selectedCategoryId = category.id
                    },
                    label = { Text("${category.name} (${category.channelCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CalmGold,
                        selectedLabelColor = NightNavy,
                        containerColor = DarkSportsBlue,
                        labelColor = SnowWhite
                    ),
                    modifier = Modifier.testTag("filter_cat_${category.id}")
                )
            }
        }

        // Channels List
        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لم يتم العثور على قنوات تطابق البحث",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "channel_results_summary") {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (showOnlyFavorites) "قنواتك المفضلة" else "القنوات المتاحة",
                            color = SnowWhite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(color = DarkSportsBlue, shape = RoundedCornerShape(50)) {
                            Text("${filteredChannels.size} قناة", color = CalmGold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
                items(filteredChannels) { channel ->
                    ChannelItemCard(
                        channel = channel,
                        onClick = { onSelectChannel(channel) },
                        onToggleFavorite = { onToggleFavorite(channel.id) }
                    )
                }
            }
        }
    }
}
