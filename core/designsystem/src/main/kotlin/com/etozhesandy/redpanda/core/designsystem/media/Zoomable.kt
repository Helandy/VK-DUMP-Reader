package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/** Scale a double tap zooms to, and returns from. */
private const val DOUBLE_TAP_SCALE = 2f

/**
 * Pinch-to-zoom state for one full-screen image.
 *
 * Held outside the zoomed composable so the screen around it can react — a pager has to stop
 * swiping while the image is magnified, or a horizontal drag would leave the photo instead of
 * panning it (see [MediaPagerScreen]).
 */
@Stable
class ZoomState internal constructor(private val maxScale: Float) {

    var scale: Float by mutableFloatStateOf(1f)
        private set

    var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    val isZoomed: Boolean get() = scale > 1f

    private var size: IntSize = IntSize.Zero

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    internal fun onSize(size: IntSize) {
        this.size = size
        offset = offset.clamped(scale)
    }

    internal fun onGesture(pan: Offset, zoom: Float) {
        scale = (scale * zoom).coerceIn(1f, maxScale)
        offset = (offset + pan).clamped(scale)
    }

    internal fun onDoubleTap() {
        scale = if (isZoomed) 1f else DOUBLE_TAP_SCALE
        offset = Offset.Zero
    }

    /**
     * Panning is bounded by how much of the image the magnification pushed outside the viewport —
     * half of the overflow on each side. Without this the photo can be flung off-screen with no
     * way back, since nothing here springs it into place.
     */
    private fun Offset.clamped(scale: Float): Offset {
        if (scale <= 1f) return Offset.Zero
        val maxX = abs(size.width * (scale - 1f)) / 2f
        val maxY = abs(size.height * (scale - 1f)) / 2f
        return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
    }
}

@Composable
fun rememberZoomState(maxScale: Float = 4f): ZoomState = remember(maxScale) { ZoomState(maxScale) }

/** Makes the content pinch-zoomable, pannable while zoomed, and double-tap toggleable. */
fun Modifier.zoomable(state: ZoomState): Modifier = this
    .onSizeChanged { state.onSize(it) }
    .pointerInput(state) { detectTapGestures(onDoubleTap = { state.onDoubleTap() }) }
    .pointerInput(state) { detectZoomAndPan(state) }
    .graphicsLayer {
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.offset.x
        translationY = state.offset.y
    }

/**
 * Pinch-to-zoom and panning that leave the pager's page turn alone.
 *
 * Compose's own `detectTransformGestures` consumes every drag past the touch slop, a one-finger
 * one included, so a sideways swipe over a photo was eaten by the image — pointer input runs
 * child-first, and [MediaPagerScreen]'s pager above never saw the gesture. A single finger is
 * only ours while the image is magnified and there is something to pan; at 1x its events are
 * left unconsumed and the swipe reaches the pager.
 */
private suspend fun PointerInputScope.detectZoomAndPan(state: ZoomState) {
    awaitEachGesture {
        val touchSlop = viewConfiguration.touchSlop
        var pastTouchSlop = false
        var zoom = 1f
        var pan = Offset.Zero

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            val isTransform = event.changes.count { it.pressed } > 1 || state.isZoomed
            if (!canceled && isTransform) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val zoomMotion = abs(1 - zoom) * event.calculateCentroidSize(useCurrent = false)
                    pastTouchSlop = zoomMotion > touchSlop || pan.getDistance() > touchSlop
                }

                if (pastTouchSlop) {
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        state.onGesture(panChange, zoomChange)
                    }
                    event.changes.fastForEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}
