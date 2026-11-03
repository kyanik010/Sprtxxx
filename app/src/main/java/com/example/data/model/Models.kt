package com.example.data.model

data class Channel(
    val id: String,
    val channelNumber: Int,
    val name: String,
    val categoryId: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val currentProgram: String = "بث مباشر - استوديو تحليلي",
    val nextProgram: String = "المباراة القادمة مباشرة",
    val programProgress: Float = 0.45f,
    val isFavorite: Boolean = false,
    val resolution: String = "1080p FHD",
    val fps: Int = 50
)

data class Category(
    val id: String,
    val name: String,
    val channelCount: Int = 0
)

enum class MatchStatus {
    LIVE,
    UPCOMING,
    FINISHED
}

data class Match(
    val id: String,
    val homeTeam: String,
    val homeLogo: String,
    val awayTeam: String,
    val awayLogo: String,
    val competition: String,
    val time: String,
    val date: String,
    val status: MatchStatus,
    val score: String? = null,
    val minute: String? = null,
    val primaryChannelId: String,
    val primaryChannelName: String,
    val secondaryAudioChannelId: String? = null,
    val commentator: String = "معلق المباراة"
)

enum class SubscriptionType {
    XTREAM_CODES,
    M3U_PLAYLIST,
    PRESET_SPORTS
}

data class SubscriptionProfile(
    val id: String = "default",
    val name: String = "اشتراك ماتش نايت الرياضي",
    val type: SubscriptionType = SubscriptionType.PRESET_SPORTS,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val isActive: Boolean = true,
    val expiryDate: String = "2027-12-31",
    val maxConnections: Int = 2,
    val activeConnections: Int = 1,
    val deviceId: String = "MN-TV-8942-X",
    val deviceModel: String = "Android TV 4K Ultra"
)

data class AudioCommentarySource(
    val channelId: String,
    val channelName: String,
    val streamUrl: String,
    val commentatorName: String,
    val logoUrl: String = ""
)

data class ExternalAudioState(
    val isExternalAudioActive: Boolean = false,
    val activeSource: AudioCommentarySource? = null,
    val delayMs: Long = 0L, // -10000ms to +10000ms
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)
