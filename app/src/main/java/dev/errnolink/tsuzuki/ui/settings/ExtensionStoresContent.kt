package dev.errnolink.tsuzuki.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.MoreHoriz
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellActionSheet
import dev.errnolink.tsuzuki.ui.shell.ShellSheetAction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.R
import kotlinx.collections.immutable.persistentListOf
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.KOMIKKU_SIGNATURE
import mihon.domain.extension.model.REPO_SIGNATURE
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Discord

@Composable
fun ExtensionStoresContent(
    repos: List<ExtensionStore>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    // KMK -->
    onClickEnable: (ExtensionStore) -> Unit,
    onClickDisable: (ExtensionStore) -> Unit,
    disabledRepos: Set<String>,
    // KMK <--
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        modifier = modifier,
    ) {
        repos.forEach {
            item {
                ExtensionStoresListItem(
                    modifier = Modifier.animateItem(),
                    store = it,
                    onOpenWebsite = { onOpenWebsite(it) },
                    onOpenDiscord = { onOpenDiscord(it) },
                    onCopy = { onCopy(it) },
                    onDelete = { onClickDelete(it) },
                    // KMK -->
                    onEnable = { onClickEnable(it) },
                    onDisable = { onClickDisable(it) },
                    isDisabled = it.indexUrl in disabledRepos,
                    // KMK <--
                )
            }
        }
    }
}

@Composable
private fun ExtensionStoresListItem(
    store: ExtensionStore,
    onOpenWebsite: () -> Unit,
    onOpenDiscord: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    // KMK -->
    isDisabled: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    // KMK <--
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TsuzukiTheme.colors.grouped)
            .clickable(onClick = onOpenWebsite).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(repoResId(store.signingKey)), null, Modifier.size(32.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            TsuzukiText(store.name, style = TsuzukiTheme.typography.headline, color = if (isDisabled) TsuzukiTheme.colors.secondary else TsuzukiTheme.colors.text)
            TsuzukiText(
                if (isDisabled) "Disabled" else store.indexUrl,
                style = TsuzukiTheme.typography.footnote,
                color = TsuzukiTheme.colors.secondary,
                maxLines = 2,
            )
        }
        ShellAction("Repository actions", Icons.Outlined.MoreHoriz, { expanded = true })
    }
    ShellActionSheet(
        visible = expanded,
        title = store.name,
        actions = buildList {
            add(ShellSheetAction("Website", onClick = onOpenWebsite))
            if (store.contact.discord != null) add(ShellSheetAction("Discord", onClick = onOpenDiscord))
            add(ShellSheetAction("Copy URL", onClick = onCopy))
            add(ShellSheetAction(if (isDisabled) "Enable repository" else "Disable repository", onClick = if (isDisabled) onEnable else onDisable))
            add(ShellSheetAction("Delete repository", destructive = true, onClick = onDelete))
        },
        onDismissRequest = { expanded = false },
    )
}

// KMK -->
fun repoResId(signKey: String) = when (signKey) {
    KOMIKKU_SIGNATURE -> R.mipmap.komikku
    REPO_SIGNATURE -> R.mipmap.repo
    else -> R.mipmap.extension
}

@PreviewLightDark
@Composable
fun ExtensionReposContentPreview() {
    val repos = persistentListOf(
        ExtensionStore("https://komikku", "Komikku", "", KOMIKKU_SIGNATURE, ExtensionStore.Contact("", ""), false, null),
        ExtensionStore("https://repo", "Repo", "", REPO_SIGNATURE, ExtensionStore.Contact("", ""), false, null),
        ExtensionStore("https://other", "Other", "", "key2", ExtensionStore.Contact("", ""), true, null),
    )
    TachiyomiPreviewTheme {
        Surface {
            ExtensionStoresContent(
                repos = repos,
                lazyListState = LazyListState(),
                paddingValues = PaddingValues(),
                onCopy = {},
                onOpenWebsite = {},
                onOpenDiscord = {},
                onClickDelete = {},
                onClickEnable = {},
                onClickDisable = {},
                disabledRepos = setOf("https://repo"),
            )
        }
    }
}
// KMK <--
