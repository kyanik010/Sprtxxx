package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.model.*
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
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()


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
                    _currentSubscription.value = saved.copy(isActive = true)
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
            expiryDate = "",
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
            prefs.edit()
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
                deviceModel = Build.MANUFACTURER + " " + Build.MODEL
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
        val categoriesSet = mutableSetOf<String>()

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
            _channels.value = parsedChannels
            _categories.value = categoriesSet.map { Category(id = it, name = it, channelCount = parsedChannels.count { c -> c.categoryId == it }) }
            return parsedChannels.size
        } else {
            throw Exception("لم يتم العثور على قنوات في هذا الاشتراك")
        }
    }

    suspend fun loadM3uFromUrl(m3uUrl: String): Int {
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
            _channels.value = parsedChannels
            _categories.value = categoriesMap.map { Category(id = it.key, name = it.key, channelCount = it.value) }
            return parsedChannels.size
        } else {
            throw Exception("لم يتم العثور على قنوات صالحة في ملف M3U")
        }
    }


}
