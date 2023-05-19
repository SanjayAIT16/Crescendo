package com.paranid5.mediastreamer.presentation

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paranid5.mediastreamer.R
import com.paranid5.mediastreamer.domain.ktor_client.github.Release
import com.paranid5.mediastreamer.domain.ktor_client.github.checkForUpdates
import com.paranid5.mediastreamer.presentation.composition_locals.LocalActivity
import com.paranid5.mediastreamer.presentation.ui.theme.LocalAppColors
import dev.jeziellago.compose.markdowntext.MarkdownText
import io.ktor.client.HttpClient
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCheckerDialog(modifier: Modifier = Modifier, ktorClient: HttpClient = koinInject()) {
    val activity = LocalActivity.current
    val colors = LocalAppColors.current.value

    var newVersion by remember { mutableStateOf<Release?>(null) }
    val isUpdateAvailable by remember { derivedStateOf { newVersion != null } }
    var isDialogShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        newVersion = ktorClient.checkForUpdates()

        if (isUpdateAvailable) {
            Log.d("UpdateCheckerDialog", "New version is available: $newVersion")
            isDialogShown = true
        }
    }

    if (isDialogShown)
        AlertDialog(
            onDismissRequest = { isDialogShown = false },
            modifier = modifier
                .background(colors.background)
                .padding(10.dp)
                .wrapContentSize()
        ) {
            Column {
                MarkdownText(
                    markdown = "# ${stringResource(R.string.update_available)}: ${newVersion!!.name}",
                    color = colors.primary,
                    fontSize = 12.sp,
                    maxLines = 1
                )

                Spacer(Modifier.height(20.dp))

                MarkdownText(markdown = newVersion!!.body, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))

                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    onClick = {
                        activity?.startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse(newVersion!!.htmlUrl))
                        )
                    }
                ) {
                    Text(stringResource(R.string.download))
                }
            }
        }
}