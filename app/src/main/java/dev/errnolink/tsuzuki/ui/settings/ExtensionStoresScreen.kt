package dev.errnolink.tsuzuki.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.material.icons.outlined.Add
import androidx.compose.ui.unit.dp
import dev.errnolink.tsuzuki.designsystem.PillButton
import dev.errnolink.tsuzuki.ui.shell.ShellAction
import dev.errnolink.tsuzuki.ui.shell.ShellContentScaffold
import dev.errnolink.tsuzuki.ui.shell.ShellEmpty
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoreScreenState
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.collections.immutable.persistentListOf
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.KOMIKKU_SIGNATURE
import mihon.domain.extension.model.REPO_HELP
import mihon.domain.extension.model.REPO_SIGNATURE
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun ExtensionStoresScreen(
    state: ExtensionStoreScreenState.Success,
    onClickCreate: () -> Unit,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    // KMK -->
    onClickEnable: (ExtensionStore) -> Unit,
    onClickDisable: (ExtensionStore) -> Unit,
    // KMK <--
    onClickRefresh: () -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    ShellContentScaffold(
        title = stringResource(MR.strings.extensionStores),
        navigateUp = navigateUp,
        actions = {
            ShellAction("Refresh repositories", Icons.Outlined.Refresh, onClickRefresh)
            ShellAction("Add repository", Icons.Outlined.Add, onClickCreate)
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            val context = LocalContext.current
            ShellEmpty("Add your catalogs", stringResource(MR.strings.extensionStoresScreen_emptyLabel)) {
                PillButton("Add repository", onClickCreate)
                PillButton(stringResource(MR.strings.label_help), { context.openInBrowser(REPO_HELP) }, prominent = false)
            }
            return@ShellContentScaffold
        }

        ExtensionStoresContent(
            repos = state.stores,
            lazyListState = lazyListState,
            paddingValues = paddingValues + PaddingValues(horizontal = 20.dp),
            onCopy = onCopy,
            onOpenWebsite = onOpenWebsite,
            onOpenDiscord = onOpenDiscord,
            onClickDelete = onClickDelete,
            // KMK -->
            onClickEnable = onClickEnable,
            onClickDisable = onClickDisable,
            disabledRepos = state.disabledRepos,
            // KMK <--
        )
    }
}

// KMK -->
@PreviewLightDark
@Composable
private fun ExtensionStoresScreenPreview() {
    val state = ExtensionStoreScreenState.Success(
        stores = persistentListOf(
            ExtensionStore("https://komikku", "Komikku", "", KOMIKKU_SIGNATURE, ExtensionStore.Contact("", ""), false, null),
            ExtensionStore("https://repo", "Repo", "", REPO_SIGNATURE, ExtensionStore.Contact("", ""), false, null),
            ExtensionStore("https://other", "Other", "", "key2", ExtensionStore.Contact("", ""), true, null),
        ),
        disabledRepos = setOf("https://repo"),
    )
    TachiyomiPreviewTheme {
        Surface {
            ExtensionStoresScreen(
                state = state,
                onClickCreate = { },
                onCopy = { },
                onOpenWebsite = { },
                onOpenDiscord = { },
                onClickDelete = { },
                onClickEnable = { },
                onClickDisable = { },
                onClickRefresh = { },
                navigateUp = { },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ExtensionStoresScreenEmptyPreview() {
    val state = ExtensionStoreScreenState.Success(stores = persistentListOf())
    TachiyomiPreviewTheme {
        Surface {
            ExtensionStoresScreen(
                state = state,
                onClickCreate = { },
                onCopy = { },
                onOpenWebsite = { },
                onOpenDiscord = { },
                onClickDelete = { },
                onClickEnable = { },
                onClickDisable = { },
                onClickRefresh = { },
                navigateUp = { },
            )
        }
    }
}
// KMK <--
