package com.paranid5.crescendo.services.track_service.notification

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.paranid5.crescendo.data.StorageHandler
import com.paranid5.crescendo.data.states.tracks.CurrentTrackStateSubscriber
import com.paranid5.crescendo.data.states.tracks.CurrentTrackStateSubscriberImpl
import com.paranid5.crescendo.services.track_service.TrackService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

internal const val TRACKS_NOTIFICATION_ID = 102
internal const val TRACKS_CHANNEL_ID = "tracks_channel"

class NotificationManager(service: TrackService, storageHandler: StorageHandler) :
    CurrentTrackStateSubscriber by CurrentTrackStateSubscriberImpl(storageHandler) {
    internal val currentTrackState by lazy {
        currentTrackFlow.stateIn(
            service.serviceScope,
            SharingStarted.WhileSubscribed(),
            null
        )
    }

    @delegate:UnstableApi
    private val playerNotificationManager by lazy {
        PlayerNotificationManager(service)
    }

    @OptIn(UnstableApi::class)
    fun initNotificationManager(player: Player) =
        playerNotificationManager.setPlayer(player)

    @OptIn(UnstableApi::class)
    fun updateNotification() = playerNotificationManager.invalidate()

    @OptIn(UnstableApi::class)
    fun releasePlayer() = playerNotificationManager.setPlayer(null)
}