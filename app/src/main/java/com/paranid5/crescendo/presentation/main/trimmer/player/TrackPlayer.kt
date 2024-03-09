package com.paranid5.crescendo.presentation.main.trimmer.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.paranid5.crescendo.core.common.tracks.Track
import com.paranid5.crescendo.utils.extensions.toMediaItem
import com.paranid5.crescendo.presentation.main.trimmer.TrimmerViewModel
import com.paranid5.crescendo.presentation.main.trimmer.effects.playback.notifyPlaybackTaskFinished
import com.paranid5.crescendo.presentation.main.trimmer.effects.playback.pausePlayback
import kotlinx.coroutines.launch

private const val TRANSITION_DURATION = 10_000L

@OptIn(UnstableApi::class)
fun TrackPlayer(
    context: Context,
    track: com.paranid5.crescendo.core.common.tracks.Track,
    viewModel: TrimmerViewModel,
    onCompletion: (Player) -> Unit
) = ExoPlayer.Builder(context)
    .setAudioAttributes(newAudioAttributes, true)
    .setHandleAudioBecomingNoisy(true)
    .setWakeMode(C.WAKE_MODE_NETWORK)
    .setPauseAtEndOfMediaItems(false)
    .build()
    .apply {
        addListener(playerStateChangedListener(this, viewModel, onCompletion))
        repeatMode = ExoPlayer.REPEAT_MODE_OFF
        setMediaItem(track.toMediaItem())
        prepare()
    }

fun TrackPlayer(context: Context, viewModel: TrimmerViewModel) =
    TrackPlayer(
        context = context,
        track = viewModel.trackState.value!!,
        viewModel = viewModel,
        onCompletion = {
            viewModel.viewModelScope.launch {
                viewModel.pausePlayback()
            }
        }
    )

@OptIn(UnstableApi::class)
private inline val newAudioAttributes
    get() = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()

private fun playerStateChangedListener(
    player: Player,
    viewModel: TrimmerViewModel,
    onCompletion: (Player) -> Unit
) = object : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)

        if (isPlaying) viewModel.launchPlaybackPosMonitorTask {
            PlaybackPositionMonitoringTask(player, viewModel)
            viewModel.notifyPlaybackTaskFinished()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)

        if (playbackState == ExoPlayer.STATE_ENDED)
            onCompletion(player)
    }
}

fun Player.seekTenSecsBack(startPosition: Long) =
    seekTo(maxOf(currentPosition - TRANSITION_DURATION, startPosition))

fun Player.seekTenSecsForward(totalDuration: Long) =
    seekTo(minOf(currentPosition + TRANSITION_DURATION, totalDuration))

fun Player.stopAndReleaseCatching() = runCatching {
    stop()
    release()
}