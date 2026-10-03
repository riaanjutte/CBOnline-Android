package io.github.riaanjutte.cbonline.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.cancellation.CancellationException

data class UpdateInfo(val version: String, val url: String)

interface UpdateSource {
    suspend fun check(currentVersion: String): UpdateInfo?
    suspend fun dismiss(version: String)
}

/** Looks for a newer GitHub release. Any failure means "no update". */
class UpdateChecker(
    private val client: OkHttpClient,
    private val json: Json,
    private val dataStore: DataStore<Preferences>,
    private val userAgent: String,
    private val releasesUrl: String = "https://api.github.com/repos/riaanjutte/CBOnline-Android/releases/latest"
) : UpdateSource {

    override suspend fun check(currentVersion: String): UpdateInfo? = try {
        val release = fetchLatest()
        val version = release?.tagName?.removePrefix("v")?.removePrefix("V")
        val dismissed = dataStore.data.first()[DISMISSED]
        if (release != null && version != null && isNewer(version, currentVersion) && version != dismissed) {
            UpdateInfo(version, release.htmlUrl)
        } else {
            null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    override suspend fun dismiss(version: String) {
        dataStore.edit { it[DISMISSED] = version }
    }

    private suspend fun fetchLatest(): ReleaseDto? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(releasesUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", userAgent)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            json.decodeFromString<ReleaseDto>(response.body?.string().orEmpty())
        }
    }

    @Serializable
    private data class ReleaseDto(
        @SerialName("tag_name") val tagName: String,
        @SerialName("html_url") val htmlUrl: String
    )

    private companion object {
        val DISMISSED = stringPreferencesKey("dismissed_update_version")
    }
}
