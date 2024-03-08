package com.paranid5.crescendo.services.video_cache_service.files

import com.paranid5.crescendo.core.common.caching.VideoCacheData
import com.paranid5.crescendo.core.common.metadata.VideoMetadata
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VideoQueueManager {
    private val _videoQueueFlow by lazy {
        MutableSharedFlow<com.paranid5.crescendo.core.common.caching.VideoCacheData>(extraBufferCapacity = 1000)
    }

    val videoQueueFlow by lazy {
        _videoQueueFlow.asSharedFlow()
    }

    private val _videoQueueLenState by lazy {
        MutableStateFlow(0)
    }

    val videoQueueLenState by lazy {
        _videoQueueLenState.asStateFlow()
    }

    private val _currentVideoMetadataState by lazy {
        MutableStateFlow(com.paranid5.crescendo.core.common.metadata.VideoMetadata())
    }

    val currentVideoMetadataState by lazy {
        _currentVideoMetadataState.asStateFlow()
    }

    fun resetVideoMetadata(videoMetadata: com.paranid5.crescendo.core.common.metadata.VideoMetadata) =
        _currentVideoMetadataState.update { videoMetadata }

    suspend fun offerNewVideo(videoCacheData: com.paranid5.crescendo.core.common.caching.VideoCacheData) {
        _videoQueueFlow.emit(videoCacheData)
        _videoQueueLenState.update { it + 1 }
    }

    fun decrementQueueLen() =
        _videoQueueLenState.update { maxOf(it - 1, 0) }

    fun onCanceledAll() =
        _videoQueueLenState.update { 0 }
}