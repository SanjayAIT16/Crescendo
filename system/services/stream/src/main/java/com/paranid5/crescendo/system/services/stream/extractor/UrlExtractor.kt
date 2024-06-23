package com.paranid5.crescendo.system.services.stream.extractor

import android.content.Context
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.paranid5.crescendo.core.common.metadata.VideoMetadata
import com.paranid5.crescendo.core.media.metadata.VideoMetadata.fromYtMeta
import com.paranid5.yt_url_extractor_kt.VideoMeta
import com.paranid5.yt_url_extractor_kt.YtFilesNotFoundException
import com.paranid5.yt_url_extractor_kt.YtRequestTimeoutException
import com.paranid5.yt_url_extractor_kt.extractYtFilesWithMeta
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TIMEOUT = 28000L
private const val DEFAULT_AUDIO_TAG = 140

internal class UrlExtractor : KoinComponent {
    private val ktorClient by inject<HttpClient>()

    suspend fun extractAudioUrlWithMeta(
        context: Context,
        ytUrl: String,
    ) = either {
        val (_, liveStreamManifestsRes, videoMetaRes) = extractYtFilesWithMeta(context, ytUrl)
            .mapLeft { YtRequestTimeoutException() }
            .bind()

        val videoMeta = videoMetaRes.getOrDefault()
        val liveStreamManifests = liveStreamManifestsRes.getOrNull()

        val audioUrl = when (videoMeta.isLiveStream) {
            true -> liveStreamManifests?.hlsManifestUrl
            else -> withContext(Dispatchers.IO) { extractWithYtDl(ytUrl) }
        }

        ensure(audioUrl != null) {
            YtFilesNotFoundException()
        }

        audioUrl to videoMeta
    }

    private suspend inline fun extractYtFilesWithMeta(context: Context, ytUrl: String) =
        Either.catch {
            withTimeout(TIMEOUT) {
                ktorClient
                    .extractYtFilesWithMeta(context, ytUrl)
                    .getOrThrow()
            }
        }
}

private fun extractWithYtDl(ytUrl: String) =
    YoutubeDL
        .getInstance()
        .getInfo(YoutubeDLRequest(ytUrl).apply { addOption("-f", "best") })
        .url

internal fun Result<VideoMeta>.getOrDefault() =
    getOrNull()?.let(::fromYtMeta) ?: VideoMetadata()