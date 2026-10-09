package dev.errnolink.tsuzuki.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellActionSheet
import dev.errnolink.tsuzuki.ui.shell.ShellSheetAction
import sh.calvin.reorderable.ReorderableCollectionItemScope
import tachiyomi.domain.category.model.Category

@Composable
fun ReorderableCollectionItemScope.CategoryListItem(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(14.dp),
    divider: Boolean = false,
) {
    var showActions by remember { mutableStateOf(false) }
    val colors = TsuzukiTheme.colors
    Row(
        modifier.fillMaxWidth().clip(shape).background(colors.grouped)
            .clickable(onClick = onRename)
            .drawBehind {
                if (divider) drawLine(colors.separator, Offset(52.dp.toPx(), size.height), Offset(size.width, size.height), 0.5.dp.toPx())
            }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(44.dp).draggableHandle().padding(12.dp)) {
            repeat(3) { index ->
                val y = size.height * (index + 1) / 4
                drawLine(colors.secondary, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), StrokeCap.Round)
            }
        }
        TsuzukiText(
            category.name + if (category.hidden) " · Hidden" else "",
            Modifier.weight(1f).padding(horizontal = 8.dp),
            color = if (category.hidden) TsuzukiTheme.colors.secondary else TsuzukiTheme.colors.text,
        )
        Box {
            ShellAction("Category actions", Icons.Outlined.MoreHoriz, { showActions = true })
            GlassMenu(
                showActions,
                category.name,
                listOf(
                    ContextMenuAction("Rename", icon = { Icon(Icons.Outlined.Edit, null, tint = colors.text) }, onClick = onRename),
                    ContextMenuAction(if (category.hidden) "Show category" else "Hide category", icon = { Icon(Icons.Outlined.VisibilityOff, null, tint = colors.text) }, onClick = onHide),
                    ContextMenuAction("Delete category", icon = { Icon(Icons.Outlined.DeleteOutline, null, tint = colors.destructive) }, destructive = true, onClick = onDelete),
                ),
                onDismissRequest = { showActions = false },
            )
        }
    }
}
