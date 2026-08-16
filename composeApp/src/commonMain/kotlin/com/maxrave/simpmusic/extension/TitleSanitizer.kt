package com.maxrave.simpmusic.extension

/**
 * Utility for sanitizing YouTube Music and video titles by removing noise tags,
 * resolution badges, and redundant artist prefixes.
 */
object TitleSanitizer {

    // Matches noisy tags inside brackets [...] or parentheses (...)
    private val BRACKETED_NOISE_REGEX = Regex(
        "(?i)[\\(\\[]\\s*(?:" +
            "official\\s+(?:music\\s+)?video|" +
            "official\\s+(?:lyric|lyrics)\\s+video|" +
            "official\\s+audio|" +
            "official\\s+visualizer|" +
            "official\\s+hd\\s+video|" +
            "official\\s+4k\\s+video|" +
            "official\\s+video|" +
            "lyric\\s+video|" +
            "lyrics\\s+video|" +
            "lyrics|" +
            "lyric|" +
            "visualizer|" +
            "music\\s+video|" +
            "audio|" +
            "hd\\s+video|" +
            "4k\\s+video|" +
            "4k\\s*60fps|" +
            "1080p|" +
            "720p|" +
            "hd|" +
            "4k|" +
            "hq|" +
            "mv|" +
            "m/v|" +
            "clip\\s+officiel|" +
            "video\\s+oficial|" +
            "video\\s+clip|" +
            "full\\s+audio|" +
            "prod\\.\\s*by[^)\\]]*|" +
            "produced\\s+by[^)\\]]*" +
            ")\\s*[\\)\\]]",
    )

    // Matches trailing noise like "| Official Video", "- Official Music Video", "// Official Audio"
    private val TRAILING_NOISE_REGEX = Regex(
        "(?i)\\s*(?:[|/\\\\-]|–|—)\\s*(?:" +
            "official\\s+(?:music\\s+)?video|" +
            "official\\s+audio|" +
            "official\\s+visualizer|" +
            "lyric\\s+video|" +
            "lyrics\\s+video|" +
            "lyrics|" +
            "visualizer|" +
            "music\\s+video|" +
            "mv|" +
            "m/v|" +
            "clip\\s+officiel" +
            ")\\s*$",
    )

    // Matches multiple whitespace
    private val MULTI_SPACE_REGEX = Regex("\\s{2,}")

    /**
     * Sanitizes a song title by stripping common YouTube video descriptors.
     *
     * @param title The raw title from YouTube Music.
     * @param artistName Optional known artist name to strip redundant "Artist - Title" prefix.
     * @return Cleaned, human-readable song title.
     */
    fun cleanTitle(title: String, artistName: String? = null): String {
        if (title.isBlank()) return title

        var result = title

        // 1. If title starts with "Artist Name - ", strip it if artist is known
        if (!artistName.isNullOrBlank()) {
            val normalizedArtist = artistName.trim().lowercase()
            val lowerResult = result.lowercase()
            if (lowerResult.startsWith("$normalizedArtist - ") ||
                lowerResult.startsWith("$normalizedArtist – ") ||
                lowerResult.startsWith("$normalizedArtist — ")
            ) {
                val separatorIndex = result.indexOfAny(charArrayOf('-', '–', '—'))
                if (separatorIndex != -1 && separatorIndex < result.length - 1) {
                    result = result.substring(separatorIndex + 1).trim()
                }
            }
        }

        // 2. Remove bracketed / parenthesized noise tags
        result = BRACKETED_NOISE_REGEX.replace(result, " ")

        // 3. Remove trailing pipe/dash noise
        result = TRAILING_NOISE_REGEX.replace(result, "")

        // 4. Clean up remaining orphan punctuation and multiple whitespace
        result = result
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
            .trimEnd('-', '–', '—', '|', '/', '\\', ':', '.')
            .trim()

        // Fallback: If stripping completely emptied the title, return the original
        return if (result.isNotBlank()) result else title.trim()
    }
}

/**
 * Extension function on [String] for convenient title sanitization.
 */
fun String.sanitizeTrackTitle(artistName: String? = null): String {
    return TitleSanitizer.cleanTitle(this, artistName)
}
