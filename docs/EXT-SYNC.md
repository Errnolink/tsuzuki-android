# EXT-SYNC: Android half of the wanted-extension sync contract

Implements `docs/research/23-extension-sync-contract.md` (MIGRATION-PLAN Decision 5) on
`feat/ext-sync`. The server half is live-verified on `server` `feat/ext-sync` @ `f72228d`.
Paths below are relative to the repo root; `A/` = `app/src/main/java/`, `T/` = `app/src/test/kotlin/`.

## Wire format (contract §1)

| Clause | Where |
|---|---|
| `Backup` field 700, nullable, omitted when absent | `A/eu/kanade/tachiyomi/data/backup/models/Backup.kt:21` |
| `BackupWantedExtensions` (wanted=1, tombstones=2, syncUninstalls=3 default true) | `A/dev/errnolink/tsuzuki/sync/BackupWantedExtensions.kt:7-10` |
| `BackupWantedExtension` (pkgName=1, repoIndexUrl=2, signingFingerprint=3, versionCode=4, updatedAt=5, versionName=6) | `A/dev/errnolink/tsuzuki/sync/BackupWantedExtensions.kt:14-21` |
| `BackupExtensionTombstone` (pkgName=1, deletedAt=2) | `A/dev/errnolink/tsuzuki/sync/BackupWantedExtensions.kt:24-27` |
| All scalars carry defaults (proto3 peers omit zero values) | same file (every field `= …` default) |
| Built-ins never exported/imported (local + merged sources, both platforms' pkg names) | `A/dev/errnolink/tsuzuki/sync/ExtensionSyncRules.kt:43-50` |

## Export (contract §2.4)

| Clause | Where |
|---|---|
| Sync payload carries the derived section when "Sync extensions" is on | `A/eu/kanade/tachiyomi/data/sync/SyncManager.kt:126-129` |
| File backup carries it under the app-settings option + pref | `A/eu/kanade/tachiyomi/data/backup/create/BackupCreator.kt:116-120` |
| Section derived from installed set + tombstones + fleet entries (`wantedAt = now` only when newly recorded) | `A/dev/errnolink/tsuzuki/sync/ExtensionSync.kt:86-128` (`buildSection`) |
| Repo URL + fingerprint from the trusted store (matched by `extension.store` or signing key) | `A/dev/errnolink/tsuzuki/sync/ExtensionSync.kt:130-133` (`storeFor`) |
| Empty state exports `null` (field omitted on wire) | `ExtensionSync.kt:122` |
| Manual install = resurrect (fresh `wantedAt`, tombstone dropped) | `A/dev/errnolink/tsuzuki/sync/ExtensionSync.kt:135-147` (`recordInstall`), fired from `A/eu/kanade/tachiyomi/extension/ExtensionManager.kt:423,431` |
| Manual uninstall = tombstone (only for tracked extension packages) | `ExtensionSync.kt:149-161` (`recordUninstall`), fired from `ExtensionManager.kt:444-451` |

## Merge rules (contract §2)

| Clause | Where |
|---|---|
| Identity = pkgName alone; newest event wins per pkg; install wins exact tie | `A/dev/errnolink/tsuzuki/sync/ExtensionSyncRules.kt:52-64` (`resolve`) |
| Duplicates inside one section resolved locally by newest | `ExtensionSyncRules.kt:53-55` |
| Merge local+remote before reconcile (absorb-before-upload) | `ExtensionSyncRules.kt:66-75` (`mergeSections`), used by `ExtensionSync.kt:166-167` |
| Fresh device adopts the merged section wholesale | `mergeSections(null, remote)` path, covered by `T/dev/errnolink/tsuzuki/sync/ExtensionSyncRulesTest.kt` (`freshDeviceAdoptsRemoteSection`) |
| File restore and sync apply are the same rules (both run through BackupRestorer) | `A/eu/kanade/tachiyomi/data/backup/restore/BackupRestorer.kt:143-147` |
| Local store update from resolution (fleet memory, tombstone memory) | `ExtensionSync.kt:272-305` (`updateStoreFromResolution`), persisted in the `extension_sync` SharedPreferences JSON (`ExtensionSync.kt:57-81`) |

## Trust gate + apply (contract §3, §2.5)

| Clause | Where |
|---|---|
| Trusted repo = configured extension store; receiver never adds a repo | `ExtensionSync.kt:83-84` (`trustedRepos` → `GetExtensionStores`/`extension_store` table) |
| Fingerprint match, case-insensitive | `ExtensionSyncRules.kt:101` |
| NOT_IN_REPO / no download URL / different store URL | `ExtensionSyncRules.kt:98`, candidate re-check `ExtensionSync.kt:215-223` |
| Skips recorded, never a failed sync/restore (runCatching around reconcile) | `BackupRestorer.kt:144-147` |
| `versionCode` advisory only (receivers install current) | not read on apply anywhere in `reconcile` |
| Uninstall only when installed, not built-in, `syncUninstalls` honored | `ExtensionSyncRules.kt:104-109`, uninstall exec `ExtensionSync.kt:228-246` |
| Installs forced through the Private installer, no system prompt | `ExtensionSync.kt:307-330` (`installPrivately` → OkHttp download + `ExtensionLoader.installPrivateExtensionFile`) |
| Catalog fetched only when a wanted pkg is actually missing (repo index `index.pb`/`repo.json` chain via `ExtensionApi`/`ExtensionStoreService`) | `ExtensionSync.kt:175-194` |
| One quiet progress notification ("Installing N extensions…") | `ExtensionSync.kt:332-342`, channel `ext_sync_channel` IMPORTANCE_LOW `A/eu/kanade/tachiyomi/data/notification/Notifications.kt:92-93,201-206`, plural `i18n-kmk/src/commonMain/moko-resources/base/plurals.xml` |

## Reconcile triggers (contract §7, Decision 5)

| Trigger | Where |
|---|---|
| Sync apply (SyncYomi v2 merge response) | `SyncManager.applyRemoteBackup` → `BackupRestorer.restore` → `BackupRestorer.kt:145` |
| Backup restore (file) | same path, `BackupRestorer.kt:143-147` (app-settings option) |
| App start | `A/eu/kanade/tachiyomi/App.kt:215-217` → `ExtensionSync.launchOnAppStart` (`ExtensionSync.kt:66-72`) |

## Preferences (Decision 5.5)

| Clause | Where |
|---|---|
| "Sync extensions" (default on) | `A/eu/kanade/domain/sync/SyncPreferences.kt:158` |
| "Sync uninstalls" (default on) | `SyncPreferences.kt:159` |
| Settings rows (route-safe, our screen) | `A/dev/errnolink/tsuzuki/ui/settings/SettingsDataScreen.kt:570-579` (sync-service group of `TsuzukiSettingsDataScreen`) |
| Installer preference defaults to Private for new installs | `A/eu/kanade/domain/base/ExtensionInstallerPreference.kt:29` |

## SyncYomi v2 passthrough (contract §5)

- Field 700 rides the hub's unknown-fields blob untouched (source-verified in the contract doc, §5).
- The phone's upload always carries the section: `selectSyncDelta`/`selectSyncSections`
  (`A/eu/kanade/tachiyomi/data/sync/SyncDelta.kt`) copy the `Backup` and only blank known
  sections, so `wantedExtensions` survives the delta path unclipped.

## Tests

| Suite | What |
|---|---|
| `T/dev/errnolink/tsuzuki/sync/ExtensionSyncRulesTest.kt` | mirrors the server's rules tests: newest-wins per pkg, tie→install, resurrect, duplicate resolution, local-vs-remote merge, fresh device, plan skip reasons (UNTRUSTED_REPO / NOT_IN_REPO / FINGERPRINT_MISMATCH), installed/built-in no-op, tombstone uninstall vs UNINSTALLS_DISABLED, tombstone-for-not-installed no-op, Android built-in pkg names |
| `T/dev/errnolink/tsuzuki/sync/WantedExtensionsProtoTest.kt` | field-700 round trip through `Backup`; proto3 omitted zero values decode to defaults (true/false/0/""); absent section → null and re-encode omits field 700; decodes the server-exported `.verification/ext-sync/exported.tachibk` (WeebCentral entry + "Default" category, serverSettings 9001 skipped), else synthesizes the same bytes from `exported-field700.json` values with an independent hand-rolled proto writer; decode→encode→decode stability |

## Device E2E (later, not this slice)

Fresh-install sync installs WeebCentral silently, tombstones propagate uninstalls; per
Decision 5 acceptance, on the phone + emulator queues owned by other agents.
