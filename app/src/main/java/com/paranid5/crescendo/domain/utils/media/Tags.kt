package com.paranid5.crescendo.domain.utils.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.paranid5.crescendo.data.VideoMetadata
import com.paranid5.crescendo.domain.media_scanner.scanNextFile
import com.paranid5.crescendo.domain.services.video_cache_service.Formats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag

private fun setVideoTagsToFile(
    file: MediaFile.VideoFile,
    videoMetadata: VideoMetadata
) = AudioFileIO.read(file).run {
    tagOrCreateAndSetDefault.let { it as Mp4Tag }.run {
        setField(Mp4FieldKey.TITLE, videoMetadata.title)
        setField(Mp4FieldKey.ARTIST, videoMetadata.author)
        commit()
    }
}

private fun setVideoTagsToFileCatching(
    file: MediaFile.VideoFile,
    videoMetadata: VideoMetadata
) = runCatching {
    setVideoTagsToFile(file, videoMetadata)
}

internal suspend inline fun setVideoTagsAsync(
    context: Context,
    videoFile: MediaFile.VideoFile,
    videoMetadata: VideoMetadata
) = coroutineScope {
    val externalContentUri = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val mediaDirectory = Environment.DIRECTORY_MOVIES
    val mimeType = "video/${videoFile.extension}"
    val absoluteFilePath = videoFile.absolutePath

    launch(Dispatchers.IO) {
        context.insertMediaFileToMediaStore(
            externalContentUri,
            absoluteFilePath,
            mediaDirectory,
            videoMetadata,
            mimeType
        )

        setVideoTagsToFileCatching(videoFile, videoMetadata)
        context.scanNextFile(absoluteFilePath)
    }
}

private fun setAudioTagsToFile(
    context: Context,
    file: MediaFile.AudioFile,
    videoMetadata: VideoMetadata
) = AudioFileIO.read(file).run {
    tagOrCreateAndSetDefault.run {
        setField(FieldKey.TITLE, videoMetadata.title)
        setField(FieldKey.ARTIST, videoMetadata.author)

        videoMetadata
            .covers
            .asSequence()
            .map { getImageBinaryDataCatching(context, it) }
            .firstOrNull { it.isSuccess }
            ?.getOrNull()
            ?.let { ArtworkFactory.getNew().apply { binaryData = it } }
            ?.let(this::setField)

        commit()
    }
}

private fun setAudioTagsToFileCatching(
    context: Context,
    file: MediaFile.AudioFile,
    videoMetadata: VideoMetadata
) = runCatching {
    setAudioTagsToFile(context, file, videoMetadata)
}

internal suspend inline fun setAudioTagsAsync(
    context: Context,
    audioFile: MediaFile.AudioFile,
    videoMetadata: VideoMetadata,
    audioFormat: Formats
) = coroutineScope {
    val externalContentUri = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val mediaDirectory = Environment.DIRECTORY_MUSIC

    val mimeType = when (audioFormat) {
        Formats.MP3 -> "audio/mpeg"
        Formats.AAC -> "audio/aac"
        else -> "audio/x-wav"
    }

    val absoluteFilePath = audioFile.absolutePath

    launch(Dispatchers.IO) {
        context.insertMediaFileToMediaStore(
            externalContentUri,
            absoluteFilePath,
            mediaDirectory,
            videoMetadata,
            mimeType
        )

        if (audioFormat == Formats.MP3)
            setAudioTagsToFileCatching(context, audioFile, videoMetadata)

        context.scanNextFile(absoluteFilePath)
    }
}

fun Context.insertMediaFileToMediaStore(
    externalContentUri: Uri,
    absoluteFilePath: String,
    relativeFilePath: String,
    videoMetadata: VideoMetadata,
    mimeType: String,
): Uri {
    val uri = applicationContext.contentResolver.insert(
        externalContentUri,
        ContentValues().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(MediaStore.MediaColumns.IS_PENDING, 1)

            put(MediaStore.MediaColumns.TITLE, videoMetadata.title)
            put(MediaStore.MediaColumns.ARTIST, videoMetadata.author)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                put(MediaStore.MediaColumns.AUTHOR, videoMetadata.author)

            put(MediaStore.MediaColumns.DURATION, videoMetadata.lenInMillis)
            put(MediaStore.MediaColumns.DATA, absoluteFilePath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, videoMetadata.title)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFilePath)
        }
    )!!

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        applicationContext.contentResolver.update(
            uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
            null, null
        )

    return uri
}