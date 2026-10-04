package io.github.riaanjutte.cbonline.data

import java.util.Locale

/** Pilot names and squad tags match ignoring case and surrounding spaces; this is the form they're compared in. */
fun nameKey(name: String): String = name.trim().lowercase(Locale.ROOT)
