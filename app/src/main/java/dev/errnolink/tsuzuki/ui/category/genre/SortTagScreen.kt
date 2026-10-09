package dev.errnolink.tsuzuki.ui.category.genre

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiCorners
import dev.errnolink.tsuzuki.designsystem.TsuzukiSpacing
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellContentScaffold
import dev.errnolink.tsuzuki.ui.shell.ShellEmpty
import eu.kanade.tachiyomi.ui.category.genre.SortTagScreenState
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun TsuzukiSortTagScreen(
    state: SortTagScreenState.Success,
    onClickCreate: () -> Unit,
    onClickDelete: (String) -> Unit,
    onClickMoveUp: (String, Int) -> Unit,
    onClickMoveDown: (String, Int) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    ShellContentScaffold(
        title = stringResource(SYMR.strings.action_edit_tags),
        navigateUp = navigateUp,
        actions = {
            PillButton(
                label = stringResource(MR.strings.action_add),
                onClick = onClickCreate,
                prominent = false,
            )
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            ShellEmpty(
                stringResource(SYMR.strings.action_edit_tags),
                stringResource(SYMR.strings.information_empty_tags),
            ) {
                PillButton(label = stringResource(MR.strings.action_add), onClick = onClickCreate)
            }
            return@ShellContentScaffold
        }

        TagContent(
            tags = state.tags,
            lazyListState = lazyListState,
            paddingValues = paddingValues,
            onClickDelete = onClickDelete,
            onMoveUp = onClickMoveUp,
            onMoveDown = onClickMoveDown,
        )
    }
}

@Composable
private fun TagContent(
    tags: ImmutableList<String>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickDelete: (String) -> Unit,
    onMoveUp: (String, Int) -> Unit,
    onMoveDown: (String, Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = paddingValues + PaddingValues(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        itemsIndexed(
            items = tags,
            key = { _, tag -> "sort-tag-$tag" },
        ) { index, tag ->
            SortTagListItem(
                modifier = Modifier.animateItem(),
                tag = tag,
                shape = RoundedCornerShape(
                    topStart = if (index == 0) TsuzukiCorners.group else 0.dp,
                    topEnd = if (index == 0) TsuzukiCorners.group else 0.dp,
                    bottomStart = if (index == tags.lastIndex) TsuzukiCorners.group else 0.dp,
                    bottomEnd = if (index == tags.lastIndex) TsuzukiCorners.group else 0.dp,
                ),
                divider = index < tags.lastIndex,
                canMoveUp = index != 0,
                canMoveDown = index != tags.lastIndex,
                onMoveUp = { onMoveUp(tag, index) },
                onMoveDown = { onMoveDown(tag, index) },
                onDelete = { onClickDelete(tag) },
            )
        }
    }
}

@Composable
private fun SortTagListItem(
    tag: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(14.dp),
    divider: Boolean = false,
) {
    var showActions by remember { mutableStateOf(false) }
    val colors = TsuzukiTheme.colors
    Row(
        modifier.fillMaxWidth().clip(shape).background(colors.grouped)
            .drawBehind {
                if (divider) drawLine(colors.separator, Offset(16.dp.toPx(), size.height), Offset(size.width, size.height), TsuzukiSpacing.hairline.toPx())
            }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TsuzukiText(tag, Modifier.weight(1f).padding(horizontal = 8.dp))
        MoveMark(up = true, enabled = canMoveUp, onClick = onMoveUp)
        MoveMark(up = false, enabled = canMoveDown, onClick = onMoveDown)
        Box {
            ShellAction("Tag actions", TsuzukiIcons.More, { showActions = true })
            GlassMenu(
                showActions,
                tag,
                listOf(
                    ContextMenuAction("Move up", enabled = canMoveUp, onClick = onMoveUp),
                    ContextMenuAction("Move down", enabled = canMoveDown, onClick = onMoveDown),
                    ContextMenuAction("Delete tag", destructive = true, onClick = onDelete),
                ),
                onDismissRequest = { showActions = false },
            )
        }
    }
}

@Composable
private fun MoveMark(up: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = TsuzukiTheme.colors.accent
    Box(
        Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 14.dp, height = 8.dp)) {
            val path = Path().apply {
                moveTo(0f, if (up) size.height else 0f)
                lineTo(size.width / 2f, if (up) 0f else size.height)
                lineTo(size.width, if (up) size.height else 0f)
            }
            drawPath(
                path,
                color,
                style = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
