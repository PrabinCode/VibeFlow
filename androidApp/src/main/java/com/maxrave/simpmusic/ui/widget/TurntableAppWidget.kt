package com.maxrave.simpmusic.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.toBitmap
import com.maxrave.common.Config
import com.maxrave.simpmusic.MainActivity
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class TurntableAppWidget : GlanceAppWidget(), KoinComponent {
    private val sharedViewModel by inject<SharedViewModel>()
    private val serviceScope by inject<CoroutineScope>(named(Config.SERVICE_SCOPE))

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        serviceScope.launch {
            val controllerJob = launch {
                sharedViewModel.controllerState.collectLatest {
                    updateWidget(context)
                }
            }
            val nowPlayingJob = launch {
                sharedViewModel.nowPlayingScreenData.collectLatest {
                    updateWidget(context)
                }
            }
            controllerJob.join()
            nowPlayingJob.join()
        }

        provideContent {
            GlanceTheme {
                val controllerState by sharedViewModel.controllerState.collectAsState()
                val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsState()

                val title = screenDataState.nowPlayingTitle.ifBlank { "VibeFlow" }
                val artist = screenDataState.artistName.ifBlank { "Tap to play" }
                val thumbUrl = screenDataState.thumbnailURL

                var trackArt by remember(thumbUrl) { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(thumbUrl) {
                    if (!thumbUrl.isNullOrBlank()) {
                        val request = ImageRequest.Builder(context)
                            .data(thumbUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumbUrl + "_TURNTABLE")
                            .crossfade(true)
                            .placeholder(R.drawable.holder)
                            .error(R.drawable.holder)
                            .allowHardware(false)
                            .build()
                        val result = ImageLoader(context).execute(request)
                        if (result is SuccessResult) {
                            trackArt = result.image.toBitmap()
                        }
                    } else {
                        trackArt = null
                    }
                }

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(24.dp)
                        .background(ColorProvider(Color(0xFF141419)))
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Vinyl Record Disc
                        Box(
                            modifier = GlanceModifier
                                .size(110.dp)
                                .cornerRadius(999.dp)
                                .background(ColorProvider(Color(0xFF22222A))),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Inner vinyl groove ring
                            Box(
                                modifier = GlanceModifier
                                    .size(94.dp)
                                    .cornerRadius(999.dp)
                                    .background(ColorProvider(Color(0xFF181820))),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Center Album Art
                                Box(
                                    modifier = GlanceModifier
                                        .size(64.dp)
                                        .cornerRadius(999.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    trackArt?.let { bm ->
                                        Image(
                                            provider = ImageProvider(bm),
                                            contentDescription = "Vinyl Album Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = GlanceModifier.fillMaxSize().cornerRadius(999.dp),
                                        )
                                    } ?: Image(
                                        provider = ImageProvider(R.drawable.mono),
                                        contentDescription = "App Logo",
                                        contentScale = ContentScale.Fit,
                                        modifier = GlanceModifier.size(40.dp),
                                    )
                                }

                                // Center Spindle Hole
                                Box(
                                    modifier = GlanceModifier
                                        .size(12.dp)
                                        .cornerRadius(999.dp)
                                        .background(ColorProvider(Color(0xFF141419))),
                                ) {}
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Title & Artist
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.fillMaxWidth(),
                        )
                        Text(
                            text = artist,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFB0B0B8)),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.fillMaxWidth(),
                        )

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Media Playback Controls
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircleIconButton(
                                modifier = GlanceModifier.size(32.dp),
                                imageProvider = ImageProvider(R.drawable.baseline_skip_previous_24),
                                contentDescription = "Previous",
                                contentColor = ColorProvider(if (controllerState.isPreviousAvailable) Color.White else Color.DarkGray),
                                backgroundColor = ColorProvider(Color.Transparent),
                                enabled = controllerState.isPreviousAvailable,
                                onClick = { sharedViewModel.onUIEvent(UIEvent.Previous) },
                            )

                            Spacer(modifier = GlanceModifier.width(12.dp))

                            CircleIconButton(
                                modifier = GlanceModifier.size(42.dp),
                                imageProvider = if (controllerState.isPlaying) {
                                    ImageProvider(R.drawable.baseline_pause_circle_24)
                                } else {
                                    ImageProvider(R.drawable.baseline_play_circle_24)
                                },
                                contentDescription = if (controllerState.isPlaying) "Pause" else "Play",
                                contentColor = ColorProvider(Color.White),
                                backgroundColor = ColorProvider(Color(0xFF2E2E38)),
                                onClick = { sharedViewModel.onUIEvent(UIEvent.PlayPause) },
                            )

                            Spacer(modifier = GlanceModifier.width(12.dp))

                            CircleIconButton(
                                modifier = GlanceModifier.size(32.dp),
                                imageProvider = ImageProvider(R.drawable.baseline_skip_next_24),
                                contentDescription = "Next",
                                contentColor = ColorProvider(if (controllerState.isNextAvailable) Color.White else Color.DarkGray),
                                backgroundColor = ColorProvider(Color.Transparent),
                                enabled = controllerState.isNextAvailable,
                                onClick = { sharedViewModel.onUIEvent(UIEvent.Next) },
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(this@TurntableAppWidget.javaClass)
        glanceIds.forEach { glanceId ->
            this@TurntableAppWidget.update(context, glanceId)
        }
    }
}
