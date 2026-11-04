package com.example

import com.example.data.model.*
import com.example.player.externalAudioRetryDelayMs
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class IptvRepositoryTest {

    @Test
    fun vodCategoriesUseProviderNamesAndCountCatalogItems() {
        val categories = buildVodCategories(
            listOf(
                VodCategory("10", "أكشن"),
                VodCategory("20", "دراما", posterUrl = "https://example.com/drama.png")
            ),
            listOf("10", "10", "20", "30", "")
        )

        assertEquals(3, categories.size)
        assertEquals("أكشن", categories[0].name)
        assertEquals(2, categories[0].itemCount)
        assertEquals(1, categories[1].itemCount)
        assertEquals("https://example.com/drama.png", categories[1].posterUrl)
        assertEquals("30", categories[2].name)
        assertEquals(1, categories[2].itemCount)
    }

    @Test
    fun audioSourceClassifierSeparatesBeinAndAlwan() {
        assertEquals(AudioSourceCategory.BEIN_SPORTS, classifyAudioSource("beIN SPORTS 1 HD", "sports"))
        assertEquals(AudioSourceCategory.ALWAN, classifyAudioSource("ALWAN SPORT", "sports"))
        assertEquals(AudioSourceCategory.ALWAN, classifyAudioSource("ألوان 2", "عام"))
        assertNull(classifyAudioSource("الكأس 1 HD", "sports"))
    }

    @Test
    fun externalAudioRetryDelayUsesCappedBackoff() {
        assertEquals(1000L, externalAudioRetryDelayMs(1))
        assertEquals(2000L, externalAudioRetryDelayMs(2))
        assertEquals(4000L, externalAudioRetryDelayMs(3))
        assertEquals(8000L, externalAudioRetryDelayMs(4))
        assertEquals(16000L, externalAudioRetryDelayMs(5))
        assertEquals(30000L, externalAudioRetryDelayMs(6))
        assertEquals(30000L, externalAudioRetryDelayMs(20))
    }

    @Test
    fun testM3uParsingLogic() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="bein1" tvg-name="beIN SPORTS 1" tvg-logo="https://example.com/logo.png" group-title="باقة beIN",beIN SPORTS 1 HD
            http://example.com/live/bein1.m3u8
            #EXTINF:-1 tvg-id="alkass1" tvg-name="Alkass 1" tvg-logo="" group-title="قنوات الكأس",الكأس 1 HD
            http://example.com/live/alkass1.m3u8
        """.trimIndent()

        val reader = BufferedReader(StringReader(sampleM3u))
        var line: String? = reader.readLine()

        val parsedChannels = mutableListOf<Channel>()
        var currentName = ""
        var currentLogo = ""
        var currentGroup = "قنوات رياضية"
        var channelIndex = 1

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
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
                parsedChannels.add(
                    Channel(
                        id = "m3u_$channelIndex",
                        channelNumber = channelIndex,
                        name = currentName.ifBlank { "قناة $channelIndex" },
                        categoryId = currentGroup,
                        streamUrl = trimmed,
                        logoUrl = currentLogo
                    )
                )
                channelIndex++
                currentName = ""
                currentLogo = ""
            }
            line = reader.readLine()
        }

        assertEquals(2, parsedChannels.size)
        assertEquals("beIN SPORTS 1 HD", parsedChannels[0].name)
        assertEquals("http://example.com/live/bein1.m3u8", parsedChannels[0].streamUrl)
        assertEquals("الكأس 1 HD", parsedChannels[1].name)
    }

    @Test
    fun testAudioCommentarySourceMapping() {
        val channel = Channel(
            id = "alkass_1",
            channelNumber = 4,
            name = "الكأس 1 HD",
            categoryId = "alkass",
            streamUrl = "http://example.com/alkass.m3u8",
            currentProgram = "تعليق خليل البلوشي"
        )

        val audioSource = AudioCommentarySource(
            channelId = channel.id,
            channelName = channel.name,
            streamUrl = channel.streamUrl,
            commentatorName = channel.currentProgram
        )

        assertEquals("alkass_1", audioSource.channelId)
        assertEquals("الكأس 1 HD", audioSource.channelName)
        assertEquals("تعليق خليل البلوشي", audioSource.commentatorName)
    }

}
