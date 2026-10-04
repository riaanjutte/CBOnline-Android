package io.github.riaanjutte.cbonline.ui

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Number formats for the stats panel, in the phone's locale. Halves round up (12.25 h → "12.3"), as people expect. */
class StatsFormat(locale: Locale = Locale.getDefault()) {

    private val wholeFormat = NumberFormat.getIntegerInstance(locale)
    private val oneDecimalFormat = decimals(locale, 1)
    private val twoDecimalsFormat = decimals(locale, 2)

    fun whole(n: Int): String = wholeFormat.format(n)
    fun oneDecimal(x: Double): String = oneDecimalFormat.format(x)
    fun twoDecimals(x: Double): String = twoDecimalsFormat.format(x)

    private fun decimals(locale: Locale, places: Int) = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = places
        maximumFractionDigits = places
        roundingMode = RoundingMode.HALF_UP
    }
}
