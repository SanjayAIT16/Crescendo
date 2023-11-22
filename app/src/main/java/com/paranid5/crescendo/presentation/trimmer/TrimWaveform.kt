package com.paranid5.crescendo.presentation.trimmer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.paranid5.crescendo.presentation.ui.theme.LocalAppColors
import com.paranid5.crescendo.presentation.ui.theme.TransparentUtility
import com.paranid5.crescendo.presentation.ui.utils.ext.pxToDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.callback.AmplitudaErrorListener
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val MIN_SPIKE_HEIGHT = 1F
private const val DEFAULT_GRAPHICS_LAYER_ALPHA = 0.99F

private const val CONTROLLER_RECT_WIDTH = 15F
private const val CONTROLLER_RECT_OFFSET = 7F

private const val CONTROLLER_CIRCLE_RADIUS = 25F
private const val CONTROLLER_CIRCLE_CENTER = 16F
private const val CONTROLLER_HEIGHT_OFFSET = CONTROLLER_CIRCLE_RADIUS + CONTROLLER_CIRCLE_CENTER

private const val CONTROLLER_ARROW_CORNER_BACK_OFFSET = 8F
private const val CONTROLLER_ARROW_CORNER_FRONT_OFFSET = 10F
private const val CONTROLLER_ARROW_CORNER_OFFSET = 12F

@Composable
fun TrimWaveform(
    model: String?,
    durationInMillis: Long,
    viewModel: TrimmerViewModel,
    modifier: Modifier = Modifier,
    spikeWidthRatio: Int = 5,
) {
    val context = LocalContext.current

    val amplituda by remember { derivedStateOf { Amplituda(context) } }
    val amplitudes by viewModel.amplitudesState.collectAsState()
    var spikesAmplitudes by remember { mutableStateOf(listOf<Float>()) }

    val startMillis by viewModel.startPosInMillisState.collectAsState()
    val offset by remember { derivedStateOf { startMillis safeDiv durationInMillis } }

    val endMillis by viewModel.endPosInMillisState.collectAsState()
    val endPoint by remember { derivedStateOf { endMillis safeDiv durationInMillis } }

    val canvasSizeState = remember { mutableStateOf(Size(1F, 1F)) }
    val spikesState = remember { mutableFloatStateOf(1F) }

    val canvasWidth = durationInMillis / 1000 * spikeWidthRatio
    val canvasWidthDp = canvasWidth.toInt().dp

    LaunchedEffect(key1 = model) {
        withContext(Dispatchers.IO) {
            if (model != null) viewModel.setAmplitudes(
                amplituda
                    .processAudio(model)
                    .get(AmplitudaErrorListener { it.printStackTrace() })
                    .amplitudesAsList()
            )
        }
    }

    LaunchedEffect(key1 = amplitudes) {
        withContext(Dispatchers.IO) {
            spikesAmplitudes = amplitudes.toDrawableAmplitudes(
                spikes = spikesState.floatValue.toInt(),
                minHeight = MIN_SPIKE_HEIGHT,
                maxHeight = canvasSizeState.value.height
                    .coerceAtLeast(MIN_SPIKE_HEIGHT) - CONTROLLER_HEIGHT_OFFSET
            )
        }
    }

    Box(modifier) {
        Waveform(
            viewModel = viewModel,
            durationInMillis = durationInMillis,
            canvasSizeState = canvasSizeState,
            spikesState = spikesState,
            spikesAmplitudes = spikesAmplitudes,
            modifier = Modifier
                .width(canvasWidthDp)
                .padding(
                    horizontal = CONTROLLER_CIRCLE_RADIUS
                        .toInt()
                        .pxToDp()
                )
                .fillMaxHeight()
        )

        StartBorder(
            viewModel = viewModel,
            spikeWidthRatio = spikeWidthRatio,
            modifier = Modifier
                .offset(x = (offset * (canvasWidth - CONTROLLER_CIRCLE_CENTER)).toInt().dp)
                .fillMaxHeight()
                .zIndex(10F)
        )

        EndBorder(
            viewModel = viewModel,
            spikeWidthRatio = spikeWidthRatio,
            durationInMillis = durationInMillis,
            modifier = Modifier
                .offset(x = (endPoint * (canvasWidth) + (1 - endPoint) * CONTROLLER_CIRCLE_CENTER).toInt().dp)
                .fillMaxHeight()
                .zIndex(10F)
        )
    }
}

