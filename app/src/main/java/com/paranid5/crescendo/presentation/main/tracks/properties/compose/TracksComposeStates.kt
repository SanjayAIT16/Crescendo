package com.paranid5.crescendo.presentation.main.tracks.properties.compose

import androidx.compose.runtime.Composable
import com.paranid5.crescendo.domain.tracks.TrackOrder
import com.paranid5.crescendo.presentation.main.tracks.TracksViewModel
import com.paranid5.crescendo.presentation.main.tracks.properties.currentTrackFlow
import com.paranid5.crescendo.presentation.main.tracks.properties.filteredTracksState
import com.paranid5.crescendo.presentation.main.tracks.properties.shownTracksFlow
import com.paranid5.crescendo.presentation.main.tracks.properties.shownTracksNumberFlow
import com.paranid5.crescendo.presentation.main.tracks.properties.trackOrderFlow
import com.paranid5.crescendo.presentation.main.tracks.properties.tracksState
import com.paranid5.crescendo.presentation.ui.extensions.collectLatestAsState
import kotlinx.collections.immutable.persistentListOf

@Composable
fun TracksViewModel.collectTracksAsState() =
    tracksState.collectLatestAsState()

@Composable
fun TracksViewModel.collectFilteredTracksAsState() =
    filteredTracksState.collectLatestAsState()

@Composable
fun TracksViewModel.collectTrackOrderAsState() =
    trackOrderFlow.collectLatestAsState(initial = TrackOrder.default)

@Composable
fun TracksViewModel.collectShownTracksAsState() =
    shownTracksFlow.collectLatestAsState(initial = persistentListOf())

@Composable
fun TracksViewModel.collectShownTracksNumberAsState() =
    shownTracksNumberFlow.collectLatestAsState(initial = 0)

@Composable
fun TracksViewModel.collectCurrentTrackAsState() =
    currentTrackFlow.collectLatestAsState(initial = null)