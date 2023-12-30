package com.paranid5.crescendo.presentation.main.trimmer.views

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paranid5.crescendo.R
import com.paranid5.crescendo.koinActivityViewModel
import com.paranid5.crescendo.presentation.main.trimmer.TrimmerViewModel
import com.paranid5.crescendo.presentation.main.trimmer.properties.compose.collectTrackDurationInMillisAsState
import com.paranid5.crescendo.presentation.main.trimmer.properties.compose.collectTrimRangeAsState
import com.paranid5.crescendo.presentation.ui.theme.LocalAppColors

@Composable
fun FileSaveButton(
    isFileSaveDialogShownState: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    viewModel: TrimmerViewModel = koinActivityViewModel(),
) {
    val colors = LocalAppColors.current

    val trimRange by viewModel.collectTrimRangeAsState()
    val trackDurationMillis by viewModel.collectTrackDurationInMillisAsState()

    var isFileSaveDialogShown by isFileSaveDialogShownState

    val isClickable by remember(trimRange, trackDurationMillis) {
        derivedStateOf { trimRange.totalDurationMillis in 1..trackDurationMillis }
    }

    Button(
        modifier = modifier,
        enabled = isClickable,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.backgroundAlternative
        ),
        onClick = { isFileSaveDialogShown = true },
        content = { FileSaveButtonLabel(textModifier) }
    )
}

@Composable
private fun FileSaveButtonLabel(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current

    Text(
        text = stringResource(R.string.save),
        color = colors.fontColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}