package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url

val AmbientGlowHeight = 360.dp

@Composable
fun AmbientThemeGlow(
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    val glow by animateColorAsState(
        targetValue =
            when {
                tint == null -> pageBackground
                isLightTheme -> lerp(tint, Color.White, 0.85f)
                else -> tint.rgbFactor(0.45f)
            },
        animationSpec = tween(500),
        label = "ambientGlow",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(AmbientGlowHeight)
            .angledGradientBackground(listOf(glow, pageBackground), 25f),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(artworkScrimBrush(pageBackground)),
        )
    }
}

@Composable
fun rememberNowPlayingGlowTint(thumbnailUrl: String?): Color? {
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = Color.Unspecified,
            defaultOnColor = Color.Unspecified,
            loader = networkLoader,
        )
    LaunchedEffect(thumbnailUrl) {
        thumbnailUrl?.let { dominantColorState.updateFrom(Url(it)) }
    }
    if (thumbnailUrl == null) return null
    return dominantColorState.color.takeIf { it.isSpecified }
}
