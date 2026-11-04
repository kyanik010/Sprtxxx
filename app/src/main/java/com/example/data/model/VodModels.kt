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


data class VodCategory(
    val id: String,
    val name: String,
    val itemCount: Int = 0,
    val posterUrl: String = ""
)

/** Combines provider category metadata with IDs found in the VOD catalog. */
internal fun buildVodCategories(
    categories: List<VodCategory>,
    itemCategoryIds: List<String>
): List<VodCategory> {
    val counts = itemCategoryIds.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
    val metadata = categories.filter { it.id.isNotBlank() }.associateBy { it.id }
    val ids = (categories.map { it.id } + counts.keys).filter { it.isNotBlank() }.distinct()
    return ids.map { id ->
        val category = metadata[id]
        VodCategory(
            id = id,
            name = category?.name?.takeIf { it.isNotBlank() } ?: id,
            itemCount = counts[id] ?: 0,
            posterUrl = category?.posterUrl.orEmpty()
        )
    }
}
