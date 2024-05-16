package com.paranid5.crescendo.system.services.track

import android.content.Intent
import android.os.Build
import com.paranid5.crescendo.core.common.tracks.Track
import com.paranid5.crescendo.data.StorageRepository
import com.paranid5.crescendo.domain.interactor.tracks.TrackServiceStart
import com.paranid5.crescendo.domain.repositories.CurrentPlaylistRepository
import com.paranid5.crescendo.system.common.broadcast.TrackServiceBroadcasts
import com.paranid5.crescendo.system.services.track.media_session.MediaSessionCallback
import com.paranid5.crescendo.system.services.track.media_session.startMetadataMonitoring
import com.paranid5.crescendo.system.services.track.media_session.startPlaybackStatesMonitoring
import com.paranid5.crescendo.system.services.track.notification.NotificationManager
import com.paranid5.crescendo.system.services.track.notification.startNotificationMonitoring
import com.paranid5.crescendo.system.services.track.playback.PlayerProvider
import com.paranid5.crescendo.system.services.track.playback.playPlaylistAsync
import com.paranid5.crescendo.system.services.track.playback.startBassMonitoring
import com.paranid5.crescendo.system.services.track.playback.startEqMonitoring
import com.paranid5.crescendo.system.services.track.playback.startPlaybackEffectsMonitoring
import com.paranid5.crescendo.system.services.track.playback.startPlaybackEventLoop
import com.paranid5.crescendo.system.services.track.playback.startResumingAsync
import com.paranid5.crescendo.system.services.track.playback.startReverbMonitoring
import com.paranid5.crescendo.system.services.track.receivers.AddTrackReceiver
import com.paranid5.crescendo.system.services.track.receivers.PauseReceiver
import com.paranid5.crescendo.system.services.track.receivers.PlaylistDraggedReceiver
import com.paranid5.crescendo.system.services.track.receivers.RemoveTrackReceiver
import com.paranid5.crescendo.system.services.track.receivers.RepeatChangedReceiver
import com.paranid5.crescendo.system.services.track.receivers.ResumeReceiver
import com.paranid5.crescendo.system.services.track.receivers.SeekToNextTrackReceiver
import com.paranid5.crescendo.system.services.track.receivers.SeekToPrevTrackReceiver
import com.paranid5.crescendo.system.services.track.receivers.SeekToReceiver
import com.paranid5.crescendo.system.services.track.receivers.SwitchPlaylistReceiver
import com.paranid5.crescendo.system.services.track.receivers.registerReceivers
import com.paranid5.crescendo.system.services.track.receivers.unregisterReceivers
import com.paranid5.system.services.common.ConnectionManager
import com.paranid5.system.services.common.SuspendService
import com.paranid5.system.services.common.connect
import com.paranid5.system.services.common.disconnect
import com.paranid5.system.services.common.media_session.MediaSessionManager
import com.paranid5.system.services.common.notification.detachNotification
import com.paranid5.system.services.common.receivers.DismissNotificationReceiver
import com.paranid5.system.services.common.receivers.StopReceiver
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val ACTION_PAUSE = "pause"
private const val ACTION_RESUME = "resume"
private const val ACTION_PREV_TRACK = "prev_track"
private const val ACTION_NEXT_TRACK = "next_track"

internal const val ACTION_REPEAT = "repeat"
internal const val ACTION_UNREPEAT = "unrepeat"
internal const val ACTION_DISMISS = "dismiss"

class TrackService : SuspendService(), KoinComponent,
    ConnectionManager by ConnectionManagerImpl() {
    private val storageRepository by inject<StorageRepository>()
    private val currentPlaylistRepository by inject<CurrentPlaylistRepository>()

    internal val mediaSessionManager by lazy {
        MediaSessionManager(
            storageRepository,
            currentPlaylistRepository
        )
    }

    internal val playerProvider by lazy {
        PlayerProvider(this, storageRepository, currentPlaylistRepository)
    }

    internal val notificationManager by lazy {
        NotificationManager(this, storageRepository, currentPlaylistRepository)
    }

    internal val commandsToActions = mapOf(
        ACTION_PAUSE to Actions.Pause,
        ACTION_RESUME to Actions.Resume,
        ACTION_PREV_TRACK to Actions.PrevTrack,
        ACTION_NEXT_TRACK to Actions.NextTrack,
        ACTION_REPEAT to Actions.Repeat,
        ACTION_UNREPEAT to Actions.Unrepeat,
        ACTION_DISMISS to Actions.Dismiss
    )

    internal val pauseReceiver = PauseReceiver(this)
    internal val resumeReceiver = ResumeReceiver(this)
    internal val switchPlaylistReceiver = SwitchPlaylistReceiver(this)
    internal val seekToReceiver = SeekToReceiver(this)
    internal val seekToNextTrackReceiver = SeekToNextTrackReceiver(this)
    internal val seekToPrevTrackReceiver = SeekToPrevTrackReceiver(this)
    internal val repeatChangedReceiver = RepeatChangedReceiver(this)
    internal val addTrackReceiver = AddTrackReceiver(this)
    internal val removeTrackReceiver = RemoveTrackReceiver(this)
    internal val playlistDraggedReceiver = PlaylistDraggedReceiver(this)
    internal val dismissNotificationReceiver = DismissNotificationReceiver(this)
    internal val stopReceiver = StopReceiver(this)

    override fun onCreate() {
        super.onCreate()
        registerReceivers()

        mediaSessionManager.initMediaSession(
            context = this,
            mediaSessionCallback = MediaSessionCallback(this)
        )

        notificationManager.initNotificationManager(playerProvider.player)

        launchMonitoringTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        connect(startId)

        when (intent!!.startType) {
            TrackServiceStart.RESUME -> startResumingAsync()
            TrackServiceStart.NEW_TRACK -> playPlaylistAsync()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()

        detachNotification()
        notificationManager.releasePlayer()

        playerProvider.releasePlayerWithEffects()
        mediaSessionManager.releaseMediaSession()

        unregisterReceivers()
    }
}

private fun TrackService.launchMonitoringTasks() {
    serviceScope.launch { startPlaybackEventLoop() }
    serviceScope.launch { startNotificationMonitoring() }
    serviceScope.launch { startPlaybackStatesMonitoring() }
    serviceScope.launch { startMetadataMonitoring() }
    serviceScope.launch { startPlaybackEffectsMonitoring() }
    serviceScope.launch { startEqMonitoring() }
    serviceScope.launch { startBassMonitoring() }
    serviceScope.launch { startReverbMonitoring() }
}

@Suppress("DEPRECATION")
internal inline val Intent.startType: TrackServiceStart
    get() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(
            TrackServiceBroadcasts.START_TYPE_ARG,
            TrackServiceStart::class.java
        )!!

        else -> getSerializableExtra(TrackServiceBroadcasts.START_TYPE_ARG) as TrackServiceStart
    }

@Suppress("DEPRECATION")
private inline val Intent.trackArgOrNull: Track?
    get() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            getParcelableExtra(TrackServiceBroadcasts.TRACK_ARG, Track::class.java)

        else -> getParcelableExtra(TrackServiceBroadcasts.TRACK_ARG)
    }

internal inline val Intent.trackArg
    get() = trackArgOrNull!!

internal inline val Intent.trackIndexArg
    get() = getIntExtra(TrackServiceBroadcasts.TRACK_INDEX_ARG, 0)

internal inline val Intent.positionArg
    get() = getLongExtra(TrackServiceBroadcasts.POSITION_ARG, 0)