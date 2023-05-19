package com.paranid5.mediastreamer.presentation.ui.extensions

import android.graphics.Color

fun Int.increaseBrightness(increase: Float = 0.25F): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    hsv[2] = maxOf(hsv[2] + increase, 1F)
    return Color.HSVToColor(hsv)
}