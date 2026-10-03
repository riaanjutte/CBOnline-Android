package io.github.riaanjutte.cbonline.roster

private val TIME_ON_MISSION = Regex("""^(\d+):(\d{2})$""")

/** "00:27" → "27 min", "01:05" → "1 h 05 min"; anything unrecognised is returned unchanged. */
fun formatTimeOnMission(raw: String): String {
    val match = TIME_ON_MISSION.matchEntire(raw) ?: return raw
    val (hours, minutes) = match.destructured
    val h = hours.toInt()
    return if (h == 0) "${minutes.toInt()} min" else "$h h $minutes min"
}
