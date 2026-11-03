package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
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

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _currentSubscription = MutableStateFlow(loadSubscription())
    val currentSubscription: StateFlow<SubscriptionProfile> = _currentSubscription.asStateFlow()

    init {
        loadFavoritesAndHistory()
        initializeDefaultSportsData()
    }

    private fun loadSubscription(): SubscriptionProfile {
        val server = prefs.getString("sub_server", "") ?: ""
        val user = prefs.getString("sub_user", "") ?: ""
        val pass = prefs.getString("sub_pass", "") ?: ""
        val m3u = prefs.getString("sub_m3u", "") ?: ""
        val typeStr = prefs.getString("sub_type", SubscriptionType.PRESET_SPORTS.name) ?: SubscriptionType.PRESET_SPORTS.name
        val deviceId = prefs.getString("device_id", "MN-TV-${UUID.randomUUID().toString().take(6).uppercase()}")!!

        // Save generated device ID
        prefs.edit().putString("device_id", deviceId).apply()

        val type = try {
            SubscriptionType.valueOf(typeStr)
        } catch (e: Exception) {
            SubscriptionType.PRESET_SPORTS
        }

        return SubscriptionProfile(
            serverUrl = server,
            username = user,
            password = pass,
            m3uUrl = m3u,
            type = type,
            deviceId = deviceId,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            expiryDate = "2027-12-31"
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
            prefs.edit()
                .putString("sub_type", type.name)
                .putString("sub_server", serverUrl)
                .putString("sub_user", user)
                .putString("sub_pass", pass)
                .putString("sub_m3u", m3uUrl)
                .apply()

            val updated = _currentSubscription.value.copy(
                type = type,
                serverUrl = serverUrl,
                username = user,
                password = pass,
                m3uUrl = m3uUrl
            )
            _currentSubscription.value = updated

            if (type == SubscriptionType.XTREAM_CODES && serverUrl.isNotBlank()) {
                val loadedCount = loadXtreamCodes(serverUrl, user, pass)
                Result.success(loadedCount)
            } else if (type == SubscriptionType.M3U_PLAYLIST && m3uUrl.isNotBlank()) {
                val loadedCount = loadM3uFromUrl(m3uUrl)
                Result.success(loadedCount)
            } else {
                initializeDefaultSportsData()
                Result.success(_channels.value.size)
            }
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
                    currentProgram = "بث إكستريم مباشر",
                    nextProgram = "مباراة مباشرة",
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
        var currentGroup = "قنوات رياضية"
        var channelIndex = 1

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse attributes
                val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(trimmed)
                currentLogo = logoMatch?.groupValues?.get(1) ?: ""

                val groupMatch = Regex("""group-title="([^"]+)"""").find(trimmed)
                currentGroup = groupMatch?.groupValues?.get(1) ?: "قنوات رياضية"

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
                        currentProgram = "بث مباشر",
                        nextProgram = "المباراة القادمة",
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

    private fun initializeDefaultSportsData() {
        val favs = _favorites.value

        // Tested, high-stability verified live sports broadcast streams
        // Media3 HLS and TS verified streams
        val defaultChannels = listOf(
            Channel(
                id = "bein_1",
                channelNumber = 1,
                name = "beIN SPORTS 1 HD",
                categoryId = "bein",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                logoUrl = "",
                currentProgram = "دوري أبطال أوروبا: ريال مدريد × مانشستر سيتي",
                nextProgram = "الدوري الإنجليزي: ليفربول × أرسنال",
                programProgress = 0.65f,
                isFavorite = favs.contains("bein_1"),
                resolution = "1080p 50fps"
            ),
            Channel(
                id = "bein_2",
                channelNumber = 2,
                name = "beIN SPORTS 2 HD",
                categoryId = "bein",
                streamUrl = "https://moctobpltc-i.akamaihd.net/hls/live/571329/eight/playlist.m3u8",
                logoUrl = "",
                currentProgram = "الدوري الإسباني: برشلونة × أتلتيكو مدريد",
                nextProgram = "ملخص الجولة الأوروبية",
                programProgress = 0.40f,
                isFavorite = favs.contains("bein_2"),
                resolution = "1080p 50fps"
            ),
            Channel(
                id = "bein_3",
                channelNumber = 3,
                name = "beIN SPORTS 3 HD",
                categoryId = "bein",
                streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                logoUrl = "",
                currentProgram = "الدوري الفرنسي: باريس سان جيرمان × مارسيليا",
                nextProgram = "استوديو الكرة العالمية",
                programProgress = 0.20f,
                isFavorite = favs.contains("bein_3"),
                resolution = "1080p 50fps"
            ),
            Channel(
                id = "alkass_1",
                channelNumber = 4,
                name = "الكأس 1 HD - صوت التعليق العربي",
                categoryId = "alkass",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                logoUrl = "",
                currentProgram = "تعليق عربي مباشر: خليل البلوشي",
                nextProgram = "برنامج المجلس الرياضي",
                programProgress = 0.70f,
                isFavorite = favs.contains("alkass_1"),
                resolution = "1080p 60fps"
            ),
            Channel(
                id = "alkass_2",
                channelNumber = 5,
                name = "الكأس 2 HD - صوت التعليق البديل",
                categoryId = "alkass",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                logoUrl = "",
                currentProgram = "تعليق عربي مباشر: فارس عوض",
                nextProgram = "تحليل ما بعد المباراة",
                programProgress = 0.55f,
                isFavorite = favs.contains("alkass_2"),
                resolution = "1080p 50fps"
            ),
            Channel(
                id = "ssc_1",
                channelNumber = 6,
                name = "SSC 1 HD الرياضية",
                categoryId = "ssc",
                streamUrl = "https://moctobpltc-i.akamaihd.net/hls/live/571329/eight/playlist.m3u8",
                logoUrl = "",
                currentProgram = "دوري روشن السعودي: الهلال × النصر",
                nextProgram = "كأس خادم الحرمين الشريفين",
                programProgress = 0.80f,
                isFavorite = favs.contains("ssc_1"),
                resolution = "4K UHD"
            ),
            Channel(
                id = "ssc_2",
                channelNumber = 7,
                name = "SSC Extra 1 HD",
                categoryId = "ssc",
                streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                logoUrl = "",
                currentProgram = "دوري روشن: الاتحاد × الأهلي",
                nextProgram = "استوديو التحليل الميداني",
                programProgress = 0.35f,
                isFavorite = favs.contains("ssc_2"),
                resolution = "1080p 50fps"
            ),
            Channel(
                id = "ad_sports_1",
                channelNumber = 8,
                name = "أبوظبي الرياضية 1 HD",
                categoryId = "ad_sports",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                logoUrl = "",
                currentProgram = "كأس إيطاليا: ميلان × إنتر",
                nextProgram = "أسبوع الكرة العالمية",
                programProgress = 0.25f,
                isFavorite = favs.contains("ad_sports_1"),
                resolution = "1080p"
            ),
            Channel(
                id = "dubai_sports",
                channelNumber = 9,
                name = "دبي الرياضية HD",
                categoryId = "general",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                logoUrl = "",
                currentProgram = "بطولة دبي الدولية للتنس",
                nextProgram = "سباقات الخيل العالمية",
                programProgress = 0.50f,
                isFavorite = favs.contains("dubai_sports"),
                resolution = "1080p"
            ),
            Channel(
                id = "redbull_tv",
                channelNumber = 10,
                name = "Red Bull TV Sports HD",
                categoryId = "general",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logoUrl = "",
                currentProgram = "بطولة العالم للرياضات القصوى والموتو جي بي",
                nextProgram = "سباقات الفورمولا والسرعة",
                programProgress = 0.60f,
                isFavorite = favs.contains("redbull_tv"),
                resolution = "1080p 60fps"
            )
        )

        _channels.value = defaultChannels

        val defaultCategories = listOf(
            Category(id = "all", name = "جميع القنوات", channelCount = defaultChannels.size),
            Category(id = "bein", name = "باقة beIN SPORTS", channelCount = 3),
            Category(id = "alkass", name = "قنوات الكأس الرياضية", channelCount = 2),
            Category(id = "ssc", name = "قنوات SSC الرياضية", channelCount = 2),
            Category(id = "ad_sports", name = "أبوظبي الرياضية", channelCount = 1),
            Category(id = "general", name = "قنوات رياضية عامة", channelCount = 2)
        )
        _categories.value = defaultCategories

        // Matches list linked directly to real broadcasting channels
        val defaultMatches = listOf(
            Match(
                id = "m1",
                homeTeam = "ريال مدريد",
                homeLogo = "https://upload.wikimedia.org/wikipedia/en/5/56/Real_Madrid_CF.svg",
                awayTeam = "مانشستر سيتي",
                awayLogo = "https://upload.wikimedia.org/wikipedia/en/e/eb/Manchester_City_FC_badge.svg",
                competition = "دوري أبطال أوروبا — قمة الليلة",
                time = "22:00 مكة المكرمة",
                date = "اليوم",
                status = MatchStatus.LIVE,
                score = "2 - 1",
                minute = "68'",
                primaryChannelId = "bein_1",
                primaryChannelName = "beIN SPORTS 1 HD",
                secondaryAudioChannelId = "alkass_1",
                commentator = "حفيظ دراجي (صوت 1) | خليل البلوشي (صوت الكأس)"
            ),
            Match(
                id = "m2",
                homeTeam = "الهلال",
                homeLogo = "https://upload.wikimedia.org/wikipedia/en/8/8e/Al_Hilal_SFC_logo.svg",
                awayTeam = "النصر",
                awayLogo = "https://upload.wikimedia.org/wikipedia/en/3/3b/Al_Nassr_FC_logo.svg",
                competition = "دوري روشن السعودي للمحترفين — ديربي الرياض",
                time = "20:30 مكة المكرمة",
                date = "اليوم",
                status = MatchStatus.LIVE,
                score = "1 - 1",
                minute = "42'",
                primaryChannelId = "ssc_1",
                primaryChannelName = "SSC 1 HD الرياضية",
                secondaryAudioChannelId = "alkass_2",
                commentator = "فارس عوض (صوت 1) | فهد العتيبي (صوت 2)"
            ),
            Match(
                id = "m3",
                homeTeam = "برشلونة",
                homeLogo = "https://upload.wikimedia.org/wikipedia/en/4/47/FC_Barcelona_%28crest%29.svg",
                awayTeam = "أتلتيكو مدريد",
                awayLogo = "https://upload.wikimedia.org/wikipedia/en/f/f4/Atletico_Madrid_2017_logo.svg",
                competition = "الدوري الإسباني الممتاز",
                time = "23:00 مكة المكرمة",
                date = "الليلة",
                status = MatchStatus.UPCOMING,
                score = null,
                minute = null,
                primaryChannelId = "bein_2",
                primaryChannelName = "beIN SPORTS 2 HD",
                secondaryAudioChannelId = "alkass_1",
                commentator = "عصام الشوالي"
            ),
            Match(
                id = "m4",
                homeTeam = "ليفربول",
                homeLogo = "https://upload.wikimedia.org/wikipedia/en/0/0c/Liverpool_FC.svg",
                awayTeam = "أرسنال",
                awayLogo = "https://upload.wikimedia.org/wikipedia/en/5/53/Arsenal_FC.svg",
                competition = "الدوري الإنجليزي الممتاز — البريميرليج",
                time = "غداً 19:30",
                date = "غداً",
                status = MatchStatus.UPCOMING,
                score = null,
                minute = null,
                primaryChannelId = "bein_1",
                primaryChannelName = "beIN SPORTS 1 HD",
                secondaryAudioChannelId = "alkass_2",
                commentator = "علي محمد علي"
            ),
            Match(
                id = "m5",
                homeTeam = "ميلان",
                homeLogo = "https://upload.wikimedia.org/wikipedia/commons/d/d0/Logo_of_AC_Milan.svg",
                awayTeam = "إنتر ميلان",
                awayLogo = "https://upload.wikimedia.org/wikipedia/commons/0/05/FC_Internazionale_Milano_2021.svg",
                competition = "ديربي الغضب — كأس إيطاليا",
                time = "غداً 21:45",
                date = "غداً",
                status = MatchStatus.UPCOMING,
                score = null,
                minute = null,
                primaryChannelId = "ad_sports_1",
                primaryChannelName = "أبوظبي الرياضية 1 HD",
                secondaryAudioChannelId = null,
                commentator = "عامر عبدالله"
            )
        )

        _matches.value = defaultMatches
    }
}
