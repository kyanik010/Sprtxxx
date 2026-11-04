package com.example.data.model

data class VodMovie(
    val id: String,
    val name: String,
    val categoryId: String,
    val posterUrl: String = "",
    val plot: String = "",
    val year: String = "",
    val rating: String = "",
    val streamUrl: String
)

data class VodSeries(
    val id: String,
    val name: String,
    val categoryId: String,
    val posterUrl: String = "",
    val plot: String = "",
    val year: String = "",
    val rating: String = ""
)

data class VodEpisode(
    val id: String,
    val name: String,
    val season: Int,
    val episodeNumber: Int,
    val posterUrl: String = "",
    val streamUrl: String
) {
    fun asChannel(): Channel = Channel(
        id = "vod_episode_$id",
        channelNumber = episodeNumber,
        name = name,
        categoryId = "مسلسلات",
        streamUrl = streamUrl,
        logoUrl = posterUrl
    )
}
