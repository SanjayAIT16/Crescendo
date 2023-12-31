package com.paranid5.crescendo.services.stream_service.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import com.paranid5.crescendo.R
import com.paranid5.crescendo.domain.utils.extensions.sendBroadcast
import com.paranid5.crescendo.media.images.getThumbnailBitmap
import com.paranid5.crescendo.media.images.getVideoCoverBitmapAsync
import com.paranid5.crescendo.presentation.main.MainActivity
import com.paranid5.crescendo.services.stream_service.ACTION_DISMISS
import com.paranid5.crescendo.services.stream_service.ACTION_REPEAT
import com.paranid5.crescendo.services.stream_service.ACTION_UNREPEAT
import com.paranid5.crescendo.services.stream_service.StreamService2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
fun PlayerNotificationManager(service: StreamService2) =
    PlayerNotificationManager.Builder(
        service,
        NOTIFICATION_ID,
        STREAM_CHANNEL_ID
    )
        .setChannelNameResourceId(R.string.app_name)
        .setChannelDescriptionResourceId(R.string.app_name)
        .setChannelImportance(NotificationUtil.IMPORTANCE_HIGH)
        .setNotificationListener(NotificationListener(service))
        .setMediaDescriptionAdapter(MediaDescriptionProvider(service))
        .setCustomActionReceiver(CustomActionsReceiver(service))
        .setFastForwardActionIconResourceId(R.drawable.next_track)
        .setRewindActionIconResourceId(R.drawable.prev_track)
        .setPlayActionIconResourceId(R.drawable.play)
        .setPauseActionIconResourceId(R.drawable.pause)
        .build()
        .apply {
            setUseStopAction(false)
            setUseChronometer(false)
            setUseNextAction(false)
            setUsePreviousAction(false)
            setUseNextActionInCompactView(false)
            setUsePreviousActionInCompactView(false)

            setPriority(NotificationCompat.PRIORITY_HIGH)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setMediaSessionToken(service.mediaSessionManager.sessionToken)
        }

@OptIn(UnstableApi::class)
private fun NotificationListener(service: StreamService2) =
    object : PlayerNotificationManager.NotificationListener {
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            super.onNotificationCancelled(notificationId, dismissedByUser)
            service.detachNotification()
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            super.onNotificationPosted(notificationId, notification, ongoing)
            service.startForeground(notificationId, notification)
        }
    }

@OptIn(UnstableApi::class)
private fun MediaDescriptionProvider(service: StreamService2) =
    object : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player) =
            player.currentMediaItem?.mediaMetadata?.title?.toString()
                ?: service.getString(R.string.stream_no_name)

        override fun getCurrentContentText(player: Player) =
            player.currentMediaItem?.mediaMetadata?.artist?.toString()
                ?: service.getString(R.string.unknown_streamer)

        override fun createCurrentContentIntent(player: Player) =
            PendingIntent.getActivity(
                service,
                0,
                Intent(service, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            service.serviceScope.launch(Dispatchers.IO) {
                callback.onBitmap(
                    service
                        .notificationManager
                        .getVideoCoverAsync(service)
                        .await()
                )
            }

            return null
        }
    }

@OptIn(UnstableApi::class)
private fun CustomActionsReceiver(service: StreamService2) =
    object : PlayerNotificationManager.CustomActionReceiver {
        override fun createCustomActions(
            context: Context,
            instanceId: Int
        ) = mutableMapOf(
            ACTION_REPEAT to RepeatActionCompat(service),
            ACTION_UNREPEAT to UnrepeatActionCompat(service),
            ACTION_DISMISS to DismissNotificationActionCompat(service)
        )

        override fun getCustomActions(player: Player) =
            CustomActions(player.repeatMode)

        override fun onCustomAction(player: Player, action: String, intent: Intent) =
            service.sendBroadcast(service.commandsToActions[action]!!.playbackAction)
    }

private suspend inline fun NotificationManager.getVideoCoverAsync(context: Context) =
    currentMetadataState.value
        ?.let { getVideoCoverBitmapAsync(context = context, videoMetadata = it) }
        ?: coroutineScope {
            async(Dispatchers.IO) {
                getThumbnailBitmap(context = context)
            }
        }

private fun CustomActions(repeatMode: Int) = mutableListOf(
    when (repeatMode) {
        Player.REPEAT_MODE_ONE -> ACTION_REPEAT
        else -> ACTION_UNREPEAT
    },
    ACTION_DISMISS
)