package moe.ouom.neriplayer.ui.viewmodel.tab

import org.junit.Assert.assertEquals
import org.junit.Test

class NeteasePodcastParserTest {
    @Test
    fun parseNeteasePodcasts_readsSubscribedDjRadios() {
        val raw = """
            {
              "code": 200,
              "djRadios": [
                {
                  "id": 123,
                  "name": "声动早咖啡",
                  "picUrl": "http://p1.music.126.net/podcast.jpg",
                  "subCount": 456,
                  "programCount": 78
                }
              ]
            }
        """.trimIndent()

        val podcasts = parseNeteasePodcasts(raw)

        assertEquals(1, podcasts.size)
        assertEquals(123L, podcasts[0].id)
        assertEquals("声动早咖啡", podcasts[0].name)
        assertEquals("https://p1.music.126.net/podcast.jpg", podcasts[0].picUrl)
        assertEquals(456L, podcasts[0].playCount)
        assertEquals(78, podcasts[0].trackCount)
    }
}
