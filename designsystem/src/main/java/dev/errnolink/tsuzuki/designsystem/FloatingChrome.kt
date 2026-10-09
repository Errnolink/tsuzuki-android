package dev.errnolink.tsuzuki.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun FloatingTopBar(
    title: String,
    collapsed: Boolean,
    context: GlassContext?,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = TsuzukiTheme.colors
    val motion = TsuzukiTheme.motion
    val edgeAlpha by animateFloatAsState(
        if (collapsed) 1f else 0f,
        if (motion.reduced) snap() else tween(motion.chromeFadeMillis),
        label = "Scroll edge",
    )
    Box(modifier.fillMaxWidth()) {
        if (edgeAlpha > 0f) {
            Spacer(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(colors.background.copy(alpha = 0.96f * edgeAlpha), Color.Transparent),
                    ),
                ),
            )
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassSurface(
                context,
                if (collapsed) Modifier.weight(1f) else Modifier,
                material = GlassMaterial.Thick,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    navigationIcon()
                    if (collapsed) {
                        TsuzukiText(
                            title,
                            Modifier.weight(1f, fill = false).padding(horizontal = 16.dp, vertical = 12.dp),
                            TsuzukiTheme.typography.headline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (!collapsed) Spacer(Modifier.weight(1f))
            GlassSurface(context) {
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}
