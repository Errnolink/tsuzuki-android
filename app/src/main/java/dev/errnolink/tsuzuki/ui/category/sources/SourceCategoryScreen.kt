package dev.errnolink.tsuzuki.ui.category.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.designsystem.TsuzukiCorners
import dev.errnolink.tsuzuki.designsystem.TsuzukiIcons
import dev.errnolink.tsuzuki.designsystem.TsuzukiSpacing
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellContentScaffold
import dev.errnolink.tsuzuki.ui.shell.ShellEmpty
import eu.kanade.tachiyomi.ui.category.sources.SourceCategoryScreenState
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun TsuzukiSourceCategoryScreen(
    state: SourceCategoryScreenState.Success,
    onClickCreate: () -> Unit,
    onClickRename: (String) -> Unit,
    onClickDelete: (String) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    ShellContentScaffold(
        title = stringResource(MR.strings.action_edit_categories),
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
                stringResource(MR.strings.action_edit_categories),
                stringResource(SYMR.strings.no_source_categories),
            ) {
                PillButton(label = stringResource(MR.strings.action_add), onClick = onClickCreate)
            }
            return@ShellContentScaffold
        }

        CategoryContent(
            categories = state.categories,
            lazyListState = lazyListState,
            paddingValues = paddingValues,
            onClickRename = onClickRename,
            onClickDelete = onClickDelete,
        )
    }
}

@Composable
private fun CategoryContent(
    categories: ImmutableList<String>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickRename: (String) -> Unit,
    onClickDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = paddingValues + PaddingValues(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        itemsIndexed(
            items = categories,
            key = { _, category -> "source-category-$category" },
        ) { index, category ->
            SourceCategoryListItem(
                modifier = Modifier.animateItem(),
                category = category,
                shape = RoundedCornerShape(
                    topStart = if (index == 0) TsuzukiCorners.group else 0.dp,
                    topEnd = if (index == 0) TsuzukiCorners.group else 0.dp,
                    bottomStart = if (index == categories.lastIndex) TsuzukiCorners.group else 0.dp,
                    bottomEnd = if (index == categories.lastIndex) TsuzukiCorners.group else 0.dp,
                ),
                divider = index < categories.lastIndex,
                onRename = { onClickRename(category) },
                onDelete = { onClickDelete(category) },
            )
        }
    }
}

@Composable
private fun SourceCategoryListItem(
    category: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
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
                if (divider) drawLine(colors.separator, Offset(16.dp.toPx(), size.height), Offset(size.width, size.height), TsuzukiSpacing.hairline.toPx())
            }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TsuzukiText(category, Modifier.weight(1f).padding(horizontal = 8.dp))
        Box {
            ShellAction("Source category actions", TsuzukiIcons.More, { showActions = true })
            GlassMenu(
                showActions,
                category,
                listOf(
                    ContextMenuAction("Rename", onClick = onRename),
                    ContextMenuAction("Delete category", destructive = true, onClick = onDelete),
                ),
                onDismissRequest = { showActions = false },
            )
        }
    }
}
