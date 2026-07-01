package moe.ouom.neriplayer.ui.viewmodel.playlist

import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class NeteasePodcastProgramParserTest {
    @Test
    fun parseNeteasePodcastPrograms_usesMainSongIdAsPlayableId() {
        val raw = """
            {
              "code": 200,
              "programs": [
                {
                  "id": 9001,
                  "name": "节目标题",
                  "coverUrl": "http://p1.music.126.net/program.jpg",
                  "mainSong": {
                    "id": 123456,
                    "duration": 300000
                  }
                }
              ]
            }
        """.trimIndent()

        val tracks = parseNeteasePodcastPrograms(
            raw,
            PlaylistSummary(
                id = 88,
                name = "收藏播客",
                picUrl = "http://p1.music.126.net/radio.jpg",
                playCount = 0,
                trackCount = 1,
                creatorName = "主播"
            )
        )

        assertEquals(1, tracks.size)
        assertEquals(123456L, tracks[0].id)
        assertEquals("123456", tracks[0].audioId)
        assertEquals("9001", tracks[0].subAudioId)
        assertEquals("88", tracks[0].playlistContextId)
    }
}
