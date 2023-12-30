package com.paranid5.crescendo.presentation.main.fetch_stream

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.paranid5.crescendo.data.StorageHandler
import com.paranid5.crescendo.data.states.playback.AudioStatusStatePublisher
import com.paranid5.crescendo.data.states.playback.AudioStatusStatePublisherImpl
import com.paranid5.crescendo.data.states.stream.CurrentUrlStateSubscriber
import com.paranid5.crescendo.data.states.stream.CurrentUrlStateSubscriberImpl
import com.paranid5.crescendo.presentation.main.fetch_stream.states.UrlStateHolder
import com.paranid5.crescendo.presentation.main.fetch_stream.states.UrlStateHolderImpl

class FetchStreamViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val storageHandler: StorageHandler,
) : ViewModel(),
    CurrentUrlStateSubscriber by CurrentUrlStateSubscriberImpl(storageHandler),
    UrlStateHolder by UrlStateHolderImpl(savedStateHandle),
    AudioStatusStatePublisher by AudioStatusStatePublisherImpl(storageHandler)