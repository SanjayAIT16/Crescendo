package com.paranid5.crescendo.current_playlist.presentation.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paranid5.crescendo.core.common.tracks.Track
import com.paranid5.crescendo.ui.drag.DraggableList
import com.paranid5.crescendo.ui.drag.DraggableListItemView
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun <T : Track> DraggableTrackList(
    tracks: ImmutableList<T>,
    currentTrackIndex: Int,
    onTrackDismissed: (index: Int, item: T) -> Boolean,
    onTrackDragged: suspend (draggedItems: ImmutableList<T>, dragIndex: Int) -> Unit,
    trackItemView: DraggableListItemView<T>,
    modifier: Modifier = Modifier,
    trackItemModifier: Modifier = Modifier,
) = DraggableList(
    items = tracks,
    currentItemIndex = currentTrackIndex,
    onDismissed = onTrackDismissed,
    onDragged = onTrackDragged,
    itemView = trackItemView,
    modifier = modifier,
    itemModifier = trackItemModifier,
    key = { index, track -> "${track.hashCode()}$index" }
)

@Composable
internal fun <T : Track> DraggableTrackList(
    tracks: ImmutableList<T>,
    currentTrackIndex: Int,
    onTrackDismissed: (index: Int, item: T) -> Boolean,
    onTrackDragged: suspend (draggedItems: ImmutableList<T>, dragIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    trackItemModifier: Modifier = Modifier,
) = DraggableTrackList(
    tracks = tracks,
    currentTrackIndex = currentTrackIndex,
    onTrackDismissed = onTrackDismissed,
    onTrackDragged = onTrackDragged,
    modifier = modifier,
    trackItemModifier = trackItemModifier,
    trackItemView = { trackList, trackInd, currentTrackDragIndex, trackModifier ->
        DraggableTrackItem(
            tracks = trackList,
            trackIndex = trackInd,
            currentTrackDragIndex = currentTrackDragIndex,
            modifier = trackModifier
        )
    }
)