package io.github.riaanjutte.cbonline.roster

import java.util.Locale

/** Manufacturer and design-bureau prefixes whose usual spelling isn't plain upper or title case. */
private val PREFIXES = mapOf(
    "bf" to "Bf", "fw" to "Fw", "he" to "He", "ju" to "Ju", "me" to "Me", "hs" to "Hs", "ar" to "Ar",
    "yak" to "Yak", "mig" to "MiG", "lagg" to "LaGG", "la" to "La", "il" to "Il", "pe" to "Pe", "li" to "Li",
    "mc" to "MC", "ki" to "Ki"
)

/** British marks: roman numerals upper, variant letters lower ("mk.ixe" → "Mk.IXe"). */
private val MARK = Regex("""^mk\.([ivx]+)([a-z]*)$""")

/** The stats feed names aircraft in lower case ("bf 109 g-14"); this restores the usual spelling ("Bf 109 G-14"). */
fun tidyAircraftName(raw: String): String =
    raw.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }.joinToString(" ") { tidyToken(it.lowercase(Locale.ROOT)) }

private fun tidyToken(token: String): String {
    MARK.matchEntire(token)?.let { match ->
        val (roman, variant) = match.destructured
        return "Mk.${roman.uppercase(Locale.ROOT)}$variant"
    }
    if (token.first().isDigit()) return token
    val dash = token.indexOf('-')
    if (dash > 0) {
        val prefix = token.substring(0, dash)
        return (PREFIXES[prefix] ?: prefix.uppercase(Locale.ROOT)) + token.substring(dash).uppercase(Locale.ROOT)
    }
    return PREFIXES[token] ?: token.replaceFirstChar { it.titlecase(Locale.ROOT) }
}
