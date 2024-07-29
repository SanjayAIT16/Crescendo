package com.paranid5.crescendo.data.stream

import com.paranid5.crescendo.data.datastore.StreamDataStore
import com.paranid5.crescendo.domain.stream.DownloadingUrlSubscriber

internal class DownloadingUrlSubscriberImpl(
    streamDataStore: StreamDataStore,
) : DownloadingUrlSubscriber {
    override val downloadingUrlFlow by lazy {
        streamDataStore.downloadingUrlFlow
    }
}
