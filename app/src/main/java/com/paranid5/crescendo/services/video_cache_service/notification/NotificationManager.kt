package com.paranid5.crescendo.services.video_cache_service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.paranid5.crescendo.core.resources.R
import com.paranid5.crescendo.core.common.caching.CachingStatus
import com.paranid5.crescendo.core.common.caching.DownloadingStatus
import com.paranid5.crescendo.core.common.metadata.VideoMetadata
import com.paranid5.crescendo.services.video_cache_service.VideoCacheService

internal const val VIDEO_CACHE_NOTIFICATION_ID = 103
internal const val VIDEO_CACHE_CHANNEL_ID = "video_cache_channel"

class NotificationManager(service: VideoCacheService) {
    private val manager by lazy {
        service.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel() = manager.createNotificationChannel(
        NotificationChannel(
            VIDEO_CACHE_CHANNEL_ID,
            "Video Cache",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
            enableLights(true)
        }
    )

    fun showNotification(
        service: VideoCacheService,
        downloadStatus: com.paranid5.crescendo.core.common.caching.DownloadingStatus,
        cacheStatus: com.paranid5.crescendo.core.common.caching.CachingStatus,
        videoMetadata: com.paranid5.crescendo.core.common.metadata.VideoMetadata,
        videoQueueLen: Int,
        downloadedBytes: Long,
        totalBytes: Long,
    ) = when (downloadStatus) {
        com.paranid5.crescendo.core.common.caching.DownloadingStatus.Downloading ->
            showDownloadNotification(
                service = service,
                videoTitle = service videoTitleOf videoMetadata,
                videoQueueLen = videoQueueLen,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes
            )

        com.paranid5.crescendo.core.common.caching.DownloadingStatus.CanceledCurrent ->
            showCanceledNotification(service)

        com.paranid5.crescendo.core.common.caching.DownloadingStatus.CanceledAll ->
            showCanceledNotification(service)

        com.paranid5.crescendo.core.common.caching.DownloadingStatus.ConnectionLost ->
            showConnectionLostNotification(service)

        com.paranid5.crescendo.core.common.caching.DownloadingStatus.Downloaded ->
            showCachingNotification(
                service = service,
                cacheStatus = cacheStatus,
                videoMetadata = videoMetadata,
            )

        else -> Unit
    }

    private fun showCachingNotification(
        service: VideoCacheService,
        cacheStatus: com.paranid5.crescendo.core.common.caching.CachingStatus,
        videoMetadata: com.paranid5.crescendo.core.common.metadata.VideoMetadata,
    ) = when (cacheStatus) {
        com.paranid5.crescendo.core.common.caching.CachingStatus.CONVERTING ->
            showConvertingNotification(
                context = service,
                videoTitle = service videoTitleOf videoMetadata
            )

        com.paranid5.crescendo.core.common.caching.CachingStatus.CONVERTED ->
            showCachedNotification(service)

        com.paranid5.crescendo.core.common.caching.CachingStatus.CANCELED_CUR ->
            showCanceledNotification(service)

        com.paranid5.crescendo.core.common.caching.CachingStatus.CANCELED_ALL ->
            showCanceledNotification(service)

        else -> Unit
    }

    private fun showDownloadNotification(
        service: VideoCacheService,
        videoTitle: String,
        videoQueueLen: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) = service.startForeground(
        VIDEO_CACHE_NOTIFICATION_ID,
        DownloadNotificationBuilder(
            context = service,
            videoTitle = videoTitle,
            videoQueueLen = videoQueueLen,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes
        ).build(),
    )

    private fun showCanceledNotification(context: Context) =
        manager.notify(
            VIDEO_CACHE_NOTIFICATION_ID,
            CanceledNotificationBuilder(context).build()
        )

    private fun showDownloadErrorNotification(context: Context, code: Int, description: String) =
        manager.notify(
            VIDEO_CACHE_NOTIFICATION_ID,
            DownloadErrorNotificationBuilder(context, code, description).build()
        )

    private fun showConnectionLostNotification(context: Context) =
        manager.notify(
            VIDEO_CACHE_NOTIFICATION_ID,
            ConnectionLostNotificationBuilder(context).build()
        )

    private fun showConvertingNotification(context: Context, videoTitle: String) =
        manager.notify(
            VIDEO_CACHE_NOTIFICATION_ID,
            ConvertingNotificationBuilder(context, videoTitle).build()
        )

    private fun showCachedNotification(context: Context) =
        manager.notify(
            VIDEO_CACHE_NOTIFICATION_ID,
            CachedNotificationBuilder(context).build()
        )
}

private infix fun Context.videoTitleOf(videoMetadata: com.paranid5.crescendo.core.common.metadata.VideoMetadata) =
    videoMetadata.title ?: getString(R.string.stream_no_name)