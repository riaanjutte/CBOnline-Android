package io.github.riaanjutte.cbonline.roster

/** Common squad tag styles, tried in order: enclosed at the start, enclosed at the end, then "TAG_" prefixes. */
private val TAG_PATTERNS = listOf(
    Regex("""^\[[^\]]+\]"""), Regex("""^=[^=]+="""), Regex("""^\|[^|]+\|"""), Regex("""^\([^)]+\)"""), Regex("""^-[^-]+-"""),
    Regex("""\[[^\]]+\]$"""), Regex("""=[^=]+=$"""), Regex("""\|[^|]+\|$"""), Regex("""\([^)]+\)$"""),
    Regex("""^[A-Za-z0-9./-]{2,10}_""")
)

/**
 * A likely squad tag in [nickname] ("=JG52=Hans" → "=JG52="), or null. Only a suggestion: the player confirms
 * or edits it before it's starred, because tag styles vary too much to guess reliably.
 */
fun suggestSquadTag(nickname: String): String? {
    val name = nickname.trim()
    val tag = TAG_PATTERNS.firstNotNullOfOrNull { it.find(name)?.value } ?: return null
    return tag.takeIf { it.length < name.length && isValidSquadTag(it) }
}

/** Shorter tags would match far too many names. */
fun isValidSquadTag(tag: String): Boolean = tag.trim().length >= 2

/** A pilot belongs to a starred squad when their name contains its tag, ignoring case. */
fun isSquadMember(nickname: String, tag: String): Boolean {
    val t = tag.trim()
    return isValidSquadTag(t) && nickname.contains(t, ignoreCase = true)
}
