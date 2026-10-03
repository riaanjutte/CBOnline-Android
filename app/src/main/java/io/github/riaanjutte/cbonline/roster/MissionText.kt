package io.github.riaanjutte.cbonline.roster

import io.github.riaanjutte.cbonline.data.Weather
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IN_GAME_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

/** "2 h 47 min left" … "Changing mission…" (up to 10 min past the end) … "Mission info may be out of date". */
fun timeLeftLabel(estimatedEnd: Instant, now: Instant): String {
    val s = Duration.between(now, estimatedEnd).seconds
    return when {
        s >= 3600 -> String.format(Locale.ROOT, "%d h %02d min left", s / 3600, s % 3600 / 60)
        s >= 60 -> "${s / 60} min left"
        s > 0 -> "Less than a minute left"
        s >= -600 -> "Changing mission…"
        else -> "Mission info may be out of date"
    }
}

/** "1 °C · Heavy cloud 1,900–4,900 m · Wind 160° 3 m/s", omitting whatever the feed didn't provide. */
fun weatherLine(weather: Weather): String {
    val parts = mutableListOf<String>()
    weather.temperatureC?.let { parts += "${number(it)} °C" }
    parts += cloudPart(weather)
    weather.surfaceWind?.let { parts += "Wind ${it.fromDeg}° ${number(it.speedMs)} m/s" }
    if (weather.precipLevel > 0) {
        parts += weather.precipType?.trim()?.takeIf { it.isNotEmpty() }
            ?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "Precipitation"
    }
    return parts.joinToString(" · ")
}

/** In-game date/time, e.g. "2 Dec 1944, 14:00". */
fun inGameLabel(start: LocalDateTime): String = start.format(IN_GAME_FORMAT)

private fun cloudPart(weather: Weather): String {
    val cover = weather.cloudCover?.trim()
    if (cover.isNullOrEmpty() || cover.equals("clear", ignoreCase = true)) return "Clear sky"
    val base = weather.cloudBaseM
    val top = weather.cloudTopM
    val heights = if (base != null && top != null) " ${grouped(base)}–${grouped(top)} m" else ""
    return "$cover cloud$heights"
}

// Locale.ROOT so the phone's language can't turn "1,900" into "1.900"
private fun number(x: Double) =
    if (x % 1.0 == 0.0) String.format(Locale.ROOT, "%d", x.toLong()) else String.format(Locale.ROOT, "%.1f", x)

private fun grouped(n: Int) = String.format(Locale.ROOT, "%,d", n)
