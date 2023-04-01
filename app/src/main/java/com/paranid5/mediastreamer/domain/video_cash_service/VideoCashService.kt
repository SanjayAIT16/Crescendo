package com.paranid5.mediastreamer.domain.video_cash_service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import arrow.core.Either
import arrow.core.merge
import com.paranid5.mediastreamer.R
import com.paranid5.mediastreamer.data.VideoMetadata
import com.paranid5.mediastreamer.domain.YoutubeUrlExtractor
import com.paranid5.mediastreamer.domain.downloadFile
import com.paranid5.mediastreamer.domain.getFileExt
import com.paranid5.mediastreamer.domain.media_scanner.MediaScannerReceiver
import com.paranid5.mediastreamer.domain.media_scanner.scanNextFile
import com.paranid5.mediastreamer.domain.utils.AsyncCondVar
import com.paranid5.mediastreamer.presentation.MainActivity
import com.paranid5.mediastreamer.presentation.streaming.Broadcast_VIDEO_CASH_COMPLETED
import com.paranid5.mediastreamer.presentation.streaming.VIDEO_CASH_STATUS
import com.paranid5.mediastreamer.domain.utils.extensions.insertMediaFileToMediaStore
import com.paranid5.mediastreamer.domain.utils.extensions.registerReceiverCompat
import com.paranid5.mediastreamer.domain.utils.extensions.setAudioTagsToFileCatching
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class VideoCashService : Service(), CoroutineScope by MainScope(), KoinComponent {
    companion object {
        private const val NOTIFICATION_ID = 102
        private const val VIDEO_CASH_CHANNEL_ID = "video_cash_channel"

        private const val SERVICE_LOCATION = "com.paranid5.mediastreamer.domain.video_cash_service"
        const val Broadcast_CASH_NEXT_VIDEO = "$SERVICE_LOCATION.CASH_NEXT_VIDEO"
        const val Broadcast_CANCEL_CUR_VIDEO = "$SERVICE_LOCATION.CANCEL_CUR_VIDEO"
        const val Broadcast_CANCEL_ALL = "$SERVICE_LOCATION.CANCEL_ALL"

        const val URL_ARG = "url"
        const val FILENAME_ARG = "filename"
        const val SAVE_AS_VIDEO_ARG = "save_as_video"

        private val TAG = VideoCashService::class.simpleName!!

        internal inline val Intent.mVideoCashDataArg
            get() = VideoCashData(
                url = getStringExtra(URL_ARG)!!,
                desiredFilename = getStringExtra(FILENAME_ARG)!!,
                isSaveAsVideo = getBooleanExtra(SAVE_AS_VIDEO_ARG, true)
            )
    }

    sealed class Actions(val requestCode: Int, val playbackAction: String) {
        object CancelCurVideo : Actions(
            requestCode = NOTIFICATION_ID + 1,
            playbackAction = Broadcast_CANCEL_CUR_VIDEO
        )

        object CancelAll : Actions(
            requestCode = NOTIFICATION_ID + 2,
            playbackAction = Broadcast_CANCEL_ALL
        )
    }

    private inline val Actions.playbackIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            this@VideoCashService,
            requestCode,
            Intent(playbackAction),
            PendingIntent.FLAG_MUTABLE
        )

    internal data class VideoCashData(
        val url: String,
        val desiredFilename: String,
        val isSaveAsVideo: Boolean
    )

    private val binder = object : Binder() {}
    private val ktorClient by inject<HttpClient>()

    private val videoCashQueue: Queue<VideoCashData> = ConcurrentLinkedQueue()
    private val videoCashQueueLenState = MutableStateFlow(0)

    private val videoCashCompletionChannel = Channel<HttpStatusCode?>()
    private val videoCashProgressState = MutableStateFlow(0L to 0L)
    private val videoCashCondVar = AsyncCondVar()

    private var cashingLoopJob: Job? = null
    private var curVideoCashJob: Deferred<HttpStatusCode?>? = null
    private var curVideoCashFile: File? = null
    private val curVideoMetadataState = MutableStateFlow<VideoMetadata?>(null)

    enum class CashingStatus { CASHING, CASHED, CANCELED, NONE }

    private val cashingStatusState = MutableStateFlow(CashingStatus.NONE)

    @Volatile
    private var wasStartForegroundUsed = false

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private inline val res
        get() = applicationContext.resources

    private val cashNextVideoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            Log.d(TAG, "Cash next is received")
            launch { mOfferVideoToQueue(videoCashData = intent.mVideoCashDataArg) }
        }
    }

    private val cancelCurVideoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Canceling video cashing")
            launch { mCancelCurVideoCashing() }
        }
    }

    private val cancelAllReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Canceling all videos cashing")
            launch { mCancelAllVideosCashing() }
        }
    }

    private val mediaScannerReceiver = MediaScannerReceiver()

    private fun registerReceivers() {
        registerReceiverCompat(cashNextVideoReceiver, Broadcast_CASH_NEXT_VIDEO)
        registerReceiverCompat(cancelCurVideoReceiver, Broadcast_CANCEL_CUR_VIDEO)
        registerReceiverCompat(cancelAllReceiver, Broadcast_CANCEL_ALL)
        registerReceiverCompat(mediaScannerReceiver, MediaScannerReceiver.Broadcast_SCAN_NEXT_FILE)
    }

    override fun onCreate() {
        super.onCreate()
        registerReceivers()
    }

    override fun onBind(intent: Intent?) = binder

    internal suspend inline fun mOfferVideoToQueue(videoCashData: VideoCashData) {
        Log.d(TAG, "New video added to queue")
        videoCashQueue.offer(videoCashData)
        videoCashQueueLenState.update { videoCashQueue.size }
        videoCashCondVar.notify()
        resetCashingJobIfCanceled()
    }

    private data class CashingNotificationData(
        val cashingState: CashingStatus,
        val metadata: VideoMetadata,
        val videoCashQueueLen: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
    )

    private suspend inline fun startNotificationObserving(): Unit = combine(
        cashingStatusState,
        curVideoMetadataState,
        videoCashQueueLenState,
        videoCashProgressState,
    ) { cashingState, videoMetadata, videoCashQueueLen, (downloadedBytes, totalBytes) ->
        CashingNotificationData(
            cashingState,
            videoMetadata ?: VideoMetadata(),
            videoCashQueueLen,
            downloadedBytes,
            totalBytes,
        )
    }.collectLatest { (cashingState, videoMetadata, videoCashQueueLen, downloadedBytes, totalBytes) ->
        updateNotification(
            cashingState,
            videoMetadata,
            videoCashQueueLen,
            downloadedBytes,
            totalBytes
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel()

        launch { mOfferVideoToQueue(videoCashData = intent!!.mVideoCashDataArg) }
        resetCashingJobIfCanceled()
        launch { startNotificationObserving() }
        return START_REDELIVER_INTENT
    }

    // --------------------- File Cashing ---------------------

    private fun getFullMediaDirectory(mediaDirectory: String) =
        Environment
            .getExternalStoragePublicDirectory(mediaDirectory)
            .absolutePath

    private fun createMediaFile(filename: String, ext: String) =
        "${getFullMediaDirectory(Environment.DIRECTORY_MOVIES)}/$filename.$ext"
            .takeIf { !File(it).exists() }
            ?.let(::File)
            ?.also { Log.d(TAG, "Creating file ${it.absolutePath}") }
            ?.also(File::createNewFile)
            ?: generateSequence(1) { it + 1 }
                .map { num -> "${getFullMediaDirectory(Environment.DIRECTORY_MOVIES)}/${filename}($num).$ext" }
                .map(::File)
                .first { !it.exists() }
                .also(File::createNewFile)

    private fun createMediaFileCatching(filename: String, ext: String) =
        kotlin.runCatching { createMediaFile(filename, ext) }

    private suspend inline fun setTags(mediaFile: File, videoMetadata: VideoMetadata) {
        val externalContentUri = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else ->
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val mediaDirectory = Environment.DIRECTORY_MOVIES
        val mimeType = "video/${mediaFile.extension}"
        val absoluteFilePath = mediaFile.absolutePath

        withContext(Dispatchers.IO) {
            insertMediaFileToMediaStore(
                externalContentUri,
                absoluteFilePath,
                mediaDirectory,
                videoMetadata,
                mimeType
            )

            setAudioTagsToFileCatching(mediaFile, videoMetadata)
            scanNextFile(absoluteFilePath)
        }
    }

    private suspend inline fun cashMediaFile(
        desiredFilename: String,
        audioOrVideoUrl: Either<String, String>,
        videoMetadata: VideoMetadata
    ): HttpStatusCode? {
        val mediaUrl = audioOrVideoUrl.merge()
        val fileExt = ktorClient.getFileExt(mediaUrl)
        val storeFile = createMediaFileCatching(desiredFilename, fileExt)
            .getOrNull()
            ?: return HttpStatusCode.BadRequest

        curVideoCashFile = storeFile

        val statusCode = ktorClient.downloadFile(
            fileUrl = mediaUrl,
            storeFile = storeFile,
            progressState = videoCashProgressState,
            cashingStatusState = cashingStatusState
        )

        statusCode?.takeIf { it.isSuccess() }?.let {
            setTags(
                mediaFile = storeFile,
                videoMetadata = videoMetadata
            )
        }

        return statusCode
    }

    private fun YoutubeUrlExtractor(desiredFilename: String, isSaveAsVideo: Boolean) =
        YoutubeUrlExtractor(
            context = applicationContext,
            videoExtractionChannel = videoCashCompletionChannel
        ) { audioUrl, videoUrl, videoMeta ->
            val videoMetadata = videoMeta?.let(::VideoMetadata) ?: VideoMetadata()

            val audioOrVideoUrl = when {
                isSaveAsVideo -> Either.Right(videoUrl)
                else -> Either.Left(audioUrl)
            }

            curVideoMetadataState.update { videoMetadata }
            cashMediaFile(desiredFilename, audioOrVideoUrl, videoMetadata)
        }

    private suspend inline fun launchExtractionAndCashingFile(
        url: String,
        desiredFilename: String,
        isSaveAsVideo: Boolean
    ): HttpStatusCode? = coroutineScope {
        launch(Dispatchers.IO) { YoutubeUrlExtractor(desiredFilename, isSaveAsVideo).extract(url) }
        videoCashCompletionChannel.receive()
    }

    private fun clearVideoCashStates() {
        curVideoCashJob = null
        curVideoCashFile = null
    }

    private fun onVideoCashStatusReceived(statusCode: HttpStatusCode?) {
        Log.d(TAG, "Cash status handling")
        clearVideoCashStates()

        when {
            statusCode == null -> onVideoCashStatusCanceled()
            statusCode.isSuccess() -> onVideoCashStatusSuccessful()
            else -> onVideoCashStatusError(statusCode.value, statusCode.description)
        }

        Log.d(TAG, "Cash status handled")
    }

    private fun onVideoCashStatusSuccessful() = sendBroadcast(
        Intent(Broadcast_VIDEO_CASH_COMPLETED)
            .putExtra(VIDEO_CASH_STATUS, VideoCashResponse.Success)
    )

    private fun onVideoCashStatusError(code: Int, description: String) = sendBroadcast(
        Intent(Broadcast_VIDEO_CASH_COMPLETED)
            .putExtra(VIDEO_CASH_STATUS, VideoCashResponse.Error(code, description))
    )

    private fun onVideoCashStatusCanceled() = sendBroadcast(
        Intent(Broadcast_VIDEO_CASH_COMPLETED)
            .putExtra(VIDEO_CASH_STATUS, VideoCashResponse.Canceled)
    )

    private suspend inline fun launchCashing() = coroutineScope {
        while (true) {
            Log.d(TAG, "QUEUE ${videoCashQueue.size}")

            if (videoCashQueue.isEmpty()) {
                detachNotification()
                videoCashCondVar.wait()
                Log.d(TAG, "Cond Var Awake")
            }

            videoCashQueue.poll()?.let { (url, desiredFilename, isSaveAsVideo) ->
                Log.d(TAG, "Prepare for cashing")
                cashingStatusState.update { CashingStatus.CASHING }
                videoCashQueueLenState.update { videoCashQueue.size }

                curVideoCashJob = async(Dispatchers.IO) {
                    launchExtractionAndCashingFile(url, desiredFilename, isSaveAsVideo)
                }

                onVideoCashStatusReceived(statusCode = curVideoCashJob!!.await())
            }
        }
    }

    private fun resetCashingJobIfCanceled() {
        Log.d(TAG, "Cashing loop status: ${cashingLoopJob?.isActive}")

        if (cashingLoopJob?.isActive != true)
            cashingLoopJob = launch(Dispatchers.IO) { launchCashing() }
    }

    internal suspend inline fun mCancelCurVideoCashing() {
        cashingStatusState.update { CashingStatus.CANCELED }
        Log.d(TAG, "Canceling job and removing file")
        curVideoCashJob?.cancelAndJoin()
        Log.d(TAG, "File is deleted ${curVideoCashFile?.delete()}")
        Log.d(TAG, "Cashing is canceled")
        clearVideoCashStates()
    }

    internal suspend inline fun mCancelAllVideosCashing() {
        videoCashQueue.clear()
        videoCashQueueLenState.update { 0 }
        mCancelCurVideoCashing()
    }

    private fun unregisterReceivers() {
        unregisterReceiver(cashNextVideoReceiver)
        unregisterReceiver(cancelCurVideoReceiver)
        unregisterReceiver(cancelAllReceiver)
        unregisterReceiver(mediaScannerReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }

    // --------------------- Notifications ---------------------

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() = notificationManager.createNotificationChannel(
        NotificationChannel(
            VIDEO_CASH_CHANNEL_ID,
            "Video Cash",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
            enableLights(true)
        }
    )

    private inline val notificationBuilder
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                Notification.Builder(applicationContext, VIDEO_CASH_CHANNEL_ID)
            else ->
                Notification.Builder(applicationContext)
        }
            .setSmallIcon(R.drawable.save_icon)
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    Intent(applicationContext, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

    private fun getCashingNotificationBuilder(
        videoMetadata: VideoMetadata,
        videoCashQueueLen: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) = notificationBuilder
        .setContentTitle("${res.getString(R.string.downloading)}: ${videoMetadata.title}")
        .setContentText("${res.getString(R.string.tracks_in_queue)}: $videoCashQueueLen")
        .setProgress(totalBytes.toInt(), downloadedBytes.toInt(), false)
        .setOngoing(true)
        .setShowWhen(false)
        .setOnlyAlertOnce(true)
        .addAction(cancelCurVideoAction)
        .addAction(cancelAllAction)

    private inline val cancelCurVideoAction
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Notification.Action.Builder(
                null,
                res.getString(R.string.cancel),
                Actions.CancelCurVideo.playbackIntent
            )

            else -> Notification.Action.Builder(
                0,
                res.getString(R.string.cancel),
                Actions.CancelCurVideo.playbackIntent
            )
        }.build()

    private inline val cancelAllAction
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Notification.Action.Builder(
                null,
                res.getString(R.string.cancel_all),
                Actions.CancelAll.playbackIntent
            )

            else -> Notification.Action.Builder(
                0,
                res.getString(R.string.cancel_all),
                Actions.CancelAll.playbackIntent
            )
        }.build()

    private fun finishedNotificationBuilder(@StringRes message: Int) = notificationBuilder
        .setContentTitle(res.getString(message))
        .setOngoing(false)
        .setShowWhen(false)

    private inline val cashedNotificationBuilder
        get() = finishedNotificationBuilder(R.string.video_cashed)

    private inline val canceledNotificationBuilder
        get() = finishedNotificationBuilder(R.string.video_canceled)

    private fun startCashingNotification(
        videoMetadata: VideoMetadata,
        videoCashQueueLen: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        wasStartForegroundUsed = true

        startForeground(
            NOTIFICATION_ID,
            getCashingNotificationBuilder(
                videoMetadata,
                videoCashQueueLen,
                downloadedBytes,
                totalBytes
            ).build()
        )
    }

    private fun updateCashingNotification(
        videoMetadata: VideoMetadata,
        videoCashQueueLen: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) = notificationManager.notify(
        NOTIFICATION_ID,
        getCashingNotificationBuilder(
            videoMetadata,
            videoCashQueueLen,
            downloadedBytes,
            totalBytes
        ).build()
    )

    private fun showCashedNotification() = notificationManager.notify(
        NOTIFICATION_ID,
        cashedNotificationBuilder.build()
    )

    private fun showCanceledNotification() = notificationManager.notify(
        NOTIFICATION_ID,
        canceledNotificationBuilder.build()
    )

    private fun updateNotification(
        cashingState: CashingStatus,
        videoMetadata: VideoMetadata,
        videoCashQueueLen: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) = when {
        !wasStartForegroundUsed -> startCashingNotification(
            videoMetadata,
            videoCashQueueLen,
            downloadedBytes,
            totalBytes
        )

        else -> when (cashingState) {
            CashingStatus.CASHING -> updateCashingNotification(
                videoMetadata,
                videoCashQueueLen,
                downloadedBytes,
                totalBytes
            )

            CashingStatus.CASHED -> showCashedNotification()
            CashingStatus.CANCELED -> showCanceledNotification()
            CashingStatus.NONE -> Unit
        }
    }

    private fun detachNotification() {
        notificationManager.cancel(NOTIFICATION_ID)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ->
                stopForeground(STOP_FOREGROUND_DETACH)
            else ->
                stopForeground(true)
        }
    }
}