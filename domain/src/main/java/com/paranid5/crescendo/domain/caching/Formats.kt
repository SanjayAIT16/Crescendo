package com.paranid5.crescendo.domain.caching

enum class Formats { MP3, WAV, AAC, MP4 }

inline val Formats.audioFileExt
    get() = when (this) {
        Formats.MP3 -> "mp3"
        Formats.AAC -> "aac"
        Formats.WAV -> "wav"
        Formats.MP4 -> throw IllegalArgumentException("MP4 passed as an audio format")
    }