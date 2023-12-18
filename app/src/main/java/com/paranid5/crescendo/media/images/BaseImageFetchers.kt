package com.paranid5.crescendo.media.images

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import coil.executeBlocking
import com.paranid5.crescendo.presentation.ui.extensions.toBitmap
import org.jaudiotagger.audio.AudioFileIO
import java.io.File

fun Any.getCoilModel(context: Context, size: ImageSize? = null) =
    when (size) {
        null -> getCoilModelBuilder(context).build()

        else -> getCoilModelBuilder(context)
            .size(size.width, size.height)
            .build()
    }

suspend inline fun getBitmapFromModel(
    context: Context,
    model: Any,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = ImageLoaderBuilder(context)
    .build()
    .execute(model.getCoilModel(context, size))
    .drawable!!
    .toResizedBitmap(size)
    .also(bitmapSettings)

inline fun getBitmapFromModelBlocking(
    context: Context,
    model: Any,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = ImageLoaderBuilder(context)
    .build()
    .executeBlocking(model.getCoilModel(context, size))
    .drawable!!
    .toResizedBitmap(size)
    .also(bitmapSettings)

suspend inline fun getBitmapFromResource(
    context: Context,
    @DrawableRes res: Int,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getBitmapFromModel(context, res, size, bitmapSettings)

inline fun getBitmapFromResourceBlocking(
    context: Context,
    @DrawableRes res: Int,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getBitmapFromModelBlocking(context, res, size, bitmapSettings)

suspend inline fun getBitmapFromResourceCatching(
    context: Context,
    @DrawableRes res: Int,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromResource(context, res, size, bitmapSettings)
}

inline fun getBitmapFromResourceBlockingCatching(
    context: Context,
    @DrawableRes res: Int,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromResourceBlocking(context, res, size, bitmapSettings)
}

suspend inline fun getBitmapFromUrl(
    context: Context,
    url: String,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getBitmapFromModel(context, url, size, bitmapSettings)

inline fun getBitmapFromUrlBlocking(
    context: Context,
    url: String,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getBitmapFromModelBlocking(context, url, size, bitmapSettings)

suspend inline fun getBitmapFromUrlWithPalette(
    context: Context,
    url: String,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getBitmapFromUrl(context, url, size, bitmapSettings).withPalette

inline fun getBitmapFromUrlWithPaletteBlocking(
    context: Context,
    url: String,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getBitmapFromUrlBlocking(context, url, size, bitmapSettings).withPalette

suspend inline fun getBitmapFromUrlCatching(
    context: Context,
    url: String,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromUrl(context, url, size, bitmapSettings)
}

inline fun getBitmapFromUrlBlockingCatching(
    context: Context,
    url: String,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromUrlBlocking(context, url, size, bitmapSettings)
}

suspend inline fun getBitmapFromUrlWithPaletteCatching(
    context: Context,
    url: String,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromUrlWithPalette(context, url, size, bitmapSettings)
}

inline fun getBitmapFromUrlWithPaletteBlockingCatching(
    context: Context,
    url: String,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromUrlWithPaletteBlocking(context, url, size, bitmapSettings)
}

private fun getCoverDataByPath(path: String?): ByteArray? {
    fun legacyCoverDataByPath(path: String) =
        AudioFileIO
            .read(File(path))
            .tagOrCreateAndSetDefault
            ?.firstArtwork
            ?.binaryData

    fun modernCoverDataByPath() =
        MediaMetadataRetriever()
            .apply { setDataSource(path) }
            .embeddedPicture

    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
            path?.let(::legacyCoverDataByPath)

        else -> modernCoverDataByPath()
    }
}

suspend fun getBitmapFromPath(
    context: Context,
    path: String?,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getCoverDataByPath(path)
    ?.toBitmap()
    ?.let { getBitmapFromModel(context, it, size, bitmapSettings) }
    ?: getThumbnailBitmap(context)

fun getBitmapFromPathBlocking(
    context: Context,
    path: String?,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getCoverDataByPath(path)
    ?.toBitmap()
    ?.let { getBitmapFromModelBlocking(context, it, size, bitmapSettings) }
    ?: getThumbnailBitmapBlocking(context)

suspend fun getBitmapFromPathWithPalette(
    context: Context,
    path: String?,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getBitmapFromPath(context, path, size, bitmapSettings).withPalette

fun getBitmapFromPathWithPaletteBlocking(
    context: Context,
    path: String?,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = getBitmapFromPathBlocking(context, path, size, bitmapSettings).withPalette

suspend fun getBitmapFromPathCatching(
    context: Context,
    path: String?,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromPath(context, path, size, bitmapSettings)
}

fun getBitmapFromPathBlockingCatching(
    context: Context,
    path: String?,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromPathBlocking(context, path, size, bitmapSettings)
}

suspend fun getBitmapFromPathWithPaletteCatching(
    context: Context,
    path: String?,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromPathWithPalette(context, path, size, bitmapSettings)
}

fun getBitmapFromPathWithPaletteBlockingCatching(
    context: Context,
    path: String?,
    size: ImageSize? = null,
    bitmapSettings: (Bitmap) -> Unit = {}
) = runCatching {
    getBitmapFromPathWithPaletteBlocking(context, path, size, bitmapSettings)
}