package dev.errnolink.tsuzuki.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.IconButton
import dev.errnolink.tsuzuki.designsystem.FloatingTopBar
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import dev.errnolink.tsuzuki.designsystem.floatingNavigationScroll
import dev.errnolink.tsuzuki.designsystem.rememberGlassContext
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import dev.errnolink.tsuzuki.designsystem.glassSource
import eu.kanade.presentation.components.AppBar
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme

@Composable
fun ShellContentScaffold(
    title: String,
    navigateUp: (() -> Unit)? = null,
    snackbar: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val clearance = shellBottomPadding()
    val glass = LocalShellGlass.current ?: rememberGlassContext()
    Column(
        Modifier.fillMaxSize().background(TsuzukiTheme.colors.background)
            .floatingNavigationScroll(glass).statusBarsPadding(),
    ) {
        FloatingTopBar(
            title = title,
            collapsed = false,
            context = glass,
            navigationIcon = {
                if (navigateUp != null) ShellAction("Back", TsuzukiIcons.Back, navigateUp)
            },
            actions = actions,
        )
        TsuzukiText(
            title,
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            TsuzukiTheme.typography.largeTitle,
        )
        header()
        Box(Modifier.weight(1f).glassSource(glass)) {
            content(PaddingValues(top = 8.dp, bottom = clearance + 24.dp))
            ShellSnackbar(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = clearance + 12.dp))
        }
    }
}

@Composable
fun ShellToolbarActions(actions: List<AppBar.AppBarAction>) {
    var overflowVisible by remember { mutableStateOf(false) }
    val overflow = actions.filterIsInstance<AppBar.OverflowAction>()
    actions.forEach { action ->
        when (action) {
            is AppBar.Action -> IconButton(action.title, action.onClick, enabled = action.enabled) {
                Icon(action.icon, null, tint = action.iconTint ?: TsuzukiTheme.colors.text)
            }
            is AppBar.ActionCompose -> action.content()
            is AppBar.OverflowAction -> Unit
        }
    }
    if (overflow.isNotEmpty()) {
        Box {
            ShellAction("More actions", Icons.Outlined.MoreHoriz, { overflowVisible = true })
            GlassMenu(
                expanded = overflowVisible,
                label = "Actions",
                actions = overflow.map { ContextMenuAction(it.title, onClick = it.onClick) },
                onDismissRequest = { overflowVisible = false },
            )
        }
    }
}
