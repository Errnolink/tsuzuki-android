package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.net.Uri
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.restore.BackupRestorer
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import dev.errnolink.tsuzuki.sync.ExtensionSync
import eu.kanade.tachiyomi.data.backup.restore.restorers.CategoriesRestorer
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService
import eu.kanade.tachiyomi.data.sync.service.SyncService as SyncServiceProvider
import eu.kanade.tachiyomi.data.sync.service.SyncData
import eu.kanade.tachiyomi.data.sync.service.SyncResult
import eu.kanade.tachiyomi.data.sync.service.SyncYomiSyncService
import eu.kanade.tachiyomi.data.sync.service.WebDavSyncService
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.manga.MangaMapper.mapManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.IOException
import java.util.Date

/**
 * A manager to handle synchronization tasks in the app, such as updating
 * sync preferences and performing synchronization with a remote server.
 *
 * @property context The application context.
 */
class SyncManager(
    private val context: Context,
    private val handler: DatabaseHandler = Injekt.get(),
    private val syncPreferences: SyncPreferences = Injekt.get(),
    private var json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
) {
    private val backupCreator: BackupCreator = BackupCreator(context, false)
    private val notifier: SyncNotifier = SyncNotifier(context)
    private val categoriesRestorer: CategoriesRestorer = CategoriesRestorer()

    enum class SyncService(val value: Int) {
        NONE(0),
        SYNCYOMI(1),
        GOOGLE_DRIVE(2),
        // KMK -->
        WEB_DAV(3),
        // KMK <--
        ;

        companion object {
            fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: NONE
        }
    }

    /**
     * Syncs data with a sync service.
     *
     * This function retrieves local data (favorites, manga, extensions, and categories)
     * from the database using the BackupManager, then synchronizes the data with a sync service.
     */
    suspend fun syncData() {
        // Reset isSyncing in case it was left over or failed syncing during restore.
        handler.await(inTransaction = true) {
            mangasQueries.resetIsSyncing()
            chaptersQueries.resetIsSyncing()
            // KMK -->
            categoriesQueries.resetIsSyncing()
            // KMK <--
        }

        val snapshotStartedAt = System.currentTimeMillis() / 1000
        val syncOptions = syncPreferences.getSyncSettings()
        val databaseManga = getAllMangaThatNeedsSync()

        val backupOptions = BackupOptions(
            libraryEntries = syncOptions.libraryEntries,
            categories = syncOptions.categories,
            chapters = syncOptions.chapters,
            tracking = syncOptions.tracking,
            history = syncOptions.history,
            extensionStores = syncOptions.extensionStores,
            appSettings = syncOptions.appSettings,
            sourceSettings = syncOptions.sourceSettings,
            privateSettings = syncOptions.privateSettings,

            // SY -->
            customInfo = syncOptions.customInfo,
            readEntries = syncOptions.readEntries,
            savedSearchesFeeds = syncOptions.savedSearchesFeeds,
            // SY <--
        )

        logcat(LogPriority.DEBUG) { "Begin create backup" }
        val backupManga = backupCreator.backupMangas(databaseManga, backupOptions)
        val backup = Backup(
            backupManga = backupManga,
            backupCategories = backupCreator.backupCategories(backupOptions),
            backupSources = backupCreator.backupSources(backupManga),
            backupPreferences = backupCreator.backupAppPreferences(backupOptions),
            backupSourcePreferences = backupCreator.backupSourcePreferences(backupOptions),
            backupExtensionStores = backupCreator.backupExtensionStores(backupOptions),

            // SY -->
            backupSavedSearches = backupCreator.backupSavedSearches(backupOptions),
            // SY <--

            // KMK -->
            backupFeeds = backupCreator.backupFeeds(backupOptions),
            // KMK <--
        )

        // KMK -->
        backup.wantedExtensions =
            if (syncPreferences.syncExtensions().get()) ExtensionSync.buildSection() else null
        val categorySnapshot = backupCreator.backupCategories(backupOptions)
        // KMK <--
        logcat(LogPriority.DEBUG) { "End create backup" }

        // Create the SyncData object
        val syncData = SyncData(
            deviceId = syncPreferences.uniqueDeviceID(),
            backup = backup,
            // KMK -->
            snapshotStartedAt = snapshotStartedAt,
            // KMK <--
        )

        // Handle sync based on the selected service
        val syncService = when (val syncService = SyncService.fromInt(syncPreferences.syncService().get())) {
            SyncService.SYNCYOMI -> {
                SyncYomiSyncService(
                    context,
                    json,
                    syncPreferences,
                    notifier,
                )
            }

            SyncService.GOOGLE_DRIVE -> {
                GoogleDriveSyncService(context, json, syncPreferences)
            }

            // KMK -->
            SyncService.WEB_DAV -> {
                WebDavSyncService(context, json, syncPreferences, notifier)
            }
            // KMK <--

            else -> {
                logcat(LogPriority.ERROR) { "Invalid sync service type: $syncService" }
                null
            }
        } ?: return

        val result = syncService.doSync(syncData)

        // KMK -->
        if (result.changed) {
            applyRemoteBackup(syncService, result, categorySnapshot)
        }
        acknowledgeResult(result)
        (syncService as? SyncYomiSyncService)?.reportSyncSuccess()
        // KMK <--
        syncPreferences.lastSyncTimestamp().set(Date().time)
        notifier.showSyncSuccess("Sync completed successfully")
    }

    // KMK -->
    private suspend fun applyRemoteBackup(
        syncService: SyncServiceProvider,
        result: SyncResult,
        categorySnapshot: List<BackupCategory>,
    ) {
        try {
            categoriesRestorer.applySync(result.backup.backupCategories, categorySnapshot)

            val backupUri = writeSyncDataToCache(context, result.backup)
                ?: throw IOException("Failed to write sync data to cache")
            logcat(LogPriority.DEBUG) { "Got Backup Uri: $backupUri" }
            val restoreErrors = BackupRestorer(context, BackupNotifier(context), isSync = true).restore(
                backupUri,
                RestoreOptions(
                    appSettings = true,
                    sourceSettings = true,
                    libraryEntries = true,
                    extensionStores = true,
                ),
            )
            if (restoreErrors.isNotEmpty()) {
                logcat(LogPriority.WARN) { "Sync restore finished with ${restoreErrors.size} per-entry errors" }
            }

            handler.await {
                mergedQueries.bindUnresolved()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (result.protocolV2) {
                // Re-deliver the unapplied response on the next sync instead of
                // acknowledging it with the cursor.
                syncPreferences.fullSyncRequested().set(true)
            }
            (syncService as? SyncYomiSyncService)?.reportSyncFailed(e.message)
            throw e
        }
    }

    /**
     * Persists protocol-v2 acknowledgement state. Only called after the remote
     * response has been applied (or there was nothing to apply).
     */
    private fun acknowledgeResult(result: SyncResult) {
        val ack = result.acknowledgement ?: return
        syncPreferences.syncCursor().set(ack.cursor)
        if (ack.fullUpload) {
            syncPreferences.lastFullSync().set(System.currentTimeMillis())
        }
        syncPreferences.fullSyncRequested().set(ack.fullRequested)
        if (ack.snapshotStartedAt > 0) {
            syncPreferences.lastPushedAt().set(ack.snapshotStartedAt)
        }
        syncPreferences.sectionDigests().set(ack.sectionDigests)
    }
    // KMK <--

    private fun writeSyncDataToCache(context: Context, backup: Backup): Uri? {
        val cacheFile = File(context.cacheDir, "tachiyomi_sync_data.proto.gz")
        return try {
            cacheFile.outputStream().use { output ->
                output.write(ProtoBuf.encodeToByteArray(Backup.serializer(), backup))
                Uri.fromFile(cacheFile)
            }
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, throwable = e) { "Failed to write sync data to cache" }
            null
        }
    }

    /**
     * Retrieves all manga from the local database that must participate in sync:
     * favorites, previously favorited (unfavorite records), merged aggregates and
     * every referenced merged child.
     *
     * @return a list of all manga stored in the database
     */
    private suspend fun getAllMangaThatNeedsSync(): List<Manga> {
        return handler.awaitList { mangasQueries.getMangasWithFavoriteTimestamp(::mapManga) }
    }
}
