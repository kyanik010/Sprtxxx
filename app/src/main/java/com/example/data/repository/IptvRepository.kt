package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.model.*
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class IptvRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("matchnight_iptv_prefs", Context.MODE_PRIVATE)

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    private val _vodMovies = MutableStateFlow<List<VodMovie>>(emptyList())
    val vodMovies: StateFlow<List<VodMovie>> = _vodMovies.asStateFlow()
    private val _vodSeries = MutableStateFlow<List<VodSeries>>(emptyList())
    val vodSeries: StateFlow<List<VodSeries>> = _vodSeries.asStateFlow()
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _audioSources = MutableStateFlow<List<AudioCommentarySource>>(emptyList())
    val audioSources: StateFlow<List<AudioCommentarySource>> = _audioSources.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _currentSubscription = MutableStateFlow(loadSubscription())
    val currentSubscription: StateFlow<SubscriptionProfile> = _currentSubscription.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        loadFavoritesAndHistory()
        val saved = _currentSubscription.value
        if (saved.type == SubscriptionType.XTREAM_CODES && saved.serverUrl.isNotBlank() && saved.username.isNotBlank() && saved.password.isNotBlank()) {
            repositoryScope.launch {
                try {
                    loadXtreamCodes(saved.serverUrl, saved.username, saved.password)
                    val expiry = runCatching { fetchXtreamExpiry(saved.serverUrl, saved.username, saved.password) }.getOrDefault(saved.expiryDate)
                    prefs.edit().putString("sub_expiry_date", expiry).apply()
                    _currentSubscription.value = saved.copy(isActive = true, expiryDate = expiry)
                } catch (_: Exception) {
                    _currentSubscription.value = saved.copy(isActive = false)
                    _channels.value = emptyList()
                    _categories.value = emptyList()
                }
            }
        } else if (saved.type == SubscriptionType.M3U_PLAYLIST && saved.m3uUrl.isNotBlank()) {
            repositoryScope.launch {
                try {
                    loadM3uFromUrl(saved.m3uUrl)
                    _currentSubscription.value = saved.copy(isActive = true)
                } catch (_: Exception) {
                    _currentSubscription.value = saved.copy(isActive = false)
                    _channels.value = emptyList()
                    _categories.value = emptyList()
                }
            }
        }
    }

    private fun loadSubscription(): SubscriptionProfile {
        val server = prefs.getString("sub_server", "") ?: ""
        val user = prefs.getString("sub_user", "") ?: ""
        val pass = prefs.getString("sub_pass", "") ?: ""
        val m3u = prefs.getString("sub_m3u", "") ?: ""
        val expiryDate = prefs.getString("sub_expiry_date", "") ?: ""
        val typeStr = prefs.getString("sub_type", SubscriptionType.NONE.name) ?: SubscriptionType.NONE.name
        val deviceId = prefs.getString("device_id", "MN-TV-${UUID.randomUUID().toString().take(6).uppercase()}")!!

        // Save generated device ID
        prefs.edit().putString("device_id", deviceId).apply()

        val type = try {
            SubscriptionType.valueOf(typeStr)
        } catch (e: Exception) {
            SubscriptionType.NONE
        }

        return SubscriptionProfile(
            serverUrl = server,
            username = user,
            password = pass,
            m3uUrl = m3u,
            type = type,
            deviceId = deviceId,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            expiryDate = expiryDate,
            isActive = false
        )
    }

    private fun loadFavoritesAndHistory() {
        val favs = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        _favorites.value = favs

        val histString = prefs.getString("history_ids", "") ?: ""
        if (histString.isNotBlank()) {
            _history.value = histString.split(",").filter { it.isNotBlank() }
        }
    }

    fun toggleFavorite(channelId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(channelId)) {
            current.remove(channelId)
        } else {
            current.add(channelId)
        }
        _favorites.value = current
        prefs.edit().putStringSet("favorite_ids", current).apply()

        // Update in channels list
        _channels.value = _channels.value.map {
            if (it.id == channelId) it.copy(isFavorite = current.contains(channelId)) else it
        }
    }

    private fun syncAudioSources(channels: List<Channel>) {
        val sources = channels.mapNotNull { channel ->
            val category = classifyAudioSource(channel.name, channel.categoryId) ?: return@mapNotNull null
            AudioCommentarySource(
                channelId = channel.id,
                channelName = channel.name,
                streamUrl = channel.streamUrl,
                commentatorName = channel.currentProgram,
                logoUrl = channel.logoUrl,
                category = category
            )
        }
        _audioSources.value = sources

        // Cache display metadata only. Stream URLs contain the subscription credentials.
        val cachedSources = JSONArray()
        sources.forEach { source ->
            cachedSources.put(
                JSONObject()
                    .put("channelId", source.channelId)
                    .put("channelName", source.channelName)
                    .put("commentatorName", source.commentatorName)
                    .put("logoUrl", source.logoUrl)
                    .put("category", source.category.name)
            )
        }
        prefs.edit().putString("audio_sources_cache", cachedSources.toString()).apply()
    }

    fun addToHistory(channelId: String) {
        val current = _history.value.toMutableList()
        current.remove(channelId)
        current.add(0, channelId)
        val trimmed = current.take(15)
        _history.value = trimmed
        prefs.edit().putString("history_ids", trimmed.joinToString(",")).apply()
    }

    suspend fun saveSubscription(
        type: SubscriptionType,
        serverUrl: String,
        user: String,
        pass: String,
        m3uUrl: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val loadedCount = when (type) {
                SubscriptionType.XTREAM_CODES -> {
                    require(serverUrl.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) { "أدخل عنوان السيرفر واسم المستخدم وكلمة المرور" }
                    loadXtreamCodes(serverUrl.trim(), user.trim(), pass)
                }
                SubscriptionType.M3U_PLAYLIST -> {
                    require(m3uUrl.isNotBlank()) { "أدخل رابط قائمة M3U" }
                    loadM3uFromUrl(m3uUrl.trim())
                }
                SubscriptionType.NONE -> throw IllegalArgumentException("اختر طريقة إضافة الاشتراك")
            }
            val expiryDate = if (type == SubscriptionType.XTREAM_CODES) {
                runCatching { fetchXtreamExpiry(serverUrl.trim(), user.trim(), pass) }.getOrDefault("")
            } else ""
            prefs.edit()
                .putString("sub_expiry_date", expiryDate)
                .putString("sub_type", type.name)
                .putString("sub_server", serverUrl.trim())
                .putString("sub_user", user.trim())
                .putString("sub_pass", pass)
                .putString("sub_m3u", m3uUrl.trim())
                .apply()
            _currentSubscription.value = SubscriptionProfile(
                name = "اشتراك IPTV", type = type, serverUrl = serverUrl.trim(),
                username = user.trim(), password = pass, m3uUrl = m3uUrl.trim(),
                isActive = true, deviceId = prefs.getString("device_id", "") ?: "",
                deviceModel = Build.MANUFACTURER + " " + Build.MODEL,
                expiryDate = expiryDate
            )
            Result.success(loadedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadXtreamCodes(server: String, user: String, pass: String): Int {
        val cleanServer = if (!server.startsWith("http://") && !server.startsWith("https://")) "http://$server" else server
        val url = "$cleanServer/player_api.php?username=$user&password=$pass&action=get_live_streams"
        val request = Request.Builder().url(url).build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("فشل الاتصال بسيرفر إكستريم: رمز ${response.code}")

        val jsonStr = response.body?.string() ?: throw Exception("بيانات فارغة من السيرفر")
        val jsonArray = JSONArray(jsonStr)

        val parsedChannels = mutableListOf<Channel>()
        val categoriesSet = linkedSetOf<String>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val streamId = obj.optString("stream_id")
            val name = obj.optString("name")
            val category = obj.optString("category_id", "عام")
            val logo = obj.optString("stream_icon", "")
            val num = obj.optInt("num", i + 1)
            val streamUrl = "$cleanServer/live/$user/$pass/$streamId.m3u8"

            categoriesSet.add(category)
            parsedChannels.add(
                Channel(
                    id = "xtream_$streamId",
                    channelNumber = num,
                    name = name,
                    categoryId = category,
                    streamUrl = streamUrl,
                    logoUrl = logo,
                    isFavorite = _favorites.value.contains("xtream_$streamId")
                )
            )
        }

        if (parsedChannels.isNotEmpty()) {
            syncAudioSources(parsedChannels)
            _channels.value = parsedChannels

            // Read display names and optional category artwork from the provider API.
            val categoryMetadata = runCatching {
                val categoriesUrl = "${cleanServer}/player_api.php?username=${user}&password=${pass}&action=get_live_categories"
                val categoriesRequest = Request.Builder().url(categoriesUrl).build()
                httpClient.newCall(categoriesRequest).execute().use { response ->
                    if (!response.isSuccessful) emptyMap<String, Pair<String, String>>()
                    else {
                        val array = JSONArray(response.body?.string().orEmpty())
                        buildMap {
                            for (index in 0 until array.length()) {
                                val item = array.optJSONObject(index) ?: continue
                                val id = item.optString("category_id")
                                if (id.isNotBlank()) {
                                    val name = item.optString("category_name").takeIf { it.isNotBlank() } ?: id
                                    val logo = item.optString("category_icon",
                                        item.optString("category_logo", item.optString("cover", "")))
                                    put(id, name to logo)
                                }
                            }
                        }
                    }
                }
            }.getOrDefault(emptyMap())

            _categories.value = categoriesSet.map { id ->
                val metadata = categoryMetadata[id]
                val representativeLogo = parsedChannels.firstOrNull { it.categoryId == id && it.logoUrl.isNotBlank() }?.logoUrl.orEmpty()
                Category(
                    id = id,
                    name = metadata?.first ?: id,
                    channelCount = parsedChannels.count { it.categoryId == id },
                    logoUrl = metadata?.second?.takeIf { it.isNotBlank() } ?: representativeLogo
                )
            }
            runCatching { loadVodCatalog(cleanServer, user, pass) }
            return parsedChannels.size
        } else {
            throw Exception("لم يتم العثور على قنوات في هذا الاشتراك")
        }
    }


    private suspend fun loadVodCatalog(server: String, user: String, pass: String) {
        val encodedUser = Uri.encode(user)
        val encodedPass = Uri.encode(pass)
        val base = server.trimEnd('/') + "/player_api.php?username=" + encodedUser + "&password=" + encodedPass

        suspend fun readArray(action: String): JSONArray {
            val request = Request.Builder().url(base + "&action=" + action).build()
            return httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("VOD API " + action + ": HTTP " + response.code)
                JSONArray(response.body?.string().orEmpty())
            }
        }

        _vodMovies.value = runCatching {
            val array = readArray("get_vod_streams")
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("stream_id").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("name").ifBlank { "فيلم " + id }
                    val ext = item.optString("container_extension").ifBlank { "mp4" }
                    add(VodMovie(
                        id = id, name = name, categoryId = item.optString("category_id"),
                        posterUrl = item.optString("stream_icon"), plot = item.optString("plot"),
                        year = item.optString("year"), rating = item.optString("rating"),
                        streamUrl = server.trimEnd('/') + "/movie/" + encodedUser + "/" + encodedPass + "/" + id + "." + ext
                    ))
                }
            }
        }.getOrDefault(emptyList())

        _vodSeries.value = runCatching {
            val array = readArray("get_series")
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("series_id").takeIf { it.isNotBlank() } ?: continue
                    add(VodSeries(
                        id = id, name = item.optString("name").ifBlank { "مسلسل " + id },
                        categoryId = item.optString("category_id"),
                        posterUrl = item.optString("cover").ifBlank { item.optString("cover_big") },
                        plot = item.optString("plot"), year = item.optString("releaseDate").take(4),
                        rating = item.optString("rating")
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun loadSeriesEpisodes(seriesId: String): List<VodEpisode> = withContext(Dispatchers.IO) {
        val profile = _currentSubscription.value
        require(profile.type == SubscriptionType.XTREAM_CODES) { "الحلقات تتطلب اشتراك Xtream Codes" }
        val server = profile.serverUrl.let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "http://" + it
        }.trimEnd('/')
        val user = Uri.encode(profile.username)
        val pass = Uri.encode(profile.password)
        val url = server + "/player_api.php?username=" + user + "&password=" + pass +
            "&action=get_series_info&series_id=" + Uri.encode(seriesId)
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("تعذر تحميل حلقات المسلسل: HTTP " + response.code)
            val root = JSONObject(response.body?.string().orEmpty())
            val seasons = root.optJSONObject("episodes") ?: JSONObject()
            val result = mutableListOf<VodEpisode>()
            val keys = seasons.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
            keys.forEach { seasonKey ->
                val season = seasonKey.toIntOrNull() ?: 0
                val list = seasons.optJSONArray(seasonKey) ?: return@forEach
                for (i in 0 until list.length()) {
                    val item = list.optJSONObject(i) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val ext = item.optString("container_extension").ifBlank { "mp4" }
                    val info = item.optJSONObject("info") ?: JSONObject()
                    val episodeNo = item.optInt("episode_num", i + 1)
                    result.add(VodEpisode(
                        id = id, name = item.optString("title").ifBlank { "الحلقة " + episodeNo },
                        season = season, episodeNumber = episodeNo,
                        posterUrl = info.optString("movie_image"),
                        streamUrl = server + "/series/" + user + "/" + pass + "/" + id + "." + ext
                    ))
                }
            }
            result.sortedWith(compareBy<VodEpisode> { it.season }.thenBy { it.episodeNumber })
        }
    }

    private suspend fun fetchXtreamExpiry(server: String, user: String, pass: String): String {
        val cleanServer = if (!server.startsWith("http://") && !server.startsWith("https://")) "http://$server" else server
        val accountUrl = "$cleanServer/player_api.php?username=$user&password=$pass"
        val request = Request.Builder().url(accountUrl).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use ""
            val payload = JSONObject(response.body?.string().orEmpty())
            val rawExpiry = payload.optJSONObject("user_info")?.optString("exp_date").orEmpty()
            val epochSeconds = rawExpiry.toLongOrNull()?.takeIf { it > 0L } ?: return@use ""
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochSeconds * 1000L))
        }
    }

    suspend fun loadM3uFromUrl(m3uUrl: String): Int {
        // Plain M3U playlists expose live entries only; they do not define Xtream VOD APIs.
        _vodMovies.value = emptyList()
        _vodSeries.value = emptyList()
        val request = Request.Builder().url(m3uUrl).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("فشل تحميل قائمة M3U: رمز ${response.code}")
        val content = response.body?.string() ?: throw Exception("محتوى M3U فارغ")
        return parseM3uContent(content)
    }

    fun parseM3uContent(content: String): Int {
        val reader = BufferedReader(StringReader(content))
        var line: String? = reader.readLine()

        val parsedChannels = mutableListOf<Channel>()
        val categoriesMap = mutableMapOf<String, Int>()

        var currentName = ""
        var currentLogo = ""
        var currentGroup = "غير مصنف"
        var channelIndex = 1

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse attributes
                val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(trimmed)
                currentLogo = logoMatch?.groupValues?.get(1) ?: ""

                val groupMatch = Regex("""group-title="([^"]+)"""").find(trimmed)
                currentGroup = groupMatch?.groupValues?.get(1) ?: "غير مصنف"

                val commaIndex = trimmed.lastIndexOf(',')
                currentName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    "قناة رياضية $channelIndex"
                }
            } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                // This is the stream URL
                val id = "m3u_$channelIndex"
                parsedChannels.add(
                    Channel(
                        id = id,
                        channelNumber = channelIndex,
                        name = currentName.ifBlank { "قناة $channelIndex" },
                        categoryId = currentGroup,
                        streamUrl = trimmed,
                        logoUrl = currentLogo,
                        isFavorite = _favorites.value.contains(id)
                    )
                )
                categoriesMap[currentGroup] = (categoriesMap[currentGroup] ?: 0) + 1
                channelIndex++
                currentName = ""
                currentLogo = ""
            }
            line = reader.readLine()
        }

        if (parsedChannels.isNotEmpty()) {
            syncAudioSources(parsedChannels)
            _channels.value = parsedChannels
            _categories.value = categoriesMap.map { entry ->
                val representativeLogo = parsedChannels.firstOrNull {
                    it.categoryId == entry.key && it.logoUrl.isNotBlank()
                }?.logoUrl.orEmpty()
                Category(id = entry.key, name = entry.key, channelCount = entry.value, logoUrl = representativeLogo)
            }
            return parsedChannels.size
        } else {
            throw Exception("لم يتم العثور على قنوات صالحة في ملف M3U")
        }
    }


}
