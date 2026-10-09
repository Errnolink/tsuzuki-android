package eu.kanade.tachiyomi.data.sync

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SyncDeltaTest {

    private fun manga(
        url: String,
        lastModifiedAt: Long,
        chapters: List<BackupChapter> = emptyList(),
    ) = BackupManga(source = 1, url = url, title = url, lastModifiedAt = lastModifiedAt, chapters = chapters)

    private fun chapter(url: String, lastModifiedAt: Long) =
        BackupChapter(url = url, name = url, lastModifiedAt = lastModifiedAt)

    @Test
    fun `selects manga modified since the boundary`() {
        val delta = selectSyncDelta(
            Backup(
                backupManga = listOf(
                    manga("/new", lastModifiedAt = 200),
                    manga("/old", lastModifiedAt = 99),
                ),
            ),
            since = 100,
        )
        delta.backupManga.map { it.url } shouldContainExactly listOf("/new")
    }

    @Test
    fun `boundary equality is retained`() {
        val delta = selectSyncDelta(
            Backup(backupManga = listOf(manga("/edge", lastModifiedAt = 100))),
            since = 100,
        )
        delta.backupManga.map { it.url } shouldContainExactly listOf("/edge")
    }

    @Test
    fun `includes manga with only changed chapters and restricts chapter list`() {
        val delta = selectSyncDelta(
            Backup(
                backupManga = listOf(
                    manga(
                        "/child",
                        lastModifiedAt = 10,
                        chapters = listOf(chapter("/c1", lastModifiedAt = 50), chapter("/c2", lastModifiedAt = 150)),
                    ),
                ),
            ),
            since = 100,
        )
        val selected = delta.backupManga.single()
        selected.url shouldBe "/child"
        selected.chapters.map { it.url } shouldContainExactly listOf("/c2")
        selected.chapters.single().lastModifiedAt shouldBe 150
    }

    @Test
    fun `manga-only change sends no chapter updates`() {
        val delta = selectSyncDelta(
            Backup(
                backupManga = listOf(
                    manga("/parent", lastModifiedAt = 300, chapters = listOf(chapter("/c1", lastModifiedAt = 1))),
                ),
            ),
            since = 100,
        )
        delta.backupManga.single().url shouldBe "/parent"
        delta.backupManga.single().chapters.shouldBeEmpty()
    }

    @Test
    fun `empty delta keeps categories and sources`() {
        val backup = Backup(
            backupManga = listOf(manga("/old", lastModifiedAt = 1)),
            backupCategories = listOf(BackupCategory(name = "Reading")),
        )
        val delta = selectSyncDelta(backup, since = 100)
        delta.backupManga.shouldBeEmpty()
        delta.backupCategories.map { it.name } shouldContainExactly listOf("Reading")
    }

    @Test
    fun `sections are included on first sync and only when digests change`() {
        val backup = Backup(
            backupPreferences = listOf(BackupPreference("key", IntPreferenceValue(1))),
            backupSavedSearches = listOf(BackupSavedSearch(name = "search")),
        )

        val first = selectSyncSections(backup, previous = emptySet(), full = true)
        first.backup.backupPreferences shouldContainExactly backup.backupPreferences
        first.backup.backupSavedSearches shouldContainExactly backup.backupSavedSearches

        val unchanged = selectSyncSections(backup, previous = first.digests, full = false)
        unchanged.backup.backupPreferences.shouldBeEmpty()
        unchanged.backup.backupSavedSearches.shouldBeEmpty()
        unchanged.digests shouldBe first.digests

        val changed = selectSyncSections(
            backup.copy(backupPreferences = listOf(BackupPreference("key", IntPreferenceValue(2)))),
            previous = first.digests,
            full = false,
        )
        changed.backup.backupPreferences shouldContainExactly listOf(BackupPreference("key", IntPreferenceValue(2)))
        changed.backup.backupSavedSearches.shouldBeEmpty()
        changed.digests.size shouldBe first.digests.size
    }
}
