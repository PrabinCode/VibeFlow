package org.simpmusic.lyrics.am

import kotlinx.serialization.Serializable

/**
 * Response shape for the AM catalog search (format[resources]=map): a flat [resources] map keyed
 * by id, plus a [results] block that preserves the search ranking order.
 */
@Serializable
data class AMSearchResponse(
    val results: AMResults? = null,
    val resources: AMResources? = null,
)

@Serializable
data class AMResults(
    val artists: AMRefList? = null,
)

@Serializable
data class AMRefList(
    val data: List<AMRef> = emptyList(),
)

@Serializable
data class AMRef(
    val id: String,
)

@Serializable
data class AMResources(
    val artists: Map<String, AMArtistResource> = emptyMap(),
)

@Serializable
data class AMArtistResource(
    val id: String,
    val attributes: AMArtistAttributes? = null,
)

@Serializable
data class AMArtistAttributes(
    val name: String? = null,
    val url: String? = null,
    // Hex string (no `#`); present on the artist-detail endpoint.
    val keyColor: String? = null,
    val artwork: AMArtwork? = null,
    // Present on the artist-detail endpoint (extend=editorialArtwork); null on plain search.
    val editorialArtwork: AMEditorialArtwork? = null,
)

/**
 * Editorial art variants. [musicContentColorLogoTrimmed] is the artist NAME rendered as a trimmed
 * color logo image (PNG) — the header title art shown in place of plain text.
 */
@Serializable
data class AMEditorialArtwork(
    val musicContentColorLogoTrimmed: AMArtwork? = null,
    val subscriptionHero: AMArtwork? = null,
    val bannerUber: AMArtwork? = null,
)

/**
 * Artwork descriptor. [url] is a template containing `{w}`, `{h}` and `{f}` placeholders that must
 * be substituted before requesting the actual image. The `*Color` fields are hex strings (no `#`).
 */
@Serializable
data class AMArtwork(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val bgColor: String? = null,
    val textColor1: String? = null,
    val textColor2: String? = null,
    val textColor3: String? = null,
    val textColor4: String? = null,
)

/**
 * Substitute the `{w}`, `{h}`, `{c}` (crop) and `{f}` (format) placeholders in [AMArtwork.url].
 * Returns null when there is no url template.
 */
fun AMArtwork.toImageUrl(
    width: Int,
    height: Int,
    format: String = "png",
    crop: String = "",
): String? =
    url
        ?.replace("{w}", width.toString())
        ?.replace("{h}", height.toString())
        ?.replace("{c}", crop)
        ?.replace("{f}", format)
