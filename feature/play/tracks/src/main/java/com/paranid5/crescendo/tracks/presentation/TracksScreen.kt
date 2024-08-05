package com.paranid5.crescendo.tracks.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.paranid5.crescendo.core.resources.ui.theme.AppTheme.dimensions
import com.paranid5.crescendo.tracks.presentation.effect.SubscribeOnBackEventsEffect
import com.paranid5.crescendo.tracks.presentation.effect.SubscribeOnLifecycleEventsEffect
import com.paranid5.crescendo.tracks.presentation.ui.DefaultTrackList
import com.paranid5.crescendo.tracks.presentation.ui.TracksBar
import com.paranid5.crescendo.tracks.view_model.TracksBackResult
import com.paranid5.crescendo.tracks.view_model.TracksUiIntent
import com.paranid5.crescendo.tracks.view_model.TracksViewModel
import com.paranid5.crescendo.tracks.view_model.TracksViewModelImpl
import com.paranid5.crescendo.ui.foundation.AppRefreshIndicator
import com.paranid5.crescendo.ui.foundation.UiState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TracksScreen(
    modifier: Modifier = Modifier,
    viewModel: TracksViewModel = koinViewModel<TracksViewModelImpl>(),
    onBack: (TracksBackResult) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsState()
    val onUiIntent = viewModel::onUiIntent

    val refreshState = rememberPullRefreshState(
        refreshing = state.shownTracksState is UiState.Refreshing,
        onRefresh = { onUiIntent(TracksUiIntent.OnRefresh) },
    )

    SubscribeOnLifecycleEventsEffect(onUiIntent = onUiIntent)
    SubscribeOnBackEventsEffect(state = state, onBack = onBack)

    Box(modifier.pullRefresh(refreshState)) {
        Column(Modifier.fillMaxSize()) {
            TracksBar(
                state = state,
                onUiIntent = onUiIntent,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensions.padding.extraMedium))

            DefaultTrackList(
                state = state,
                onUiIntent = onUiIntent,
                bottomPadding = dimensions.padding.small,
                modifier = Modifier.fillMaxSize(1F),
            )
        }

        AppRefreshIndicator(
            refreshing = state.shownTracksState is UiState.Refreshing,
            refreshState = refreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
