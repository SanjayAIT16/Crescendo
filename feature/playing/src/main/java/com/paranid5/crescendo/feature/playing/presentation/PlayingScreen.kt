package com.paranid5.crescendo.feature.playing.presentation

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.paranid5.crescendo.core.common.AudioStatus
import com.paranid5.crescendo.feature.playing.presentation.effect.LifecycleEffect
import com.paranid5.crescendo.feature.playing.presentation.effect.ScreenEffect
import com.paranid5.crescendo.feature.playing.presentation.effect.UpdateUiParamsEffect
import com.paranid5.crescendo.feature.playing.presentation.ui.PlayingScreenLandscape
import com.paranid5.crescendo.feature.playing.presentation.ui.PlayingScreenPortrait
import com.paranid5.crescendo.feature.playing.view_model.PlayingScreenEffect
import com.paranid5.crescendo.feature.playing.view_model.PlayingUiIntent
import com.paranid5.crescendo.feature.playing.view_model.PlayingViewModel
import com.paranid5.crescendo.feature.playing.view_model.PlayingViewModelImpl
import com.paranid5.crescendo.utils.extensions.collectLatestAsState
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayingScreen(
    screenAudioStatus: AudioStatus,
    coverAlpha: Float,
    modifier: Modifier = Modifier,
    viewModel: PlayingViewModel = koinViewModel<PlayingViewModelImpl>(),
    onBack: (PlayingScreenEffect) -> Unit,
) {
    val config = LocalConfiguration.current
    val state by viewModel.stateFlow.collectLatestAsState()
    val onUiIntent = viewModel::onUiIntent

    LifecycleEffect(onUiIntent = onUiIntent)

    UpdateUiParamsEffect(
        screenAudioStatus = screenAudioStatus,
        coverAlpha = coverAlpha,
        onUiIntent = onUiIntent,
    )

    ScreenEffect(state = state, onScreenEffect = onBack) {
        onUiIntent(PlayingUiIntent.ScreenEffect.ClearScreenEffect)
    }

    when (config.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> PlayingScreenLandscape(
            state = state,
            onUiIntent = onUiIntent,
            modifier = modifier,
        )

        else -> PlayingScreenPortrait(
            state = state,
            onUiIntent = onUiIntent,
            modifier = modifier,
        )
    }
}
