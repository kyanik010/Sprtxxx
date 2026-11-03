package com.example.data.model

data class Channel(
    val id: String,
    val channelNumber: Int,
    val name: String,
    val categoryId: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val currentProgram: String = "",
    val nextProgram: String = "",
    val programProgress: Float = 0f,
    val isFavorite: Boolean = false,
    val resolution: String = "",
    val fps: Int = 0
)

data class Category(
    val id: String,
    val name: String,
    val channelCount: Int = 0,
    val logoUrl: String = ""
)

enum class MatchStatus { LIVE, UPCOMING, FINISHED }

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
    val commentator: String = ""
)

enum class SubscriptionType { XTREAM_CODES, M3U_PLAYLIST, NONE }

data class SubscriptionProfile(
    val id: String = "default",
    val name: String = "اشتراك IPTV",
    val type: SubscriptionType = SubscriptionType.NONE,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val isActive: Boolean = false,
    val expiryDate: String = "",
    val maxConnections: Int = 0,
    val activeConnections: Int = 0,
    val deviceId: String = "",
    val deviceModel: String = ""
)

enum class AudioSourceCategory { BEIN_SPORTS, ALWAN }

data class AudioCommentarySource(
    val channelId: String,
    val channelName: String,
    val streamUrl: String,
    val commentatorName: String,
    val logoUrl: String = "",
    val category: AudioSourceCategory = AudioSourceCategory.BEIN_SPORTS
)

fun classifyAudioSource(name: String, categoryId: String): AudioSourceCategory? {
    val searchable = "$name $categoryId"
        .lowercase()
        .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace(Regex("[^a-z0-9\\u0600-\\u06FF]+"), " ").trim()
    val isBein = listOf("bein sports", "bein sport", "beinsports", "beinsport",
        "be in sports", "be in sport", "بي ان سبورت", "بي ان", "بين سبورت").any(searchable::contains)
    if (isBein) return AudioSourceCategory.BEIN_SPORTS
    val isAlwan = Regex("""\b(alwan|al wan)\b""").containsMatchIn(searchable) || searchable.contains("الوان")
    if (isAlwan) return AudioSourceCategory.ALWAN
    return null
}

data class ExternalAudioState(
    val isExternalAudioActive: Boolean = false,
    val activeSource: AudioCommentarySource? = null,
    val delayMs: Long = 0L,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)
