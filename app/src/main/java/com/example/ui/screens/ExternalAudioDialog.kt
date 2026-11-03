package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.model.Channel
import com.example.data.model.ExternalAudioState
import com.example.ui.theme.*

@Composable
fun ExternalAudioDialog(
    currentVideoChannel: Channel,
    externalAudioState: ExternalAudioState,
    allChannels: List<Channel>,
    onSelectAudioSource: (AudioCommentarySource) -> Unit,
    onAdjustSync: (Long) -> Unit,
    onResetSync: () -> Unit,
    onRevertToOriginalAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Map channels to commentary sources
    val audioSources = remember(allChannels, searchQuery) {
        allChannels
            .filter { it.id != currentVideoChannel.id }
            .filter {
                if (searchQuery.isBlank()) true
                else it.name.contains(searchQuery, ignoreCase = true) ||
                     it.currentProgram.contains(searchQuery, ignoreCase = true)
            }
            .map {
                AudioCommentarySource(
                    channelId = it.id,
                    channelName = it.name,
                    streamUrl = it.streamUrl,
                    commentatorName = it.currentProgram,
                    logoUrl = it.logoUrl
                )
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("external_audio_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = NightNavy,
            border = BorderStroke(1.5.dp, CalmGold.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                                style = MaterialTheme.typography.titleLarge,
                                color = SnowWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "شاهد الصورة من قناة واستمع لتعليق قناة ثانية",
                                style = MaterialTheme.typography.labelSmall,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Current Active Channels Status Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSportsBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = CardBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

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
                                    text = "جاري تحميل وتخزين مسار الصوت البديل...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CalmGold
                                )
                            }
                        }
                    }
                }

                // Audio Synchronization Section (When External Audio Active)
                if (externalAudioState.isExternalAudioActive) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
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

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sync Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { onAdjustSync(-500L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.testTag("sync_minus_500")
                                ) {
                                    Text("-500ms", style = MaterialTheme.typography.labelSmall, color = SnowWhite)
                                }
                                Button(
                                    onClick = { onAdjustSync(-100L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.testTag("sync_minus_100")
                                ) {
                                    Text("-100ms", style = MaterialTheme.typography.labelSmall, color = SnowWhite)
                                }
                                OutlinedButton(
                                    onClick = onResetSync,
                                    border = BorderStroke(1.dp, CalmGold),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.testTag("sync_reset")
                                ) {
                                    Text("0.0s ضبط", style = MaterialTheme.typography.labelSmall, color = CalmGold)
                                }
                                Button(
                                    onClick = { onAdjustSync(100L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.testTag("sync_plus_100")
                                ) {
                                    Text("+100ms", style = MaterialTheme.typography.labelSmall, color = SnowWhite)
                                }
                                Button(
                                    onClick = { onAdjustSync(500L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSportsBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.testTag("sync_plus_500")
                                ) {
                                    Text("+500ms", style = MaterialTheme.typography.labelSmall, color = SnowWhite)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Revert Button
                            OutlinedButton(
                                onClick = onRevertToOriginalAudio,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LiveRed),
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    text = "العودة للصوت الأصلي للقناة",
                                    color = LiveRed,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Available Commentary Sources List
                Text(
                    text = "اختر القناة الصوتية المعلقة:",
                    style = MaterialTheme.typography.titleMedium,
                    color = SnowWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        .padding(bottom = 8.dp)
                        .testTag("audio_search_input")
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(audioSources) { source ->
                        val isSelected = externalAudioState.isExternalAudioActive &&
                                externalAudioState.activeSource?.channelId == source.channelId

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkSportsBlue else CardBackground
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CalmGold else CardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("audio_source_${source.channelId}")
                                .clickable {
                                    onSelectAudioSource(source)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isSelected) CalmGold else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = source.channelName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) CalmGold else SnowWhite,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = source.commentatorName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "محدد",
                                        tint = CalmGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Surface(
                                        color = DarkSportsBlue,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, CardBorder)
                                    ) {
                                        Text(
                                            text = "تفعيل الصوت",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SnowWhite,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
