package io.github.riaanjutte.cbonline.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UpdateCheckerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val server = MockWebServer()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var checker: UpdateChecker

    @Before
    fun setUp() {
        server.start()
        checker = UpdateChecker(
            OkHttpClient(),
            Json { ignoreUnknownKeys = true; coerceInputValues = true },
            PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") },
            "CBOnline-Android/test",
            server.url("/releases/latest").toString()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        scope.cancel()
    }

    // Trimmed real GitHub shape: the fields we read plus some we must ignore
    private fun release(tag: String) = MockResponse().setBody(
        """{"id":1,"tag_name":"$tag","name":"CB Online $tag","draft":false,"prerelease":false,
           "html_url":"https://github.com/riaanjutte/CBOnline-Android/releases/tag/$tag",
           "assets":[{"name":"app-release.apk","size":2500000}]}"""
    )

    @Test
    fun `newer release is reported`() = runTest {
        server.enqueue(release("v1.1.0"))
        assertEquals(
            UpdateInfo("1.1.0", "https://github.com/riaanjutte/CBOnline-Android/releases/tag/v1.1.0"),
            checker.check("1.0.0")
        )
        val req = server.takeRequest()
        assertEquals("application/vnd.github+json", req.getHeader("Accept"))
        assertEquals("CBOnline-Android/test", req.getHeader("User-Agent"))
    }

    @Test
    fun `same or older release is ignored`() = runTest {
        server.enqueue(release("v1.0.0"))
        assertNull(checker.check("1.0.0"))
        server.enqueue(release("v0.9.0"))
        assertNull(checker.check("1.0.0"))
    }

    @Test
    fun `dismissed version is hidden but newer shows`() = runTest {
        checker.dismiss("1.1.0")
        server.enqueue(release("v1.1.0"))
        assertNull(checker.check("1.0.0"))
        server.enqueue(release("v1.2.0"))
        assertEquals("1.2.0", checker.check("1.0.0")?.version)
    }

    @Test
    fun `404 and malformed body return null`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertNull(checker.check("1.0.0"))
        server.enqueue(MockResponse().setBody("not json"))
        assertNull(checker.check("1.0.0"))
    }
}
