package dev.errnolink.tsuzuki.designsystem

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SheetDetent { Medium, Large }

@Composable
fun DetentSheet(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    initialDetent: SheetDetent = SheetDetent.Medium,
    scrollContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    val colors = TsuzukiTheme.colors
    val motion = TsuzukiTheme.motion
    val scope = rememberCoroutineScope()
    val dismissRequest by rememberUpdatedState(onDismissRequest)
    var offset by remember { mutableFloatStateOf(Float.NaN) }
    var detent by remember { mutableStateOf(initialDetent) }
    var animation by remember { mutableStateOf<Job?>(null) }
    var height by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val flingThreshold = with(density) { 800.dp.toPx() }
    fun settle(target: Float, velocity: Float = 0f, dismiss: Boolean = false) {
        animation?.cancel()
        animation = scope.launch {
            animateSheetOffset(offset.takeIf { it.isFinite() } ?: height, target, velocity, height, motion, dismiss) {
                offset = it.coerceIn(0f, height)
            }
            if (dismiss) dismissRequest()
        }
    }
    fun close() = settle(height, dismiss = true)
    fun resize(target: SheetDetent) {
        detent = target
        settle(if (target == SheetDetent.Large) 0f else height / 2)
    }
    fun drag(delta: Float): Float {
        animation?.cancel()
        val old = offset.takeIf { it.isFinite() } ?: 0f
        offset = (old + delta).coerceIn(0f, height)
        return offset - old
    }
    fun release(velocity: Float) {
        val projected = offset + velocity * 0.16f
        val target = when {
            velocity > flingThreshold && offset >= height * 0.45f -> height
            velocity < -flingThreshold -> if (offset > height * 0.6f) height / 2 else 0f
            projected > height * 0.76f -> height
            projected > height * 0.25f -> height / 2
            else -> 0f
        }
        detent = if (target == 0f) SheetDetent.Large else SheetDetent.Medium
        settle(target, velocity, dismiss = target == height)
    }
    val nestedScroll = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (source == NestedScrollSource.UserInput && available.y < 0f && offset > 0f) Offset(0f, drag(available.y)) else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (source == NestedScrollSource.UserInput && available.y > 0f) Offset(0f, drag(available.y)) else Offset.Zero

        override suspend fun onPreFling(available: Velocity): Velocity = if (offset > 0f) {
            release(available.y)
            Velocity(0f, available.y)
        } else {
            Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (offset > 0f) release(available.y)
            return Velocity.Zero
        }
    }
    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        CompositionLocalProvider(LocalInsideGlass provides false) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = ::close)
                    .clearAndSetSemantics {},
            )
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(top = 12.dp)) {
                val heightPx = with(density) { maxHeight.toPx() }
                val imeVisible = WindowInsets.ime.getBottom(density) > 0
                LaunchedEffect(heightPx, imeVisible, motion.reduced, density.fontScale) {
                    animation?.cancel()
                    height = heightPx
                    if (imeVisible || density.fontScale >= 1.5f || maxHeight < 360.dp) detent = SheetDetent.Large
                    val target = if (detent == SheetDetent.Large) 0f else height / 2
                    if (!offset.isFinite()) offset = height
                    settle(target)
                }
                GlassSurface(
                    context = null,
                    modifier = modifier.align(Alignment.BottomCenter).widthIn(max = 640.dp).fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(with(density) { (heightPx - (offset.takeIf { it.isFinite() } ?: heightPx)).coerceAtLeast(0f).toDp() })
                        .nestedScroll(nestedScroll)
                        .semantics { paneTitle = title; dismiss { close(); true } },
                    shape = RoundedCornerShape(TsuzukiCorners.sheet),
                    material = if (detent == SheetDetent.Large) GlassMaterial.Opaque else GlassMaterial.Thick,
                ) {
                    Column(Modifier.fillMaxSize()) {
                    val draggable = Modifier.draggable(
                        state = rememberDraggableState { drag(it) }, orientation = Orientation.Vertical,
                        onDragStarted = { animation?.cancel() }, onDragStopped = { release(it) },
                    )
                    Box(
                        Modifier.fillMaxWidth().height(44.dp).then(draggable)
                            .clickable { resize(if (detent == SheetDetent.Medium) SheetDetent.Large else SheetDetent.Medium) }
                            .semantics {
                                contentDescription = "Resize sheet"
                                stateDescription = if (detent == SheetDetent.Medium) "Medium" else "Large"
                                expand { resize(SheetDetent.Large); true }
                                collapse { resize(SheetDetent.Medium); true }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Spacer(Modifier.size(36.dp, 5.dp).background(colors.separator, CircleShape))
                    }
                    Row(
                        Modifier.fillMaxWidth().then(draggable).padding(start = 12.dp, end = 20.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconButton("Close $title", ::close) { CloseMark() }
                        TsuzukiText(title, Modifier.weight(1f), TsuzukiTheme.typography.title3)
                    }
                    Column(
                        Modifier.weight(1f)
                            .then(if (scrollContent) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp), content = content,
                    )
                    }
                }
            }
        }
        }
    }
}

private suspend fun animateSheetOffset(
    start: Float,
    target: Float,
    velocity: Float,
    height: Float,
    motion: TsuzukiMotion,
    closing: Boolean,
    update: (Float) -> Unit,
) {
    if (motion.reduced || start == target) {
        update(target)
        return
    }
    val animation = TargetBasedAnimation(motion.sheetSpring(), Float.VectorConverter, start, target, velocity)
    val fraction = (abs(target - start) / height.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val millis = if (closing) motion.sheetCloseMillis else
        (motion.sheetSettleMinMillis + (motion.sheetSettleMaxMillis - motion.sheetSettleMinMillis) * fraction).roundToInt()
    val duration = millis * 1_000_000L
    val started = withFrameNanos { it }
    do {
        val elapsed = withFrameNanos { it } - started
        val playTime = (animation.durationNanos * (elapsed.toDouble() / duration).coerceAtMost(1.0)).toLong()
        update(animation.getValueFromNanos(playTime))
    } while (elapsed < duration)
    update(target)
}
