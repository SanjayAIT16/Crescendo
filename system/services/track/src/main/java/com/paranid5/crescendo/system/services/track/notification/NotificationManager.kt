package com.paranid5.crescendo.system.services.track.notification

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.paranid5.crescendo.data.datastore.DataStoreProvider
import com.paranid5.crescendo.data.datastore.sources.tracks.CurrentTrackSubscriberImpl
import com.paranid5.crescendo.domain.repositories.CurrentPlaylistRepository
import com.paranid5.crescendo.domain.sources.tracks.CurrentTrackSubscriber
import com.paranid5.crescendo.system.services.track.TrackService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

internal const val TRACKS_NOTIFICATION_ID = 102
internal const val TRACKS_CHANNEL_ID = "tracks_channel"

internal class NotificationManager(
    service: TrackService,
    dataStoreProvider: DataStoreProvider,
    currentPlaylistRepository: CurrentPlaylistRepository
) : CurrentTrackSubscriber by CurrentTrackSubscriberImpl(
    dataStoreProvider,
    currentPlaylistRepository,
) {
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