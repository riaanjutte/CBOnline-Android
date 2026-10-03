package io.github.riaanjutte.cbonline.data

/**
 * True if [candidate] is a strictly higher dotted-numeric version than [current]
 * ("1.0.10" > "1.0.9", "1.0" == "1.0.0"). Any non-numeric segment means "not newer".
 */
fun isNewer(candidate: String, current: String): Boolean {
    val a = candidate.split(".")
    val b = current.split(".")
    if ((a + b).any { it.isEmpty() || !it.all(Char::isDigit) }) return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrNull(i)?.toInt() ?: 0
        val y = b.getOrNull(i)?.toInt() ?: 0
        if (x != y) return x > y
    }
    return false
}
