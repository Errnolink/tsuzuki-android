package eu.kanade.tachiyomi.data.sync

import eu.kanade.tachiyomi.data.backup.models.Backup
import kotlinx.serialization.protobuf.ProtoBuf
import java.security.MessageDigest

internal fun selectSyncDelta(backup: Backup, since: Long): Backup = backup.copy(
    backupManga = backup.backupManga.mapNotNull { manga ->
        val chapters = manga.chapters.filter { it.lastModifiedAt >= since }
        if (manga.lastModifiedAt >= since || chapters.isNotEmpty()) manga.copy(chapters = chapters) else null
    },
)

internal data class SyncSections(val backup: Backup, val digests: Set<String>)

internal fun selectSyncSections(backup: Backup, previous: Set<String>, full: Boolean): SyncSections {
    val sections = listOf(
        "preferences" to Backup(backupPreferences = backup.backupPreferences),
        "sources" to Backup(backupSourcePreferences = backup.backupSourcePreferences),
        "stores" to Backup(backupExtensionStores = backup.backupExtensionStores),
        "searches" to Backup(backupSavedSearches = backup.backupSavedSearches),
        "feeds" to Backup(backupFeeds = backup.backupFeeds),
    )
    val digests = sections.map { (name, section) ->
        name + ":" + MessageDigest.getInstance("SHA-256")
            .digest(ProtoBuf.encodeToByteArray(Backup.serializer(), section))
            .joinToString("") { "%02x".format(it) }
    }.toSet()
    fun changed(name: String) = full || digests.first { it.startsWith("$name:") } !in previous
    return SyncSections(
        backup.copy(
            backupPreferences = backup.backupPreferences.takeIf { changed("preferences") }.orEmpty(),
            backupSourcePreferences = backup.backupSourcePreferences.takeIf { changed("sources") }.orEmpty(),
            backupExtensionStores = backup.backupExtensionStores.takeIf { changed("stores") }.orEmpty(),
            backupSavedSearches = backup.backupSavedSearches.takeIf { changed("searches") }.orEmpty(),
            backupFeeds = backup.backupFeeds.takeIf { changed("feeds") }.orEmpty(),
        ),
        digests,
    )
}
