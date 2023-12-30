package com.paranid5.crescendo.presentation.main.tracks.views.item

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.paranid5.crescendo.domain.tracks.Track
import com.paranid5.crescendo.domain.utils.extensions.artistAlbum

@Composable
fun TrackInfo(
    track: Track,
    textColor: Color,
    modifier: Modifier = Modifier
) = Column(modifier) {
    TrackTitle(
        modifier = Modifier.align(Alignment.Start),
        trackTitle = track.title,
        textColor = textColor,
    )

    TrackArtistAlbum(
        modifier = Modifier.align(Alignment.Start),
        trackArtistAlbum = track.artistAlbum,
        textColor = textColor,
    )
}