package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.expect.ui.PlatformCastButton
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.now_playing_upper
import kotlin.math.roundToLong

/**
 * Modern Apple Music style player view for VibeFlow.
 *
 * Features:
 * - Dynamic animated fluid gradient backdrop extracted from artwork palette
 * - Breathing artwork physics with smooth spring scaling (1.0x playing / 0.86x paused)
 * - Modern thick pill scrub bar with elapsed and remaining countdown
 * - Symmetrical, bold playback controls
 * - Iconic Apple Music 3-button bottom action pill (Lyrics, Cast/Output, Queue)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicPlayerView(
    screenDataState: NowPlayingScreenData,
    controllerState: ControlState,
    timelineState: TimeLine,
    castState: GenericCastState,
    likeStatus: Boolean,
    startColor: Color,
    endColor: Color,
    dismissIcon: ImageVector,
    onDismiss: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    onArtistClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onUIEvent: (UIEvent) -> Unit,
    onSetBitmap: (androidx.compose.ui.graphics.ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localDensity = LocalDensity.current
    val scrollState = rememberScrollState()

    // Breathing artwork physics: Spring animation scaling up on play and relaxing down on pause
    val isPlaying = controllerState.isPlaying
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "ArtworkSpringScale",
    )
    val artworkElevation by animateDpAsState(
        targetValue = if (isPlaying) 20.dp else 6.dp,
        animationSpec = tween(durationMillis = 350),
        label = "ArtworkElevation",
    )

    // Fluid background motion transitions
    val infiniteTransition = rememberInfiniteTransition(label = "FluidMeshTransition")
    val fluidAnimOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "FluidOffset1",
    )
    val fluidAnimOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "FluidOffset2",
    )

    // Progress bar slider state
    var isSliding by rememberSaveable { mutableStateOf(false) }
    var sliderValue by rememberSaveable { mutableFloatStateOf(0f) }

    LaunchedEffect(timelineState.current, timelineState.total) {
        if (!isSliding && timelineState.total > 0) {
            sliderValue = ((timelineState.current.toFloat() / timelineState.total.toFloat()) * 100f).coerceIn(0f, 100f)
        }
    }

    // Dynamic background blend colors
    val vibrantColor = remember(startColor, endColor) {
        if (startColor != Color.Black && startColor != Color.Transparent) startColor else endColor
    }
    val animatedBgStart by animateColorAsState(targetValue = vibrantColor, animationSpec = tween(700), label = "BgStart")
    val animatedBgEnd by animateColorAsState(targetValue = endColor, animationSpec = tween(700), label = "BgEnd")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .drawBehind {
                val width = size.width
                val height = size.height

                // Fluid Mesh Layer 1 (Top-Left Radial Accent)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedBgStart.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                        center = Offset(
                            x = width * (0.2f + 0.3f * fluidAnimOffset1),
                            y = height * (0.15f + 0.2f * fluidAnimOffset2),
                        ),
                        radius = width * 0.95f,
                    ),
                    center = Offset(
                        x = width * (0.2f + 0.3f * fluidAnimOffset1),
                        y = height * (0.15f + 0.2f * fluidAnimOffset2),
                    ),
                    radius = width * 0.95f,
                )

                // Fluid Mesh Layer 2 (Bottom-Right Radial Accent)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedBgEnd.copy(alpha = 0.45f),
                            Color.Transparent,
                        ),
                        center = Offset(
                            x = width * (0.8f - 0.3f * fluidAnimOffset2),
                            y = height * (0.55f + 0.25f * fluidAnimOffset1),
                        ),
                        radius = width * 1.1f,
                    ),
                    center = Offset(
                        x = width * (0.8f - 0.3f * fluidAnimOffset2),
                        y = height * (0.55f + 0.25f * fluidAnimOffset1),
                    ),
                    radius = width * 1.1f,
                )

                // Bottom Scrim for crystal clear typography
                drawRect(
                    brush = smoothScrimBrush(
                        from = Color.Transparent,
                        to = Color(0xFF0C0C0E).copy(alpha = 0.88f),
                        startY = height * 0.45f,
                        endY = height,
                    ),
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() },
                    bottom = with(localDensity) { WindowInsets.navigationBars.getBottom(localDensity).toDp() },
                ),
        ) {
            // ── Top Header Navigation ──
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = dismissIcon,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.now_playing_upper),
                            style = typo().labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        if (screenDataState.playlistName.isNotBlank()) {
                            Text(
                                text = screenDataState.playlistName,
                                style = typo().labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .basicMarquee(iterations = Int.MAX_VALUE),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onMoreOptionsClick) {
                        Icon(
                            imageVector = SimpIcons.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            )

            // ── Scrollable Body Area ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── 1. Breathing Album Artwork ──
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .scale(artworkScale)
                            .shadow(
                                elevation = artworkElevation,
                                shape = RoundedCornerShape(22.dp),
                                spotColor = vibrantColor.copy(alpha = 0.5f),
                                ambientColor = Color.Black.copy(alpha = 0.6f),
                            )
                            .clip(RoundedCornerShape(22.dp))
                            .clickable {
                                // Tap artwork to toggle lyrics
                                onLyricsClick()
                            },
                        color = Color.DarkGray.copy(alpha = 0.4f),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(screenDataState.thumbnailURL)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(screenDataState.thumbnailURL + "BIGGER")
                                .crossfade(400)
                                .build(),
                            contentDescription = screenDataState.nowPlayingTitle,
                            contentScale = ContentScale.Crop,
                            placeholder = rememberHolderPainter(),
                            error = rememberHolderPainter(),
                            onSuccess = { state ->
                                onSetBitmap(state.result.image.toImageBitmap())
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 2. Track Title & Artist Metadata Row ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = screenDataState.nowPlayingTitle,
                            style = typo().titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable(),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onArtistClick() },
                        ) {
                            if (screenDataState.isExplicit) {
                                ExplicitBadge(modifier = Modifier.padding(end = 6.dp))
                            }
                            Text(
                                text = screenDataState.artistName,
                                style = typo().bodyLarge.copy(fontWeight = FontWeight.Normal),
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            )
                        }
                    }

                    // Like / Favorite Heart Button
                    HeartCheckBox(
                        size = 32,
                        checked = likeStatus,
                        onStateChange = { onUIEvent(UIEvent.ToggleLike) },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── 3. Apple Music Thick Pill Progress Slider ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Background buffer progress track
                        LinearProgressIndicator(
                            progress = { (timelineState.bufferedPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color.White.copy(alpha = 0.25f),
                            trackColor = Color.White.copy(alpha = 0.12f),
                            strokeCap = StrokeCap.Round,
                            drawStopIndicator = {},
                        )

                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            Slider(
                                value = (sliderValue / 100f).coerceIn(0f, 1f),
                                onValueChange = {
                                    isSliding = true
                                    sliderValue = it * 100f
                                },
                                onValueChangeFinished = {
                                    isSliding = false
                                    onUIEvent(UIEvent.UpdateProgress(sliderValue))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp),
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        modifier = Modifier.height(6.dp),
                                        sliderState = sliderState,
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.Transparent,
                                        ),
                                        thumbTrackGapSize = 0.dp,
                                        drawTick = { _, _ -> },
                                        drawStopIndicator = null,
                                    )
                                },
                                thumb = {
                                    // Interactive Pill Thumb
                                    val thumbSize = if (isSliding) 14.dp else 10.dp
                                    Box(
                                        modifier = Modifier
                                            .size(thumbSize)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .shadow(4.dp, CircleShape),
                                    )
                                },
                            )
                        }
                    }

                    // Elapsed and Remaining Time Stamps
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val currentMs = (timelineState.total * (sliderValue / 100f)).roundToLong()
                        val remainingMs = (timelineState.total - currentMs).coerceAtLeast(0L)

                        Text(
                            text = formatDuration(currentMs),
                            style = typo().labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.55f),
                        )

                        if (timelineState.isCrossfading) {
                            Text(
                                text = stringResource(Res.string.crossfading),
                                style = typo().labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = seed,
                            )
                        }

                        Text(
                            text = "-${formatDuration(remainingMs)}",
                            style = typo().labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.55f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── 4. Main Playback Controls Cluster ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { onUIEvent(UIEvent.Shuffle) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (controllerState.isShuffle) seed else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    // Skip Previous
                    IconButton(
                        onClick = {
                            if (controllerState.isPreviousAvailable) {
                                onUIEvent(UIEvent.Previous)
                            }
                        },
                        enabled = controllerState.isPreviousAvailable,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (controllerState.isPreviousAvailable) Color.White else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    // Center Play / Pause (Apple Music Style Big Bold Glyph)
                    Surface(
                        onClick = { onUIEvent(UIEvent.PlayPause) },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(12.dp, CircleShape, spotColor = Color.White.copy(alpha = 0.3f)),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Crossfade(targetState = controllerState.isPlaying, label = "PlayPauseCrossfade") { playing ->
                                Icon(
                                    imageVector = if (playing) SimpIcons.Pause else SimpIcons.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(38.dp),
                                )
                            }
                        }
                    }

                    // Skip Next
                    IconButton(
                        onClick = {
                            if (controllerState.isNextAvailable) {
                                onUIEvent(UIEvent.Next)
                            }
                        },
                        enabled = controllerState.isNextAvailable,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.SkipNext,
                            contentDescription = "Next",
                            tint = if (controllerState.isNextAvailable) Color.White else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    // Repeat
                    val isRepeatOne = controllerState.repeatState is RepeatState.One
                    val isRepeatAll = controllerState.repeatState is RepeatState.All
                    IconButton(
                        onClick = { onUIEvent(UIEvent.Repeat) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = if (isRepeatOne) SimpIcons.RepeatOne else SimpIcons.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeatOne || isRepeatAll) seed else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── 5. Floating Bottom Action Pill (Lyrics / Cast / Queue) ──
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .height(52.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Lyrics Button
                        IconButton(
                            onClick = onLyricsClick,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                imageVector = SimpIcons.Lyrics,
                                contentDescription = "Lyrics",
                                tint = if (screenDataState.lyricsData != null) seed else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        // Cast / Output Device Button
                        PlatformCastButton(
                            modifier = Modifier.size(22.dp),
                            tint = if (castState.isRemote) Color.Cyan else Color.White.copy(alpha = 0.75f),
                        )

                        // Up Next / Queue Button
                        IconButton(
                            onClick = onQueueClick,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                imageVector = SimpIcons.QueueMusic,
                                contentDescription = "Queue",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
