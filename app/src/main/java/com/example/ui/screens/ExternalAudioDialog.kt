package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AudioCommentarySource
import com.example.data.model.AudioSourceCategory
import com.example.data.model.Channel
import com.example.data.model.ExternalAudioState
import com.example.ui.theme.*

@Composable
fun ExternalAudioDialog(
    currentVideoChannel: Channel,
    externalAudioState: ExternalAudioState,
    audioSources: List<AudioCommentarySource>,
    onSelectAudioSource: (AudioCommentarySource) -> Unit,
    onAdjustSync: (Long) -> Unit,
    onResetSync: () -> Unit,
    onRevertToOriginalAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStep by remember { mutableStateOf(0) }

    LaunchedEffect(externalAudioState.isExternalAudioActive) {
        selectedStep = if (externalAudioState.isExternalAudioActive) 1 else 0
    }

    // Sources are prepared and cached by the repository as subscription data is loaded.
    val filteredSources = remember(audioSources, searchQuery, currentVideoChannel.id) {
        audioSources.filter { source ->
            source.channelId != currentVideoChannel.id &&
                (searchQuery.isBlank() ||
                    source.channelName.contains(searchQuery, ignoreCase = true) ||
                    source.commentatorName.contains(searchQuery, ignoreCase = true))
        }
    }
    val beinSources = filteredSources.filter { it.category == AudioSourceCategory.BEIN_SPORTS }.sortedBy { it.channelName.lowercase() }
    val alwanSources = filteredSources.filter { it.category == AudioSourceCategory.ALWAN }.sortedBy { it.channelName.lowercase() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(if (isLandscape) 0.78f else 0.92f)
                .fillMaxHeight(0.60f)
                .heightIn(max = if (isLandscape) 560.dp else maxHeight * 0.60f)
                .widthIn(max = if (isLandscape) 980.dp else 560.dp)
                .testTag("external_audio_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = NightNavy,
            border = BorderStroke(1.5.dp, CalmGold.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isLandscape) 10.dp else 12.dp)
            ) {
                // Dialog Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CalmGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = CalmGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الصوت الخارجي المنفصل",
                                style = MaterialTheme.typography.titleMedium,
                                color = SnowWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "شاهد الصورة من قناة واستمع لتعليق قناة ثانية",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = CalmGold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_audio_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Active Channels Status Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSportsBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Video source row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = SportsGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مصدر الصورة الحالي: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            Text(
                                text = currentVideoChannel.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = SnowWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = CardBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        // Audio source row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = if (externalAudioState.isExternalAudioActive) CalmGold else SnowWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مصدر الصوت: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            if (externalAudioState.isExternalAudioActive && externalAudioState.activeSource != null) {
                                Text(
                                    text = externalAudioState.activeSource.channelName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = CalmGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = CalmGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "صوت بديل نشط",
                                        color = CalmGold,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "صوت القناة الأصلي (${currentVideoChannel.name})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SnowWhite,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (externalAudioState.isExternalAudioActive) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onRevertToOriginalAudio,
                                modifier = Modifier.fillMaxWidth().height(38.dp).testTag("stop_external_audio_btn"),
                                border = BorderStroke(1.dp, LiveRed),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.VolumeOff, contentDescription = null, tint = LiveRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إيقاف الصوت الخارجي والعودة للصوت الأصلي", color = LiveRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Error or Buffering status
                        if (externalAudioState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = externalAudioState.errorMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = LiveRed
                            )
                        } else if (externalAudioState.isBuffering && externalAudioState.isExternalAudioActive) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = CalmGold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "جارٍ الاتصال بمصدر الصوت الخارجي...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CalmGold
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStep == 0,
                        onClick = { selectedStep = 0 },
                        label = { Text("1. اختيار المعلق") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmGold, selectedLabelColor = NightNavy,
                            containerColor = DarkSportsBlue, labelColor = SnowWhite
                        )
                    )
                    FilterChip(
                        selected = selectedStep == 1,
                        onClick = { if (externalAudioState.isExternalAudioActive) selectedStep = 1 },
                        enabled = externalAudioState.isExternalAudioActive,
                        label = { Text("2. ضبط المزامنة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CalmGold, selectedLabelColor = NightNavy,
                            containerColor = DarkSportsBlue, labelColor = SnowWhite,
                            disabledLabelColor = TextMuted
                        )
                    )
                }

                // Audio Synchronization Section (When External Audio Active)
                if (externalAudioState.isExternalAudioActive && selectedStep == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مزامنة الصوت والصورة اليدوية:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SnowWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (externalAudioState.delayMs >= 0) "+${externalAudioState.delayMs} ms" else "${externalAudioState.delayMs} ms",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CalmGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Sync Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { onAdjustSync(-500L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp).testTag("sync_minus_500")
                                ) {
                                    Text("-500ms", style = MaterialTheme.typography.labelSmall, color = SnowWhite)
                                }
                                Button(
                                    onClick = { onAdjustSync(-100L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp).testTag("sync_minus_100")
                                ) {
                                    Text("-100ms", style = MaterialTheme.typography.labelSmall, color = SnowWhite)
                                }
                                OutlinedButton(
                                    onClick = onResetSync,
                                    border = BorderStroke(1.dp, CalmGold),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp).testTag("sync_reset")
                                ) {
                                    Text("0.0s ضبط", style = MaterialTheme.typography.labelSmall, color = CalmGold)
                                }
                                Button(
                                    onClick = { onAdjustSync(100L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp).testTag("sync_plus_100")
                                ) {
                                    Text("+100ms", style = MaterialTheme.typography.labelSmall, color = SnowWhite)
                                }
                                Button(
                                    onClick = { onAdjustSync(500L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp).testTag("sync_plus_500")
                                ) {
                                    Text("+500ms", style = MaterialTheme.typography.labelSmall, color = SnowWhite)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(onClick = { selectedStep = 0 }, modifier = Modifier.fillMaxWidth()) {
                                Text("تغيير المعلق أو القناة الصوتية", color = CalmGold, fontWeight = FontWeight.Bold)
                            }

                            // Revert Button
                            OutlinedButton(
                                onClick = onRevertToOriginalAudio,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LiveRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("revert_original_audio_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    tint = LiveRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إيقاف الصوت الخارجي",
                                    color = LiveRed,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedStep == 0) {
                // Available Commentary Sources List
                Text(
                    text = "اختر القناة الصوتية المعلقة:",
                    style = MaterialTheme.typography.titleMedium,
                    color = SnowWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن معلق أو قناة صوتية...", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
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
                        .height(54.dp)
                        .testTag("audio_search_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    AudioSourceGroupHeader("beIN SPORTS", beinSources.size, "audio_group_bein")
                    if (beinSources.isEmpty()) AudioSourceEmptyState("لا توجد قنوات beIN SPORTS في هذا الاشتراك.")
                    beinSources.forEach { source ->
                        AudioSourceRow(
                            source = source,
                            isSelected = externalAudioState.isExternalAudioActive && externalAudioState.activeSource?.channelId == source.channelId,
                            onClick = { onSelectAudioSource(source) }
                        )
                    }
                    AudioSourceGroupHeader("ALWAN", alwanSources.size, "audio_group_alwan")
                    if (alwanSources.isEmpty()) AudioSourceEmptyState("لا توجد قنوات ALWAN في هذا الاشتراك.")
                    alwanSources.forEach { source ->
                        AudioSourceRow(
                            source = source,
                            isSelected = externalAudioState.isExternalAudioActive && externalAudioState.activeSource?.channelId == source.channelId,
                            onClick = { onSelectAudioSource(source) }
                        )
                    }
                }
                }
            }
        }
        }
    }
}


@Composable
private fun AudioSourceGroupHeader(title: String, count: Int, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = CalmGold, fontWeight = FontWeight.Bold)
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun AudioSourceEmptyState(message: String) {
    Text(
        text = message,
        color = TextMuted,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

@Composable
private fun AudioSourceRow(source: AudioCommentarySource, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) DarkSportsBlue else CardBackground),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) CalmGold else CardBorder),
        modifier = Modifier.fillMaxWidth().testTag("audio_source_${source.channelId}").clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Headphones, contentDescription = null, tint = if (isSelected) CalmGold else TextMuted, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(source.channelName, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) CalmGold else SnowWhite, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (source.commentatorName.isNotBlank()) Text(source.commentatorName, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (isSelected) {
                Surface(color = CalmGold.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "مفعل", tint = CalmGold, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مفعل", color = CalmGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    "تفعيل الصوت",
                    style = MaterialTheme.typography.labelSmall,
                    color = SnowWhite,
                    modifier = Modifier
                        .background(DarkSportsBlue, RoundedCornerShape(6.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
        }
    }
}
