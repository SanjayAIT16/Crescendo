package com.paranid5.crescendo.presentation.playing

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import com.paranid5.crescendo.R
import com.paranid5.crescendo.data.utils.extensions.artistAlbum
import com.paranid5.crescendo.domain.StorageHandler
import com.paranid5.crescendo.presentation.ui.AudioStatus
import com.paranid5.crescendo.presentation.ui.extensions.increaseDarkness
import com.paranid5.crescendo.presentation.ui.getCurrentTrackCoverModel
import com.paranid5.crescendo.presentation.ui.getCurrentTrackCoverModelWithPalette
import com.paranid5.crescendo.presentation.ui.getVideoCoverModel
import com.paranid5.crescendo.presentation.ui.getVideoCoverModelWithPalette
import org.koin.compose.koinInject

private const val BROADCAST_LOCATION = "com.paranid5.mediastreamer.presentation.playing"
const val Broadcast_CUR_POSITION_CHANGED = "$BROADCAST_LOCATION.CUR_POSITION_CHANGED"
const val CUR_POSITION_ARG = "cur_position"

@Composable
fun PlayingScreen(
    coverAlpha: Float,
    viewModel: PlayingViewModel,
    modifier: Modifier = Modifier,
    storageHandler: StorageHandler = koinInject()
) {
    val audioStatus by storageHandler.audioStatusState.collectAsState()
    val currentMetadata by storageHandler.currentMetadataState.collectAsState()
    val currentTrack by storageHandler.currentTrackState.collectAsState()

    val isLiveStreaming by remember {
        derivedStateOf {
            audioStatus == AudioStatus.STREAMING && currentMetadata?.isLiveStream == true
        }
    }

    val title = when (audioStatus) {
        AudioStatus.STREAMING -> currentMetadata?.title ?: stringResource(R.string.stream_no_name)
        AudioStatus.PLAYING -> currentTrack?.title ?: stringResource(R.string.unknown_track)
        else -> stringResource(R.string.unknown_track)
    }

    val author = when (audioStatus) {
        AudioStatus.STREAMING ->
            currentMetadata?.author ?: stringResource(R.string.unknown_streamer)

        AudioStatus.PLAYING ->
            currentTrack?.artistAlbum ?: stringResource(R.string.unknown_artist)

        else -> stringResource(R.string.unknown_artist)
    }

    val length = when (audioStatus) {
        AudioStatus.STREAMING -> currentMetadata?.lenInMillis ?: 0
        AudioStatus.PLAYING -> currentTrack?.duration ?: 0
        else -> 0
    }

    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> PlayingScreenLandscape(
            title = title,
            author = author,
            sliderLength = length,
            coverAlpha = coverAlpha,
            audioStatus = audioStatus,
            isLiveStreaming = isLiveStreaming,
            viewModel = viewModel,
            modifier = modifier,
        )

        else -> PlayingScreenPortrait(
            title = title,
            author = author,
            sliderLength = length,
            coverAlpha = coverAlpha,
            audioStatus = audioStatus,
            isLiveStreaming = isLiveStreaming,
            viewModel = viewModel,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlayingScreenPortrait(
    title: String,
    author: String,
    sliderLength: Long,
    coverAlpha: Float,
    audioStatus: AudioStatus?,
    isLiveStreaming: Boolean,
    viewModel: PlayingViewModel,
    modifier: Modifier,
) {
    var coverSize by remember { mutableStateOf(1 to 1) }

    val (coverModel, palette) = when (audioStatus) {
        AudioStatus.STREAMING -> getVideoCoverModelWithPalette(
            isPlaceholderRequired = true,
            size = coverSize
        )

        else -> getCurrentTrackCoverModelWithPalette(
            isPlaceholderRequired = true,
            size = coverSize
        )
    }

    ConstraintLayout(modifier.fillMaxSize()) {
        val (
            cover,
            audioWave,
            slider,
            titleAndPropertiesButton,
            playbackButtons,
            utilsButtons
        ) = createRefs()

        BackgroundImage(
            Modifier
                .fillMaxSize()
                .alpha(coverAlpha)
        )

        Cover(
            coverModel = coverModel,
            palette = palette,
            modifier = Modifier
                .alpha(coverAlpha)
                .constrainAs(cover) {
                    top.linkTo(parent.top, margin = 30.dp)
                    bottom.linkTo(audioWave.top, margin = 10.dp)
                    start.linkTo(parent.start, margin = 15.dp)
                    end.linkTo(parent.end, margin = 15.dp)
                    height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints
                }
                .onGloballyPositioned { coordinates ->
                    coverSize = coordinates.size.width to coordinates.size.height
                },
        )

        AudioWaveform(
            palette = palette,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .height(40.dp)
                .fillMaxWidth()
                .constrainAs(audioWave) {
                    bottom.linkTo(slider.top, margin = 10.dp)
                    start.linkTo(parent.start, margin = 20.dp)
                    end.linkTo(parent.end, margin = 20.dp)
                }
        )

        PlaybackSlider(
            length = sliderLength,
            palette = palette,
            modifier = Modifier.constrainAs(slider) {
                bottom.linkTo(titleAndPropertiesButton.top, margin = 15.dp)
                start.linkTo(parent.start, margin = 20.dp)
                end.linkTo(parent.end, margin = 20.dp)
            }
        ) { curPosition, videoLength, color ->
            TimeContainer(isLiveStreaming, curPosition, videoLength, color)
        }

        TitleAndPropertiesButton(
            title = title,
            author = author,
            palette = palette,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .constrainAs(titleAndPropertiesButton) {
                    bottom.linkTo(playbackButtons.top, margin = 15.dp)
                    start.linkTo(parent.start, margin = 20.dp)
                    end.linkTo(parent.end, margin = 20.dp)
                }
        )

        PlaybackButtons(
            playingPresenter = viewModel.presenter,
            palette = palette,
            modifier = Modifier.constrainAs(playbackButtons) {
                bottom.linkTo(utilsButtons.top, margin = 5.dp)
                start.linkTo(parent.start, margin = 20.dp)
                end.linkTo(parent.end, margin = 20.dp)
            }
        )

        UtilsButtons(
            palette = palette,
            playingPresenter = viewModel.presenter,
            modifier = Modifier.constrainAs(utilsButtons) {
                bottom.linkTo(parent.bottom, margin = 20.dp)
                start.linkTo(parent.start, margin = 20.dp)
                end.linkTo(parent.end, margin = 20.dp)
            }
        )
    }
}

@Composable
private fun PlayingScreenLandscape(
    title: String,
    author: String,
    sliderLength: Long,
    coverAlpha: Float,
    audioStatus: AudioStatus?,
    isLiveStreaming: Boolean,
    viewModel: PlayingViewModel,
    modifier: Modifier,
) {
    var coverSize by remember { mutableStateOf(1 to 1) }

    val (coverModel, palette) = when (audioStatus) {
        AudioStatus.STREAMING -> getVideoCoverModelWithPalette(
            isPlaceholderRequired = true,
            size = coverSize
        )

        else -> getCurrentTrackCoverModelWithPalette(
            isPlaceholderRequired = true,
            size = coverSize
        )
    }

    ConstraintLayout(modifier.fillMaxSize()) {
        val (
            cover,
            audioWave,
            propertiesButton,
            slider,
            playbackButtons,
            utilsButtons
        ) = createRefs()

        BackgroundImage(
            Modifier
                .fillMaxSize()
                .alpha(coverAlpha)
        )

        AudioWaveform(
            palette = palette,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(audioWave) {
                    top.linkTo(parent.top, margin = 8.dp)
                    bottom.linkTo(slider.top, margin = 2.dp)
                    height = Dimension.fillToConstraints

                    start.linkTo(parent.start, margin = 20.dp)
                    end.linkTo(parent.end, margin = 20.dp)
                }
        )

        Cover(
            coverModel = coverModel,
            palette = palette,
            modifier = Modifier
                .alpha(coverAlpha)
                .aspectRatio(1F)
                .constrainAs(cover) {
                    centerHorizontallyTo(parent)
                    top.linkTo(parent.top, margin = 8.dp)
                    bottom.linkTo(slider.top, margin = 2.dp)
                    height = Dimension.fillToConstraints
                }
                .onGloballyPositioned { coordinates ->
                    coverSize = coordinates.size.width to coordinates.size.height
                },
        )

        PropertiesButton(
            palette = palette,
            modifier = Modifier.constrainAs(propertiesButton) {
                top.linkTo(parent.top, margin = 8.dp)
                end.linkTo(parent.end, margin = 5.dp)
            },
        )

        PlaybackSlider(
            length = sliderLength,
            palette = palette,
            modifier = Modifier.constrainAs(slider) {
                top.linkTo(parent.top, margin = 20.dp)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start, margin = 20.dp)
                end.linkTo(parent.end, margin = 20.dp)
            }
        ) { curPosition, videoLength, color ->
            if (isLiveStreaming) LiveText(color) else TimeText(curPosition, color)

            TitleAndAuthor(
                title = title,
                author = author,
                palette = palette,
                textAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .weight(1F, fill = true),
            )

            if (isLiveStreaming) Unit else TimeText(videoLength, color)
        }

        PlaybackButtons(
            playingPresenter = viewModel.presenter,
            palette = palette,
            modifier = Modifier.constrainAs(playbackButtons) {
                top.linkTo(slider.bottom, margin = 5.dp)
                start.linkTo(parent.start, margin = 20.dp)
                end.linkTo(parent.end, margin = 20.dp)
            }
        )

        UtilsButtons(
            palette = palette,
            playingPresenter = viewModel.presenter,
            modifier = Modifier.constrainAs(utilsButtons) {
                top.linkTo(playbackButtons.bottom, margin = 2.dp)
                start.linkTo(parent.start, margin = 20.dp)
                end.linkTo(parent.end, margin = 20.dp)
            }
        )
    }
}

@Composable
private fun BackgroundImage(
    modifier: Modifier = Modifier,
    storageHandler: StorageHandler = koinInject()
) {
    val config = LocalConfiguration.current
    val audioStatus by storageHandler.audioStatusState.collectAsState()

    val coverModel = when (audioStatus) {
        AudioStatus.STREAMING -> getVideoCoverModel(
            isPlaceholderRequired = true,
            size = config.screenWidthDp to config.screenHeightDp,
            isBlured = Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
            bitmapSettings = Bitmap::increaseDarkness,
        )

        else -> getCurrentTrackCoverModel(
            isPlaceholderRequired = true,
            size = config.screenWidthDp to config.screenHeightDp,
            isBlured = Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
            bitmapSettings = Bitmap::increaseDarkness,
        )
    }

    AsyncImage(
        model = coverModel,
        modifier = modifier.blur(radius = 15.dp),
        contentDescription = stringResource(R.string.video_cover),
        contentScale = ContentScale.FillBounds,
        alignment = Alignment.Center,
    )
}