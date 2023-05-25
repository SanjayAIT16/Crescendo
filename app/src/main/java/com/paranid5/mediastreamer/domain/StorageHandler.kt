package com.paranid5.mediastreamer.domain

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.paranid5.mediastreamer.data.VideoMetadata
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
        context
            .dataStore
            .edit { preferences ->
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
}