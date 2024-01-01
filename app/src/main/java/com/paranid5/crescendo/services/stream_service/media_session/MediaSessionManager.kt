package com.paranid5.crescendo.services.stream_service.media_session

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.paranid5.crescendo.data.StorageHandler
import com.paranid5.crescendo.data.states.stream.CurrentMetadataStatePublisher
import com.paranid5.crescendo.data.states.stream.CurrentMetadataStatePublisherImpl
import com.paranid5.crescendo.data.states.stream.CurrentMetadataStateSubscriber
import com.paranid5.crescendo.data.states.stream.CurrentMetadataStateSubscriberImpl

class MediaSessionManager(storageHandler: StorageHandler) :
    CurrentMetadataStateSubscriber by CurrentMetadataStateSubscriberImpl(storageHandler),
    CurrentMetadataStatePublisher by CurrentMetadataStatePublisherImpl(storageHandler) {
    private lateinit var mediaSession: MediaSessionCompat

    val sessionToken: MediaSessionCompat.Token
        get() = mediaSession.sessionToken

    fun initMediaSession(
        context: Context,
        mediaSessionCallback: MediaSessionCompat.Callback,
    ) {
        mediaSession = MediaSession(context, mediaSessionCallback)
    }

    fun updatePlaybackState(state: PlaybackStateCompat) =
        mediaSession.setPlaybackState(state)

    fun updateMetadata(metadata: MediaMetadataCompat) =
        mediaSession.setMetadata(metadata)

    fun releaseMediaSession() = mediaSession.release()
}