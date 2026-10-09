package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabNavigator
import dev.errnolink.tsuzuki.designsystem.*
import dev.errnolink.tsuzuki.ui.shell.LocalShellArrivals
import dev.errnolink.tsuzuki.ui.shell.LocalShellBottomPadding
import dev.errnolink.tsuzuki.ui.shell.LocalShellGlass
import dev.errnolink.tsuzuki.ui.shell.ShellBadge
import dev.errnolink.tsuzuki.ui.shell.rememberTabNavigationMotion
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.libraryUpdateError.LibraryUpdateErrorScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object HomeScreen : Screen() {
    private fun readResolve(): Any = HomeScreen
    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()
    private val reselect = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val reselectEvents = reselect.asSharedFlow()
    private val tabs = listOf(LibraryTab, UpdatesTab, HistoryTab, BrowseTab, MoreTab)

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val glass = rememberGlassContext()
        val arrivals = rememberCoverArrivalState()
        val density = LocalDensity.current
        var bottomHeight by remember { mutableIntStateOf(0) }
        val bottomVisible by produceState(true) {
            showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
        }
        TabNavigator(tab = LibraryTab, key = "HomeTabs") { tabNavigator ->
            CompositionLocalProvider(
                LocalNavigationMotion provides rememberTabNavigationMotion(tabNavigator.current.key),
                LocalNavigator provides navigator,
                LocalShellGlass provides glass,
                LocalShellArrivals provides arrivals,
                LocalShellBottomPadding provides if (bottomVisible) with(density) { bottomHeight.toDp() } else 0.dp,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        tabNavigator.saveableState(key = "currentTab", tabNavigator.current) {
                            tabNavigator.current.Content()
                        }
                    }
                    if (bottomVisible) {
                        val visible = tabs.filter { it.isEnabled() }.let { if (it.size in 4..5) it else tabs }
                        val glassTabs = visible.map { tab ->
                            GlassTab(
                                tab.options.title,
                                icon = { TabIcon(tab) },
                                selectedIcon = { TabIcon(tab) },
                            )
                        }
                        val selectedIndex = visible.indexOfFirst { it.key == tabNavigator.current.key }.coerceAtLeast(0)
                        Box(Modifier.align(Alignment.BottomCenter).onSizeChanged { bottomHeight = it.height }
                            .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = 540.dp)) {
                            GlassTabCapsule(glassTabs, selectedIndex, { index ->
                                val tab = visible[index]
                                if (tabNavigator.current.key == tab.key) {
                                    reselect.tryEmit(tab.key)
                                    if (tab == MoreTab) scope.launch { tab.onReselect(navigator) }
                                } else tabNavigator.current = tab
                            }, glass)
                        }
                    }
                }
            }
            BackHandler(enabled = tabNavigator.current != LibraryTab) { tabNavigator.current = LibraryTab }
            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = LibraryTab
                        LibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Library -> LibraryTab
                            Tab.Updates -> UpdatesTab
                            Tab.History -> HistoryTab
                            is Tab.Browse -> { if (it.toExtensions) BrowseTab.showExtension(); BrowseTab }
                            is Tab.More -> MoreTab
                        }
                        if (it is Tab.Library && it.mangaIdToOpen != null) navigator.push(MangaScreen(it.mangaIdToOpen))
                        if (it is Tab.More) {
                            if (it.toDownloads) navigator.push(DownloadQueueScreen)
                            else if (it.toLibraryUpdateErrors) navigator.push(LibraryUpdateErrorScreen())
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TabIcon(tab: eu.kanade.presentation.util.Tab) {
        val count by produceState(0, tab.key) {
            when (tab) {
                UpdatesTab -> {
                    val preferences = Injekt.get<LibraryPreferences>()
                    combine(preferences.newShowUpdatesCount().changes(), preferences.newUpdatesCount().changes()) { show, count -> if (show) count else 0 }
                        .collectLatest { value = it }
                }
                BrowseTab -> Injekt.get<SourcePreferences>().extensionUpdatesCount().changes().collectLatest { value = it }
                else -> Unit
            }
        }
        Box {
            dev.errnolink.tsuzuki.ui.shell.ShellTabIcon(tab)
            if (count > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-7).dp)) { ShellBadge(count.toString()) }
        }
    }

    suspend fun search(query: String) { librarySearchEvent.send(query) }
    suspend fun openTab(tab: Tab) { openTabEvent.send(tab) }
    suspend fun showBottomNav(show: Boolean) { showBottomNavEvent.send(show) }

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean, val toLibraryUpdateErrors: Boolean = false) : Tab
    }
}
