package com.paranid5.crescendo.feature.stream.domain

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import com.paranid5.crescendo.core.common.PlaybackStatus
import com.paranid5.crescendo.domain.playback.AudioStatusPublisher
import com.paranid5.crescendo.domain.stream.PlayingStreamUrlPublisher
import com.paranid5.crescendo.system.services.stream.StreamServiceAccessor

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
internal suspend fun <P> startStreaming(
    publisher: P,
    streamServiceAccessor: StreamServiceAccessor,
    currentText: String,
    playingPagerState: PagerState?,
    playingSheetState: BottomSheetScaffoldState?,
) where P : AudioStatusPublisher, P : PlayingStreamUrlPublisher {
    val url = currentText.trim()
    publisher.updateAudioStatus(PlaybackStatus.STREAMING)
    publisher.updatePlayingUrl(url)
    startStreamingImpl(url, streamServiceAccessor)
    playingPagerState?.animateScrollToPage(1)
    playingSheetState?.bottomSheetState?.expand()
}

private fun startStreamingImpl(url: String, serviceAccessor: StreamServiceAccessor) =
    serviceAccessor.startStreaming(url)