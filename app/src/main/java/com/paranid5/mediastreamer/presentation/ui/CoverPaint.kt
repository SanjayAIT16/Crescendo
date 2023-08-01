package com.paranid5.mediastreamer.presentation.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.paranid5.mediastreamer.R
import com.paranid5.mediastreamer.domain.StorageHandler
import com.paranid5.mediastreamer.presentation.ui.utils.BlurTransformation
import com.paranid5.mediastreamer.presentation.ui.utils.GlideUtils
import org.koin.compose.koinInject

@Composable
internal inline fun rememberVideoCoverPainter(
    isPlaceholderRequired: Boolean,
    size: Pair<Int, Int>? = null,
    isBlured: Boolean = false,
    usePrevCoverAsPlaceholder: Boolean = true,
    animationMillis: Int = 400,
    storageHandler: StorageHandler = koinInject(),
    crossinline bitmapSettings: (Bitmap) -> Unit = {}
): AsyncImagePainter {
    val metadata by storageHandler.currentMetadataState.collectAsState()
    val context = LocalContext.current
    val glideUtils = GlideUtils(context)

    var coverModel by remember { mutableStateOf<BitmapDrawable?>(null) }
    var prevCoverModel by remember { mutableStateOf<BitmapDrawable?>(null) }

    LaunchedEffect(metadata) {
        val newModel = metadata?.let {
            glideUtils
                .getVideoCoverAsync(it, size, bitmapSettings)
                .await()
        }

        prevCoverModel = coverModel ?: newModel
        coverModel = newModel
    }

    return rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(coverModel)
            .prevCoverErrorOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)
            .prevCoverFallbackOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)
            .apply {
                if (isPlaceholderRequired)
                    prevCoverPlaceholderOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)

                if (isBlured)
                    transformations(BlurTransformation(context))

                size?.run { size(first, second) }
            }
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .crossfade(animationMillis)
            .build()
    )
}

@Composable
internal inline fun rememberVideoCoverPainterWithPalette(
    isPlaceholderRequired: Boolean,
    size: Pair<Int, Int>? = null,
    isBlured: Boolean = false,
    usePrevCoverAsPlaceholder: Boolean = true,
    animationMillis: Int = 400,
    storageHandler: StorageHandler = koinInject(),
    crossinline bitmapSettings: (Bitmap) -> Unit = {}
): Pair<AsyncImagePainter, Palette?> {
    val metadata by storageHandler.currentMetadataState.collectAsState()
    val context = LocalContext.current
    val glideUtils = GlideUtils(context)

    var coverModel by remember { mutableStateOf<BitmapDrawable?>(null) }
    var prevCoverModel by remember { mutableStateOf<BitmapDrawable?>(null) }
    var palette by remember { mutableStateOf<Palette?>(null) }

    LaunchedEffect(metadata) {
        val newPaletteAndModel = metadata?.let {
            glideUtils.getVideoCoverWithPaletteAsync(it, size, bitmapSettings).await()
        }

        prevCoverModel = coverModel ?: newPaletteAndModel?.second
        coverModel = newPaletteAndModel?.second
        palette = newPaletteAndModel?.first
    }

    return rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(coverModel)
            .prevCoverErrorOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)
            .prevCoverFallbackOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)
            .apply {
                if (isPlaceholderRequired)
                    prevCoverPlaceholderOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)

                if (isBlured)
                    transformations(BlurTransformation(context))

                size?.run { size(first, second) }
            }
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .crossfade(animationMillis)
            .build()
    ) to palette
}

@Composable
internal inline fun rememberTrackCoverModel(
    path: String?,
    isPlaceholderRequired: Boolean,
    size: Pair<Int, Int>? = null,
    isBlured: Boolean = false,
    usePrevCoverAsPlaceholder: Boolean = false,
    animationMillis: Int = 400,
    crossinline bitmapSettings: (Bitmap) -> Unit = {}
): ImageRequest {
    val context = LocalContext.current
    val glideUtils = GlideUtils(context)

    var coverModel by remember { mutableStateOf<BitmapDrawable?>(null) }
    var prevCoverModel by remember { mutableStateOf<BitmapDrawable?>(null) }

    LaunchedEffect(key1 = path, key2 = size) {
        prevCoverModel = coverModel

        coverModel = glideUtils
            .getTrackCoverAsync(path, size, bitmapSettings)
            .await()
    }

    return ImageRequest.Builder(context)
        .data(coverModel)
        .prevCoverErrorOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)
        .prevCoverFallbackOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)
        .apply {
            if (isPlaceholderRequired)
                prevCoverPlaceholderOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)

            if (isBlured)
                transformations(BlurTransformation(context))

            size?.run { size(first, second) }
        }
        .precision(Precision.EXACT)
        .scale(Scale.FILL)
        .crossfade(animationMillis)
        .build()
}

@Composable
internal inline fun rememberTrackCoverPainter(
    path: String?,
    isPlaceholderRequired: Boolean,
    size: Pair<Int, Int>? = null,
    isBlured: Boolean = false,
    usePrevCoverAsPlaceholder: Boolean = false,
    animationMillis: Int = 400,
    crossinline bitmapSettings: (Bitmap) -> Unit = {}
): AsyncImagePainter {
    val trackCoverPainter = rememberTrackCoverModel(
        path = path,
        isPlaceholderRequired = isPlaceholderRequired,
        size = size,
        isBlured = isBlured,
        usePrevCoverAsPlaceholder = usePrevCoverAsPlaceholder,
        animationMillis = animationMillis,
        bitmapSettings = bitmapSettings
    )

    return rememberAsyncImagePainter(model = trackCoverPainter)
}

