package dev.errnolink.tsuzuki.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun InsetGroupedList(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.padding(horizontal = TsuzukiSpacing.gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let {
            val locale = Locale.getDefault()
            val label = remember(it, locale) { it.uppercase(locale) }
            TsuzukiText(label, Modifier.padding(horizontal = 16.dp), TsuzukiTheme.typography.sectionHeader, TsuzukiTheme.colors.secondary)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(TsuzukiCorners.group)).background(TsuzukiTheme.colors.grouped),
            content = content,
        )
        footer?.let {
            TsuzukiText(it, Modifier.padding(horizontal = 16.dp), TsuzukiTheme.typography.footnote, TsuzukiTheme.colors.secondary)
        }
    }
}

@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    divider: Boolean = true,
    destructive: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    require((checked == null) == (onCheckedChange == null))
    require(checked == null || onClick == null)
    val colors = TsuzukiTheme.colors
    val interaction = when {
        checked != null -> Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange!!)
        onClick != null -> Modifier.clickable(enabled = enabled, onClick = onClick)
        else -> Modifier
    }
    val stackValue = LocalDensity.current.fontScale >= 1.5f
    Column(modifier.then(interaction)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TsuzukiText(title, color = if (!enabled) colors.secondary else if (destructive) colors.destructive else colors.text)
                subtitle?.let { TsuzukiText(it, style = TsuzukiTheme.typography.footnote, color = colors.secondary) }
                if (stackValue && value != null) TsuzukiText(value, style = TsuzukiTheme.typography.subhead, color = colors.secondary)
            }
            if (!stackValue && value != null) {
                Box(Modifier.weight(0.65f), contentAlignment = Alignment.CenterEnd) {
                    TsuzukiText(
                        value, style = TsuzukiTheme.typography.subhead, color = colors.secondary,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            if (checked != null) ToggleMark(checked, enabled)
            if (chevron) Chevron()
        }
        if (divider) Spacer(Modifier.padding(start = 16.dp).fillMaxWidth().height(TsuzukiSpacing.hairline).background(colors.separator))
    }
}

@Composable
private fun ToggleMark(checked: Boolean, enabled: Boolean) {
    val motion = TsuzukiTheme.motion
    val progress by animateFloatAsState(if (checked) 1f else 0f, if (motion.reduced) snap() else motion.tabSpring(), label = "Toggle")
    val colors = TsuzukiTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.size(50.dp, 30.dp)) {
        drawRoundRect(if (checked) colors.accent else colors.separator, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        val position = if (rtl) 1f - progress else progress
        drawCircle(
            color = if (enabled) androidx.compose.ui.graphics.Color.White else colors.elevated,
            radius = 12.dp.toPx(), center = Offset(15.dp.toPx() + 20.dp.toPx() * position, size.height / 2),
        )
    }
}

@Composable
internal fun Chevron() {
    val color = TsuzukiTheme.colors.secondary
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.size(8.dp, 14.dp)) {
        val path = Path().apply {
            moveTo(if (rtl) size.width else 0f, 0f)
            lineTo(if (rtl) 0f else size.width, size.height / 2)
            lineTo(if (rtl) size.width else 0f, size.height)
        }
        drawPath(path, color, style = Stroke(1.7.dp.toPx()))
    }
}
