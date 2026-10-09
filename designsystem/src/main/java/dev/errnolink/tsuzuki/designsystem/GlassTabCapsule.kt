package dev.errnolink.tsuzuki.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class GlassTab(
    val label: String,
    val icon: @Composable () -> Unit,
    val selectedIcon: @Composable () -> Unit,
)

@Composable
fun GlassTabCapsule(
    tabs: List<GlassTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    context: GlassContext?,
    modifier: Modifier = Modifier,
    hero: Boolean = false,
) {
    require(tabs.size in 2..5 && selectedIndex in tabs.indices)
    val colors = TsuzukiTheme.colors
    val motion = TsuzukiTheme.motion
    val minimized = context?.navigationMinimized == true
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val index by animateFloatAsState(
        selectedIndex.toFloat(), if (motion.reduced) snap() else motion.tabSpring(), label = "Tab fill",
    )
    LaunchedEffect(selectedIndex) { context?.navigationMinimized = false }
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val expandedWidth = (maxWidth - 18.dp).coerceAtMost(
            when (tabs.size) { 2 -> 180.dp; 3 -> 266.dp; else -> 352.dp },
        )
        val width by animateDpAsState(
            if (minimized) 64.dp else expandedWidth,
            if (motion.reduced) snap() else motion.tabSpring(), label = "Tab capsule width",
        )
        GlassSurface(context, Modifier.width(width).height(if (minimized) 40.dp else 62.dp), hero = hero, material = GlassMaterial.Thick) {
            if (minimized) {
                Box(
                    Modifier.fillMaxSize().clickable(role = Role.Button) { context?.navigationMinimized = false }
                        .semantics { contentDescription = "${tabs[selectedIndex].label}, show tabs" },
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalTsuzukiContentColor provides colors.text) {
                        Box(Modifier.size(22.dp)) { tabs[selectedIndex].selectedIcon() }
                    }
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp).clip(CircleShape)) {
                    val cellWidth = maxWidth / tabs.size
                    val cellPx = with(LocalDensity.current) { cellWidth.toPx() }
                    val selection = Color(if (colors.isDark) 0xFF0091FF else 0xFF0088FF)
                    Box(Modifier.matchParentSize()) {
                        Box(
                            Modifier.width(cellWidth).fillMaxHeight().graphicsLayer {
                                translationX = index.coerceIn(0f, tabs.lastIndex.toFloat()) * cellPx * if (rtl) -1f else 1f
                            }.background(selection.copy(alpha = if (colors.isDark) 0.32f else 0.18f), CircleShape),
                        )
                    }
                    Row(Modifier.fillMaxSize().selectableGroup()) {
                        tabs.forEachIndexed { tabIndex, tab ->
                            val selected = tabIndex == selectedIndex
                            Column(
                                Modifier.weight(1f).fillMaxHeight()
                                    .selectable(selected, role = Role.Tab, onClick = { onSelect(tabIndex) })
                                    .padding(horizontal = 3.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                            ) {
                                CompositionLocalProvider(LocalTsuzukiContentColor provides colors.text) {
                                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                        if (selected) tab.selectedIcon() else tab.icon()
                                    }
                                    TsuzukiText(tab.label, style = TsuzukiTheme.typography.tabLabel, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
