package com.paranid5.mediastreamer.presentation.audio_effects

import android.content.Context
import com.paranid5.mediastreamer.data.eq.EqualizerParameters
import com.paranid5.mediastreamer.domain.StorageHandler
import com.paranid5.mediastreamer.domain.stream_service.StreamService
import com.paranid5.mediastreamer.domain.track_service.TrackService
import com.paranid5.mediastreamer.domain.utils.extensions.sendBroadcast
import com.paranid5.mediastreamer.presentation.UIHandler
import com.paranid5.mediastreamer.presentation.ui.handleOrIgnore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AudioEffectsUIHandler :
    UIHandler, KoinComponent, CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val storageHandler by inject<StorageHandler>()
    private val audioStatusState = storageHandler.audioStatusState

    private inline val audioStatus
        get() = audioStatusState.value

    private fun Context.sendAudioEffectsBroadcast(
        streamBroadcastAction: String,
        trackBroadcastAction: String
    ) = audioStatus.handleOrIgnore(
        streamAction = { sendBroadcast(streamBroadcastAction) },
        trackAction = { sendBroadcast(trackBroadcastAction) }
    )

    fun storeAudioEffectsEnabledAsync(context: Context, isEnabled: Boolean) = launch {
        storageHandler.storeAudioEffectsEnabled(isEnabled)

        context.sendAudioEffectsBroadcast(
            streamBroadcastAction = StreamService.Broadcast_AUDIO_EFFECTS_ENABLED_UPDATE,
            trackBroadcastAction = TrackService.Broadcast_AUDIO_EFFECTS_ENABLED_UPDATE
        )
    }

    fun isParamInputValid(input: String) =
        input.takeIf { s -> s.toFloatOrNull()?.takeIf { it in 0.25F..2F } != null } != null

    fun storePitchAsync(pitch: Float) = launch { storageHandler.storePitch(pitch) }

    fun storeSpeedAsync(speed: Float) = launch { storageHandler.storeSpeed(speed) }

    private fun sendEqualizerParamUpdate(context: Context) = context.sendAudioEffectsBroadcast(
        streamBroadcastAction = StreamService.Broadcast_EQUALIZER_PARAM_UPDATE,
        trackBroadcastAction = TrackService.Broadcast_EQUALIZER_PARAM_UPDATE
    )

    fun storeAndSwitchToPresetAsync(context: Context, preset: Short) = launch {
        storageHandler.storeEqualizerPreset(preset)
        storageHandler.storeEqualizerParam(EqualizerParameters.PRESET)
        sendEqualizerParamUpdate(context)
    }

    fun switchToBandsAsync(context: Context) = launch {
        storageHandler.storeEqualizerParam(EqualizerParameters.BANDS)
        sendEqualizerParamUpdate(context)
    }

    fun storeAndSwitchToBandsAsync(context: Context, bandLevels: List<Short>) = launch {
        storageHandler.storeEqualizerBands(bandLevels)
        switchToBandsAsync(context).join()
    }

    fun storeAndSendBassStrengthAsync(context: Context, bassStrength: Short) = launch {
        storageHandler.storeBassStrength(bassStrength)

        context.sendAudioEffectsBroadcast(
            streamBroadcastAction = StreamService.Broadcast_BASS_STRENGTH_UPDATE,
            trackBroadcastAction = TrackService.Broadcast_BASS_STRENGTH_UPDATE
        )
    }

    fun storeAndSendReverbPresetAsync(context: Context, reverbPreset: Short) = launch {
        storageHandler.storeReverbPreset(reverbPreset)

        context.sendAudioEffectsBroadcast(
            streamBroadcastAction = StreamService.Broadcast_REVERB_PRESET_UPDATE,
            trackBroadcastAction = TrackService.Broadcast_REVERB_PRESET_UPDATE
        )
    }
}