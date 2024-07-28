package com.paranid5.crescendo.data.di

import com.paranid5.crescendo.data.audio_effects.di.audioEffectsModule
import com.paranid5.crescendo.data.current_playlist.di.currentPlaylistModule
import com.paranid5.crescendo.data.datastore.di.dataStoreModule
import com.paranid5.crescendo.data.ktor_client.KtorClient
import com.paranid5.crescendo.data.playback.di.playbackModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    includes(
        audioEffectsModule,
        currentPlaylistModule,
        dataStoreModule,
        playbackModule,
    )

    singleOf(::KtorClient)
}