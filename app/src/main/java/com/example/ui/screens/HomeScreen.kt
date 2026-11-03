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
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.ui.components.ChannelItemCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    channels: List<Channel>,
    favorites: Set<String>,
    history: List<String>,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteChannels = channels.filter { favorites.contains(it.id) }
    val historyChannels = channels.filter { history.contains(it.id) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NightNavy)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // App Header Brand
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ماتش نايت",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SnowWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "CLASSIC SPORTS LUXURY — المنصة الرياضية",
                        style = MaterialTheme.typography.labelSmall,
                        color = CalmGold
                    )
                }
                Text("IPTV", color = CalmGold, fontWeight = FontWeight.Bold)
            }
        }

        if (channels.isNotEmpty()) {
        item {
            SectionHeader(
                title = "أبرز القنوات الرياضية",
                badgeText = "${channels.size} قناة"
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(channels.take(8)) { channel ->
                    QuickChannelCard(
                        channel = channel,
                        onClick = { onSelectChannel(channel) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        }

        // Favorites Section
        if (favoriteChannels.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "القنوات المفضلة",
                    badgeText = "${favoriteChannels.size}"
                )
            }
            items(favoriteChannels) { channel ->
                ChannelItemCard(
                    channel = channel,
                    onClick = { onSelectChannel(channel) },
                    onToggleFavorite = { onToggleFavorite(channel.id) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // History Section
        if (historyChannels.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "آخر المشاهدات",
                    badgeText = "${historyChannels.size}"
                )
            }
            items(historyChannels) { channel ->
                ChannelItemCard(
                    channel = channel,
                    onClick = { onSelectChannel(channel) },
                    onToggleFavorite = { onToggleFavorite(channel.id) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun QuickChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .width(140.dp)
            .testTag("quick_channel_${channel.id}")
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(DarkSportsBlue)
                    .border(1.dp, CalmGold.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${channel.channelNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CalmGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelMedium,
                color = SnowWhite,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (channel.resolution.isNotBlank()) {
                Surface(
                    color = SportsGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = channel.resolution,
                        color = SportsGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
