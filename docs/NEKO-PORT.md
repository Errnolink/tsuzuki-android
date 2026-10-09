# Neko port ledger

Donor: Neko 3.8.2 (`reference/neko/`, commit `c43e64d`). Ledger for every added/modified file under `app/src/main/java/exh/md/**` and `app/src/main/java/dev/errnolink/tsuzuki/mangadex/**` versus the pristine Komikku import (`eb26a2e`). Statuses: **ported** (Neko behaviour re-implemented on Komikku's `exh/md` stack), **extended** (pre-existing Komikku `exh/md` file grown for the port), **original** (no Neko counterpart; Tsuzuki-only).

Neko paths are relative to `reference/neko/app/src/main/java/`.

## exh/md

| Neko file | Our file | Status | Deviations |
|---|---|---|---|
| `…/source/online/handlers/StatusHandler.kt` | `exh/md/handlers/StatusHandler.kt` | ported | Neko pulls/pushes read status from its source calls; ours observes `ChapterUpdate` events (`onReadStatusChanged`) and pushes batched `read`/`unread` marks via `MangaDexFeatureRequests.markRead`, plus `pull(title)` syncing `chapterIdsRead` into Komikku chapter state. Komikku `Result` types replaced Neko's `Result<T, ResultError>`. |
| `…/source/online/handlers/ArtworkHandler.kt` | `exh/md/handlers/MangaDexFeatures.kt` (`covers`) + `exh/md/handlers/MangaDexArtworkStore.kt` + `exh/md/handlers/MangaDexDynamicCovers.kt` | ported (split) | Cover-art gallery split into fetch (`covers`), on-device cache (`MangaDexArtworkStore` persists chapters/covers between openings, Neko refetches per screen) and per-volume/`language` cover selection + `update(manga, chapter, source)` applied after chapter fetch. |
| `…/source/online/handlers/ListHandler.kt` | `exh/md/handlers/MangaDexFeatures.kt` (`lists`, `list`) + `service/MangaDexFeatureRequests.kt` (`lists`, `list`) | ported (split) | Custom MDLists are read for Browse discovery/feed shelves; list membership editing from Neko's list screen is not ported. |
| `…/source/online/handlers/FeedUpdatesHandler.kt` | `exh/md/handlers/MangaDexFeatures.kt` (`feed`) + `exh/md/service/MangaDexFeatureRequests.kt` (`feed`) + `exh/md/handlers/MangaDexDiscover.kt` | ported (split) | Feed pagination driven by `MangaDexFeedPage` (offset-based) instead of Neko's `FeedUpdatesHandler` paging internals; discovery rows add signed-in-aware shelves built from lists/popular/latest. |
| `…/source/online/handlers/LatestChapterHandler.kt` | `exh/md/handlers/MangaDexDiscover.kt` (`rows` latest shelf) | ported (partial) | Only the latest-chapters shelf of the Neko feed is surfaced; no background latest-chapter polling job. |
| `…/usecases/tracking/AutoAddTrackers.kt` | `exh/md/handlers/AutoAddTrackers.kt` | ported | Triggered from `onAddedToLibrary(mangaId)`; resolves tracker ids from MangaDex links (AniList/Kitsu/MAL) and binds via Komikku track interactor APIs; Kitsu lookup by id goes through `network.client` like Neko. Registered through Komikku's DI/module wiring instead of Neko use-case classes. |
| `…/source/online/handlers/SearchHandler.kt` | `exh/md/handlers/MangaDexSearch.kt` | adapted | Neko's search internals reworked into Komikku's `MangasPage` paging; adds UUID/prefix `resolve` for search-by-id behaviour. |
| `…/source/online/handlers/ApiMangaParser.kt` | `exh/md/handlers/ApiMangaParser.kt` | extended | Pre-existing Komikku parser grew `chapterListParse(chapterListResponse, groupMap, showLanguage)` so cached/fetched chapter DTOs can be re-parsed without a network round-trip (used by StatusHandler pull and MangaUp merge). |
| `…/source/online/handlers/PageHandler.kt` | `exh/md/handlers/PageHandler.kt` | extended | External-host and at-home image resolution paths adjusted for the fallback chain in `MangaDexImageFallback`. |
| `…/source/online/handlers/MangaHandler.kt` | `exh/md/handlers/MangaHandler.kt` | extended | `getChapterList` gains aggregate parsing tolerant of both keyed-object and array aggregate responses; feeds `MangaDexArtworkStore.saveChapters`. |
| `…/source/online/MangaDex.kt` (preferences/constants) | `exh/md/MangaDexPreferences.kt` | adapted | Neko's source-held preferences re-homed into Komikku's `PreferenceStore` (`readingSync`, `autoAddToLibrary`, blocked groups/uploaders, per-manga languages, unavailable-chapter inclusion …); Komikku `MangaDex.kt` reads them instead of owning new pref keys. |
| — | `exh/md/handlers/MangaDexBlocks.kt` | original | Block/undo of groups and uploaders (grouped with Neko's blocked-groups concept, persisted in `MangaDexPreferences`, merged with legacy source pref keys). |
| — | `exh/md/handlers/MangaDexImageFallback.kt` | original | URL fallback order for pages (data-saver/external hosts). |
| — | `exh/md/handlers/MangaDexStatistics.kt` | original | Ratings/statistics model surfaced on the details screen. |
| — | `mangadex/MissingChapterEstimate.kt`, `exh/md/handlers/MangaDexFeatureMapper.kt` | original | Round 4: reject aggregate/declared maxima above ten times the distinct observed positive chapter count; nullable statistics hide unreliable estimates. Stored legacy metadata records zero rather than an outlier-derived missing count. |
| — | `exh/md/handlers/MangaUpHandler.kt` + `exh/md/dto/MangaUpDto.kt` | original | MangaUp external chapter host (protobuf page blocks, image interceptor). |
| — | `exh/md/service/MangaDexFeatureRequests.kt` | original | OkHttp request builders for covers/lists/read-markers/feed/search aggregates backing the ported handlers. |
| — | `exh/md/dto/AggregateSerializers.kt` | original | `JsonTransformingSerializer`s normalizing MD aggregate arrays vs keyed objects (regression-tested against captured fixtures). |
| — | `exh/md/dto/MangaDexFeatureDto.kt` | original | Mark-status, custom-list, feed/aggregate DTOs for the ported endpoints. |
| — | `exh/md/dto/ChapterDto.kt` | extended | Adds fields used by read-markers and cached chapter re-parse. |
| — | `exh/md/dto/MangaDto.kt` | extended | Adds fields used by covers/lists/aggregates. |
| — | `exh/md/dto/StatisticsDto.kt` | extended | Adds `StatisticsCommentsDto` (thread id/replies) for the details section. |
| — | `exh/md/handlers/ComikeyHandler.kt`, `NamicomiHandler.kt`, `FilterHandler.kt` | extended | Small wiring changes (filter state ownership, per-language filter sets, header fixes) needed by the new handlers; no Neko behaviour. |
| `eu/kanade/tachiyomi/network/MangaDexTokenAuthenticator.kt`, `org/nekomanga/App.kt` | `exh/md/network/MangaDexAuthInterceptor.kt`, `mangadex/MangaDexSessionNotification.kt` | adapted | Persistent token rejection posts a session-expired notification; temporary network/server failures retain authentication. Successful login dismisses the notification. |
| — | `exh/md/service/MangaDexService.kt` | extended | Endpoint reshuffles backing `MangaDexFeatureRequests` (legacy per-call builders consolidated). |

## dev.errnolink.tsuzuki.mangadex (UI)

Tsuzuki-original presentation on top of the ported handlers; Neko screens (M3, `org.nekomanga.presentation.*`) were used as behavioural reference only, no code copied.

| Neko reference | Our file | Status | Notes |
|---|---|---|---|
| `…/presentation/screens/feed/FeedScreen.kt` | `MangaDexHomeScreen.kt`, `MangaDexScaffold.kt`, `MangaDexScreens.kt` | original | Discovery home with shelves from `MangaDexDiscover.rows`, destinations Home/Feed/Lists. Round 3 main destinations reuse owned ShellList/ShellMasonry, inherit the existing theme, and keep navigation/background during source loading. API and ScreenModel behavior unchanged. |
| `…/presentation/screens/manga/*` | `MangaDexDetailsSection.kt`, `MangaDexUi.kt` | original | Statistics, discussion and creator/group sections; glass design system, not Neko M3. |
| Neko cover-art screen | `MangaDexScreens.kt` (`MangaDexCoverGalleryScreen`) | original | Per-volume/language cover gallery over `MangaDexArtworkStore`/`MangaDexDynamicCovers`. Round 3 keeps Back/large title in loading and empty states through ShellMasonry; cover actions and persistence unchanged. |
| — | `MangaDexChapterScreen.kt`, `MangaDexChapterPresentation.kt` | original | Chapter list with read-marker merge (`StatusHandler.pull`), language labels, unavailable-chapter styling. |
| `org/nekomanga/presentation/components/ChapterRow.kt` | `MangaDexChapterMenu.kt` | original | Active chapter-row trailing slot opens an anchored glass menu for chapter details, comments/forum and global blocking; chapter screen uses a persistent Undo banner. |
| — | `MangaDexContext.kt`, `MangaDexScreenModel.kt`, `MangaDexLookupScreen.kt` | original | ScreenModel/state holder and creator/group lookup. |
| `eu/kanade/tachiyomi/ui/main/DeepLinks.kt` and main intent dispatch | `MangaDexIntents.kt` | adapted | Settings action, author/group/list HTTPS links and external tracker links (anilist.co, myanimelist.net, mangaupdates.com, mangabaka.org) are dispatched by a thin MainActivity hook; source browse/list screens and the shared import intercept handle the destinations. |
| `eu/kanade/tachiyomi/util/manga/MangaMappings.kt` | `MangaMappings.kt`, `assets/2026-07-23_neko_mapping.db` | ported (trimmed) | Tracker IDs resolve to MangaDex UUIDs through the bundled donor mapping database (`al`, `mal`, `mu_new` after base36 URL decoding, `mb`); write-back helpers were not ported. |
| `eu/kanade/tachiyomi/jobs/tracking/TrackingSyncJob.kt` | `TrackingSyncJob.kt` | adapted | Reuses Komikku RefreshTracks and logged-in services; unique network-constrained worker, cancellable progress notification/settings row and isolated per-title failures. An optional daily periodic schedule sits behind the tracking-settings switch (donor is manual-only). |
| `org/nekomanga/domain/library/ScanlatorFilterOption.kt`, `…/util/chapter/ChapterUtil.kt` | `mangadex/ScanlatorFilterOption.kt`, `data/…/view/excludedScanlatorsView.sq` | adapted | One persisted SQL rule handles collaboration exclusion in reader/download/library queries. Existing uploader fallback names work as a single credit. Generic SY merge identities replace donor source-name exclusions. |
| `…/presentation/screens/library/filter/{FilterMerged,FilterMissingChapters,FilterUnavailable}.kt` | `ui/library/LibrarySettingsDialog.kt`, `LibraryScreenModel`, `libraryView.sq` | adapted | Existing tri-state preferences/predicates; gaps and availability are bulk aggregates over stored chapters, not per-title network requests. |

## Backup compatibility

| Neko reference | Our file | Status | Notes |
|---|---|---|---|
| `eu/kanade/tachiyomi/data/backup/models/BackupManga.kt`, `BackupChapter.kt`, `BackupMergeManga.kt` | Upstream backup models plus `mangadex/backup/BackupMergeManga.kt` | adapted | Tags 900–907 coexist with all SY/Komikku fields; uploader, unavailable and smart order survive in chapter memo. |
| `eu/kanade/tachiyomi/data/backup/RestoreHelper.kt` | `mangadex/backup/NekoBackupMapper.kt` and documented backup restore hooks | adapted | Neko descriptors become SY parent/children/references using actual installed/stub identities. Missing or ambiguous extensions produce explicit per-title warnings; descriptor and chapter data remain recoverable in memo. |

## Verification at time of writing

`exh.md.*` unit tests (40) green including aggregate-array fixture regression; MangaDex features exercised on emulator via the Browse MangaDex home. See `docs/MANGADEX_PARITY.md` for the integration contract and settings parity.
