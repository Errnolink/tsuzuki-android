package dev.errnolink.tsuzuki.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

class ContextMenuAction(
    val label: String,
    val enabled: Boolean = true,
    val icon: (@Composable () -> Unit)? = null,
    val subtitle: String? = null,
    val group: Int = 0,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun GlassMenu(
    expanded: Boolean,
    label: String,
    actions: List<ContextMenuAction>,
    onDismissRequest: () -> Unit,
) {
    if (!expanded) return
    val motion = TsuzukiTheme.motion
    val position = remember { MenuPosition() }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        if (entered) 1f else 0.92f,
        if (motion.reduced) snap() else motion.sheetSpring(),
        label = "Menu reveal",
    )
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        CompositionLocalProvider(LocalInsideGlass provides false) {
        GlassSurface(
            context = null,
            modifier = Modifier.widthIn(min = 260.dp, max = 340.dp).heightIn(max = 520.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = position.origin
                }.semantics { paneTitle = label },
            shape = RoundedCornerShape(TsuzukiCorners.sheet),
            material = GlassMaterial.Thick,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                actions.forEachIndexed { index, action ->
                    if (index > 0 && actions[index - 1].group != action.group) Spacer(Modifier.height(8.dp))
                    GroupedRow(
                        title = action.label,
                        subtitle = action.subtitle,
                        enabled = action.enabled,
                        divider = index < actions.lastIndex && action.group == actions[index + 1].group,
                        leading = action.icon ?: {
                            val color = if (action.destructive) TsuzukiTheme.colors.destructive else TsuzukiTheme.colors.secondary
                            Canvas(Modifier.size(20.dp)) {
                                repeat(3) { drawCircle(color, 1.5.dp.toPx(), Offset(size.width * (it + 1) / 4, size.height / 2)) }
                            }
                        },
                        onClick = { onDismissRequest(); action.onClick() },
                        destructive = action.destructive,
                    )
                }
            }
        }
        }
    }
}

@Composable
fun ContextMenu(
    label: String,
    actions: List<ContextMenuAction>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier.combinedClickable(
            onClick = onClick, onLongClickLabel = "Show actions for $label",
            onLongClick = { expanded = true },
        ),
    ) {
        content()
        GlassMenu(expanded, label, actions, onDismissRequest = { expanded = false })
    }
}

private class MenuPosition : PopupPositionProvider {
    var origin by mutableStateOf(TransformOrigin.Center)
        private set
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val y = if (anchorBounds.bottom + popupContentSize.height <= windowSize.height) anchorBounds.bottom else anchorBounds.top - popupContentSize.height
        val position = IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
        origin = TransformOrigin(
            ((anchorBounds.center.x - position.x).toFloat() / popupContentSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
            ((anchorBounds.center.y - position.y).toFloat() / popupContentSize.height.coerceAtLeast(1)).coerceIn(0f, 1f),
        )
        return position
    }
}
