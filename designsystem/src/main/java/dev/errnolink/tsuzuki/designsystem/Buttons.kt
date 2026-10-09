package dev.errnolink.tsuzuki.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = true,
    destructive: Boolean = false,
) {
    val colors = TsuzukiTheme.colors
    val foreground = when {
        !enabled -> colors.secondary
        destructive -> colors.destructive
        prominent -> colors.onAccent
        else -> colors.accent
    }
    Box(
        modifier.clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .background(if (enabled && prominent && !destructive) colors.accent else colors.elevated)
            .defaultMinSize(minHeight = TsuzukiSpacing.touchTarget)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        TsuzukiText(label, style = TsuzukiTheme.typography.headline, color = foreground)
    }
}

@Composable
fun GlassButton(
    context: GlassContext?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hero: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    GlassSurface(context, modifier, hero = hero) {
        Row(
            Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .alpha(if (enabled) 1f else 0.45f)
                .defaultMinSize(minHeight = TsuzukiSpacing.touchTarget)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun IconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    context: GlassContext? = null,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    GlassSurface(context, modifier, shape = CircleShape) {
        Box(
            Modifier.sizeIn(minWidth = TsuzukiSpacing.touchTarget, minHeight = TsuzukiSpacing.touchTarget)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = label }
                .alpha(if (enabled) 1f else 0.45f)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalTsuzukiContentColor provides TsuzukiTheme.colors.text, content = icon)
        }
    }
}
