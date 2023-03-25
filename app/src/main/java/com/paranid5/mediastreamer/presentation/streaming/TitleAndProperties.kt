package com.paranid5.mediastreamer.presentation.streaming

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paranid5.mediastreamer.R
import com.paranid5.mediastreamer.data.VideoMetadata
import com.paranid5.mediastreamer.presentation.ui.extensions.primaryColorShadow
import com.paranid5.mediastreamer.presentation.ui.theme.LocalAppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleAndAuthor(metadata: VideoMetadata?, modifier: Modifier = Modifier) =
    Column(modifier) {
        Text(
            modifier = Modifier.basicMarquee().primaryColorShadow(),
            text = metadata?.title ?: stringResource(R.string.stream_no_name),
            fontSize = 18.sp,
            maxLines = 1,
        )

        Spacer(Modifier.height(5.dp))

        Text(
            modifier = Modifier.basicMarquee().primaryColorShadow(),
            text = metadata?.author ?: stringResource(R.string.unknown_streamer),
            fontSize = 16.sp,
            maxLines = 1,
        )
    }

@Composable
private fun PropertiesButton(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current.value

    IconButton(modifier = modifier, onClick = { /*TODO*/ }) {
        Icon(
            modifier = Modifier.height(50.dp).width(25.dp),
            painter = painterResource(R.drawable.three_dots),
            contentDescription = stringResource(R.string.settings),
            tint = colors.primary
        )
    }
}

@Composable
fun TitleAndPropertiesButton(metadata: VideoMetadata?, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        TitleAndAuthor(
            metadata = metadata,
            modifier = Modifier.weight(1F)
        )

        Spacer(Modifier.width(10.dp))
        PropertiesButton()
    }