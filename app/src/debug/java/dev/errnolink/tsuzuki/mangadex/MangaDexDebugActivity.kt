package dev.errnolink.tsuzuki.mangadex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.errnolink.tsuzuki.designsystem.TsuzukiTheme
import eu.kanade.presentation.util.Screen
import exh.md.handlers.MangaDexFeatures
import exh.md.utils.MdUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaDexDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = intent.getStringExtra("theme") == "dark"
        val bars = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        val screen: cafe.adriel.voyager.core.screen.Screen = when (intent.getStringExtra("screen")) {
            "covers" -> DebugCoverResolver(intent.getStringExtra("manga") ?: "f9c33607-9180-4ba6-b85c-e4b5faee7192")
            "details" -> DebugCoverResolver(intent.getStringExtra("manga") ?: "f9c33607-9180-4ba6-b85c-e4b5faee7192", details = true)
            "session" -> {
                MangaDexSessionNotification.show()
                eu.kanade.presentation.more.settings.screen.SettingsMangadexScreen
            }
            "feed" -> MangaDexFeedScreen()
            "lists" -> MangaDexListsScreen()
            "tracking" -> eu.kanade.presentation.more.settings.screen.SettingsTrackingScreen
            "library" -> eu.kanade.tachiyomi.ui.home.HomeScreen
            "list" -> MangaDexListScreen(intent.getStringExtra("list") ?: "805ba886-dd99-4aa4-b460-4bd7c7b71352", "Staff picks")
            else -> MangaDexHomeScreen()
        }
        setContent { TsuzukiTheme(dark = dark) { Navigator(screen) } }
    }
}

private class DebugCoverResolver(private val remoteId: String, private val details: Boolean = false) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var error by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(remoteId) {
            try {
                val local = withContext(Dispatchers.IO) {
                    val sources = Injekt.get<SourceManager>()
                    sources.isInitialized.first { it }
                    val source = MdUtil.getEnabledMangaDex(sourceManager = sources) ?: error("Enable a MangaDex source in Browse first")
                    val features = MangaDexFeatures(source)
                    val dto = features.manga(listOf(remoteId)).single()
                    Injekt.get<NetworkToLocalManga>()(MdUtil.createMangaEntry(dto, features.language).toDomainManga(source.id), updateInfo = false)
                }
                navigator.replace(if (details) eu.kanade.tachiyomi.ui.manga.MangaScreen(local.id) else MangaDexCoverGalleryScreen(local.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Could not load the requested title"
            }
        }
        if (error != null) MangaDexEmptyState("Cover art unavailable", error!!) else MangaDexLoading("Opening cover art…")
    }
}
