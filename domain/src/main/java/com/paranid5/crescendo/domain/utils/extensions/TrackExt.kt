package com.paranid5.crescendo.domain.utils.extensions

import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import androidx.media3.common.MediaItem
import com.paranid5.crescendo.domain.tracks.DefaultTrack
import com.paranid5.crescendo.domain.tracks.Track

inline val Track.artistAlbum
    get() = "$artist / $album"

fun Track.toAndroidMetadata(cover: Bitmap? = null): MediaMetadataCompat =
    MediaMetadataCompat.Builder()
        .putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMillis)
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, cover)
        .build()

fun Track.toMediaItem() = MediaItem.fromUri(path)

fun Iterable<Track>.toDefaultTrackList() = map(::DefaultTrack)

fun Iterable<Track>.toMediaItemList() = map(Track::toMediaItem)

inline val Iterable<Track>.totalDuration
    get() = fold(0L) { acc, track -> acc + track.durationMillis }