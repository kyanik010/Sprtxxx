package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Channel
import com.example.data.model.Match
import com.example.data.model.MatchStatus
import com.example.ui.components.ChannelItemCard
import com.example.ui.components.LivePulseBadge
import com.example.ui.components.MatchCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    matches: List<Match>,
    channels: List<Channel>,
    favorites: Set<String>,
    history: List<String>,
    onSelectMatch: (Match) -> Unit,
    onSelectChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val featuredMatch = matches.firstOrNull { it.status == MatchStatus.LIVE } ?: matches.firstOrNull()
    val todayMatches = matches.take(4)
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
                LivePulseBadge()
            }
        }

        // Featured Match Hero Banner
        if (featuredMatch != null) {
            item {
                FeaturedMatchBanner(
                    match = featuredMatch,
                    onClick = { onSelectMatch(featuredMatch) }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Today's Live & Upcoming Matches
        item {
            SectionHeader(
                title = "مباريات اليوم المباشرة",
                badgeText = "${matches.count { it.status == MatchStatus.LIVE }} مباشر"
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                todayMatches.forEach { match ->
                    MatchCard(
                        match = match,
                        onClick = { onSelectMatch(match) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Live Channels Carousel
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
fun FeaturedMatchBanner(
    match: Match,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CalmGold.copy(alpha = 0.8f)),
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .testTag("featured_match_banner")
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Stadium Image Background
            Image(
                painter = painterResource(id = R.drawable.img_match_night_stadium),
                contentDescription = "ملعب المباراة",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Luxury Dark Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NightNavy.copy(alpha = 0.6f),
                                NightNavy.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = DarkSportsBlue.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, CalmGold)
                    ) {
                        Text(
                            text = match.competition,
                            style = MaterialTheme.typography.labelSmall,
                            color = CalmGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (match.status == MatchStatus.LIVE) {
                        LivePulseBadge()
                    }
                }

                // Teams Clash Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Home
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(DarkSportsBlue)
                                .border(1.5.dp, CalmGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = match.homeTeam.take(2),
                                style = MaterialTheme.typography.titleLarge,
                                color = SnowWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = match.homeTeam,
                            style = MaterialTheme.typography.titleMedium,
                            color = SnowWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Score or VS
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (match.status == MatchStatus.LIVE && match.score != null) {
                            Text(
                                text = match.score,
                                style = MaterialTheme.typography.displayLarge,
                                color = SnowWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (match.minute != null) {
                                Text(
                                    text = match.minute,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LiveRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "ضد",
                                style = MaterialTheme.typography.titleLarge,
                                color = CalmGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = match.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    // Away
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(DarkSportsBlue)
                                .border(1.5.dp, CalmGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = match.awayTeam.take(2),
                                style = MaterialTheme.typography.titleLarge,
                                color = SnowWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = match.awayTeam,
                            style = MaterialTheme.typography.titleMedium,
                            color = SnowWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom Action Button
                Surface(
                    color = CalmGold,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("featured_match_watch_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "مشاهدة المباراة البارزة",
                            tint = NightNavy
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مشاهدة البث المباشر الآن — ${match.primaryChannelName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NightNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
