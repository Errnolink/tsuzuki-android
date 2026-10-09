package dev.errnolink.tsuzuki.ui.details

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updatePadding
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import dev.errnolink.tsuzuki.designsystem.glassSource
import dev.errnolink.tsuzuki.designsystem.rememberGlassContext
import eu.kanade.presentation.manga.EditCoverAction
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import tachiyomi.domain.manga.model.Manga

@Composable
fun MangaCoverDialog(
    manga: Manga,
    isCustomCover: Boolean,
    snackbarHostState: SnackbarHostState,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: ((EditCoverAction) -> Unit)?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editMenu by remember { mutableStateOf(false) }
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val context = LocalContext.current
        val glass = rememberGlassContext()
        val view = remember(context) {
            ReaderPageImageView(context).apply { clipToPadding = false; clipChildren = false }
        }
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 72.dp
        val topPx = with(LocalDensity.current) { top.roundToPx() }
        val bottomPx = with(LocalDensity.current) { bottom.roundToPx() }
        DisposableEffect(view, manga) {
            val request = ImageRequest.Builder(context)
                .data(manga).size(Size.ORIGINAL).memoryCachePolicy(CachePolicy.DISABLED)
                .target { image ->
                    val drawable = image.asDrawable(context.resources)
                    val bitmap = (drawable as? BitmapDrawable)?.bitmap
                    val config = bitmap?.config?.takeIf { it != Bitmap.Config.HARDWARE } ?: Bitmap.Config.ARGB_8888
                    val owned = bitmap?.copy(config, false)?.toDrawable(context.resources) ?: drawable
                    view.setImage(owned, ReaderPageImageView.Config(zoomDuration = 0))
                }.build()
            val load = context.imageLoader.enqueue(request)
            onDispose { load.dispose() }
        }
        Box(modifier.fillMaxSize().background(TsuzukiTheme.colors.background)) {
            AndroidView(
                factory = { view },
                update = { it.updatePadding(top = topPx, bottom = bottomPx); it.onViewClicked = onDismissRequest },
                modifier = Modifier.fillMaxSize().glassSource(glass),
            )
            DetailGlassGroup(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp), context = glass) {
                DetailIcon("Close cover", Icons.Outlined.Close, onDismissRequest)
                TsuzukiText("Cover", Modifier.weight(1f).padding(horizontal = 12.dp), TsuzukiTheme.typography.headline)
            }
            DetailGlassGroup(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), context = glass) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CoverAction("Share", onShareClick)
                    CoverAction("Save", onSaveClick)
                    if (onEditClick != null && manga.favorite) {
                        Box {
                            CoverAction("Edit", { editMenu = true })
                            if (editMenu) {
                                GlassMenu(
                                    expanded = true,
                                    label = "Cover",
                                    actions = buildList {
                                        add(ContextMenuAction("Choose image", icon = { Icon(Icons.Outlined.Image, null, tint = TsuzukiTheme.colors.text) }) { onEditClick(EditCoverAction.EDIT) })
                                        if (isCustomCover) {
                                            add(
                                                ContextMenuAction(
                                                    "Reset custom cover",
                                                    icon = { Icon(Icons.Outlined.DeleteOutline, null, tint = TsuzukiTheme.colors.destructive) },
                                                    destructive = true,
                                                ) { onEditClick(EditCoverAction.DELETE) },
                                            )
                                        }
                                    },
                                    onDismissRequest = { editMenu = false },
                                )
                            }
                        }
                    }
                }
            }
            GlassSnackbar(snackbarHostState.currentSnackbarData, Modifier.align(Alignment.BottomCenter).padding(bottom = bottom))
        }
    }
}

@Composable
private fun CoverAction(label: String, onClick: () -> Unit) {
    TsuzukiText(label, Modifier.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), TsuzukiTheme.typography.headline)
}