@Composable
internal inline fun rememberTrackCoverPainterWithPalette(
    path: String?,
    isPlaceholderRequired: Boolean,
    size: Pair<Int, Int>? = null,
    isBlured: Boolean = false,
    usePrevCoverAsPlaceholder: Boolean = true,
    animationMillis: Int = 400,
    crossinline bitmapSettings: (Bitmap) -> Unit = {}
): Pair<AsyncImagePainter, Palette?> {
    val context = LocalContext.current
    val glideUtils = GlideUtils(context)

    var coverModel by remember { mutableStateOf<BitmapDrawable?>(null) }
    var prevCoverModel by remember { mutableStateOf<BitmapDrawable?>(null) }
    var palette by remember { mutableStateOf<Palette?>(null) }

    LaunchedEffect(key1 = path, key2 = size) {
        val (plt, cover) = glideUtils
            .getTrackCoverWithPaletteAsync(path, size, bitmapSettings)
            .await()

        prevCoverModel = coverModel
        coverModel = cover
        palette = plt
    }

    return rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(coverModel)
            .prevCoverErrorOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)
            .prevCoverFallbackOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)
            .apply {
                if (isPlaceholderRequired)
                    prevCoverPlaceholderOrDefault(usePrevCoverAsPlaceholder, prevCoverModel)

                if (isBlured)
                    transformations(BlurTransformation(context))

                size?.run { size(first, second) }
            }
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .crossfade(animationMillis)
            .build()
    ) to palette
}

@Composable
internal inline fun rememberCurrentTrackCoverPainter(
    isPlaceholderRequired: Boolean,
    size: Pair<Int, Int>? = null,
    isBlured: Boolean = false,
    usePrevCoverAsPlaceholder: Boolean = true,
    animationMillis: Int = 400,
    storageHandler: StorageHandler = koinInject(),
    crossinline bitmapSettings: (Bitmap) -> Unit = {}
): AsyncImagePainter {
    val curTrack by storageHandler.currentTrackState.collectAsState()
    val path by remember { derivedStateOf { curTrack?.path } }

    return rememberTrackCoverPainter(
        path = path,
        isPlaceholderRequired = isPlaceholderRequired,
        size = size,
        isBlured = isBlured,
        usePrevCoverAsPlaceholder = usePrevCoverAsPlaceholder,
        animationMillis = animationMillis,
        bitmapSettings = bitmapSettings
    )
}

@Composable
internal inline fun rememberCurrentTrackCoverPainterWithPalette(
    isPlaceholderRequired: Boolean,
    size: Pair<Int, Int>? = null,
    isBlured: Boolean = false,
    usePrevCoverAsPlaceholder: Boolean = true,
    animationMillis: Int = 400,
    storageHandler: StorageHandler = koinInject(),
    crossinline bitmapSettings: (Bitmap) -> Unit = {}
): Pair<AsyncImagePainter, Palette?> {
    val curTrack by storageHandler.currentTrackState.collectAsState()
    val path by remember { derivedStateOf { curTrack?.path } }

    return rememberTrackCoverPainterWithPalette(
        path = path,
        isPlaceholderRequired = isPlaceholderRequired,
        size = size,
        isBlured = isBlured,
        usePrevCoverAsPlaceholder = usePrevCoverAsPlaceholder,
        animationMillis = animationMillis,
        bitmapSettings = bitmapSettings
    )
}

private fun ImageRequest.Builder.prevCoverPlaceholder(prevCoverModel: BitmapDrawable?) =
    when (prevCoverModel) {
        null -> placeholder(R.drawable.cover_thumbnail)
        else -> placeholder(prevCoverModel)
    }

private fun ImageRequest.Builder.prevCoverError(prevCoverModel: BitmapDrawable?) =
    when (prevCoverModel) {
        null -> error(R.drawable.cover_thumbnail)
        else -> error(prevCoverModel)
    }

private fun ImageRequest.Builder.prevCoverFallback(prevCoverModel: BitmapDrawable?) =
    when (prevCoverModel) {
        null -> fallback(R.drawable.cover_thumbnail)
        else -> fallback(prevCoverModel)
    }

private fun ImageRequest.Builder.prevCoverPlaceholderOrDefault(
    usePrevCoverAsPlaceholder: Boolean,
    prevCoverModel: BitmapDrawable?,
) = when {
    usePrevCoverAsPlaceholder -> prevCoverPlaceholder(prevCoverModel)
    else -> placeholder(R.drawable.cover_thumbnail)
}

private fun ImageRequest.Builder.prevCoverErrorOrDefault(
    usePrevCoverAsPlaceholder: Boolean,
    prevCoverModel: BitmapDrawable?,
) = when {
    usePrevCoverAsPlaceholder -> prevCoverError(prevCoverModel)
    else -> error(R.drawable.cover_thumbnail)
}

private fun ImageRequest.Builder.prevCoverFallbackOrDefault(
    usePrevCoverAsPlaceholder: Boolean,
    prevCoverModel: BitmapDrawable?,
) = when {
    usePrevCoverAsPlaceholder -> prevCoverFallback(prevCoverModel)
    else -> fallback(R.drawable.cover_thumbnail)
}