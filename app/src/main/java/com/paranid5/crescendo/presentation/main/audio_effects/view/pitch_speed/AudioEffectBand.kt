package com.paranid5.crescendo.presentation.main.audio_effects.view.pitch_speed

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paranid5.crescendo.core.resources.R
import com.paranid5.crescendo.presentation.main.audio_effects.view.rememberBandTrackPainter
import com.paranid5.crescendo.core.resources.ui.theme.LocalAppColors

@Composable
internal fun AudioEffectBand(
    effectTitle: String,
    effectValState: MutableFloatState,
    effectInputState: MutableState<String>,
    onValueChanged: (effectVal: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderWidthState = remember { mutableIntStateOf(1) }
    val sliderWidth by sliderWidthState

    val sliderHeightState = remember { mutableIntStateOf(1) }
    val sliderHeight by sliderHeightState

    Box(modifier) {
        AudioEffectSlider(
            sliderWidthState = sliderWidthState,
            sliderHeightState = sliderHeightState,
            effectValState = effectValState,
            effectInputState = effectInputState,
            onValueChanged = onValueChanged,
            modifier = Modifier.align(Alignment.Center),
            thumbModifier = Modifier
                .align(Alignment.Center)
                .height(20.dp)
        )

        BandTrack(
            sliderWidth = sliderWidth,
            sliderHeight = sliderHeight,
            effectTitle = effectTitle,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioEffectSlider(
    sliderWidthState: MutableState<Int>,
    sliderHeightState: MutableState<Int>,
    effectValState: MutableFloatState,
    effectInputState: MutableState<String>,
    onValueChanged: (effectVal: Float) -> Unit,
    modifier: Modifier = Modifier,
    thumbModifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    var sliderWidth by sliderWidthState
    var sliderHeight by sliderHeightState

    var effectVal by effectValState
    var effectInput by effectInputState

    Slider(
        value = effectVal,
        valueRange = 0.25F..2F,
        colors = SliderDefaults.colors(activeTrackColor = colors.primary),
        modifier = modifier.onGloballyPositioned {
            sliderWidth = it.size.width
            sliderHeight = it.size.height
        },
        onValueChange = { newEffectVal ->
            effectInput = newEffectVal.toString().take(MAX_INPUT_LENGTH)
            effectVal = newEffectVal
            onValueChanged(effectVal)
        },
        thumb = { SliderThumb(thumbModifier) },
    )
}

@Composable
private fun SliderThumb(modifier: Modifier = Modifier) =
    Image(
        painter = painterResource(R.drawable.audio_band_button),
        contentDescription = stringResource(R.string.pitch),
        contentScale = ContentScale.FillHeight,
        modifier = modifier,
    )

@Composable
private fun BandTrack(
    sliderWidth: Int,
    sliderHeight: Int,
    effectTitle: String,
    modifier: Modifier = Modifier
) {
    val bandTrackPainter = rememberBandTrackPainter(sliderWidth, sliderHeight)

    Image(
        painter = bandTrackPainter,
        contentDescription = effectTitle,
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center,
        modifier = modifier,
    )
}