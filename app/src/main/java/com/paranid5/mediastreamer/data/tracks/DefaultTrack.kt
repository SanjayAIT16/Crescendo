package com.paranid5.mediastreamer.data.tracks

import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Immutable
data class DefaultTrack(
    override val androidId: Long,
    override val title: String,
    override val artist: String,
    override val album: String,
    override val path: String,
    override val duration: Long,
    override val displayName: String,
    override val dateAdded: Long,
    override val numberInAlbum: Int
) : Track {
    constructor(track: Track) : this(
        androidId = track.androidId,
        title = track.title,
        artist = track.artist,
        album = track.album,
        path = track.path,
        duration = track.duration,
        displayName = track.displayName,
        dateAdded = track.dateAdded,
        numberInAlbum = track.numberInAlbum
    )
}
