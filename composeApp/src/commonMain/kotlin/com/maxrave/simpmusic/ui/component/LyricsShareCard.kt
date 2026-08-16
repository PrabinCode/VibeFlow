package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.expect.copyToClipboard
import com.maxrave.simpmusic.ui.icon.ContentCopy
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.card_dark
import simpmusic.composeapp.generated.resources.card_gradient
import simpmusic.composeapp.generated.resources.card_light
import simpmusic.composeapp.generated.resources.card_style
import simpmusic.composeapp.generated.resources.copy_lyrics
import simpmusic.composeapp.generated.resources.lyrics_copied
import simpmusic.composeapp.generated.resources.share_lyrics
import simpmusic.composeapp.generated.resources.share_quote_card

enum class LyricsCardStyle {
    GRADIENT,
    DARK_GLASS,
    LIGHT_MINIMAL,
}

@Composable
fun LyricsShareCard(
    trackTitle: String,
    artistName: String,
    thumbnailUrl: String?,
    selectedLines: List<String>,
    style: LyricsCardStyle,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (style) {
        LyricsCardStyle.GRADIENT -> Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.85f),
                accentColor.copy(alpha = 0.5f),
                Color(0xFF121212),
            ),
        )
        LyricsCardStyle.DARK_GLASS -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF222228),
                Color(0xFF141416),
            ),
        )
        LyricsCardStyle.LIGHT_MINIMAL -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFAFAFC),
                Color(0xFFECEEF2),
            ),
        )
    }

    val textColor = when (style) {
        LyricsCardStyle.LIGHT_MINIMAL -> Color(0xFF1A1A1A)
        else -> Color.White
    }

    val secondaryTextColor = when (style) {
        LyricsCardStyle.LIGHT_MINIMAL -> Color(0xFF555555)
        else -> Color.White.copy(alpha = 0.7f)
    }

    val borderColor = when (style) {
        LyricsCardStyle.LIGHT_MINIMAL -> Color.Black.copy(alpha = 0.08f)
        LyricsCardStyle.DARK_GLASS -> Color.White.copy(alpha = 0.12f)
        LyricsCardStyle.GRADIENT -> accentColor.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Track Info Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackTitle,
                        style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artistName,
                        style = typo().bodyMedium,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    imageVector = SimpIcons.Lyrics,
                    contentDescription = null,
                    tint = secondaryTextColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lyric Quotes Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                selectedLines.forEach { line ->
                    Text(
                        text = line,
                        style = typo().headlineSmall.copy(
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = textColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // VibeFlow Footer Watermark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (style == LyricsCardStyle.LIGHT_MINIMAL) {
                                Color.Black.copy(alpha = 0.06f)
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            },
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Icon(
                        imageVector = SimpIcons.LibraryMusic,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VibeFlow",
                        style = typo().labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                    )
                }

                Text(
                    text = "pcshrestha.com.np",
                    style = typo().bodySmall.copy(fontSize = 11.sp),
                    color = secondaryTextColor.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsShareBottomSheet(
    trackTitle: String,
    artistName: String,
    thumbnailUrl: String?,
    selectedLines: List<String>,
    accentColor: Color,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentStyle by remember { mutableStateOf(LyricsCardStyle.GRADIENT) }
    var showCopiedNotice by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.share_quote_card),
                style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Live Card Preview
            LyricsShareCard(
                trackTitle = trackTitle,
                artistName = artistName,
                thumbnailUrl = thumbnailUrl,
                selectedLines = selectedLines,
                style = currentStyle,
                accentColor = accentColor,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Style Selection Chips
            Text(
                text = stringResource(Res.string.card_style),
                style = typo().labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = currentStyle == LyricsCardStyle.GRADIENT,
                    onClick = { currentStyle = LyricsCardStyle.GRADIENT },
                    label = { Text(stringResource(Res.string.card_gradient)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.White,
                    ),
                )
                FilterChip(
                    selected = currentStyle == LyricsCardStyle.DARK_GLASS,
                    onClick = { currentStyle = LyricsCardStyle.DARK_GLASS },
                    label = { Text(stringResource(Res.string.card_dark)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3A3A3C),
                        selectedLabelColor = Color.White,
                    ),
                )
                FilterChip(
                    selected = currentStyle == LyricsCardStyle.LIGHT_MINIMAL,
                    onClick = { currentStyle = LyricsCardStyle.LIGHT_MINIMAL },
                    label = { Text(stringResource(Res.string.card_light)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE5E5EA),
                        selectedLabelColor = Color.Black,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons (Copy / Done)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val formattedLyrics = buildString {
                            appendLine("♫ $trackTitle — $artistName")
                            appendLine()
                            selectedLines.forEach { appendLine(it) }
                            appendLine()
                            append("Shared via VibeFlow • https://pcshrestha.com.np")
                        }
                        copyToClipboard("VibeFlow Lyrics", formattedLyrics)
                        showCopiedNotice = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = SimpIcons.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showCopiedNotice) {
                            stringResource(Res.string.lyrics_copied)
                        } else {
                            stringResource(Res.string.copy_lyrics)
                        },
                    )
                }

                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(Res.string.share_lyrics),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
