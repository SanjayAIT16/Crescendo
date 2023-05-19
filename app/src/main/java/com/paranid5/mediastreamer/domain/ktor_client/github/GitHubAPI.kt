package com.paranid5.mediastreamer.domain.ktor_client.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val CUR_VERSION = "V0.0.0.3"

private suspend inline fun HttpClient.getLatestRelease() =
    get("https://api.github.com/repos/dinaraparanid/MediaStreamer/releases")
        .body<List<Release>>()
        .first()

private suspend inline fun HttpClient.getLatestReleaseAsync() = coroutineScope {
    async(Dispatchers.IO) { getLatestRelease() }
}

internal suspend inline fun HttpClient.checkForUpdates() =
    getLatestReleaseAsync().await().takeIf { it.tagName > CUR_VERSION }