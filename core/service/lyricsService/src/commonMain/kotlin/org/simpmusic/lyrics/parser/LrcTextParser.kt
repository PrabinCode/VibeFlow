package org.simpmusic.lyrics.parser

import com.maxrave.domain.extension.decodeHtmlEntities
import org.simpmusic.lyrics.domain.Lyrics

private val offsetRegex = Regex("""\[offset:\s*([+-]?\d+)\s*\]""", RegexOption.IGNORE_CASE)
private val lrcTimestampRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:,](\d{1,3}))?\]""")

fun parseSyncedLyrics(data: String): Lyrics {
    val lines = data.lines()
    var fileOffset = 0L
    for (line in lines) {
        val trimmed = line.trim()
        val offsetMatch = offsetRegex.find(trimmed)
        if (offsetMatch != null) {
            fileOffset = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
            break
        }
    }

    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isBlank() ||
            trimmed.startsWith("[offset:", ignoreCase = true) ||
            trimmed.startsWith("[ar:", ignoreCase = true) ||
            trimmed.startsWith("[ti:", ignoreCase = true) ||
            trimmed.startsWith("[al:", ignoreCase = true) ||
            trimmed.startsWith("[by:", ignoreCase = true) ||
            trimmed.startsWith("[length:", ignoreCase = true)
        ) {
            continue
        }

        val timestampMatches = lrcTimestampRegex.findAll(trimmed).toList()
        if (timestampMatches.isNotEmpty()) {
            val lastMatch = timestampMatches.last()
            val rawContent = trimmed.substring(lastMatch.range.last + 1).trimStart()
            val content = if (rawContent.isBlank()) "♫" else rawContent
            val decodedWords = decodeHtmlEntities(content)

            for (match in timestampMatches) {
                val timeMs =
                    parseLrcTimestamp(
                        match.groupValues[1],
                        match.groupValues[2],
                        match.groupValues.getOrNull(3),
                    )
                val finalTimeMs = (timeMs + fileOffset).coerceAtLeast(0L)
                linesLyrics.add(
                    Lyrics.LyricsX.Line(
                        endTimeMs = "0",
                        startTimeMs = finalTimeMs.toString(),
                        syllables = listOf(),
                        words = decodedWords,
                    ),
                )
            }
        }
    }

    linesLyrics.sortBy { it.startTimeMs.toLongOrNull() ?: 0L }

    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = "LINE_SYNCED",
            ),
    )
}

fun parseRichSyncLyrics(data: String): Lyrics {
    // Unescape JSON string if needed (remove quotes and replace \n with actual newlines)
    val unescapedData =
        data
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")

    // Handle different line separators (Unix \n, Windows \r\n, Mac \r)
    val lines = unescapedData.lines()
    var fileOffset = 0L
    for (line in lines) {
        val trimmed = line.trim()
        val offsetMatch = offsetRegex.find(trimmed)
        if (offsetMatch != null) {
            fileOffset = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
            break
        }
    }

    val lyricsLines =
        lines.filter { line ->
            val trimmed = line.trim()
            trimmed.isNotBlank() && !trimmed.startsWith("[offset:", ignoreCase = true)
        }

    // Regex to match [MM:SS.mm] format (flexible with 1-3 digit minutes/fractions)
    val lineRegex = Regex("""^\[(\d{1,3}):(\d{2})(?:[.:,](\d{1,3}))?\](.*)$""")
    // Strip a leading voice marker (e.g. "v1:", "v2:") that some lyrics
    // providers prepend before the first word timestamp, so it doesn't leak
    // into the word content and stick to the first word.
    val voiceMarkerRegex = Regex("""^v\d+:""")
    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()

    lyricsLines.forEachIndexed { index, line ->
        val matchResult = lineRegex.matchEntire(line.trim())
        if (matchResult != null) {
            val timeInMillis =
                parseLrcTimestamp(
                    matchResult.groupValues[1],
                    matchResult.groupValues[2],
                    matchResult.groupValues.getOrNull(3),
                )
            val finalTimeMs = (timeInMillis + fileOffset).coerceAtLeast(0L)

            // Keep the rich sync content as-is (with <MM:SS.mm> word format)
            val content =
                matchResult.groupValues[4]
                    .trimStart()
                    .replace(voiceMarkerRegex, "")
                    .trimStart()

            if (content.isNotBlank()) {
                linesLyrics.add(
                    Lyrics.LyricsX.Line(
                        endTimeMs = "0",
                        startTimeMs = finalTimeMs.toString(),
                        syllables = listOf(),
                        words = content,
                    ),
                )
            }
        }
    }

    linesLyrics.sortBy { it.startTimeMs.toLongOrNull() ?: 0L }

    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = "RICH_SYNCED",
            ),
    )
}

private fun parseLrcTimestamp(
    minutesStr: String,
    secondsStr: String,
    fractionStr: String?,
): Long {
    val minutes = minutesStr.toLongOrNull() ?: 0L
    val seconds = secondsStr.toLongOrNull() ?: 0L
    val millis = parseMillisPart(fractionStr)
    return minutes * 60_000L + seconds * 1000L + millis
}

/**
 * Parse TTML (Timed Text Markup Language) lyrics from BetterLyrics.
 * Supports both line-synced and word-by-word synced lyrics.
 *
 * TTML format: `<p begin="M:SS.mmm" end="M:SS.mmm">` contains `<span begin="..." end="...">word</span>`
 * If spans with timing exist → word-by-word (RICH_SYNCED)
 * If no spans → line-synced (LINE_SYNCED)
 */
fun parseTtmlLyrics(data: String): Lyrics {
    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()
    var hasWordTiming = false

    // Match each <p ...>...</p> element (use [\s\S] instead of . with DOT_MATCHES_ALL)
    val pRegex = Regex("""<p\s[^>]*begin="([^"]+)"[^>]*end="([^"]+)"[^>]*>([\s\S]*?)</p>""")
    // Match each <span ...>word</span> element
    val spanRegex = Regex("""<span\s[^>]*begin="([^"]+)"[^>]*end="([^"]+)"[^>]*>(.*?)</span>""")

    for (pMatch in pRegex.findAll(data)) {
        val lineBegin = parseTtmlTime(pMatch.groupValues[1])
        val lineEnd = parseTtmlTime(pMatch.groupValues[2])
        val innerContent = pMatch.groupValues[3]

        val spans = spanRegex.findAll(innerContent).toList()

        if (spans.isNotEmpty()) {
            hasWordTiming = true
            // Build word-by-word content with <MM:SS.mm> timing format for rich sync
            val wordParts = StringBuilder()
            for (span in spans) {
                val spanBegin = parseTtmlTime(span.groupValues[1])
                val word = span.groupValues[3].trim()
                if (word.isNotEmpty()) {
                    val beginFormatted = formatMsToLrc(spanBegin)
                    wordParts.append("<$beginFormatted>$word ")
                }
            }
            val words = wordParts.toString().trimEnd()
            if (words.isNotBlank()) {
                linesLyrics.add(
                    Lyrics.LyricsX.Line(
                        startTimeMs = lineBegin.toString(),
                        endTimeMs = lineEnd.toString(),
                        syllables = listOf(),
                        words = words,
                    ),
                )
            }
        } else {
            // No spans — extract plain text (strip any remaining tags)
            val plainText = innerContent.replace(Regex("<[^>]*>"), "").trim()
            if (plainText.isNotBlank()) {
                linesLyrics.add(
                    Lyrics.LyricsX.Line(
                        startTimeMs = lineBegin.toString(),
                        endTimeMs = lineEnd.toString(),
                        syllables = listOf(),
                        words = plainText,
                    ),
                )
            }
        }
    }

    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = if (hasWordTiming) "RICH_SYNCED" else "LINE_SYNCED",
            ),
    )
}

/**
 * Parse TTML time format to milliseconds.
 * Supports: "M:SS.mmm", "MM:SS.mmm", "H:MM:SS.mmm", "SS.mmm"
 */
private fun parseTtmlTime(time: String): Long {
    val parts = time.split(":")
    return when (parts.size) {
        3 -> {
            val hours = parts[0].toLongOrNull() ?: 0L
            val minutes = parts[1].toLongOrNull() ?: 0L
            val secParts = parts[2].split(".")
            val seconds = secParts[0].toLongOrNull() ?: 0L
            val millis = parseMillisPart(secParts.getOrNull(1))
            hours * 3_600_000L + minutes * 60_000L + seconds * 1000L + millis
        }
        2 -> {
            val minutes = parts[0].toLongOrNull() ?: 0L
            val secParts = parts[1].split(".")
            val seconds = secParts[0].toLongOrNull() ?: 0L
            val millis = parseMillisPart(secParts.getOrNull(1))
            minutes * 60_000L + seconds * 1000L + millis
        }
        else -> {
            val secParts = time.split(".")
            val seconds = secParts[0].toLongOrNull() ?: 0L
            val millis = parseMillisPart(secParts.getOrNull(1))
            seconds * 1000L + millis
        }
    }
}

private fun parseMillisPart(part: String?): Long {
    if (part.isNullOrEmpty()) return 0L
    val value = part.toLongOrNull() ?: return 0L
    return when (part.length) {
        1 -> value * 100
        2 -> value * 10
        3 -> value
        else -> value
    }
}

private fun formatMsToLrc(ms: Long): String {
    val minutes = ms / 60_000L
    val seconds = (ms % 60_000L) / 1000L
    val centis = (ms % 1000L) / 10L
    val m = if (minutes < 10) "0$minutes" else "$minutes"
    val s = if (seconds < 10) "0$seconds" else "$seconds"
    val c = if (centis < 10) "0$centis" else "$centis"
    return "$m:$s.$c"
}

fun parseUnsyncedLyrics(data: String): Lyrics {
    val lines = data.lines()
    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()
    lines.map { line ->
        linesLyrics.add(
            Lyrics.LyricsX.Line(
                endTimeMs = "0",
                startTimeMs = "0",
                syllables = listOf(),
                words = line,
            ),
        )
    }
    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = "UNSYNCED",
            ),
    )
}