package com.paranid5.crescendo.services.track_service.media_session

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.paranid5.crescendo.domain.tracks.Track
import com.paranid5.crescendo.domain.utils.extensions.toAndroidMetadata
import com.paranid5.crescendo.media.images.getTrackCoverBitmapAsync
import com.paranid5.crescendo.services.track_service.TrackService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

suspend fun TrackService.startMetadataMonitoring() =
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        mediaSessionManager
            .currentTrackFlow
            .distinctUntilChanged()
            .map { it?.toAndroidMetadata(context = this@startMetadataMonitoring) }
            .collectLatest { metadata ->
                metadata?.let(mediaSessionManager::updateMetadata)
            }
    }

private suspend inline fun Track.toAndroidMetadata(context: Context) =
    toAndroidMetadata(getTrackCoverBitmapAsync(context, path).await())