@Composable
private fun Waveform(
    viewModel: TrimmerViewModel,
    durationInMillis: Long,
    canvasSizeState: MutableState<Size>,
    spikesState: MutableFloatState,
    spikesAmplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current.value
    val waveformBrush = SolidColor(colors.inverseSurface)
    val progressBrush = SolidColor(colors.primary.copy(alpha = 0.25F))
    val progressWaveformBrush = SolidColor(colors.onBackground)

    val startMillis by viewModel.startPosInMillisState.collectAsState()
    val offset by remember(startMillis) { derivedStateOf { startMillis safeDiv durationInMillis } }

    val endMillis by viewModel.endPosInMillisState.collectAsState()
    val endPoint by remember(endMillis) { derivedStateOf { endMillis safeDiv durationInMillis } }

    Canvas(modifier.graphicsLayer(alpha = DEFAULT_GRAPHICS_LAYER_ALPHA)) {
        canvasSizeState.value = size
        spikesState.floatValue = size.width

        spikesAmplitudes.forEachIndexed { index, amplitude ->
            drawRect(
                brush = waveformBrush,
                topLeft = Offset(
                    x = index.toFloat(),
                    y = (size.height - CONTROLLER_HEIGHT_OFFSET) / 2F - amplitude / 2F
                ),
                size = Size(
                    width = 1F,
                    height = amplitude
                ),
                style = Fill
            )
        }

        drawRect(
            brush = progressWaveformBrush,
            topLeft = Offset(offset * size.width, 0F),
            size = Size(
                width = (endPoint - offset) * size.width,
                height = size.height - CONTROLLER_HEIGHT_OFFSET
            ),
            blendMode = BlendMode.SrcAtop
        )
    }

    Canvas(modifier.graphicsLayer(alpha = DEFAULT_GRAPHICS_LAYER_ALPHA)) {
        drawRect(
            brush = progressBrush,
            topLeft = Offset(offset * size.width, 0F),
            size = Size(
                width = (endPoint - offset) * size.width,
                height = size.height - CONTROLLER_HEIGHT_OFFSET
            )
        )
    }
}

@Composable
private fun StartBorder(
    viewModel: TrimmerViewModel,
    spikeWidthRatio: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current.value
    val progressBrush = SolidColor(colors.primary)

    val startMillis by viewModel.startPosInMillisState.collectAsState()
    val endMillis by viewModel.endPosInMillisState.collectAsState()

    Canvas(
        modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                change.consume()
                viewModel.setStartPosInMillis(
                    (startMillis + (dragAmount * 200 / spikeWidthRatio))
                        .toLong()
                        .coerceIn(0 until endMillis)
                )
            }
        }
    ) {
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(CONTROLLER_CIRCLE_CENTER - CONTROLLER_RECT_OFFSET, 0F),
            size = Size(
                width = CONTROLLER_RECT_WIDTH,
                height = size.height
            ),
            cornerRadius = CornerRadius(10F, 10F),
            style = Fill
        )

        drawCircle(
            brush = progressBrush,
            radius = CONTROLLER_CIRCLE_RADIUS,
            center = Offset(CONTROLLER_CIRCLE_CENTER, size.height - CONTROLLER_CIRCLE_CENTER)
        )

        drawPath(
            path = Path().apply {
                moveTo(
                    x = CONTROLLER_CIRCLE_CENTER + CONTROLLER_ARROW_CORNER_BACK_OFFSET,
                    y = size.height - CONTROLLER_CIRCLE_CENTER - CONTROLLER_ARROW_CORNER_OFFSET
                )

                lineTo(
                    x = CONTROLLER_CIRCLE_CENTER - CONTROLLER_ARROW_CORNER_FRONT_OFFSET,
                    size.height - CONTROLLER_CIRCLE_CENTER
                )

                lineTo(
                    CONTROLLER_CIRCLE_CENTER + CONTROLLER_ARROW_CORNER_BACK_OFFSET,
                    size.height - CONTROLLER_CIRCLE_CENTER + CONTROLLER_ARROW_CORNER_OFFSET
                )

                close()
            },
            color = colors.inverseSurface,
            style = Fill
        )
    }
}

