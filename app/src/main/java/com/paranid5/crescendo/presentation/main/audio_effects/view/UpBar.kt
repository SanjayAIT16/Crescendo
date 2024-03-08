package com.paranid5.crescendo.presentation.main.audio_effects.view

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.paranid5.crescendo.core.resources.R
import com.paranid5.crescendo.koinActivityViewModel
import com.paranid5.crescendo.presentation.main.audio_effects.AudioEffectsViewModel
import com.paranid5.crescendo.presentation.main.audio_effects.properties.compose.collectAreAudioEffectsEnabledAsState
import com.paranid5.crescendo.presentation.ui.extensions.decreaseBrightness
import com.paranid5.crescendo.presentation.ui.extensions.simpleShadow
import com.paranid5.crescendo.presentation.ui.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun UpBar(modifier: Modifier = Modifier) = Box(modifier) {
    AudioEffectsLabel(Modifier.align(Alignment.Center))
    AudioEffectsSwitch(Modifier.align(Alignment.CenterEnd))
}

@Composable
private fun AudioEffectsLabel(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current

    Text(
        text = stringResource(R.string.audio_effects),
        color = colors.primary,
        fontSize = 20.sp,
        modifier = modifier.simpleShadow(color = colors.primary)
    )
}

@Composable
private fun AudioEffectsSwitch(
    modifier: Modifier = Modifier,
    viewModel: AudioEffectsViewModel = koinActivityViewModel(),
) {
    val colors = LocalAppColors.current

    val primaryColor = colors.primary
    val argbPrimaryColor by rememberArgbPrimaryColor(primaryColor)

    val areAudioEffectsEnabled by viewModel.collectAreAudioEffectsEnabledAsState()

    Switch(
        modifier = modifier,
        checked = areAudioEffectsEnabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = primaryColor,
            checkedTrackColor = Color(argbPrimaryColor.decreaseBrightness(0.5F)),
            checkedBorderColor = Color(argbPrimaryColor.decreaseBrightness(0.25F))
        ),
        onCheckedChange = {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.setAudioEffectsEnabled(it)
            }
        }
    )
}

@Composable
private fun rememberArgbPrimaryColor(primaryColor: Color) =
    remember(primaryColor) { derivedStateOf { primaryColor.toArgb() } }