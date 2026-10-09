package dev.errnolink.tsuzuki.mangadex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.errnolink.tsuzuki.designsystem.ContextMenuAction
import dev.errnolink.tsuzuki.designsystem.GlassMenu
import dev.errnolink.tsuzuki.designsystem.TsuzukiText
import tachiyomi.domain.chapter.model.Chapter

@Composable
fun MangaDexChapterMenu(chapter: Chapter) {
    if (!chapter.url.startsWith("/chapter/")) return
    val navigator = LocalNavigator.currentOrThrow
    var expanded by remember(chapter.id) { mutableStateOf(false) }
    Box {
        Box(
            Modifier.size(44.dp)
                .semantics { contentDescription = "MangaDex chapter actions" }
                .clickable(role = Role.Button) { expanded = true },
            contentAlignment = Alignment.Center,
        ) { TsuzukiText("•••") }
        GlassMenu(
            expanded, chapter.name,
            listOf(
                ContextMenuAction("Chapter details and availability") { navigator.push(MangaDexChapterScreen(chapter.mangaId, chapter.id)) },
                ContextMenuAction("Comments and forum") { navigator.push(MangaDexChapterScreen(chapter.mangaId, chapter.id, openComments = true)) },
                ContextMenuAction("Block group or uploader", group = 1) { navigator.push(MangaDexChapterScreen(chapter.mangaId, chapter.id)) },
            ),
            onDismissRequest = { expanded = false },
        )
    }
}
