package com.maxrave.domain.manager

import com.maxrave.domain.data.model.network.ProxyConfiguration
import com.maxrave.domain.data.player.ReverbPreset
import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    val appVersion: Flow<String>

    suspend fun setAppVersion(version: String)

    val openAppTime: Flow<Int>

    suspend fun openApp()

    suspend fun resetOpenAppTime()

    suspend fun doneOpenAppTime()

    val location: Flow<String>

    suspend fun setLocation(location: String)

    val quality: Flow<String>

    suspend fun setQuality(quality: String)

    val downloadQuality: Flow<String>

    suspend fun setDownloadQuality(quality: String)

    val videoDownloadQuality: Flow<String>

    suspend fun setVideoDownloadQuality(quality: String)

    val language: Flow<String>

    /**
     * Serialized "Moods & Genres" browse result, so the search and home screens can paint their
     * category grid immediately instead of waiting on the network every time. Null until the
     * first successful fetch.
     */
    val moodAndGenresCache: Flow<String?>

    suspend fun setMoodAndGenresCache(json: String)

    /**
     * Cover art resolved per browse category, keyed by its params. The category list itself
     * carries no artwork, so each cover costs one full category browse — worth remembering on
     * disk rather than paying again every time the search screen opens.
     */
    val moodArtworkCache: Flow<String?>

    suspend fun setMoodArtworkCache(json: String)

    fun getString(key: String): Flow<String?>

    suspend fun putString(
        key: String,
        value: String,
    )

    val loggedIn: Flow<String>
    val cookie: Flow<String>
    val pageId: Flow<String>
    val authUser: Flow<Int>

    suspend fun setCookie(
        cookie: String,
        pageId: String?,
        authUser: Int = 0,
    )

    suspend fun setLoggedIn(logged: Boolean)

    val normalizeVolume: Flow<String>

    suspend fun setNormalizeVolume(normalize: Boolean)

    val cleanTrackTitles: Flow<String>

    suspend fun setCleanTrackTitles(clean: Boolean)

    val playerStyle: Flow<String>

    suspend fun setPlayerStyle(style: String)

    val skipSilent: Flow<String>

    suspend fun setSkipSilent(skip: Boolean)

    val saveStateOfPlayback: Flow<String>

    suspend fun setSaveStateOfPlayback(save: Boolean)

    val shuffleKey: Flow<String>
    val repeatKey: Flow<String>

    suspend fun recoverShuffleAndRepeatKey(
        shuffle: Boolean,
        repeat: Int,
    )

    val saveRecentSongAndQueue: Flow<String>

    suspend fun setSaveRecentSongAndQueue(save: Boolean)

    val recentMediaId: Flow<String>
    val recentPosition: Flow<String>

    suspend fun saveRecentSong(
        mediaId: String,
        position: Long,
    )

    val playlistFromSaved: Flow<String>

    suspend fun setPlaylistFromSaved(playlist: String)

    val sendBackToGoogle: Flow<String>

    suspend fun setSendBackToGoogle(send: Boolean)

    val sponsorBlockEnabled: Flow<String>

    suspend fun setSponsorBlockEnabled(enabled: Boolean)

    suspend fun getSponsorBlockCategories(): ArrayList<String>

    suspend fun setSponsorBlockCategories(categories: ArrayList<String>)

    val enableTranslateLyric: Flow<String>

    suspend fun setEnableTranslateLyric(enable: Boolean)

    val lyricsProvider: Flow<String>

    suspend fun setLyricsProvider(provider: String)

    val lyricsOffset: Flow<Long>

    suspend fun setLyricsOffset(offsetMs: Long)

    val translationLanguage: Flow<String>

    suspend fun setTranslationLanguage(language: String)

    val maxSongCacheSize: Flow<Int>

    suspend fun setMaxSongCacheSize(size: Int)

    val watchVideoInsteadOfPlayingAudio: Flow<String>

    suspend fun setWatchVideoInsteadOfPlayingAudio(watch: Boolean)

    val playerVolume: Flow<Float>

    suspend fun setPlayerVolume(volume: Float)

    val equalizerEnabled: Flow<String>

    suspend fun setEqualizerEnabled(enabled: Boolean)

    val equalizerBands: Flow<String>

    suspend fun setEqualizerBands(bandsDb: List<Float>)

    val equalizerPreamp: Flow<Float>

    suspend fun setEqualizerPreamp(preampDb: Float)

    val equalizerAutoEqProfile: Flow<String>

    suspend fun setEqualizerAutoEqProfile(name: String)

    /**
     * One of [Values.EQUALIZER_TYPE_BUILT_IN], [Values.EQUALIZER_TYPE_SYSTEM]. Android only — Desktop has no system
     * equalizer and always runs the built-in one. The two never run together.
     */
    val equalizerType: Flow<String>

    suspend fun setEqualizerType(type: String)

    /**
     * Whether the echo/delay effect is applied.
     *
     * Off passes audio straight through without computing any taps. When switched off, the
     * numbers the user dialled in stay put, so switching it back on returns to their echo rather
     * than to the default one.
     */
    val delayEnabled: Flow<String>

    suspend fun setDelayEnabled(enabled: Boolean)

    /** Spacing between echo repeats in milliseconds — where the taps land, not how many there are. */
    val delayTimeMs: Flow<Int>

    suspend fun setDelayTimeMs(timeMs: Int)

    /**
     * How much of each repeat survives into the next one, 0..0.9.
     *
     * The tap count is derived from this via [com.maxrave.domain.data.player.DelayEffect.taps]: decay
     * and a tap count are two ways of saying the same thing, and storing both creates a pair that
     * can disagree.
     */
    val delayFeedback: Flow<Float>

    suspend fun setDelayFeedback(feedback: Float)

    /** Echo level against the dry signal, 0..1. */
    val delayMix: Flow<Float>

    suspend fun setDelayMix(mix: Float)

    /**
     * Whether the reverb is applied at all. Separate from the room and the mix on the same
     * reasoning as [delayEnabled] — the room the user picked outlives the switch.
     */
    val reverbEnabled: Flow<String>

    suspend fun setReverbEnabled(enabled: Boolean)

    /**
     * The room being convolved against, as the raw [ReverbPreset] name.
     *
     * Stored as the name and handed back unresolved because a name survives the list growing —
     * an index is a position, and inserting a room in the middle of the list would silently move
     * every device onto a different one. Readers resolve it themselves and fall back to the
     * default when the stored name comes from a newer build than the one reading it.
     */
    val reverbPreset: Flow<String>

    suspend fun setReverbPreset(preset: ReverbPreset)

    /** Wet level of the reverb against the dry signal, 0..1. */
    val reverbMix: Flow<Float>

    suspend fun setReverbMix(mix: Float)

    val lyricsRomanization: Flow<String>

    suspend fun setLyricsRomanization(enabled: Boolean)

    val videoQuality: Flow<String>

    suspend fun setVideoQuality(quality: String)

    val spdc: Flow<String>

    suspend fun setSpdc(spdc: String)

    val spotifyLyrics: Flow<String>

    suspend fun setSpotifyLyrics(spotifyLyrics: Boolean)

    val spotifyCanvas: Flow<String>

    suspend fun setSpotifyCanvas(spotifyCanvas: Boolean)

    /**
     * Animated album artwork from the hidden AM catalog, used in place of a Spotify canvas. It
     * needs no account of any kind, so unlike [spotifyCanvas] it is never gated on a login.
     */
    val amAnimatedArtwork: Flow<String>

    suspend fun setAMAnimatedArtwork(enabled: Boolean)

    val spotifyClientToken: Flow<String>

    suspend fun setSpotifyClientToken(token: String)

    val spotifyClientTokenExpires: Flow<Long>

    suspend fun setSpotifyClientTokenExpires(expires: Long)

    val tidalClientId: Flow<String>

    suspend fun setTidalClientId(value: String)

    val tidalClientSecret: Flow<String>

    suspend fun setTidalClientSecret(value: String)

    val spotifyPersonalToken: Flow<String>

    suspend fun setSpotifyPersonalToken(token: String)

    val spotifyPersonalTokenExpires: Flow<Long>

    suspend fun setSpotifyPersonalTokenExpires(expires: Long)

    val homeLimit: Flow<Int>

    suspend fun setHomeLimit(limit: Int)

    val chartKey: Flow<String>

    suspend fun setChartKey(key: String)

    val translucentBottomBar: Flow<String>

    suspend fun setTranslucentBottomBar(translucent: Boolean)

    val usingProxy: Flow<String>

    suspend fun setUsingProxy(usingProxy: Boolean)

    val proxyType: Flow<ProxyType>

    suspend fun setProxyType(proxyType: ProxyType)

    val proxyHost: Flow<String>

    suspend fun setProxyHost(proxyHost: String)

    val proxyPort: Flow<Int>

    suspend fun setProxyPort(proxyPort: Int)

    val proxyUsername: Flow<String>

    suspend fun setProxyUsername(proxyUsername: String)

    val proxyPassword: Flow<String>

    suspend fun setProxyPassword(proxyPassword: String)

    fun getJVMProxy(): ProxyConfiguration?

    val endlessQueue: Flow<String>

    suspend fun setEndlessQueue(endlessQueue: Boolean)

    val keepYouTubePlaylistOffline: Flow<String>

    suspend fun setKeepYouTubePlaylistOffline(keep: Boolean)

    val combineLocalAndYouTubeLiked: Flow<String>

    suspend fun setCombineLocalAndYouTubeLiked(combine: Boolean)

    val shouldShowLogInRequiredAlert: Flow<String>

    suspend fun setShouldShowLogInRequiredAlert(shouldShow: Boolean)

    val autoCheckForUpdates: Flow<String>

    suspend fun setAutoCheckForUpdates(autoCheck: Boolean)

    val updateChannel: Flow<String>

    suspend fun setUpdateChannel(channel: String)

    val playbackSpeed: Flow<Float>

    fun setPlaybackSpeed(speed: Float)

    val pitch: Flow<Int>

    fun setPitch(pitch: Int)

    val dataSyncId: Flow<String>

    suspend fun setDataSyncId(dataSyncId: String)

    val visitorData: Flow<String>

    suspend fun setVisitorData(visitorData: String)

    suspend fun setAIProvider(provider: String)

    val aiProvider: Flow<String>

    suspend fun setAIApiKey(apiKey: String)

    val aiApiKey: Flow<String>

    val useAITranslation: Flow<String>

    suspend fun setUseAITranslation(use: Boolean)

    val customModelId: Flow<String>

    suspend fun setCustomModelId(modelId: String)

    val customOpenAIBaseUrl: Flow<String>

    suspend fun setCustomOpenAIBaseUrl(baseUrl: String)

    val customOpenAIHeaders: Flow<String>

    suspend fun setCustomOpenAIHeaders(headers: String)

    val localPlaylistFilter: Flow<String>

    suspend fun setLocalPlaylistFilter(filter: String)

    val killServiceOnExit: Flow<String>

    suspend fun setKillServiceOnExit(kill: Boolean)

    val crossfadeEnabled: Flow<String>

    suspend fun setCrossfadeEnabled(enabled: Boolean)

    val crossfadeDuration: Flow<Int>

    suspend fun setCrossfadeDuration(duration: Int)

    val crossfadeDjMode: Flow<String>

    suspend fun setCrossfadeDjMode(enabled: Boolean)

    val youtubeSubtitleLanguage: Flow<String>

    suspend fun setYoutubeSubtitleLanguage(language: String)

    /**
     * Language code (e.g. "vi") of the audio track to prefer on videos that ship several — dubbed
     * podcasts, mostly. Empty means the original track. Deliberately NOT defaulted to the app
     * language the way [youtubeSubtitleLanguage] is: that would swap the speaker's own voice for an
     * AI dub for every user whose app language has one.
     */
    val preferredAudioLanguage: Flow<String>

    suspend fun setPreferredAudioLanguage(language: String)

    val helpBuildLyricsDatabase: Flow<String>

    suspend fun setHelpBuildLyricsDatabase(help: Boolean)

    val contributorName: Flow<String>
    val contributorEmail: Flow<String>

    suspend fun setContributorLyricsDatabase(contributor: Pair<String, String>?)

    val backupDownloaded: Flow<String>

    suspend fun setBackupDownloaded(backupDownloaded: Boolean)

    val enableLiquidGlass: Flow<String>

    suspend fun setEnableLiquidGlass(enable: Boolean)

    /** One of [THEME_MODE_SYSTEM], [THEME_MODE_DARK], [THEME_MODE_LIGHT]. */
    val themeMode: Flow<String>

    suspend fun setThemeMode(mode: String)

    /** One of [THEME_COLOR_DEFAULT], [THEME_COLOR_WALLPAPER], [THEME_COLOR_CUSTOM]. */
    val themeColorSource: Flow<String>

    suspend fun setThemeColorSource(source: String)

    /** Seed color for the custom theme as an 8-digit ARGB hex string (e.g. "FF8ECAE6"). */
    val customThemeColor: Flow<String>

    suspend fun setCustomThemeColor(argbHex: String)

    val crossfadeSkipAlbum: Flow<String>

    suspend fun setCrossfadeSkipAlbum(enabled: Boolean)

    val autoDownloadLikedSongs: Flow<String>

    suspend fun setAutoDownloadLikedSongs(enabled: Boolean)

    val keepServiceAlive: Flow<String>

    suspend fun setKeepServiceAlive(keep: Boolean)

    val nowPlayingStyle: Flow<String>

    suspend fun setNowPlayingStyle(style: String)

    val lyricsStyle: Flow<String>

    suspend fun setLyricsStyle(style: String)

    val romanizationLanguages: Flow<String>

    suspend fun setRomanizationLanguages(languages: String)

    val lyricsOffsetMs: Flow<Int>

    suspend fun setLyricsOffsetMs(offsetMs: Int)

    val explicitContentEnabled: Flow<String>

    suspend fun setExplicitContentEnabled(enabled: Boolean)

    val discordToken: Flow<String>

    suspend fun setDiscordToken(token: String)

    val richPresenceEnabled: Flow<String>

    suspend fun setRichPresenceEnabled(enabled: Boolean)

    /** Last.fm session key. Has no expiry — it stays valid until the user revokes it on last.fm. */
    val lastfmSessionKey: Flow<String>

    /** The logged-in Last.fm username, kept only so settings can show whose account is connected. */
    val lastfmUsername: Flow<String>

    suspend fun setLastfmSession(
        sessionKey: String,
        username: String,
    )

    val lastfmScrobbleEnabled: Flow<String>

    suspend fun setLastfmScrobbleEnabled(enabled: Boolean)

    val localTrackingEnabled: Flow<String>

    suspend fun setLocalTrackingEnabled(enabled: Boolean)

    val blogNotificationEnabled: Flow<String>

    suspend fun setBlogNotificationEnabled(enabled: Boolean)

    // Auto Backup
    val autoBackupEnabled: Flow<String>

    suspend fun setAutoBackupEnabled(enabled: Boolean)

    val autoBackupFrequency: Flow<String>

    suspend fun setAutoBackupFrequency(frequency: String)

    val autoBackupMaxFiles: Flow<Int>

    suspend fun setAutoBackupMaxFiles(max: Int)

    val autoBackupLastTime: Flow<Long>

    suspend fun setAutoBackupLastTime(time: Long)

    enum class ProxyType {
        PROXY_TYPE_HTTP,
        PROXY_TYPE_SOCKS,
    }

    companion object Values {
        const val SIMPMUSIC = "simpmusic"
        const val YOUTUBE = "youtube"
        const val LRCLIB = "lrclib"
        const val BETTER_LYRICS = "better_lyrics"

        const val FDROID = "fdroid"
        const val GITHUB_FOSS_NIGHTLY = "github_foss_nightly"
        const val GITHUB = "github_release"

        const val REPEAT_MODE_OFF = "REPEAT_MODE_OFF"
        const val REPEAT_ONE = "REPEAT_ONE"
        const val REPEAT_ALL = "REPEAT_ALL"

        const val TRUE = "TRUE"
        const val FALSE = "FALSE"

        const val THEME_MODE_SYSTEM = "SYSTEM"
        const val THEME_MODE_DARK = "DARK"
        const val THEME_MODE_LIGHT = "LIGHT"

        const val THEME_COLOR_DEFAULT = "DEFAULT"
        const val THEME_COLOR_WALLPAPER = "WALLPAPER"
        const val THEME_COLOR_CUSTOM = "CUSTOM"

        const val DEFAULT_THEME_COLOR_HEX = "FF8ECAE6"

        const val CROSSFADE_DURATION_AUTO = 0

        const val EQUALIZER_TYPE_BUILT_IN = "BUILT_IN"
        const val EQUALIZER_TYPE_SYSTEM = "SYSTEM"

        const val DEFAULT_DELAY_TIME_MS = 250
        const val DEFAULT_DELAY_FEEDBACK = 0.4f
        const val DEFAULT_DELAY_MIX = 0.35f
        const val DEFAULT_REVERB_PRESET = "HALL"
        const val DEFAULT_REVERB_MIX = 0.3f

        const val PROXY_TYPE_HTTP = "http"
        const val PROXY_TYPE_SOCKS = "socks"

        // AI
        const val AI_PROVIDER_GEMINI = "gemini"
        const val AI_PROVIDER_OPENAI = "openai"
        const val AI_PROVIDER_CUSTOM_OPENAI = "custom_openai"

        const val LOCAL_PLAYLIST_FILTER_OLDER_FIRST = "older_first"
        const val LOCAL_PLAYLIST_FILTER_NEWER_FIRST = "newer_first"
        const val LOCAL_PLAYLIST_FILTER_TITLE = "title"
        const val LOCAL_PLAYLIST_FILTER_CUSTOM_ORDER = "custom_order"

        // Auto Backup Frequency
        const val AUTO_BACKUP_FREQUENCY_DAILY = "daily"
        const val AUTO_BACKUP_FREQUENCY_WEEKLY = "weekly"
        const val AUTO_BACKUP_FREQUENCY_MONTHLY = "monthly"

        // Player Styles
        const val NOW_PLAYING_STYLE_SPOTIFY = "SPOTIFY"
        const val NOW_PLAYING_STYLE_M3_EXPRESSIVE = "M3_EXPRESSIVE"
        const val NOW_PLAYING_STYLE_APPLE_MUSIC = "APPLE_MUSIC"

        const val LYRICS_STYLE_CLASSIC = "CLASSIC"
        const val LYRICS_STYLE_APPLE_MUSIC = "APPLE_MUSIC"

        const val PLAYER_STYLE_APPLE = "apple_music"
        const val PLAYER_STYLE_CLASSIC = "classic"
    }
}