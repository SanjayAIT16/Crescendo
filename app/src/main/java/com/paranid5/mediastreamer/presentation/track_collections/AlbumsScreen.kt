package com.paranid5.mediastreamer.presentation.track_collections

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paranid5.mediastreamer.R
import com.paranid5.mediastreamer.presentation.Screens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun AlbumsScreen(curScreenState: MutableStateFlow<Screens>) {
    curScreenState.update { Screens.TrackCollections.Albums }
    Text(stringResource(R.string.albums))
}