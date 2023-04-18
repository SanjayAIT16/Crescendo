package com.paranid5.mediastreamer.domain

import android.util.Log
import com.paranid5.mediastreamer.domain.video_cash_service.VideoCashService
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import java.io.File

private const val TAG = "Ktor Client"

fun KtorClient() = HttpClient(Android) {
    install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.ALL
    }
}

suspend inline fun HttpClient.getFileExt(fileUrl: String) =
    prepareGet(fileUrl).execute { response ->
        response.headers["content-type"]!!.split("/")[1]
    }

/**
 * Downloads youtube file by url
 * @param fileUrl url from youtube to media file
 * @param storeFile file in which all content will be stored
 * @param progressState channel that indicates downloading progress
 * with both current progress in bytes and total file's size
 * @return HTTP status code on this request
 */

internal suspend inline fun HttpClient.downloadFile(
    fileUrl: String,
    storeFile: File,
    progressState: MutableStateFlow<Pair<Long, Long>>? = null,
    cashingStatusState: MutableStateFlow<VideoCashService.CashingStatus>
): HttpStatusCode? {
    Log.d(TAG, "Downloading $fileUrl")

    val status = prepareGet(fileUrl).execute { response ->
        val status = response.status

        if (!status.isSuccess()) {
            cashingStatusState.update { VideoCashService.CashingStatus.ERR }
            return@execute status
        }

        val channel = response.bodyAsChannel()
        var progress = 0L

        while (!channel.isClosedForRead
            && cashingStatusState.value == VideoCashService.CashingStatus.CASHING
        ) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())

            while (packet.isNotEmpty
                && cashingStatusState.value == VideoCashService.CashingStatus.CASHING
            ) {
                val bytes = packet.readBytes()
                storeFile.appendBytes(bytes)

                progress += bytes.size
                progressState?.update { progress to response.contentLength()!! }

                Log.d(
                    TAG,
                    "Read ${bytes.size} bytes from ${response.contentLength()}. Total progress: $progress bytes"
                )
            }
        }

        val cashingStatus = cashingStatusState.updateAndGet {
            when (it) {
                VideoCashService.CashingStatus.CANCELED -> it
                else -> VideoCashService.CashingStatus.CASHED
            }
        }

        Log.d(TAG, "CashingStatus: ${cashingStatus.name}")
        if (cashingStatus == VideoCashService.CashingStatus.CANCELED) null else status
    }

    Log.d(TAG, "Done, status: ${status?.value}")
    return status
}