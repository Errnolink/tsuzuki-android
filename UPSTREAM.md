# Upstream base

| Field | Value |
|---|---|
| Project | Komikku (komikku-app/komikku) |
| Version | v1.14.1 |
| Tag commit | e30ff83265d7affcc14e172ea9eadf469db1f4aa |
| Source | GitHub release source zip `komikku-v1.14.1.zip` (copy in `../.downloads/`) |
| Imported | 2026-10-05 |

Removed on import: `.github/`, `.gemini/`, `.idea/`, `.weblate/`, `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `renovate.json`. Upstream `AGENTS.md` and `README.md` moved to `docs/upstream/`.

To take upstream changes: download the newer release zip, diff it against this tag's zip, and apply the relevant parts on a `feat/upstream-<version>` branch.

## Vendor branch

`vendor/komikku` points at the pristine import commit `eb26a2e` (Komikku v1.14.1). It is the merge base for every future upstream release: import a newer release zip onto that branch (replace tree, keep `.git`, commit `vendor: Komikku vX.Y.Z`), then `git merge vendor/komikku` on the product branch. Conflicts should appear only at the hook points listed below, in required-logic files, and in deleted files (resolve as "keep deleted").

Restore source used by the isolation migration: `git show eb26a2e:<path>` is pristine for every file; the four files that pristine shares with the rebrand (`AboutScreen.kt`, `SettingsAdvancedScreen.kt`, `SettingsSecurityScreen.kt`, `onboarding/PermissionStep.kt`) are restored from `b4c0c05` instead (pristine + updater/telemetry prune).

## Local deviations

- 2026-10-06: Use the published `com.github.arkon:FlexibleAdapter:844a07002c` AAR instead of upstream's unavailable `com.github.arkon.FlexibleAdapter:flexible-adapter:c8013533`. The [unavailable commit](https://github.com/arkon/FlexibleAdapter/commit/c80135339bcff5f7f8c2c2380329dfc155b26232) directly follows this parent and only changes dependency wiring for the unused livedata module, not the core adapter code. [Published artifact metadata](https://jitpack.io/com/github/arkon/FlexibleAdapter/844a07002c/FlexibleAdapter-844a07002c.module) identifies the Android release AAR. This repairs release dependency resolution without introducing another library.

- 2026-10-06 behaviour fix (user-reported broken swipe page turns, `docs/research/18-ios27-reference-and-round2.md` §7 issue 15): the reader keeps the upstream layout — `readerContainer` and `navigationOverlay` stay native siblings inside `binding.root` with the `composeOverlay` ComposeView on top. The `ReaderActivity` re-parenting (`removeView`/`addView` into `readerContainer`) and the `AndroidView(factory = { readerContainer }, Modifier.glassSource(glass))` block are gone; our reader chrome in `dev.errnolink.tsuzuki.ui.reader` draws its glass with the tint fallback instead of live-sampling the page, so the View pager/webtoon keep native touch handling.

- 2026-10-07 reader canvas default (`ReaderPreferences.readerTheme`): change the unset preference from black (`1`) to automatic (`3`) so upstream page-edge background selection blends fit-mode letterboxing with the artwork. Explicit saved theme choices remain untouched. This is a required preference-default deviation, not a reader-engine or fit/zoom change.
- 2026-10-07 clean-build fix: `buildSrc/src/main/kotlin/mihon/buildlogic/tasks/LocalesConfigTask.kt` generates `locales_config.xml` in the task action, with declared resource inputs and output file. Upstream wrote it during configuration, so `clean :app:assembleDebug` deleted it before resource linking.

## Migration inventory (2026-10-06, `feat/isolate`, base `feat/ui-lead` @ 002a711)

Classification of every file in `git diff --name-status eb26a2e HEAD`:

- **(a)** Tsuzuki presentation: moved to `app/src/main/java/dev/errnolink/tsuzuki/ui/<area>/`, upstream file restored to pristine.
- **(b)** Hook point: upstream file kept, edited only to call our code (see "Hook points" below for the surviving list).
- **(c)** Required logic/rebrand: kept as our change in upstream code, with reason.
- **(d)** Deletion: upstream file removed; nothing restored references it.

### (a) Presentation moved to `dev.errnolink.tsuzuki.ui.*`

Settings (`ui.settings`):

| Upstream file | Notes |
|---|---|
| `presentation/more/settings/PreferenceItem.kt` | our preference row renderer |
| `presentation/more/settings/SettingsDialogControls.kt` (A) | |
| `presentation/more/settings/SettingsForms.kt` (A) | defines our `PreferenceScaffold`/`SettingsScaffold` |
| `presentation/more/settings/TsuzukiSettingsUi.kt` (A) | |
| `presentation/more/settings/screen/{ConfigureExhDialog, SettingsAdvancedScreen, SettingsAppearanceScreen, SettingsConnectionScreen, SettingsDataScreen, SettingsDiscordAccountsScreen, SettingsDiscordScreen, SettingsEhScreen, SettingsMainScreen, SettingsMangadexScreen, SettingsSearchScreen, SettingsSecurityScreen, SettingsTrackingScreen}.kt` | Voyager `Screen` objects; our object `Tsuzuki…` mirrors them, restored upstream object hooks `Content()` to ours |
| `presentation/more/settings/screen/about/AboutScreen.kt` | restored from `b4c0c05` (updater refs pruned by rebrand) |
| `presentation/more/settings/screen/advanced/ClearDatabaseScreen.kt` | |
| `presentation/more/settings/screen/appearance/AppLanguageScreen.kt` | |
| `presentation/more/settings/screen/browse/components/{ExtensionStoresContent, ExtensionStoresDialogs, ExtensionStoresScreen}.kt` | |
| `presentation/more/settings/screen/data/{CreateBackupScreen, RestoreBackupScreen, StorageInfo, SyncSettingsSelector, SyncTriggerOptionsScreen}.kt` | |
| `presentation/more/settings/widget/{ConnectionPreferenceWidget, EditTextPreferenceWidget, InfoWidget, ListPreferenceWidget, MultiSelectListPreferenceWidget, SwitchPreferenceWidget, TextPreferenceWidget, TrackingPreferenceWidget, TriStateListDialog}.kt` | our row widgets; pristine widgets restored upstream |
| `presentation/more/settings/widget/TsuzukiRows.kt` (A) | |

Restored to pristine (were deleted, pristine code still references them): `settings/PreferenceScreen.kt`, `settings/PreferenceScaffold.kt`, `settings/widget/{AppThemeModePreferenceWidget, AppThemePreferenceWidget, BasePreferenceWidget, PreferenceGroupHeader, ThemeColorPickerWidget}.kt`, `settings/screen/appearance/AppCustomThemeColorPickerScreen.kt`.

More / onboarding (`ui.more`, `ui.onboarding`):

| Upstream file | Notes |
|---|---|
| `presentation/more/LogoHeader.kt` | restored copy keeps the rebrand icon swap |
| `presentation/more/MoreScreen.kt` | caller `ui/more/MoreTab.kt` hooked |
| `presentation/more/onboarding/{GuidesStep, OnboardingScreen, PermissionStep, StorageStep, ThemeStep}.kt` | restored `PermissionStep` from `b4c0c05` |
| `presentation/more/stats/StatsScreenContent.kt` | caller `ui/stats/StatsScreen.kt` hooked |

Library (`ui.library`): `presentation/library/{DeleteLibraryMangaDialog, LibrarySettingsDialog, TsuzukiLibraryScreen}.kt`; caller `ui/library/LibraryTab.kt` hooked.

Updates / history (`ui.updates`, `ui.history`): `presentation/updates/{UpdatesDeleteConfirmationDialog, UpdatesFilterDialog, UpdatesScreen, UpdatesUiItem}.kt`; `presentation/history/{HistoryScreen, components/HistoryDialogs, components/HistoryFilterDialog, components/HistoryItem}.kt`; callers `ui/updates/UpdatesTab.kt`, `ui/history/HistoryTab.kt` hooked.

Browse (`ui.browse`): `presentation/browse/{BrowseSourceScreen, BrowseTabWrapper, ExtensionsScreen, FeedScreen, GlobalSearchScreen, SourceFeedScreen, SourcesScreen}.kt`; `presentation/browse/components/{GlobalSearchCardRow, GlobalSearchResultItems}.kt`; callers `ui/browse/BrowseTab.kt`, `ui/browse/source/SourcesTab.kt`, `ui/browse/source/browse/BrowseSourceScreen.kt`, `ui/browse/source/globalsearch/GlobalSearchScreen.kt`, `ui/browse/extension/ExtensionsScreen.kt` hooked. New ours-only screens moved: `ui/browse/SourceMigrationScreen.kt` (A).

Details (`ui.details`): `presentation/manga/{MangaScreen, ChapterSettingsDialog}.kt`; `presentation/manga/components/{ChapterDownloadIndicator, DetailControls (A), MangaBottomActionMenu, MangaChapterListItem, MangaCoverDialog, MangaInfoHeader}.kt`; callers `ui/manga/MangaScreen.kt` hooked; `ui/manga/MangaCoversScreen.kt` (A) moved to ours. `ui/manga/PaletteScreen.kt` restored to pristine (pristine `MangaScreen` still references it; the seed-colour path is dormant because `themeCoverBased` defaults false).

Reader chrome (`ui.reader`): `presentation/reader/{OrientationSelectDialog, ReadingModeSelectDialog}.kt`; `presentation/reader/appbars/{ExhUtils, ReaderAppBars, ReaderBottomBar, ReaderTopBar}.kt`; `presentation/reader/components/{ChapterNavigator, ReaderSlider (A)}.kt`; `presentation/reader/settings/{ColorFilterPage, GeneralSettingsPage, ReaderSettingsControls (A), ReaderSettingsDialog, ReadingModePage}.kt`; caller `ui/reader/ReaderActivity.kt` hooked. `reader/components/ModeSelectionDialog.kt` restored to pristine.

Categories (`ui.category`): `presentation/category/{CategoryScreen, components/CategoryDialogs, components/CategoryListItem}.kt` moved; caller `ui/category/CategoryScreen.kt` hooked via import swap; the routed screens repoint `ChangeCategoryDialog` to ours (Library/History/Manga/BrowseSource hooks). Pristine callers (BulkFavoriteDialogs, biometric/genre/source-category sub-screens, `exh/md/follows`) keep the upstream Material dialogs because those screens are themselves pristine.

Downloads (`ui.downloads`): the redesigned `ui/download/DownloadQueueScreen.kt` content moved to ours; upstream file restored pristine with a `Content()` → `TsuzukiDownloadQueueScreen.Content()` hook; the upstream RecyclerView adapter/holders and `res/layout/download_*.xml` were never deleted and stay pristine.

Shell / shared (`ui.shell`): our `presentation/components/{TsuzukiShell, ShellContentScaffold}.kt` (added) plus our redesigned `Banners.kt` and `TabbedScreen.kt` moved to `ui.shell`; upstream `TabbedScreen.kt` and `Banners.kt` restored pristine. Hooks `HomeScreen`, `MainActivity`, `BrowseTab`, `SourcesTab`/`ExtensionsTab`/`FeedTab` (our `TabContent` feeds our `TabbedScreen`), extension `ExtensionsScreen`, `BrowseSourceScreen`, `SourceFilterDialog` import ours; `MigrateSourceTab` returns our `TabContent` because its consumers route through `BrowseTabWrapper`. Pristine callers keep upstream `WarningBanner`/`AppStateBanners` (only our screens and `MainActivity` render ours).

### (b) Hook points (final list after the migration)

See "Hook points" section below (file, function, reason). One line summary: Voyager `Screen`/`Tab` `Content()` delegations, `MainActivity`/`ReaderActivity` `setContent`, `HomeScreen` shell host, `TachiyomiTheme` token bridge, `UiPreferences` defaults, DI modules, build/registration files.

### (c) Required logic / rebrand (kept in upstream files)

Rebrand: `App.kt`, `util/system/BuildConfig.kt`, `data/backup/BackupNotifier.kt`, `data/download/{DownloadJob, DownloadNotifier}.kt`, `data/download/Downloader.kt` (also the cold-start ANR fix: lazy XML), `data/library/LibraryUpdateNotifier.kt`, `data/notification/{NotificationReceiver, Notifications}.kt` (updater ids removed), `data/connections/discord/DiscordRPCService.kt`, `extension/api/ExtensionUpdateNotifier.kt`, `extension/util/ExtensionInstallService.kt`, `exh/eh/EHentaiUpdateNotifier.kt`, `ui/reader/SaveImageNotifier.kt`, `presentation/more/LogoHeader.kt` (restored copy keeps `ic_tsuzuki`), all `res/` icon/splash/colour files, `app/src/main/AndroidManifest.xml`, `app/src/debug/**`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/baseline-prof.txt`, `build.gradle.kts`, `buildSrc/…/BuildConfig.kt`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `.gitignore`, `macrobenchmark/**`, `i18n*/…/strings.xml` (Tsuzuki strings), `designsystem/**` (our module).

SyncYomi v2 (protocol v2 client + triggers): `domain/sync/SyncCategoryState.kt` (A), `data/sync/{SyncDataJob, SyncManager, SyncNotifier, models/SyncTriggerOptions, service/BackupRequestBody (A), service/SyncService, service/SyncYomiSyncService, service/GoogleDriveSyncService, service/WebDavSyncService}.kt`, `domain/sync/SyncCategoryState` interplay in `data/sqldelight/{categories, chapters, mangas, merged}.sq` + `migrations/47.sqm`, `data/category/CategoryMapper.kt`, `data/category/CategoryRepositoryImpl.kt`, `data/manga/MangaRepositoryImpl.kt`, `domain/category/{DeleteCategory, model/Category, model/CategoryUpdate}.kt`, backup model defaults `data/backup/models/**` (all-optional proto ctors for v2 payloads), `data/backup/restore/BackupRestorer.kt`, `restore/restorers/{CategoriesRestorer, MangaRestorer, MergedReferenceReconciliation (A), PreferenceRestorer}.kt`, `eu/kanade/domain/sync/SyncPreferences.kt`, `ui/reader/ReaderViewModel.kt` (debounced page/lifecycle sync triggers).

Neko / MangaDex parity: `exh/md/**` (new handlers, DTOs, preferences; ledger in `docs/NEKO-PORT.md`), `eu/kanade/domain/chapter/interactor/SetReadStatus.kt`, `SyncChaptersWithSource.kt`, `eu/kanade/domain/manga/interactor/UpdateManga.kt`, `domain/chapter/interactor/{UpdateChapter, GetMergedChaptersByMangaId}.kt`, `domain/chapter/service/ChapterReadSync.kt` (A), `source/online/all/MangaDex.kt`, `exh/recs/sources/{MangaDexRecommendedPagingSource (A), RecommendationPagingSource}.kt`, `source-api/…/MangaDexSearchMetadata.kt`, `domain/exh/source/ExhPreferences.kt` (updater pref removed), `eu/kanade/domain/DomainModule.kt`, `di/{AppModule (also ANR prewarm), PreferenceModule}.kt`, `mihon/core/migration/migrations/Migrations.kt` (migration 47 registered, `SetupAppUpdateMigration` dropped), `app/src/debug/java/dev/errnolink/tsuzuki/mangadex/MangaDexDebugActivity.kt`, tests under `app/src/test/**`.

Round 3 Neko backup compatibility: `data/backup/models/{BackupManga,BackupChapter}.kt` add donor protobuf tags 900–907 without renumbering SY/Komikku tags; `BackupChapter` becomes a data class for non-mutating migration copies. `restore/BackupRestorer.kt` delegates Neko merge expansion to `dev.errnolink.tsuzuki.mangadex.backup.NekoBackupMapper`, resolving only installed/stub source identities and surfacing missing-extension warnings. `restore/restorers/MangaRestorer.kt` maps exclusions/custom artwork, persists imported Neko memo fields, hides resolved source children, reconciles converted SY references, and moves previously unresolved progress to its canonical child on re-restore. No source IDs are invented; unresolved descriptors/progress remain in memo and the restore report names the required extension.

Round 3 authentication: `exh/md/network/MangaDexAuthInterceptor.kt` invokes the isolated session notifier only after persistent refresh rejection (400/401/403); transient HTTP/network failures retain tokens. Successful authentication dismisses the notice. `AndroidManifest.xml` registers MangaDex settings and author/group/list intents; `MainActivity.handleIntentAction` delegates them to `mangadex/MangaDexIntents`.

Round 3 bulk tracking: `eu/kanade/domain/track/interactor/RefreshTracks.kt` propagates cancellation and accepts `notifyProgress=false` for the isolated `mangadex/TrackingSyncJob`; interactive callers retain progress toasts. The job reuses the existing tracker progress preference.

Round 3 library filters: `LibraryPreferences` and `ui/library/LibraryScreenModel` add persisted tri-state filter inputs to the existing reactive predicate and active-filter indicator. Existing performance subscription/debounce/download batching is preserved.

Round 3 chapter filtering: `data/…/view/excludedScanlatorsView.sq` is the shared ALL/ANY exclusion rule; `chapters.sq`, `mangas.sq`, and library/history/update views consume it. `GetAvailableScanlators` and `GetExcludedScanlators` expose individual collaboration credits; `SyncChaptersWithSource` uses effective exclusions for auto-downloads. `LibraryManga`/`MangaMapper` carry bulk missing/unavailable counts, with no per-library-title queries. `48.sqm` preserves data and updates views; Main reserved 48 here and will renumber prune's independent EH-table drops to 49 at integration. Migration APKs are emulator-only until the final integrated phone build.

Theme bridge (required so leftover Material screens are neutral): `presentation/theme/TachiyomiTheme.kt`, `eu/kanade/domain/ui/UiPreferences.kt` (`appTheme` default `DEFAULT`, `themeCoverBased` default false). Reader engine files restored to pristine (`ui/reader/viewer/**`, `setting/ReadingMode.kt`, `exh/ui/metadata/MetadataViewScreen.kt`): the seed-colour path stays dormant behind the false default.

### (d) Deletions (updater / telemetry)

`data/updater/**` (5 files), `domain/release/**` (4 + test), `core/common/…/security/PrivacyPreferences.kt`, `telemetry/**` (7), `mihon/core/migration/migrations/SetupAppUpdateMigration.kt`, updater UI: `presentation/more/{ComingUpdatesScreen, NewUpdateScreen, WhatsNewScreen}.kt`, `ui/more/{ComingUpdatesScreen, NewUpdateScreen, WhatsNewScreen}.kt`, `presentation/more/settings/screen/about/WhatsNewDialog.kt`.

Docs/tests/other (ours, no upstream counterpart): `docs/LOG.md`, `docs/MANGADEX_PARITY.md`, `app/src/test/**`, `app/src/main/res/drawable/ic_tsuzuki.xml`, `tsuzuki.png`.

### (e) Deletions (Decision 4 feature prune, `feat/prune`)

Commits `379e42d` (Discord RPC / Connections) and `e6bac62` (page previews):
`data/connections/**`, `domain/connections/**`, `ui/setting/connections/**`,
`presentation/connection/**`, Connection settings screens + widget + preference
item (ours + upstream twins), `exh/pagepreview/**`, `GetPagePreviews`,
domain `PagePreview`, `PagePreviewCache`, coil `PagePreviewFetcher/Keyer`,
`presentation/manga/components/PagePreviews.kt`,
`ClearBrokenPagePreviewCacheMigration`. Manifest: DiscordRPCService service.
`source-api PagePreviewSource.kt` deliberately kept until the NHentai/Lanraragi
delegate deletion (they still implement it). Remaining planned deletions and
their caller maps: `docs/PRUNE.md`.

| Feature family | Deleted upstream paths | Retained compatibility |
|---|---|---|
| E-Hentai/ExHentai core | `exh/eh/**`, `exh/uconfig/**`, EH source/metadata, login/settings/configuration UI, EH description adapter and logo, EH-only utility helpers and startup migrations | `EXHMigrations` and source-id constants; MangaDex assets and delegate plumbing |
| Gallery pipeline | `exh/favorites/**`, `GalleryAdder`, batch-add UI, favorites domain/repository/schema, favorites dialogs and EH paging | MangaDex-only `InterceptActivity`; SQLDelight `48.sqm` drops legacy favorites tables |
| NSFW delegates | NHentai/Pururin/EightMuses/Lanraragi sources, metadata and description adapters; DelegateNHentaiMigration; source-api `PagePreviewSource` | MangaDex delegate/metadata/description/viewer; old source-id backup migration and external extension support |

## Hook points (final table)

Upstream files that differ from `vendor/komikku` for UI routing. Function = where the delegation happens; reason = what it buys.

| File | Function | Reason |
|---|---|---|
| `eu/kanade/tachiyomi/ui/main/MainActivity.kt` | `setContent` | hosts `TsuzukiShell` + our `AppStateBanners`/`LocalAppStateBottomPadding`; ConfigureExhDialog |
| `eu/kanade/tachiyomi/ui/main/MainActivity.kt` | navigator transition | Round 4: delegates to owned compositor-only 350ms transition, disabling size animation and exposing a glass/deferred-content motion phase |
| `eu/kanade/tachiyomi/ui/home/HomeScreen.kt` | tab composition provider | Round 4: owned tab-settle phase temporarily uses frosted tint instead of live blur during first composition and selection animation |
| `eu/kanade/tachiyomi/ui/main/MainActivity.kt` | `handleIntentAction` | early delegation to owned MangaDex settings/entity link router; upstream intent cases remain unchanged |
| `eu/kanade/tachiyomi/ui/home/HomeScreen.kt` | `Content` | shell host: glass capsule tab bar, `LocalShell*` providers, badge counts |
| `eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt` | `setComposeOverlay` + `AppBars` | our reader chrome (`ui.reader.ReaderAppBars`, settings dialog, mode/orientation dialogs, glass context, contrast-aware system bars); also carries the deliberate swipe fix (see Local deviations) and `flushReaderState` sync trigger |
| `eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt` | `ChapterListDialog` import | Round 3: our titled Chapters detent and grouped current-chapter list; native reader View hierarchy and engine unchanged |
| `eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt` | overlay state subscription / cover theme | Round 4: owned allocation-free hidden-chrome flow gate; page indicator alone observes page state; system-bar styling runs only when canvas theme changes. Disabled cover theming creates no collector; enabled cover theming observes distinct manga snapshots only |
| `ui/manga/MangaScreen.kt`, `MangaScreenModel.kt` | prepared state / chapter comparator | Round 4: one-call owned background preparation warms immutable chapter filtering/sorting/separators before publishing to Compose; resolve chapter comparator once per sort rather than once per comparison |
| `exh/md/handlers/{ApiMangaParser,MangaDexFeatureMapper,MangaDexStatistics}.kt` | missing chapter estimate | Round 4: delegate numbering credibility to owned bounded helper; represent unavailable live statistics as null so Details omits unreliable estimates; legacy integer metadata retains zero for unknown |
| `mihon/feature/upcoming/UpcomingScreen.kt` | `UpcomingScreenContent` call | Round 3: one-call delegation to our calendar and grouped release list; upstream ScreenModel and release prediction untouched |
| `eu/kanade/tachiyomi/ui/home/HomeScreen.kt` | `TabIcon` icon call | Round 3: one-call outline tab symbol renderer; existing update-count collection and badges remain upstream |
| `ui/library/LibraryTab.kt` | `Content` | `TsuzukiLibraryScreen` + library dialogs + our `ChangeCategoryDialog` |
| `ui/updates/UpdatesTab.kt` | `Content` | our `UpdateScreen` + filters/confirm dialogs |
| `ui/history/HistoryTab.kt` | `Content` | our `HistoryScreen` + history dialogs + our `ChangeCategoryDialog` |
| `ui/browse/BrowseTab.kt` | `Content` | our `TabbedScreen` (glass capsule browse tabs) |
| `ui/more/MoreTab.kt` | `Content` | our `MoreScreen` |
| `ui/browse/source/SourcesTab.kt` | `sourcesTab` | our `SourcesScreen` composable; our `TabContent` |
| `ui/browse/extension/ExtensionsTab.kt` | `extensionsTab` | our `ExtensionScreen`; our `TabContent` |
| `ui/browse/feed/FeedTab.kt` | `feedTab` | our `FeedScreen` + feed dialogs; our `TabContent` |
| `ui/browse/source/SourcesScreen.kt` | `Content` | our `BrowseTabWrapper` |
| `ui/browse/source/browse/BrowseSourceScreen.kt` | `Content` | our `BrowseSourceContent` + our `ChangeCategoryDialog` |
| `ui/browse/source/browse/SourceFilterDialog.kt` | dialog body | our `ShellFilterRow` + settings sheets |
| `ui/browse/source/feed/SourceFeedScreen.kt` | `Content` | our `SourceFeedScreen` |
| `ui/browse/source/globalsearch/GlobalSearchScreen.kt` | `Content` | our `GlobalSearchScreen` |
| `ui/browse/extension/ExtensionsScreen.kt` | `Content` | our `ExtensionScreen` |
| `ui/browse/migration/search/MigrateSourceSearchScreen.kt` | `Content` | our `BrowseSourceContent` for migration search |
| `ui/manga/MangaScreen.kt` | `Content` | our details UI (`ui.details.MangaScreen`), chapter settings dialog, cover content |
| `ui/manga/track/TrackInfoDialog.kt` | selector/remover/search screen `Content` | Round 5: the seven tracker dialog screens delegate to owned glass sheets in `dev.errnolink.tsuzuki.ui.manga.track` (Models moved with them) |
| `ui/manga/PaletteScreen.kt` | `Content` | Round 5: dormant palette screen delegates to the owned glass token swatch screen; the material seed-colour preview is retired |
| `ui/category/CategoryScreen.kt` | `Content` | our category screen + dialogs (import swap) |
| `ui/category/genre/SortTagScreen.kt` | `Content` | our tag sort list + dialogs (import swap) |
| `ui/category/sources/SourceCategoryScreen.kt` | `Content` | our source category list + dialogs (import swap) |
| `ui/download/DownloadQueueScreen.kt` | `Content` | delegates to `ui.downloads.DownloadQueueScreen.Content()` |
| `ui/more/OnboardingScreen.kt` | `Content` | our onboarding steps |
| `ui/stats/StatsScreen.kt` | `Content` | our stats sections + `SettingsScaffold` |
| `presentation/more/settings/screen/SearchableSettings.kt` | `PreferenceScaffold` usage | our settings scaffold keeps pristine widget signatures compiling |
| `presentation/more/settings/screen/{SettingsMainScreen, SettingsAppearanceScreen, SettingsConnectionScreen, SettingsDataScreen, SettingsDiscordScreen, SettingsDiscordAccountsScreen, SettingsEhScreen, SettingsMangadexScreen, SettingsSearchScreen, SettingsSecurityScreen, SettingsTrackingScreen, SettingsAdvancedScreen}.kt` | `Content` | `Content()` delegates to the mirrored `Tsuzuki…` object in `ui.settings` |
| `presentation/more/settings/screen/about/AboutScreen.kt` | `Content` | `TsuzukiAboutScreen` (restored from `b4c0c05` rebrand base) |
| `presentation/more/settings/screen/about/OpenSourceLicensesScreen.kt` | `Content` | `TsuzukiOpenSourceLicensesScreen` (grouped licence list) |
| `presentation/more/settings/screen/about/OpenSourceLibraryLicenseScreen.kt` | `Content` | `TsuzukiOpenSourceLibraryLicenseScreen` (monospace licence text) |
| `presentation/more/settings/screen/debug/BackupSchemaScreen.kt` | `Content` | `TsuzukiBackupSchemaScreen` (monospace proto schema in `ui.settings.debug`) |
| `presentation/more/settings/screen/debug/WorkerInfoScreen.kt` | `Content` | `TsuzukiWorkerInfoScreen` (grouped worker sections in `ui.settings.debug`) |
| `presentation/more/settings/screen/advanced/ClearDatabaseScreen.kt` | `Content` | `TsuzukiClearDatabaseScreen` |
| `presentation/more/settings/screen/appearance/AppLanguageScreen.kt` | `Content` | `TsuzukiAppLanguageScreen` |
| `presentation/more/settings/screen/browse/ExtensionStoresScreen.kt` | `Content` | our `ExtensionStoresScreen` composable + dialogs |
| `presentation/more/settings/screen/data/{CreateBackupScreen, RestoreBackupScreen, SyncSettingsSelector, SyncTriggerOptionsScreen}.kt` | `Content` | `Tsuzuki…` screens |
| `presentation/theme/TachiyomiTheme.kt` | theme bridge | maps Material tokens onto Tsuzuki tokens so leftover pristine screens render neutral |
| `eu/kanade/domain/ui/UiPreferences.kt` | theme prefs | prune §7: deleted dead appTheme, colorTheme, customThemeStyle, themeCoverBased, preloadLibraryColor |
| `data/…/DatabaseHandler.kt`, `AndroidDatabaseHandler.kt`, `manga/MangaRepositoryImpl.kt` | library subscription | required logic: conflate/debounce invalidations before executing SQL, immediate first result, suppress equal library results |
| `domain/…/MangaMergeRepository.kt`, `data/…/MangaMergeRepositoryImpl.kt` | `getMergedMangaForLibrary` | required logic: bulk merged-child snapshot replaces per-library-item SQL |
| `data/download/DownloadCache.kt`, `ui/library/{LibraryScreenModel,LibraryItem}.kt` | library mapping/filtering | required logic: lifecycle/restore/sync collection gate, background mapping, batched download counts shared by badges and filters |
| `data/BannerProgressStatus.kt`, `data/backup/BackupNotifier.kt` | active status / progress publishing | required logic: immediate bulk-operation state for the library gate, restore notifications limited to one per 250 ms |
| `data/coil/{MangaCoverFetcher,MangaCoverMetadata}.kt`, `ui/main/MainActivity.kt`, `domain/…/MangaCover.kt` | cover metadata / pause | required logic: bounds-only ratio extraction on the fetch dispatcher, compact incremental SQLite cache, no palette preloading or full preference rewrites on pause; obsolete metadata comments removed |
| `App.kt` | `onCreate`, `onStart` | required logic: minimal immediate log sink, background full XLog/storage setup and cover-cache warmup; widgets and start/resume sync move off-main after migration readiness; WorkManager stays initialized before migration consumers |
| `app/src/main/baseline-prof.txt` | Tsuzuki hot-path rules | build/profile: inherited Komikku profile now includes migrated library/shell/details/reader and design-system class/method rules; phone-generated refinement remains deferred |
| `ui/setting/track/{BaseOAuthLoginActivity,TrackLoginActivity}.kt` | callback / task return | required logic: reject foreign/expired/replayed state, bound sign-in to 20 seconds, report failures, finish and reuse MainActivity with NEW_TASK/CLEAR_TOP/SINGLE_TOP |
| `data/track/{anilist/AnilistApi,bangumi/BangumiApi,shikimori/ShikimoriApi,myanimelist/MyAnimeListApi}.kt` | authorization URL / token exchange | required logic: persist per-flow OAuth state; MAL verifier generated for each flow, persisted with it and explicitly supplied to exchange (plain PKCE, as required by MAL) |
| `data/track/{anilist/Anilist,bangumi/Bangumi,shikimori/Shikimori,myanimelist/MyAnimeList}.kt` | login | required logic: propagate login errors/cancellation after clearing failed credentials rather than swallowing them |
| `util/CrashLogUtil.kt` | construction / `getExtensionsInfo` | required logic: resolve ExtensionManager only when an actual extension diagnostic dump is requested, never for ordinary boot/device information |
| `buildSrc/…/ProjectExtensions.kt` | `configureCompose` | build: share an exact-type stability allowlist for Manga, Chapter, LibraryManga, Category and LibraryItem snapshots; no package-wide mutable-model exemption |
| `ui/reader/viewer/ReaderPageImageView.kt` | `setImage`, `recycle`, Coil enqueue | required logic: retain both static/webtoon and animated lambda-target Coil Disposables; cancel previous work on rebind and recycle before releasing/hiding page views |
| `app/proguard-rules.pro` | kotlinx serialization rules | build: preserve Tsuzuki serializable DTOs, generated serializers and companion serializer entry points, mirroring existing upstream package protections without keeping every UI class/member |
| `buildSrc/src/main/kotlin/mihon/buildlogic/tasks/LocalesConfigTask.kt` | `getLocalesConfigTask` | required build logic: execute locale resource generation after clean, not during configuration |
| `data/backup/models/Backup.kt` | field 700 | required logic: nullable `wantedExtensions` section (`dev.errnolink.tsuzuki.sync.BackupWantedExtensions`), docs/research/23-extension-sync-contract.md |
| `data/backup/create/BackupCreator.kt` | `backup` | appends the wanted-extensions section to file backups (app-settings option + sync-extensions pref) |
| `data/sync/SyncManager.kt` | `syncData` | appends the wanted-extensions section to the SyncYomi v2 payload |
| `data/backup/restore/BackupRestorer.kt` | `restoreFromFile` | reconciles wanted vs installed extensions after restore/sync apply (app-settings option) |
| `data/notification/Notifications.kt` | `CHANNEL_EXT_SYNC` | quiet ext-sync progress channel + id |
| `extension/ExtensionManager.kt` | `InstallationListener` | records installs/updates and deliberate uninstalls into the ext-sync store |
| `eu/kanade/domain/sync/SyncPreferences.kt` | `syncExtensions`/`syncUninstalls` | ext-sync preferences (both default on) |
| `eu/kanade/domain/base/ExtensionInstallerPreference.kt` | `defaultValue` | default installer is `PRIVATE` for new installs (Decision 5.5) |
| `App.kt` | `onCreate` | launches the wanted-extension reconcile and updater jobs on app start |

Not routed (still upstream Material, listed by `tools/check-routes.py`): debug/about sub-screens, `SortTagScreen`/`SourceCategoryScreen`/`BiometricTimesScreen`, `WebViewScreen`, exh debug/search screens.

## Importing a Komikku release

1. Download the new release source zip; on `vendor/komikku` replace the whole tree (keep `.git`), commit `vendor: Komikku vX.Y.Z`.
2. Full import: on the product branch run `git merge vendor/komikku`. Conflicts are expected only at the hook points above, in the required-logic files of §(c), and in deleted files (resolve deleted as "keep deleted"). At hook conflicts keep our delegation lines and re-check the new upstream body for anything our composable must consume.
3. Partial import (single feature/fix): `git diff <old-vendor-tag> vendor/komikku -- <paths> > feature.patch` on the vendor branch, then `git apply -3 feature.patch` (or `git am -3` for commits cherry-picked off the vendor branch) on the product branch. `-3` uses our pristine-restored files as the merge base, so patches that only touch upstream code apply cleanly; patches touching hooks/required logic stop exactly at the lines listed here.
4. After either path: `python tools/check-routes.py` (new upstream screens appear as "not yet redesigned" — style them in `ui.*` and add `Routes.kt` rows, or leave them on the neutral bridge), then `:app:compileDebugKotlin` and `:app:testDebugUnitTest`.
5. Record the new version in this file's table and any new hooks/required-logic in the same commit; Neko deltas go through `docs/NEKO-PORT.md`.

## Round 3 glass capture hook correction

`HomeScreen.Content` no longer adds `glassSource` to the ancestor wrapping all tab chrome. Each owned list/masonry/content scaffold registers only its scrolling content, reusing `LocalShellGlass`. This leaves one active capture per screen and lets both top groups and the tab capsule sample it. The first round-3 emulator scroll capture showed why ancestor capture is invalid: top effects inside the captured subtree were transparent over unblurred covers. No source/data/navigation logic changes.
