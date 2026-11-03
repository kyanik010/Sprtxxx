package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.AudioCommentarySource
import com.example.data.model.Channel
import com.example.player.MatchNightPlayerEngine
import com.example.player.PlayerAspectRatio
import com.example.ui.components.LivePulseBadge
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    playerEngine: MatchNightPlayerEngine,
    audioSources: List<AudioCommentarySource>,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentChannel by playerEngine.currentChannel.collectAsState()
    val allChannels by playerEngine.allChannels.collectAsState()
    val isPlaying by playerEngine.isPlaying.collectAsState()
    val isBuffering by playerEngine.isBuffering.collectAsState()
    val aspectRatio by playerEngine.aspectRatio.collectAsState()
    val errorMessage by playerEngine.errorMessage.collectAsState()
    val externalAudioState by playerEngine.externalAudioState.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showChannelDrawer by remember { mutableStateOf(false) }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000L)
            showControls = false
        }
    }

    BackHandler {
        if (showChannelDrawer) {
            showChannelDrawer = false
        } else if (showAudioDialog) {
            showAudioDialog = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .testTag("player_screen_root")
    ) {
        // Video Surface via AndroidView & Media3 PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = playerEngine.primaryPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = when (aspectRatio) {
                        PlayerAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        PlayerAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        PlayerAspectRatio.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                }
            },
            update = { view ->
                view.player = playerEngine.primaryPlayer
                view.resizeMode = when (aspectRatio) {
                    PlayerAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    PlayerAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    PlayerAspectRatio.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = NightNavy.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CalmGold.copy(alpha = 0.6f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CalmGold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "جاري تحميل البث المباشر...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SnowWhite
                        )
                    }
                }
            }
        }

        // Error Recovery Overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NightNavy.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = LiveRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "حدث خطأ في تشغيل القناة",
                        style = MaterialTheme.typography.titleMedium,
                        color = SnowWhite,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { playerEngine.retryPlayback() },
                        colors = ButtonDefaults.buttonColors(containerColor = CalmGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("retry_playback_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = NightNavy)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إعادة المحاولة",
                            color = NightNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Overlay Controls (Animated)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar Gradient & Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(NightNavy.copy(alpha = 0.95f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back Button & Channel Info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(DarkSportsBlue)
                                    .testTag("player_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = SnowWhite
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            currentChannel?.let { ch ->
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ch.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = SnowWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = DarkSportsBlue,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, CalmGold)
                                        ) {
                                            Text(
                                                text = "${ch.channelNumber}",
                                                color = CalmGold,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = ch.currentProgram,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Right: Live Badge & External Audio Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (externalAudioState.isExternalAudioActive) {
                                Surface(
                                    color = CalmGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, CalmGold),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clickable { showAudioDialog = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Headphones,
                                            contentDescription = null,
                                            tint = CalmGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "صوت: ${externalAudioState.activeSource?.channelName ?: ""}",
                                            color = CalmGold,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            LivePulseBadge()
                        }
                    }
                }

                // Center Controls: Prev Channel, Play/Pause, Next Channel
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Previous Channel
                    IconButton(
                        onClick = { playerEngine.previousChannel() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DarkSportsBlue.copy(alpha = 0.85f))
                            .border(1.dp, CardBorder, CircleShape)
                            .testTag("player_prev_channel")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "القناة السابقة",
                            tint = SnowWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play/Pause
                    IconButton(
                        onClick = { playerEngine.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CalmGold)
                            .testTag("player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = NightNavy,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Next Channel
                    IconButton(
                        onClick = { playerEngine.nextChannel() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DarkSportsBlue.copy(alpha = 0.85f))
                            .border(1.dp, CardBorder, CircleShape)
                            .testTag("player_next_channel")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "القناة التالية",
                            tint = SnowWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Bar Controls & EPG
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, NightNavy.copy(alpha = 0.95f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: External Audio Trigger Button
                        Button(
                            onClick = { showAudioDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (externalAudioState.isExternalAudioActive) CalmGold else DarkSportsBlue
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CalmGold),
                            modifier = Modifier.testTag("open_external_audio_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (externalAudioState.isExternalAudioActive) NightNavy else CalmGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (externalAudioState.isExternalAudioActive) "الصوت الخارجي (مفعل)" else "الصوت الخارجي",
                                color = if (externalAudioState.isExternalAudioActive) NightNavy else SnowWhite,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Right: Channel List Drawer, Aspect Ratio, Favorite
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Channel Drawer
                            IconButton(
                                onClick = { showChannelDrawer = true },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .testTag("in_player_channel_drawer_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = "قائمة القنوات",
                                    tint = SnowWhite
                                )
                            }

                            // Aspect Ratio
                            IconButton(
                                onClick = { playerEngine.toggleAspectRatio() },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .testTag("aspect_ratio_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = aspectRatio.label,
                                    tint = CalmGold
                                )
                            }

                            // Favorite
                            currentChannel?.let { ch ->
                                IconButton(
                                    onClick = { onToggleFavorite(ch.id) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (ch.isFavorite) Icons.Filled.Star else Icons.Default.StarOutline,
                                        contentDescription = "المفضلة",
                                        tint = if (ch.isFavorite) CalmGold else SnowWhite
                                    )
                                }
                            }
                        }
                    }

                    // EPG Progress Timeline Banner
                    currentChannel?.let { ch ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الآن: ${ch.currentProgram}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SnowWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "التالي: ${ch.nextProgram}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ch.programProgress },
                            color = CalmGold,
                            trackColor = DarkSportsBlue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                        )
                    }
                }
            }
        }

        // In-Player Channel Drawer
        if (showChannelDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterEnd)
                    .background(NightNavy.copy(alpha = 0.95f))
                    .border(BorderStroke(1.dp, CardBorder))
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .testTag("in_player_channel_drawer")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تصفح القنوات",
                            style = MaterialTheme.typography.titleMedium,
                            color = SnowWhite,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showChannelDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allChannels) { ch ->
                            val isCurrent = currentChannel?.id == ch.id
                            Surface(
                                color = if (isCurrent) DarkSportsBlue else CardBackground,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) CalmGold else CardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerEngine.playChannel(ch)
                                        showChannelDrawer = false
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = "${ch.channelNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CalmGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ch.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCurrent) CalmGold else SnowWhite,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // External Audio Dialog
        if (showAudioDialog && currentChannel != null) {
            ExternalAudioDialog(
                currentVideoChannel = currentChannel!!,
                externalAudioState = externalAudioState,
                audioSources = audioSources,
                onSelectAudioSource = { source ->
                    playerEngine.attachExternalAudioSource(source)
                },
                onAdjustSync = { delta ->
                    playerEngine.adjustAudioSync(delta)
                },
                onResetSync = {
                    playerEngine.resetAudioSync()
                },
                onRevertToOriginalAudio = {
                    playerEngine.detachExternalAudio()
                },
                onDismiss = {
                    showAudioDialog = false
                }
            )
        }
    }
}
