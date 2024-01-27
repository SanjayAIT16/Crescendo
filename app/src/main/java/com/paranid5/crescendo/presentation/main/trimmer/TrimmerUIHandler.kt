package com.paranid5.crescendo.presentation.main.trimmer

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import arrow.core.Either
import com.paranid5.crescendo.R
import com.paranid5.crescendo.domain.caching.Formats
import com.paranid5.crescendo.domain.media.files.MediaFile
import com.paranid5.crescendo.domain.media.files.trimmedCatching
import com.paranid5.crescendo.domain.metadata.AudioMetadata
import com.paranid5.crescendo.domain.tracks.Track
import com.paranid5.crescendo.domain.trimming.FadeDurations
import com.paranid5.crescendo.domain.trimming.PitchAndSpeed
import com.paranid5.crescendo.domain.trimming.TrimRange
import com.paranid5.crescendo.domain.utils.extensions.sendBroadcastCompat
import com.paranid5.crescendo.media.tags.setAudioTags
import com.paranid5.crescendo.presentation.UIHandler
import com.paranid5.crescendo.receivers.TrimmingStatusReceiver
import java.io.File

@Immutable
class TrimmerUIHandler : UIHandler {
    suspend fun trimTrackAndSendBroadcast(
        context: Context,
        track: Track,
        outputFilename: String,
        audioFormat: Formats,
        trimRange: TrimRange,
        pitchAndSpeed: PitchAndSpeed,
        fadeDurations: FadeDurations
    ) = context.sendTrimmingStatusBroadcast(
        trimTrackResult(
            context = context,
            track = track,
            outputFilename = outputFilename,
            audioFormat = audioFormat,
            trimRange = trimRange,
            pitchAndSpeed = pitchAndSpeed,
            fadeDurations = fadeDurations
        )
    )

    private suspend inline fun trimTrackResult(
        context: Context,
        track: Track,
        outputFilename: String,
        audioFormat: Formats,
        trimRange: TrimRange,
        pitchAndSpeed: PitchAndSpeed,
        fadeDurations: FadeDurations
    ) = MediaFile.AudioFile(File(track.path))
        .trimmedCatching(outputFilename, audioFormat, trimRange, pitchAndSpeed, fadeDurations)
        .onRight { file ->
            setAudioTags(
                context = context,
                audioFile = file,
                metadata = AudioMetadata.extract(track),
                audioFormat = audioFormat
            )
        }

    private fun Context.sendTrimmingStatusBroadcast(trimmingResult: Either<Throwable, MediaFile.AudioFile>) =
        sendBroadcastCompat(
            Intent(applicationContext, TrimmingStatusReceiver::class.java)
                .setAction(TrimmingStatusReceiver.Broadcast_TRIMMING_COMPLETED)
                .putExtra(
                    TrimmingStatusReceiver.TRIMMING_STATUS_ARG,
                    trimmingStatusMessage(trimmingResult)
                )
        )

    private fun Context.trimmingStatusMessage(trimmingResult: Either<Throwable, MediaFile.AudioFile>) =
        when (trimmingResult) {
            is Either.Right -> getString(R.string.file_trimmed)

            is Either.Left -> "${getString(R.string.error)}: " +
                    (trimmingResult.value.message
                        ?: getString(R.string.unknown_error))
        }
}