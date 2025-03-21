package app.ui.pioneer

private val glyphMapper = mapOf(
    "U+25CF" to "-"
)

fun extractMissingGlyph(t: Throwable) = if (t is IllegalArgumentException) t.message?.let {
    it.substringAfter("No glyph for ")
        .substringBefore(" ")
} else null

fun getMissingGlyphReplacement(glyph: String) = glyphMapper[glyph]