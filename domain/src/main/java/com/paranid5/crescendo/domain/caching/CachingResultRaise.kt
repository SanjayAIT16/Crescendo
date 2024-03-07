package com.paranid5.crescendo.domain.caching

import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.paranid5.crescendo.domain.media.files.MediaFile
import kotlin.experimental.ExperimentalTypeInference

@JvmInline
value class CachingResultRaise(private val raise: Raise<CachingResult>) :
    Raise<CachingResult> by raise {
    fun CachingResult.bind() =
        when (this) {
            is CachingResult.Success -> listOf(file)
            is CachingResult.DownloadResult.Success -> files
            else -> raise.raise(this)
        }
}

@OptIn(ExperimentalTypeInference::class)
inline fun cachingResult(
    @BuilderInference block: CachingResultRaise.() -> MediaFile
): CachingResult = recover(
    block = { CachingResult.Success(block(CachingResultRaise(this))) },
    recover = { it }
)