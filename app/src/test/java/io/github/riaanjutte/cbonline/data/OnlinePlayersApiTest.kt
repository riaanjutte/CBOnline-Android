package io.github.riaanjutte.cbonline.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OnlinePlayersApiTest {

    private val server = MockWebServer()
    private lateinit var api: OnlinePlayersApi

    @Before
    fun setUp() {
        server.start()
        api = OnlinePlayersApi(
            OkHttpClient(),
            Json { ignoreUnknownKeys = true; coerceInputValues = true },
            server.url("/").toString().trimEnd('/'),
            "CBOnline-Android/test"
        )
    }

    @After
    fun tearDown() = server.shutdown()

    private fun respond(body: String) = server.enqueue(MockResponse().setBody(body))

    @Test
    fun `parses real-shape payload and maps coalitions`() = runTest {
        respond(
            """[
              {"nickname":"Alpha","coalition":1,"date":"2026-10-03T19:11:18.78Z","timeOnMission":"00:12"},
              {"nickname":"Bravo","coalition":2,"date":"2026-10-03T19:03:07.19Z","timeOnMission":"01:05"},
              {"nickname":"Charlie","coalition":0,"timeOnMission":"00:02"},
              {"nickname":"Delta","coalition":99,"timeOnMission":"00:01"},
              {"nickname":"Echo","timeOnMission":"00:03","extra":"ignored"}]"""
        )
        assertEquals(
            listOf(
                OnlinePlayer("Alpha", Coalition.Allied, "00:12"),
                OnlinePlayer("Bravo", Coalition.Axis, "01:05"),
                OnlinePlayer("Charlie", Coalition.Unassigned, "00:02"),
                OnlinePlayer("Delta", Coalition.Unassigned, "00:01"),
                OnlinePlayer("Echo", Coalition.Unassigned, "00:03")
            ),
            api.fetch()
        )
        val req = server.takeRequest()
        assertEquals("/api/OnlinePlayers", req.path)
        assertEquals("CBOnline-Android/test", req.getHeader("User-Agent"))
    }

    @Test
    fun `drops blank or missing nicknames`() = runTest {
        respond("""[{"nickname":"  ","coalition":1},{"coalition":2},{"nickname":"Fox","coalition":2,"timeOnMission":"00:05"}]""")
        assertEquals(listOf(OnlinePlayer("Fox", Coalition.Axis, "00:05")), api.fetch())
    }

    @Test
    fun `trims nicknames`() = runTest {
        respond("""[{"nickname":" Bob ","coalition":1,"timeOnMission":"00:01"}]""")
        assertEquals("Bob", api.fetch().single().nickname)
    }

    @Test
    fun `missing timeOnMission becomes empty`() = runTest {
        respond("""[{"nickname":"Bob","coalition":1}]""")
        assertEquals("", api.fetch().single().timeOnMission)
    }

    @Test
    fun `empty array returns empty list`() = runTest {
        respond("[]")
        assertEquals(emptyList<OnlinePlayer>(), api.fetch())
    }

    @Test
    fun `http error throws IOException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val error = runCatching { api.fetch() }.exceptionOrNull()
        assertTrue("expected IOException, got $error", error is IOException)
    }

    @Test
    fun `html body with 200 throws`() = runTest {
        respond("<html>Sign in to Wi-Fi</html>")
        assertNotNull(runCatching { api.fetch() }.exceptionOrNull())
    }

    @Test
    fun `json object instead of array throws`() = runTest {
        respond("""{"players":[]}""")
        assertNotNull(runCatching { api.fetch() }.exceptionOrNull())
    }
}
