package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.equalizer_autoeq
import simpmusic.composeapp.generated.resources.equalizer_autoeq_choose
import simpmusic.composeapp.generated.resources.equalizer_autoeq_empty
import simpmusic.composeapp.generated.resources.equalizer_autoeq_search

/**
 * Headphone profile derived from jaakkopasanen/AutoEq database.
 * Fixed 10-band ISO peaking filters at 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz.
 */
data class AutoEqHeadphone(
    val name: String,
    val brand: String,
    val preampDb: Float,
    val bandsDb: List<Float>,
)

val POPULAR_AUTOEQ_HEADPHONES: List<AutoEqHeadphone> = listOf(
    // Sony
    AutoEqHeadphone("Sony WH-1000XM4", "Sony", -5.9f, listOf(4.6f, 2.5f, -2.1f, -4.7f, -1.8f, 1.2f, -1.0f, 3.8f, 1.9f, -3.2f)),
    AutoEqHeadphone("Sony WH-1000XM5", "Sony", -4.8f, listOf(3.2f, 1.8f, -1.5f, -3.8f, -1.2f, 0.8f, -0.5f, 3.1f, 1.5f, -2.8f)),
    AutoEqHeadphone("Sony WF-1000XM4", "Sony", -4.2f, listOf(2.8f, 1.5f, -0.8f, -2.5f, -0.5f, 1.1f, -1.2f, 2.4f, 1.2f, -1.5f)),
    AutoEqHeadphone("Sony WF-1000XM5", "Sony", -3.6f, listOf(1.8f, 1.0f, -0.5f, -1.8f, 0.2f, 0.8f, -0.8f, 2.1f, 0.9f, -1.2f)),
    AutoEqHeadphone("Sony LinkBuds S", "Sony", -3.8f, listOf(2.1f, 1.2f, -0.6f, -2.1f, 0.0f, 0.9f, -1.0f, 2.5f, 1.1f, -1.4f)),

    // Apple
    AutoEqHeadphone("Apple AirPods Pro (1st gen)", "Apple", -3.2f, listOf(-0.5f, 0.2f, 0.8f, 1.2f, -0.4f, -1.1f, 2.4f, -0.8f, 1.5f, 0.0f)),
    AutoEqHeadphone("Apple AirPods Pro 2", "Apple", -2.8f, listOf(0.1f, 0.5f, 0.2f, -0.3f, -0.8f, -0.2f, 1.9f, -1.2f, 1.1f, 0.2f)),
    AutoEqHeadphone("Apple AirPods 3", "Apple", -4.5f, listOf(4.2f, 3.1f, 0.8f, -0.5f, -0.9f, 0.4f, 1.2f, -1.8f, 0.8f, -1.2f)),
    AutoEqHeadphone("Apple AirPods Max", "Apple", -3.5f, listOf(1.2f, 0.8f, -0.4f, -1.2f, 0.2f, 1.1f, -1.8f, 2.5f, 1.0f, -1.5f)),
    AutoEqHeadphone("Apple EarPods", "Apple", -5.8f, listOf(5.5f, 4.2f, 1.5f, -0.8f, -1.5f, -0.4f, 1.8f, -2.1f, 0.5f, -2.0f)),

    // Sennheiser
    AutoEqHeadphone("Sennheiser HD 600", "Sennheiser", -6.5f, listOf(6.2f, 5.8f, 3.1f, 0.8f, -0.5f, -1.2f, 1.5f, -2.0f, 0.5f, -1.0f)),
    AutoEqHeadphone("Sennheiser HD 650 / 6XX", "Sennheiser", -6.8f, listOf(6.5f, 6.0f, 3.4f, 0.9f, -0.8f, -1.5f, 1.8f, -1.8f, 0.8f, -1.2f)),
    AutoEqHeadphone("Sennheiser HD 560S", "Sennheiser", -3.1f, listOf(2.8f, 2.1f, 0.8f, -0.4f, -0.2f, 0.5f, -1.2f, 1.8f, -0.5f, -0.8f)),
    AutoEqHeadphone("Sennheiser Momentum 4", "Sennheiser", -4.5f, listOf(-3.1f, -2.2f, -0.8f, 0.5f, 1.2f, 0.8f, -1.5f, 3.2f, 1.5f, -1.8f)),
    AutoEqHeadphone("Sennheiser IE 200", "Sennheiser", -3.9f, listOf(3.5f, 2.4f, 0.5f, -0.8f, 0.2f, 0.6f, -1.4f, 2.2f, 0.8f, -1.5f)),

    // Bose
    AutoEqHeadphone("Bose QuietComfort 35 II", "Bose", -4.2f, listOf(2.5f, 1.8f, -0.5f, -1.8f, 0.5f, 1.8f, -2.1f, 3.2f, 1.5f, -1.2f)),
    AutoEqHeadphone("Bose QuietComfort 45 / SE", "Bose", -5.1f, listOf(3.8f, 2.5f, -1.2f, -2.5f, 0.2f, 1.5f, -3.2f, 4.1f, 2.0f, -2.1f)),
    AutoEqHeadphone("Bose QC Ultra", "Bose", -4.0f, listOf(1.8f, 1.2f, -0.6f, -1.5f, 0.4f, 1.1f, -1.8f, 2.8f, 1.2f, -1.5f)),

    // Samsung
    AutoEqHeadphone("Samsung Galaxy Buds Pro", "Samsung", -3.0f, listOf(1.5f, 0.8f, -0.2f, -0.8f, 0.4f, 0.8f, -1.5f, 2.2f, 0.8f, -0.5f)),
    AutoEqHeadphone("Samsung Galaxy Buds 2", "Samsung", -2.8f, listOf(0.9f, 0.4f, -0.1f, -0.6f, 0.3f, 0.6f, -1.2f, 1.9f, 0.6f, -0.2f)),
    AutoEqHeadphone("Samsung Galaxy Buds 2 Pro", "Samsung", -2.5f, listOf(0.8f, 0.5f, 0.1f, -0.5f, 0.2f, 0.5f, -0.8f, 1.8f, 0.5f, 0.0f)),
    AutoEqHeadphone("Samsung Galaxy Buds FE", "Samsung", -3.2f, listOf(1.8f, 1.1f, 0.0f, -0.7f, 0.4f, 0.7f, -1.4f, 2.1f, 0.7f, -0.4f)),

    // Moondrop
    AutoEqHeadphone("Moondrop Chu / Chu II", "Moondrop", -3.5f, listOf(1.5f, 1.0f, 0.0f, -0.6f, 0.4f, 0.5f, -1.8f, 2.8f, 1.0f, -1.0f)),
    AutoEqHeadphone("Moondrop Aria", "Moondrop", -3.8f, listOf(1.8f, 1.2f, 0.2f, -0.5f, 0.2f, 0.8f, -1.5f, 2.5f, 1.2f, -0.8f)),
    AutoEqHeadphone("Moondrop Blessing 2 / Dusk", "Moondrop", -2.8f, listOf(0.8f, 0.4f, 0.1f, -0.2f, 0.1f, 0.4f, -0.8f, 1.9f, 0.8f, -0.2f)),
    AutoEqHeadphone("Moondrop Kato", "Moondrop", -3.2f, listOf(1.2f, 0.8f, 0.1f, -0.4f, 0.2f, 0.6f, -1.2f, 2.2f, 0.9f, -0.5f)),
    AutoEqHeadphone("Moondrop Space Travel", "Moondrop", -3.4f, listOf(1.4f, 0.9f, 0.0f, -0.5f, 0.3f, 0.6f, -1.5f, 2.4f, 0.9f, -0.8f)),

    // Budget IEMs
    AutoEqHeadphone("7Hz Salnotes Zero / Zero 2", "7Hz", -2.9f, listOf(1.0f, 0.6f, 0.2f, -0.4f, 0.2f, 0.6f, -1.1f, 2.1f, 0.9f, -0.4f)),
    AutoEqHeadphone("Tangzu Wan'er S.G", "Tangzu", -3.2f, listOf(1.4f, 0.9f, 0.1f, -0.5f, 0.3f, 0.7f, -1.4f, 2.4f, 1.1f, -0.6f)),
    AutoEqHeadphone("QCY T13", "QCY", -4.8f, listOf(-3.5f, -2.4f, -0.8f, 0.6f, 1.5f, 1.0f, -1.8f, 3.5f, 1.8f, -2.0f)),

    // Studio & Audiophile
    AutoEqHeadphone("Audio-Technica ATH-M50x", "Audio-Technica", -5.5f, listOf(-2.1f, -1.5f, 0.5f, 1.8f, 2.1f, -1.2f, 3.5f, -2.8f, 1.2f, -1.8f)),
    AutoEqHeadphone("Audio-Technica ATH-M40x", "Audio-Technica", -4.2f, listOf(-1.2f, -0.8f, 0.4f, 1.2f, 1.5f, -0.8f, 2.4f, -1.9f, 0.8f, -1.2f)),
    AutoEqHeadphone("Beyerdynamic DT 770 Pro (80 Ohm)", "Beyerdynamic", -4.9f, listOf(1.2f, 0.5f, -1.1f, -1.8f, 1.5f, 2.1f, -3.8f, -1.5f, 3.2f, -2.1f)),
    AutoEqHeadphone("Beyerdynamic DT 990 Pro (250 Ohm)", "Beyerdynamic", -6.2f, listOf(4.2f, 3.1f, 0.8f, -0.5f, 1.2f, 1.8f, -4.5f, -2.2f, 4.1f, -3.0f)),
    AutoEqHeadphone("AKG K371", "AKG", -2.1f, listOf(0.5f, 0.2f, -0.2f, -0.4f, 0.1f, 0.3f, -0.5f, 1.2f, 0.4f, 0.0f)),
    AutoEqHeadphone("AKG K240", "AKG", -5.8f, listOf(5.2f, 4.5f, 2.1f, 0.5f, -0.8f, -1.2f, 1.5f, -2.4f, 1.8f, -1.5f)),
    AutoEqHeadphone("Hifiman Sundara", "Hifiman", -4.8f, listOf(4.5f, 3.8f, 1.9f, 0.4f, -0.2f, -0.5f, 0.8f, -1.5f, 0.8f, -0.8f)),

    // Lifestyle
    AutoEqHeadphone("Beats Studio Pro", "Beats", -4.8f, listOf(-2.8f, -1.9f, -0.5f, 0.8f, 1.8f, 1.2f, -1.5f, 3.2f, 1.2f, -1.5f)),
    AutoEqHeadphone("Anker Soundcore Life Q30", "Anker", -6.5f, listOf(-5.2f, -4.1f, -2.0f, 0.5f, 2.8f, 2.1f, -2.8f, 4.2f, 1.8f, -2.5f)),
    AutoEqHeadphone("Koss Porta Pro", "Koss", -5.2f, listOf(-3.5f, -2.8f, -1.0f, 0.8f, 2.1f, 1.8f, -1.5f, 3.5f, 2.0f, -1.8f)),
    AutoEqHeadphone("Shure SE215", "Shure", -5.0f, listOf(-3.2f, -2.5f, -0.8f, 1.1f, 2.4f, 1.2f, -1.8f, 3.2f, 2.2f, -1.5f)),
    AutoEqHeadphone("Google Pixel Buds Pro", "Google", -3.6f, listOf(1.5f, 0.9f, -0.4f, -1.2f, 0.4f, 0.8f, -1.4f, 2.4f, 0.9f, -1.0f)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoEqPicker(
    label: String?,
    viewModel: SettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            POPULAR_AUTOEQ_HEADPHONES
        } else {
            val q = searchQuery.trim().lowercase()
            POPULAR_AUTOEQ_HEADPHONES.filter {
                it.name.lowercase().contains(q) || it.brand.lowercase().contains(q)
            }
        }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { isSheetOpen = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label ?: stringResource(Res.string.equalizer_autoeq_choose),
                style = typo().bodyMedium,
                color = if (label != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = SimpIcons.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                isSheetOpen = false
                searchQuery = ""
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.equalizer_autoeq),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.equalizer_autoeq_search),
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = SimpIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = SimpIcons.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.equalizer_autoeq_empty),
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(filtered, key = { it.name }) { item ->
                            val isSelected = label == item.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.applyEqualizerPreset(item.bandsDb, item.preampDb)
                                        viewModel.setEqualizerAutoEqProfile("${item.name}\n${item.bandsDb.joinToString(",")}")
                                        isSheetOpen = false
                                        searchQuery = ""
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = typo().bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "${item.brand} • Preamp: ${item.preampDb} dB",
                                        style = typo().bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = SimpIcons.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}
