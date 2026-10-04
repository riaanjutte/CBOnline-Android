package io.github.riaanjutte.cbonline.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class PilotStatsApiTest {

    private val server = MockWebServer()
    private lateinit var api: PilotStatsApi
    private val requests = CopyOnWriteArrayList<RecordedRequest>()

    /** Responses by endpoint; the two requests run concurrently, so they're routed by path, not order. */
    private var lifetime: MockResponse = MockResponse().setBody(LIFETIME)
    private var pvp: MockResponse = MockResponse().setBody(PVP)

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return when (request.requestUrl?.encodedPath) {
                    "/api/LifetimeStats" -> lifetime
                    "/api/PvpStats" -> pvp
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
        api = PilotStatsApi(
            OkHttpClient(),
            Json { ignoreUnknownKeys = true; coerceInputValues = true },
            server.url("/").toString().trimEnd('/'),
            "CBOnline-Android/test"
        )
    }

    @After
    fun tearDown() = server.shutdown()

    private suspend fun found(name: String = "Hans"): PilotStats {
        val result = api.fetch(name)
        assertTrue("expected Found, got $result", result is StatsResult.Found)
        return (result as StatsResult.Found).stats
    }

    @Test
    fun `parses lifetime, top aircraft and PvP`() = runTest {
        assertEquals(
            PilotStats(
                nickname = "Hans",
                flightHours = 123.4,
                score = 98765,
                alliedSorties = 120,
                axisSorties = 412,
                airKills = 310,
                bestAirStreak = 14,
                groundKills = 75,
                bestGroundStreak = 9,
                deaths = 140,
                aircraftLost = 160,
                airKillsPerAircraftLost = 1.94,
                airKillsPerHour = 2.51,
                topAircraft = listOf(AircraftHours("bf 109 g-14", 60.5), AircraftHours("fw 190 a-8", 30.0)),
                vsPlayers = PvpRecord(victories = 190, defeats = 70, airToAirRatio = 2.7, aiVictories = 120)
            ),
            found()
        )
    }

    @Test
    fun `asks both endpoints for the name, encoded, with the user agent`() = runTest {
        found("=JG52= Hans&Co")
        assertEquals(setOf("/api/LifetimeStats", "/api/PvpStats"), requests.map { it.requestUrl?.encodedPath }.toSet())
        for (r in requests) {
            assertEquals("=JG52= Hans&Co", r.requestUrl?.queryParameter("name"))
            assertEquals("CBOnline-Android/test", r.getHeader("User-Agent"))
        }
    }

    @Test
    fun `unknown pilot is not found`() = runTest {
        lifetime = MockResponse().setBody("""{"message":"No lifetime stats found for name: 'X'.","name_searched":"X","suggestion":"..."}""")
        assertEquals(StatsResult.NotFound, api.fetch("X"))
    }

    @Test
    fun `an empty lifetime list is not found`() = runTest {
        lifetime = MockResponse().setBody("""{"LifetimeStats":[],"Top 3 aircraft":[]}""")
        assertEquals(StatsResult.NotFound, api.fetch("X"))
    }

    @Test
    fun `missing PvP stats leave the rest`() = runTest {
        pvp = MockResponse().setBody("""{"message":"No PvP stats found for player: 'Hans'.","suggestion":"..."}""")
        val stats = found()
        assertNull(stats.vsPlayers)
        assertEquals(310, stats.airKills)
    }

    @Test
    fun `a failing PvP request leaves the rest, marked incomplete`() = runTest {
        pvp = MockResponse().setResponseCode(500)
        val result = api.fetch("Hans") as StatsResult.Found
        assertNull(result.stats.vsPlayers)
        assertEquals(false, result.complete)
    }

    @Test
    fun `missing PvP stats are a complete answer`() = runTest {
        pvp = MockResponse().setBody("""{"message":"No PvP stats found for player: 'Hans'."}""")
        assertEquals(true, (api.fetch("Hans") as StatsResult.Found).complete)
    }

    @Test
    fun `a failing lifetime request throws`() = runTest {
        lifetime = MockResponse().setResponseCode(500)
        try {
            api.fetch("Hans")
            fail("expected an exception")
        } catch (e: Exception) {
            // the screen shows "Couldn't load stats" with Retry
        }
    }

    @Test
    fun `malformed lifetime payload throws`() = runTest {
        lifetime = MockResponse().setBody("<html>busy</html>")
        try {
            api.fetch("Hans")
            fail("expected an exception")
        } catch (e: Exception) {
            // as above
        }
    }

    @Test
    fun `missing numbers become zero and missing aircraft an empty list`() = runTest {
        lifetime = MockResponse().setBody("""{"LifetimeStats":[{"nickname":"Hans","total_air_kills":3}]}""")
        val stats = found()
        assertEquals(3, stats.airKills)
        assertEquals(0, stats.groundKills)
        assertEquals(0.0, stats.flightHours, 0.0)
        assertEquals(emptyList<AircraftHours>(), stats.topAircraft)
    }

    @Test
    fun `whole numbers sent as decimals are read`() = runTest {
        lifetime = MockResponse().setBody("""{"LifetimeStats":[{"nickname":"Hans","total_air_kills":12.0,"total_score":4567.6}]}""")
        val stats = found()
        assertEquals(12, stats.airKills)
        assertEquals(4568, stats.score)
    }

    private companion object {
        const val LIFETIME = """{
          "LifetimeStats":[{"nickname":"Hans","total_flight_time_hours":123.4,"red_sorties":120,"blue_sorties":412,"red_bias":0.23,
            "total_air_kills":310,"max_air_kill_streak":14,"total_ground_kills":75,"max_ground_kill_streak":9,
            "total_deaths_plus_captures":140,"lost_aircraft":160,"air_kills_per_lost_aircraft":1.94,"air_kills_per_hour":2.51,
            "ground_kills_per_lost_aircraft":0.47,"ground_kills_per_hour":0.61,"total_score":98765}],
          "Top 3 aircraft":[{"log_name":"bf 109 g-14","total_flight_time_hours":60.5},{"log_name":"fw 190 a-8","total_flight_time_hours":30.0}]
        }"""
        const val PVP = """{"total_victories":310,"pvp_victories":190,"ai_victories":120,"ratio_ai_victories":0.39,"pvp_defeats":70,
          "pvp_air_to_air_win_loss_ratio":2.7,"pvp_air_to_air_victories":180,"pvp_air_to_air_defeats":66}"""
    }
}
