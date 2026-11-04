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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.VodEpisode
import com.example.data.model.VodMovie
import com.example.data.model.VodSeries
import com.example.ui.theme.*

@Composable
fun VODBrowserScreen(
    section: String,
    movies: List<VodMovie>,
    series: List<VodSeries>,
    episodes: List<VodEpisode>,
    selectedSeriesTitle: String,
    loadingEpisodes: Boolean,
    onBack: () -> Unit,
    onSelectMovie: (VodMovie) -> Unit,
    onSelectSeries: (VodSeries) -> Unit,
    onPlayEpisode: (VodEpisode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovies = section == "أفلام"
    val showingEpisodes = selectedSeriesTitle.isNotBlank()
    val count = if (isMovies) movies.size else if (showingEpisodes) episodes.size else series.size

    Column(modifier.fillMaxSize().background(NightNavy).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = CalmGold)
            }
            Icon(if (isMovies) Icons.Default.Movie else Icons.Default.Tv, null,
                tint = CalmGold, modifier = Modifier.size(27.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (showingEpisodes) selectedSeriesTitle else section,
                    color = SnowWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count عنصر", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }

        when {
            loadingEpisodes -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CalmGold)
            }
            count == 0 -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (isMovies) Icons.Default.Movie else Icons.Default.Tv, null,
                        tint = CalmGold.copy(alpha = 0.65f), modifier = Modifier.size(54.dp))
                    Text(
                        if (isMovies) "لا توجد أفلام في بيانات اشتراكك"
                        else if (showingEpisodes) "لم يعثر السيرفر على حلقات لهذا المسلسل"
                        else "لا توجد مسلسلات في بيانات اشتراكك",
                        color = SnowWhite, fontWeight = FontWeight.Bold)
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
                    items(movies, key = { "movie_\${it.id}" }) { movie ->
                        VodPosterCard(movie.name, movie.posterUrl, movie.year, movie.rating) { onSelectMovie(movie) }
                    }
                } else if (showingEpisodes) {
                    items(episodes, key = { "episode_\${it.id}" }) { episode ->
                        VodPosterCard(episode.name, episode.posterUrl, "الموسم \${episode.season} • الحلقة \${episode.episodeNumber}", "") {
                            onPlayEpisode(episode)
                        }
                    }
                } else {
                    items(series, key = { "series_\${it.id}" }) { item ->
                        VodPosterCard(item.name, item.posterUrl, item.year, item.rating) { onSelectSeries(item) }
                    }
                }
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
