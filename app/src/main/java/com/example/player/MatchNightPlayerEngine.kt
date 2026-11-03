package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultDataSource
import com.example.data.model.AudioCommentarySource
import com.example.data.model.Channel
import com.example.data.model.ExternalAudioState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlayerAspectRatio(val label: String) {
    FIT("ملاءمة (Fit)"),
    FILL("ملء الشاشة (Fill)"),
    FIXED_16_9("16:9 كلاسيك")
}

@OptIn(UnstableApi::class)
class MatchNightPlayerEngine(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Primary Video & Audio Player
    var primaryPlayer: ExoPlayer? = null
        private set

    // Independent Secondary Audio Player for Commentary Channel
    private var secondaryAudioPlayer: ExoPlayer? = null

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    val allChannels: StateFlow<List<Channel>> = _allChannels.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(true)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _aspectRatio = MutableStateFlow(PlayerAspectRatio.FIT)
    val aspectRatio: StateFlow<PlayerAspectRatio> = _aspectRatio.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _externalAudioState = MutableStateFlow(ExternalAudioState())
    val externalAudioState: StateFlow<ExternalAudioState> = _externalAudioState.asStateFlow()

    private val _syncOffsetMs = MutableStateFlow(0L)
    val syncOffsetMs: StateFlow<Long> = _syncOffsetMs.asStateFlow()

    private var retryCount = 0
    private var retryJob: Job? = null

    init {
        initializePrimaryPlayer()
    }

    fun setChannelsList(list: List<Channel>) {
        _allChannels.value = list
    }

    private fun initializePrimaryPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1000,  // min buffer
                5000,  // max buffer
                500,   // buffer for playback
                1000   // buffer for playback after rebuffer
            )
            .build()

        primaryPlayer = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                _isBuffering.value = true
                                _errorMessage.value = null
                            }
                            Player.STATE_READY -> {
                                _isBuffering.value = false
                                _errorMessage.value = null
                                retryCount = 0
                            }
                            Player.STATE_ENDED -> {
                                _isBuffering.value = false
                            }
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _isBuffering.value = false
                        handlePlaybackFailure(error)
                    }
                })
            }
    }

    fun playChannel(channel: Channel) {
        _currentChannel.value = channel
        _errorMessage.value = null
        _isBuffering.value = true

        val player = primaryPlayer ?: return

        try {
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            // If external audio was already active, ensure primary audio remains muted
            if (_externalAudioState.value.isExternalAudioActive) {
                player.volume = 0f
            } else {
                player.volume = 1f
            }
        } catch (e: Exception) {
            _errorMessage.value = "تعذر تشغيل القناة: ${e.localizedMessage ?: "رابط البث غير متاح"}"
        }
    }

    fun nextChannel() {
        val list = _allChannels.value
        val current = _currentChannel.value ?: return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && list.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % list.size
            playChannel(list[nextIndex])
        }
    }

    fun previousChannel() {
        val list = _allChannels.value
        val current = _currentChannel.value ?: return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && list.isNotEmpty()) {
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            playChannel(list[prevIndex])
        }
    }

    fun togglePlayPause() {
        val player = primaryPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            secondaryAudioPlayer?.pause()
        } else {
            player.play()
            secondaryAudioPlayer?.play()
        }
    }

    fun toggleAspectRatio() {
        _aspectRatio.value = when (_aspectRatio.value) {
            PlayerAspectRatio.FIT -> PlayerAspectRatio.FILL
            PlayerAspectRatio.FILL -> PlayerAspectRatio.FIXED_16_9
            PlayerAspectRatio.FIXED_16_9 -> PlayerAspectRatio.FIT
        }
    }

    /**
     * Flagship Feature: Separate External Audio Commentary
     * Connects to a second channel stream URL, mutes primary player video stream audio,
     * decodes purely audio, and coordinates manual audio delay synchronization.
     */
    fun attachExternalAudioSource(source: AudioCommentarySource) {
        val primary = primaryPlayer ?: return

        // 1. Mute primary player audio immediately without stopping video rendering
        primary.volume = 0f

        // 2. Release previous secondary audio player if present
        releaseSecondaryAudio()

        // 3. Instantiate independent audio-only player
        val audioPlayer = ExoPlayer.Builder(context).build().apply {
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                .build()
        }

        secondaryAudioPlayer = audioPlayer

        _externalAudioState.value = ExternalAudioState(
            isExternalAudioActive = true,
            activeSource = source,
            delayMs = _syncOffsetMs.value,
            isBuffering = true,
            errorMessage = null
        )

        audioPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        _externalAudioState.value = _externalAudioState.value.copy(isBuffering = true)
                    }
                    Player.STATE_READY -> {
                        _externalAudioState.value = _externalAudioState.value.copy(isBuffering = false, errorMessage = null)
                        applyAudioSyncOffset()
                    }
                    Player.STATE_ENDED, Player.STATE_IDLE -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Critical Rule: If external audio drops or fails, DO NOT fail the main video!
                _externalAudioState.value = _externalAudioState.value.copy(
                    isBuffering = false,
                    errorMessage = "تعذر تحميل الصوت الخارجي من قناة ${source.channelName}. الفيديو مستمر."
                )
            }
        })

        try {
            audioPlayer.setMediaItem(MediaItem.fromUri(source.streamUrl))
            audioPlayer.prepare()
            audioPlayer.volume = 1f
            audioPlayer.play()
        } catch (e: Exception) {
            _externalAudioState.value = _externalAudioState.value.copy(
                isBuffering = false,
                errorMessage = "خطأ في اتصال الصوت البديل: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Adjust audio sync offset in milliseconds (-10000ms to +10000ms)
     * Positive = delay audio (plays later than video)
     * Negative = advance audio (plays earlier)
     */
    fun adjustAudioSync(deltaMs: Long) {
        val newOffset = (_syncOffsetMs.value + deltaMs).coerceIn(-10000L, 10000L)
        _syncOffsetMs.value = newOffset
        _externalAudioState.value = _externalAudioState.value.copy(delayMs = newOffset)
        applyAudioSyncOffset()
    }

    fun resetAudioSync() {
        _syncOffsetMs.value = 0L
        _externalAudioState.value = _externalAudioState.value.copy(delayMs = 0L)
        applyAudioSyncOffset()
    }

    private fun applyAudioSyncOffset() {
        val audioPlayer = secondaryAudioPlayer ?: return
        val offset = _syncOffsetMs.value

        // In live streams, micro-timing adjustments can be applied by small playback speed modulation
        // or position offset seek
        try {
            if (offset > 0) {
                // Audio is delayed: temporarily slow down audio slightly until sync achieved or seek back
                val currentPos = audioPlayer.currentPosition
                if (currentPos > offset) {
                    audioPlayer.seekTo(currentPos - offset)
                }
            } else if (offset < 0) {
                // Audio is advanced
                val currentPos = audioPlayer.currentPosition
                audioPlayer.seekTo(currentPos + Math.abs(offset))
            }
        } catch (e: Exception) {
            // Ignore minor seek adjustments on pure live manifests
        }
    }

    /**
     * Reverts seamlessly to original channel audio
     */
    fun detachExternalAudio() {
        releaseSecondaryAudio()
        primaryPlayer?.volume = 1f
        _externalAudioState.value = ExternalAudioState(isExternalAudioActive = false)
        _syncOffsetMs.value = 0L
    }

    private fun releaseSecondaryAudio() {
        secondaryAudioPlayer?.stop()
        secondaryAudioPlayer?.release()
        secondaryAudioPlayer = null
    }

    private fun handlePlaybackFailure(error: PlaybackException) {
        if (retryCount < 3) {
            retryCount++
            retryJob?.cancel()
            retryJob = coroutineScope.launch {
                delay(2000L * retryCount)
                _currentChannel.value?.let { playChannel(it) }
            }
        } else {
            _errorMessage.value = "انقطع اتصال البث المباشر. اضغط لإعادة المحاولة أو اختر قناة بديلة."
        }
    }

    fun retryPlayback() {
        retryCount = 0
        _currentChannel.value?.let { playChannel(it) }
    }

    fun release() {
        coroutineScope.cancel()
        primaryPlayer?.release()
        primaryPlayer = null
        releaseSecondaryAudio()
    }
}
