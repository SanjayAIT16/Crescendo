package com.paranid5.crescendo.presentation.composition_locals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.compositionLocalOf

@OptIn(ExperimentalFoundationApi::class)
val LocalPlayingPagerState = compositionLocalOf<PagerState?> { null }