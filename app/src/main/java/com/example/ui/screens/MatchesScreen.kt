package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Match
import com.example.data.model.MatchStatus
import com.example.ui.components.MatchCard
import com.example.ui.theme.*

@Composable
fun MatchesScreen(
    matches: List<Match>,
    onSelectMatch: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredMatches = remember(matches, selectedFilter) {
        when (selectedFilter) {
            "LIVE" -> matches.filter { it.status == MatchStatus.LIVE }
            "UPCOMING" -> matches.filter { it.status == MatchStatus.UPCOMING }
            else -> matches
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NightNavy)
            .padding(horizontal = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = CalmGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "جدول المباريات والبطولات",
                style = MaterialTheme.typography.headlineMedium,
                color = SnowWhite,
                fontWeight = FontWeight.Bold
            )
        }

        // Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("جميع المباريات (${matches.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CalmGold,
                        selectedLabelColor = NightNavy,
                        containerColor = DarkSportsBlue,
                        labelColor = SnowWhite
                    ),
                    modifier = Modifier.testTag("filter_all_matches")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "LIVE",
                    onClick = { selectedFilter = "LIVE" },
                    label = { Text("مباشر الآن (${matches.count { it.status == MatchStatus.LIVE }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LiveRed,
                        selectedLabelColor = SnowWhite,
                        containerColor = DarkSportsBlue,
                        labelColor = SnowWhite
                    ),
                    modifier = Modifier.testTag("filter_live_matches")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "UPCOMING",
                    onClick = { selectedFilter = "UPCOMING" },
                    label = { Text("المباريات القادمة (${matches.count { it.status == MatchStatus.UPCOMING }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CalmGold,
                        selectedLabelColor = NightNavy,
                        containerColor = DarkSportsBlue,
                        labelColor = SnowWhite
                    ),
                    modifier = Modifier.testTag("filter_upcoming_matches")
                )
            }
        }

        // Matches List
        if (filteredMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد مباريات في هذا التصنيف حالياً",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMatches) { match ->
                    MatchCard(
                        match = match,
                        onClick = { onSelectMatch(match) }
                    )
                }
            }
        }
    }
}
