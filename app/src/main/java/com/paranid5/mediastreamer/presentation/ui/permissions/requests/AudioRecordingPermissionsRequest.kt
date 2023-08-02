package com.paranid5.mediastreamer.presentation.ui.permissions.requests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.paranid5.mediastreamer.AUDIO_RECORDING_PERMISSION_QUEUE
import com.paranid5.mediastreamer.presentation.ui.permissions.description_providers.AudioRecordingDescriptionProvider
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import java.util.Queue

@Composable
fun audioRecordingPermissionsRequestLauncher(
    isAudioRecordingPermissionDialogShownState: MutableState<Boolean>,
    modifier: Modifier = Modifier
): Pair<Boolean, () -> Unit> {
    val audioRecordingPermissionQueue = koinInject<Queue<String>>(
        named(AUDIO_RECORDING_PERMISSION_QUEUE)
    )

    val audioRecordingDescriptionProvider = koinInject<AudioRecordingDescriptionProvider>()

    return permissionsRequestLauncher(
        modifier = modifier,
        permissionQueue = audioRecordingPermissionQueue,
        descriptionProvider = audioRecordingDescriptionProvider,
        isPermissionDialogShownState = isAudioRecordingPermissionDialogShownState,
    )
}