@Composable
private fun EndBorder(
    viewModel: TrimmerViewModel,
    spikeWidthRatio: Int,
    durationInMillis: Long,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current.value
    val progressBrush = SolidColor(colors.primary)

    val startMillis by viewModel.startPosInMillisState.collectAsState()
    val endMillis by viewModel.endPosInMillisState.collectAsState()

    Canvas(
        modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                change.consume()
                viewModel.setEndPosInMillis(
                    (endMillis + (dragAmount * 200 / spikeWidthRatio))
                        .toLong()
                        .coerceIn(startMillis + 1..durationInMillis)
                )
            }
        }
    ) {
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(-CONTROLLER_CIRCLE_CENTER - CONTROLLER_RECT_OFFSET, 0F),
            size = Size(
                width = CONTROLLER_RECT_WIDTH,
                height = size.height
            ),
            cornerRadius = CornerRadius(10F, 10F),
            style = Fill
        )

        drawCircle(
            brush = progressBrush,
            radius = CONTROLLER_CIRCLE_RADIUS,
            center = Offset(-CONTROLLER_CIRCLE_CENTER, size.height - CONTROLLER_CIRCLE_CENTER)
        )

        drawPath(
            path = Path().apply {
                moveTo(
                    x = -CONTROLLER_CIRCLE_CENTER - CONTROLLER_ARROW_CORNER_BACK_OFFSET,
                    y = size.height - CONTROLLER_CIRCLE_CENTER - CONTROLLER_ARROW_CORNER_OFFSET
                )

                lineTo(
                    x = -CONTROLLER_CIRCLE_CENTER + CONTROLLER_ARROW_CORNER_FRONT_OFFSET,
                    size.height - CONTROLLER_CIRCLE_CENTER
                )

                lineTo(
                    x = -CONTROLLER_CIRCLE_CENTER - CONTROLLER_ARROW_CORNER_BACK_OFFSET,
                    y = size.height - CONTROLLER_CIRCLE_CENTER + CONTROLLER_ARROW_CORNER_OFFSET
                )

                close()
            },
            color = colors.inverseSurface,
            style = Fill,
        )
    }
}

private fun List<Int>.toDrawableAmplitudes(
    spikes: Int,
    minHeight: Float,
    maxHeight: Float
): List<Float> {
    val amplitudes = map(Int::toFloat)

    if (amplitudes.isEmpty() || spikes == 0)
        return List(spikes) { minHeight }

    val transform = { data: List<Float> ->
        data.average().toFloat().coerceIn(minHeight, maxHeight)
    }

    return when {
        spikes > amplitudes.size -> amplitudes.fillToSize(spikes, transform)
        else -> amplitudes.chunkToSize(spikes, transform)
    }.normalize(minHeight, maxHeight)
}

private fun <T> Iterable<T>.fillToSize(size: Int, transform: (List<T>) -> T): List<T> {
    val len = ceil(size safeDiv count()).roundToInt()
    return map { data -> List(len) { data } }.flatten().chunkToSize(size, transform)
}

private fun <T> Iterable<T>.chunkToSize(size: Int, transform: (List<T>) -> T): List<T> {
    val chunkSize = count() / size
    val remainder = count() % size
    val remainderIndex = ceil(count() safeDiv remainder).roundToInt()

    val chunkIteration = filterIndexed { index, _ ->
        remainderIndex == 0 || index % remainderIndex != 0
    }.chunked(chunkSize, transform)

    return when (size) {
        chunkIteration.size -> chunkIteration
        else -> chunkIteration.chunkToSize(size, transform)
    }
}

private fun Iterable<Float>.normalize(min: Float, max: Float) =
    map { (max - min) * ((it - min()) safeDiv (max() - min())) + min }

private infix fun Int.safeDiv(value: Int) = when (value) {
    0 -> 0F
    else -> this / value.toFloat()
}

private infix fun Long.safeDiv(value: Long) = when (value) {
    0L -> 0F
    else -> this / value.toFloat()
}

private infix fun Float.safeDiv(value: Float) = when (value) {
    0F -> 0F
    else -> this / value
}