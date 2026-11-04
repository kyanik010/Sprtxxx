package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.VodCategory
import com.example.data.model.VodEpisode
import com.example.data.model.VodMovie
import com.example.data.model.VodSeries
import com.example.ui.theme.*

@Composable
fun VODBrowserScreen(
    section: String,
    movies: List<VodMovie>,
    series: List<VodSeries>,
    movieCategories: List<VodCategory>,
    seriesCategories: List<VodCategory>,
    selectedCategoryId: String,
    episodes: List<VodEpisode>,
    selectedSeriesTitle: String,
    loadingEpisodes: Boolean,
    onBack: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectMovie: (VodMovie) -> Unit,
    onSelectSeries: (VodSeries) -> Unit,
    onPlayEpisode: (VodEpisode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovies = section == "أفلام"
    val showingEpisodes = selectedSeriesTitle.isNotBlank()
    val categories = if (isMovies) movieCategories else seriesCategories
    val showingCategories = !showingEpisodes && selectedCategoryId.isBlank() && categories.isNotEmpty()
    val visibleMovies = remember(movies, selectedCategoryId) {
        if (selectedCategoryId.isBlank()) movies else movies.filter { it.categoryId == selectedCategoryId }
    }
    val visibleSeries = remember(series, selectedCategoryId) {
        if (selectedCategoryId.isBlank()) series else series.filter { it.categoryId == selectedCategoryId }
    }
    val count = when {
        showingCategories -> categories.size
        isMovies -> visibleMovies.size
        showingEpisodes -> episodes.size
        else -> visibleSeries.size
    }

    Column(modifier.fillMaxSize().background(NightNavy).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!showingCategories) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = CalmGold)
                }
            }
            Icon(if (isMovies) Icons.Default.Movie else Icons.Default.Tv, null,
                tint = CalmGold, modifier = Modifier.size(27.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
                Text(
                    when {
                        showingEpisodes -> selectedSeriesTitle
                        showingCategories -> "تصنيفات $section"
                        selectedCategory != null -> selectedCategory.name
                        else -> section
                    },
                    color = SnowWhite, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        showingCategories -> "$count تصنيف"
                        showingEpisodes -> "$count حلقة"
                        isMovies -> "$count فيلم"
                        else -> "$count مسلسل"
                    },
                    color = TextMuted, style = MaterialTheme.typography.labelSmall
                )
            }
        }

        when {
            loadingEpisodes -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CalmGold)
            }
            showingCategories -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    VodCategoryCard(
                        category = category,
                        isMovies = isMovies,
                        onClick = { onSelectCategory(category.id) }
                    )
                }
            }
            count == 0 -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (isMovies) Icons.Default.Movie else Icons.Default.Tv, null,
                        tint = CalmGold.copy(alpha = 0.65f), modifier = Modifier.size(54.dp))
                    Text(
                        if (isMovies) "لا توجد أفلام في بيانات اشتراكك"
                        else if (showingEpisodes) "لم يعثر السيرفر على حلقات لهذا المسلسل"
                        else "لا توجد مسلسلات في بيانات اشتراكك",
                        color = SnowWhite, fontWeight = FontWeight.Bold
                    )
                    Text("تأكد أن اشتراك Xtream Codes يتضمن مكتبة VOD وأن مزود الخدمة فعّلها.",
                        color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 132.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isMovies) {
                    items(visibleMovies, key = { "movie_${it.id}" }) { movie ->
                        VodPosterCard(movie.name, movie.posterUrl, movie.year, movie.rating) { onSelectMovie(movie) }
                    }
                } else if (showingEpisodes) {
                    items(episodes, key = { "episode_${it.id}" }) { episode ->
                        VodPosterCard(episode.name, episode.posterUrl,
                            "الموسم ${episode.season} • الحلقة ${episode.episodeNumber}", "") {
                            onPlayEpisode(episode)
                        }
                    }
                } else {
                    items(visibleSeries, key = { "series_${it.id}" }) { item ->
                        VodPosterCard(item.name, item.posterUrl, item.year, item.rating) { onSelectSeries(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VodCategoryCard(
    category: VodCategory,
    isMovies: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().heightIn(min = 142.dp)
            .clickable(onClick = onClick).focusable()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (category.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = category.posterUrl,
                    contentDescription = "صورة ${category.name}",
                    modifier = Modifier.size(62.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(color = DarkSportsBlue, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(62.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isMovies) Icons.Default.Movie else Icons.Default.Tv, null,
                            tint = CalmGold, modifier = Modifier.size(31.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(category.name, color = SnowWhite, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Surface(color = CalmGold.copy(alpha = 0.13f), shape = RoundedCornerShape(20.dp)) {
                Text(
                    if (isMovies) "${category.itemCount} فيلم" else "${category.itemCount} مسلسل",
                    color = CalmGold, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun VodPosterCard(
    title: String,
    poster: String,
    subtitle: String,
    rating: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(232.dp).clickable(onClick = onClick).focusable(),
        color = CardBackground,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().weight(1f).background(DarkSportsBlue), contentAlignment = Alignment.Center) {
                if (poster.isNotBlank()) {
                    AsyncImage(poster, contentDescription = title, modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Movie, null, tint = CalmGold, modifier = Modifier.size(42.dp))
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp),
                    color = Color(0xCC080D13), shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = CalmGold, modifier = Modifier.padding(4.dp).size(19.dp))
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp)) {
                Text(title, color = SnowWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank() || rating.isNotBlank()) {
                    Text(listOf(subtitle, rating.takeIf { it.isNotBlank() }?.let { "★ $it" })
                        .filterNotNull().joinToString("   "), color = TextMuted, fontSize = 9.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
