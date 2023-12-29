package com.paranid5.crescendo.presentation.main.playing.views

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.palette.graphics.Palette
import com.paranid5.crescendo.domain.media.AudioStatus
import com.paranid5.crescendo.presentation.main.playing.PlayingViewModel
import com.paranid5.crescendo.presentation.main.playing.views.utils_buttons.EqualizerButton
import com.paranid5.crescendo.presentation.main.playing.views.utils_buttons.LikeButton
import com.paranid5.crescendo.presentation.main.playing.views.utils_buttons.PlaylistOrDownloadButton
import com.paranid5.crescendo.presentation.main.playing.views.utils_buttons.RepeatButton

@Composable
fun UtilsButtons(
    viewModel: PlayingViewModel,
    audioStatus: AudioStatus,
    palette: Palette?,
    modifier: Modifier = Modifier
) = Row(modifier) {
    EqualizerButton(
        palette = palette,
        modifier = Modifier.weight(1F)
    )

    RepeatButton(
        viewModel = viewModel,
        palette = palette,
        modifier = Modifier.weight(1F)
    )

    LikeButton(
        palette = palette,
        modifier = Modifier.weight(1F)
    )

    PlaylistOrDownloadButton(
        viewModel = viewModel,
        palette = palette,
        audioStatus = audioStatus,
        modifier = Modifier.weight(1F)
    )
}