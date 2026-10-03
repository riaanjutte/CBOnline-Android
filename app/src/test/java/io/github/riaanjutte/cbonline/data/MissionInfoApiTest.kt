package io.github.riaanjutte.cbonline.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime

class MissionInfoApiTest {

    private val server = MockWebServer()
    private lateinit var api: MissionInfoApi

    @Before
    fun setUp() {
        server.start()
        api = MissionInfoApi(
            OkHttpClient(),
            Json { ignoreUnknownKeys = true; coerceInputValues = true },
            server.url("/mission-info-tempest.json").toString(),
            "CBOnline-Android/test"
        )
    }

    @After
    fun tearDown() = server.shutdown()

    private fun respond(body: String) = server.enqueue(MockResponse().setBody(body))

    // Minimal valid file: a current mission and whatever extra top-level parts a test needs
    private fun file(vararg parts: String, mission: String = """{"name":"M","estimated_end":"2026-10-04T00:42:00Z"}""") =
        "{" + (listOf(""""mission":$mission""") + parts).joinToString(",") + "}"

    private fun weather(extra: String = "", winds: String = """[{"altitude_m":0,"direction_from_deg":210,"speed_ms":3}]""") =
        """"weather":{"temperature_c":14,"cloud_cover":"Medium","cloud_base_m":2800,"cloud_top_m":4300,"precip_level":0,"wind_layers":$winds$extra}"""

    @Test
    fun `parses the real file`() = runTest {
        respond(javaClass.getResource("/mission-info-tempest.json")!!.readText())
        assertEquals(
            MissionInfo(
                "Crimean Resolve (Dec. 1944)", LocalDateTime.of(1944, 12, 2, 14, 0), Instant.parse("2026-10-04T00:42:00Z"),
                Weather(1.0, "Heavy", 1900, 4900, 0.0, null, Wind(160, 3.0)),
                NextMission(
                    "Mitchell's Men (Mar. 1945)", Instant.parse("2026-10-04T00:42:00Z"), LocalDateTime.of(1945, 3, 5, 9, 0),
                    Weather(-15.0, "Heavy", 3500, 9500, 0.0, null, Wind(280, 4.0))
                )
            ),
            api.fetch()
        )
        val req = server.takeRequest()
        assertEquals("/mission-info-tempest.json", req.path)
        assertEquals("CBOnline-Android/test", req.getHeader("User-Agent"))
    }

    @Test
    fun `surface wind is the lowest layer even when unordered`() = runTest {
        respond(file(weather(winds = """[{"altitude_m":500,"direction_from_deg":150,"speed_ms":4},{"altitude_m":0,"direction_from_deg":210,"speed_ms":3},{"altitude_m":1000,"direction_from_deg":140,"speed_ms":5}]""")))
        assertEquals(Wind(210, 3.0), api.fetch().weather?.surfaceWind)
    }

    @Test
    fun `missing weather and next mission give null`() = runTest {
        respond(file())
        val info = api.fetch()
        assertNull(info.weather)
        assertNull(info.next)
    }

    @Test
    fun `null or incomplete next mission gives null`() = runTest {
        respond(file(""""next_mission":null"""))
        val explicitNull = api.fetch()
        assertNull(explicitNull.next)
        assertEquals("M", explicitNull.name)

        respond(file(""""next_mission":{"name":"N","historical_start":"1945-03-05T09:00:00"}"""))
        val noStart = api.fetch()
        assertNull(noStart.next)
        assertEquals("M", noStart.name)
    }

    @Test
    fun `precipitation is read`() = runTest {
        respond(file(weather(extra = ""","precip_type":"snow"""").replace(""""precip_level":0""", """"precip_level":0.5""")))
        val w = api.fetch().weather!!
        assertEquals(0.5, w.precipLevel, 0.0)
        assertEquals("snow", w.precipType)
    }

    @Test
    fun `empty wind layers give no surface wind`() = runTest {
        respond(file(weather(winds = "[]")))
        val w = api.fetch().weather
        assertNotNull(w)
        assertNull(w!!.surfaceWind)
    }

    @Test
    fun `bad historical start gives null`() = runTest {
        respond(file(mission = """{"name":"M","historical_start":"soon","estimated_end":"2026-10-04T00:42:00Z"}"""))
        val info = api.fetch()
        assertNull(info.historicalStart)
        assertEquals(Instant.parse("2026-10-04T00:42:00Z"), info.estimatedEnd)
    }

    @Test
    fun `decimal numbers are accepted and rounded`() = runTest {
        respond(
            file(
                """"weather":{"temperature_c":14,"cloud_cover":"Medium","cloud_base_m":2800.0,"cloud_top_m":4300.4,"precip_level":0,
                   "wind_layers":[{"altitude_m":0.0,"direction_from_deg":210.5,"speed_ms":3.5}]}"""
            )
        )
        val w = api.fetch().weather!!
        assertEquals(2800, w.cloudBaseM)
        assertEquals(4300, w.cloudTopM)
        assertEquals(Wind(211, 3.5), w.surfaceWind)
    }

    @Test
    fun `missing mission throws`() = runTest {
        respond("""{"server":"tempest"}""")
        assertNotNull(runCatching { api.fetch() }.exceptionOrNull())
    }

    @Test
    fun `bad estimated end throws`() = runTest {
        respond(file(mission = """{"name":"M","estimated_end":"later"}"""))
        assertNotNull(runCatching { api.fetch() }.exceptionOrNull())
    }

    @Test
    fun `http error throws IOException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val error = runCatching { api.fetch() }.exceptionOrNull()
        assertTrue("expected IOException, got $error", error is IOException)
    }

    @Test
    fun `html body throws`() = runTest {
        respond("<html>Just a moment...</html>")
        assertNotNull(runCatching { api.fetch() }.exceptionOrNull())
    }
}
