package com.paranid5.crescendo.presentation.composition_locals

import androidx.compose.runtime.staticCompositionLocalOf
import com.paranid5.crescendo.presentation.main_activity.MainActivity

val LocalActivity = staticCompositionLocalOf<MainActivity?> { null }