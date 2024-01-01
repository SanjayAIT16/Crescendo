package com.paranid5.crescendo.services.track_service.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.paranid5.crescendo.services.track_service.TrackService
import com.paranid5.crescendo.services.track_service.sendErrorBroadcast
import kotlinx.coroutines.launch

fun PlayerStateChangedListener(service: TrackService) =
    object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            if (playbackState == Player.STATE_IDLE)
                service.serviceScope.launch {
                    service.playerProvider.restartPlayer()
                }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            service.playerProvider.isPlaying = isPlaying

            when {
                isPlaying -> service.serviceScope.launch {
                    service.startPlaybackPositionMonitoring()
                }

                else -> service.serviceScope.launch {
                    stopPlaybackPositionMonitoring()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            service.playerProvider.isStoppedWithError = true
            service.sendErrorBroadcast(error)
        }
    }