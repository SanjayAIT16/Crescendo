package com.paranid5.crescendo.domain.media.files

import android.os.Environment
import android.util.Log
import arrow.core.Either
import arrow.core.flatMap
import com.arthenica.mobileffmpeg.FFmpeg
import com.paranid5.crescendo.domain.caching.Formats
import com.paranid5.crescendo.domain.caching.audioFileExt
import com.paranid5.crescendo.domain.trimming.FadeDurations
import com.paranid5.crescendo.domain.trimming.PitchAndSpeed
import com.paranid5.crescendo.domain.trimming.TrimRange
import com.paranid5.crescendo.domain.trimming.totalDurationSecs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private const val TAG = "AudioFiles"

suspend fun createAudioFileCatching(
    mediaDirectory: MediaDirectory,
    filename: String,
    ext: String
) = createFileCatching(mediaDirectory, filename, ext)
    .map { MediaFile.AudioFile(it) }

suspend fun MediaFile.AudioFile.trimmedCatching(
    outputFilename: String,
    audioFormat: Formats,
    trimRange: TrimRange,
    pitchAndSpeed: PitchAndSpeed,
    fadeDurations: FadeDurations,
) = createAudioFileCatching(
    mediaDirectory = MediaDirectory(Environment.DIRECTORY_MUSIC),
    filename = outputFilename,
    ext = audioFormat.audioFileExt
).flatMap { outputFile ->
    Either.catch {
        trim(
            inputPath = absolutePath,
            outputPath = outputFile.absolutePath,
            trimRange = trimRange,
            pitchAndSpeed = pitchAndSpeed,
            fadeDurations = fadeDurations
        )

        outputFile
    }
}

private suspend inline fun trim(
    inputPath: String,
    outputPath: String,
    trimRange: TrimRange,
    pitchAndSpeed: PitchAndSpeed,
    fadeDurations: FadeDurations
) = coroutineScope {
    withContext(Dispatchers.IO) {
        val command = ffmpegTrimCommand(
            inputPath = inputPath,
            outputPath = outputPath,
            trimRange = trimRange,
            pitchAndSpeed = pitchAndSpeed,
            fadeDurations = fadeDurations
        )

        Log.d(TAG, command)
        Log.d(TAG, "FFmpeg status: ${FFmpeg.execute(command)}")
    }
}

private fun ffmpegTrimCommand(
    inputPath: String,
    outputPath: String,
    trimRange: TrimRange,
    pitchAndSpeed: PitchAndSpeed,
    fadeDurations: FadeDurations
) = "-y -ss ${trimRange.startPointMillis}ms " +
        "-i \"$inputPath\" " +
        "-t ${trimRange.totalDurationMillis}ms " +
        "-af asetrate=44100*${pitchAndSpeed.pitch}," +
        "aresample=44100," +
        "atempo=${pitchAndSpeed.speed}/${pitchAndSpeed.pitch}," +
        "afade=in:0:d=${fadeDurations.fadeInSecs}," +
        "afade=out:st=${trimRange.totalDurationSecs - fadeDurations.fadeOutSecs}:d=${fadeDurations.fadeOutSecs} " +
        "\"$outputPath\""