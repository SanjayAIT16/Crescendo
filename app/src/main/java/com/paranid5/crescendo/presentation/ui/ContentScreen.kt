package com.paranid5.crescendo.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.paranid5.crescendo.R
import com.paranid5.crescendo.presentation.Screens
import com.paranid5.crescendo.presentation.about_app.AboutApp
import com.paranid5.crescendo.presentation.audio_effects.AudioEffectsScreen
import com.paranid5.crescendo.presentation.audio_effects.AudioEffectsViewModel
import com.paranid5.crescendo.presentation.composition_locals.LocalActivity
import com.paranid5.crescendo.presentation.composition_locals.LocalCurrentPlaylistSheetState
import com.paranid5.crescendo.presentation.composition_locals.LocalNavController
import com.paranid5.crescendo.presentation.composition_locals.LocalPlayingSheetState
import com.paranid5.crescendo.presentation.favourites.FavouritesScreen
import com.paranid5.crescendo.presentation.fetch_stream.FetchStreamViewModel
import com.paranid5.crescendo.presentation.fetch_stream.SearchStreamScreen
import com.paranid5.crescendo.presentation.main_activity.MainActivityViewModel
import com.paranid5.crescendo.presentation.settings.SettingsScreen
import com.paranid5.crescendo.presentation.track_collections.AlbumsScreen
import com.paranid5.crescendo.presentation.tracks.TracksScreen
import com.paranid5.crescendo.presentation.tracks.TracksViewModel
import com.paranid5.crescendo.presentation.trimmer.TrimmerScreen
import com.paranid5.crescendo.presentation.trimmer.TrimmerViewModel
import com.paranid5.crescendo.presentation.ui.utils.OnBackPressedHandler
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ContentScreen(
    padding: PaddingValues,
    viewModel: MainActivityViewModel,
    fetchStreamViewModel: FetchStreamViewModel = koinViewModel(),
    audioEffectsViewModel: AudioEffectsViewModel = koinViewModel(),
    trimmerViewModel: TrimmerViewModel = koinViewModel(),
    tracksViewModel: TracksViewModel = koinViewModel()
) {
    val backPressedCounter = AtomicInteger()
    val activity = LocalActivity.current
    val playingSheetState = LocalPlayingSheetState.current
    val currentPlaylistSheetState = LocalCurrentPlaylistSheetState.current

    OnBackPressedHandler { isStackEmpty ->
        if (currentPlaylistSheetState?.isVisible == true) {
            currentPlaylistSheetState.hide()
            return@OnBackPressedHandler
        }

        if (playingSheetState?.bottomSheetState?.isExpanded == true) {
            playingSheetState.bottomSheetState.collapse()
            return@OnBackPressedHandler
        }

        if (isStackEmpty) {
            when (backPressedCounter.incrementAndGet()) {
                2 -> activity?.finish()

                else -> {
                    Toast.makeText(
                        activity?.applicationContext,
                        R.string.press_twice_to_exit,
                        Toast.LENGTH_SHORT
                    ).show()

                    delay(500L)
                    backPressedCounter.set(0)
                }
            }
        }
    }

    NavHost(
        navController = LocalNavController.current.value!!,
        startDestination = Screens.Tracks.title,
        modifier = Modifier.padding(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding()
        )
    ) {
        composable(route = Screens.Tracks.title) {
            viewModel.setCurScreen(Screens.Tracks)

            TracksScreen(
                tracksViewModel = tracksViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            )
        }

        composable(route = Screens.TrackCollections.Albums.title) {
            viewModel.setCurScreen(Screens.TrackCollections.Albums)
            AlbumsScreen()
        }

        composable(route = Screens.StreamFetching.title) {
            viewModel.setCurScreen(Screens.StreamFetching)
            SearchStreamScreen(fetchStreamViewModel)
        }

        composable(route = Screens.Audio.AudioEffects.title) {
            viewModel.setCurScreen(Screens.Audio.AudioEffects)
            AudioEffectsScreen(audioEffectsViewModel)
        }

        composable(route = Screens.Audio.Trimmer.title) {
            viewModel.setCurScreen(Screens.Audio.Trimmer)

            TrimmerScreen(
                trimmerViewModel = trimmerViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp)
            )
        }

        composable(route = Screens.AboutApp.title) {
            viewModel.setCurScreen(Screens.AboutApp)
            AboutApp()
        }

        composable(route = Screens.Favourites.title) {
            viewModel.setCurScreen(Screens.Favourites)
            FavouritesScreen()
        }

        composable(route = Screens.Settings.title) {
            viewModel.setCurScreen(Screens.Settings)
            SettingsScreen()
        }
    }
}