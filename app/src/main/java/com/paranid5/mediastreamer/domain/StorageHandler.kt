package com.paranid5.mediastreamer.domain

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.paranid5.mediastreamer.data.VideoMetadata
import com.paranid5.mediastreamer.data.eq.EqualizerData
import com.paranid5.mediastreamer.data.eq.EqualizerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StorageHandler(private val context: Context) : CoroutineScope by MainScope() {
    companion object {
        private val CURRENT_URL = stringPreferencesKey("current_url")
        private val CURRENT_METADATA = stringPreferencesKey("current_metadata")
        private val PLAYBACK_POSITION = longPreferencesKey("playback_position")
        private val IS_REPEATING = booleanPreferencesKey("is_repeating")
        private val AUDIO_EFFECTS_ENABLED = booleanPreferencesKey("audio_effects_enabled")
        private val PITCH_VALUE = floatPreferencesKey("pitch_value")
        private val SPEED_VALUE = floatPreferencesKey("speed_value")
        private val EQ_PARAMS = intPreferencesKey("eq_params")
        private val EQ_BANDS = stringPreferencesKey("eq_bands")
        private val EQ_PRESET = intPreferencesKey("eq_preset")
    }

    private val Context.dataStore by preferencesDataStore(name = "params")

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUrlState = context.dataStore.data
        .mapLatest { preferences -> preferences[CURRENT_URL] }
        .mapLatest { it ?: "" }
        .stateIn(this, SharingStarted.Eagerly, "")

    internal suspend inline fun storeCurrentUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[CURRENT_URL] = url }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMetadataState = context.dataStore.data
        .mapLatest { preferences -> preferences[CURRENT_METADATA] }
        .mapLatest { metaString -> metaString?.let { Json.decodeFromString<VideoMetadata>(it) } }
        .stateIn(this, SharingStarted.Eagerly, null)

    internal suspend inline fun storeCurrentMetadata(metadata: VideoMetadata?) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_METADATA] = Json.encodeToString(metadata)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val playbackPositionState = context.dataStore.data
        .mapLatest { preferences -> preferences[PLAYBACK_POSITION] }
        .mapLatest { it ?: 0 }
        .stateIn(this, SharingStarted.Eagerly, 0)

    internal suspend inline fun storePlaybackPosition(position: Long) {
        context.dataStore.edit { preferences -> preferences[PLAYBACK_POSITION] = position }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val isRepeatingState = context.dataStore.data
        .mapLatest { preferences -> preferences[IS_REPEATING] }
        .mapLatest { it ?: false }
        .stateIn(this, SharingStarted.Eagerly, false)

    internal suspend inline fun storeIsRepeating(isRepeating: Boolean) {
        context.dataStore.edit { preferences -> preferences[IS_REPEATING] = isRepeating }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val areAudioEffectsEnabledState = context.dataStore.data
        .mapLatest { preferences -> preferences[AUDIO_EFFECTS_ENABLED] }
        .mapLatest { it ?: false }
        .stateIn(this, SharingStarted.Eagerly, false)

    internal suspend inline fun storeAudioEffectsEnabled(areAudioEffectsEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_EFFECTS_ENABLED] = areAudioEffectsEnabled
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pitchState = context.dataStore.data
        .mapLatest { preferences -> preferences[PITCH_VALUE] }
        .mapLatest { it ?: 1.0F }
        .stateIn(this, SharingStarted.Eagerly, 1.0F)

    internal suspend inline fun storePitch(pitch: Float) {
        context.dataStore.edit { preferences -> preferences[PITCH_VALUE] = pitch }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val speedState = context.dataStore.data
        .mapLatest { preferences -> preferences[SPEED_VALUE] }
        .mapLatest { it ?: 1.0F }
        .stateIn(this, SharingStarted.Eagerly, 1.0F)

    internal suspend inline fun storeSpeed(speed: Float) {
        context.dataStore.edit { preferences -> preferences[SPEED_VALUE] = speed }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val equalizerBandsState = context.dataStore.data
        .mapLatest { preferences -> preferences[EQ_BANDS] }
        .mapLatest { bandsStr -> bandsStr?.let { Json.decodeFromString<List<Short>?>(it) } }
        .stateIn(this, SharingStarted.Eagerly, null)

    internal suspend inline fun storeEqualizerBands(bands: List<Short>?) {
        context.dataStore.edit { preferences ->
            preferences[EQ_BANDS] = Json.encodeToString(bands)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val equalizerPresetState = context.dataStore.data
        .mapLatest { preferences -> preferences[EQ_PRESET] }
        .mapLatest { preset -> preset?.toShort() ?: EqualizerData.NO_EQ_PRESET }
        .stateIn(this, SharingStarted.Eagerly, EqualizerData.NO_EQ_PRESET)

    internal suspend inline fun storeEqualizerPreset(preset: Short) {
        context.dataStore.edit { preferences -> preferences[EQ_PRESET] = preset.toInt() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val equalizerParamsState = context.dataStore.data
        .mapLatest { preferences -> preferences[EQ_PARAMS] }
        .mapLatest { param -> param ?: 0 }
        .mapLatest { param -> EqualizerParameters.values()[param] }
        .stateIn(this, SharingStarted.Eagerly, EqualizerParameters.NIL)

    internal suspend inline fun storeEqualizerParam(param: EqualizerParameters) {
        context.dataStore.edit { preferences -> preferences[EQ_PARAMS] = param.ordinal }
    }
}