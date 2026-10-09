from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[1] / "data/src/main/sqldelight/tachiyomi"


def source(path):
    return (ROOT / path).read_text(encoding="utf-8")


def statement(path, label):
    return source(path).split(label + ":", 1)[1].split(";", 1)[0]


def view(name):
    return re.search(r"CREATE VIEW[\s\S]+?;", source("view/" + name + ".sq")).group()


class LibraryFilterTests(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.row_factory = sqlite3.Row
        for name in ["mangas", "chapters", "merged", "history", "mangas_categories", "excluded_scanlators"]:
            sql = re.search(r"CREATE TABLE[\s\S]+?;", source("data/" + name + ".sq")).group()
            sql = re.sub(r"\b(INTEGER|TEXT|BLOB|REAL) AS [\w<>]+", r"\1", sql)
            self.db.executescript(sql)
        for name in ["libraryView", "historyView", "updatesView"]:
            self.db.executescript(view(name))
        self.manga(1)
        self.chapter(1, 1, 1, "A & B", read=1)
        self.chapter(2, 1, 3, "B", name="Chapter 3 · Unavailable")
        self.chapter(3, 1, 3.5, "C", memo='{"nekoUnavailable":true}')
        self.chapter(4, 1, -1, None)
        self.db.executescript(source("migrations/48.sqm"))

    def tearDown(self):
        self.db.close()

    def manga(self, manga_id, source_id=1):
        self.db.execute(
            "INSERT INTO mangas(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added) VALUES (?,?,?,'Title',0,1,1,0,0,0,0)",
            (manga_id, source_id, str(manga_id)),
        )

    def chapter(self, chapter_id, manga_id, number, scanlator, read=0, name="Chapter", memo="{}"):
        self.db.execute(
            "INSERT INTO chapters(_id,manga_id,url,name,scanlator,read,bookmark,last_page_read,chapter_number,source_order,date_fetch,date_upload,memo) VALUES (?,?,?,?,?,?,0,0,?,?,1,1,?)",
            (chapter_id, manga_id, str(chapter_id), name, scanlator, read, number, chapter_id, memo),
        )

    def library(self, manga_id=1):
        return self.db.execute("SELECT * FROM libraryView WHERE _id = ?", (manga_id,)).fetchone()

    def chapters(self, manga_id=1, merged=False):
        query = statement("data/chapters.sq", "getMergedChaptersByMangaId" if merged else "getChaptersByMangaId")
        return self.db.execute(query, {"mangaId": manga_id, "applyFilter": 1, "bookmarkMask": 1, "bookmarkUnmask": 2}).fetchall()

    def mode(self, all_groups):
        self.db.execute(statement("data/tsuzuki_chapter_filter.sq", "setMatchAll"), {"matchAll": int(all_groups)})

    def test_migration_preserves_chapters_and_matches_source_views(self):
        self.assertEqual(4, self.db.execute("SELECT count(*) FROM chapters").fetchone()[0])
        for name in ["excludedScanlatorsView", "libraryView", "historyView", "updatesView"]:
            installed = self.db.execute("SELECT sql FROM sqlite_master WHERE name = ?", (name,)).fetchone()[0]
            normalize = lambda sql: re.sub(r"\s+", " ", re.sub(r"--[^\n]*", "", sql)).strip().rstrip(";")
            self.assertEqual(normalize(view(name)), normalize(installed))

    def test_stored_gaps_ignore_duplicates_fractional_and_unknown_numbers(self):
        row = self.library()
        self.assertEqual(1, row["missingCount"])
        self.assertEqual(2, row["unavailableCount"])
        self.chapter(5, 1, 2, "B")
        self.assertEqual(0, self.library()["missingCount"])

    def test_any_and_all_match_reader_download_and_library_counts(self):
        self.db.execute("INSERT INTO excluded_scanlators VALUES (1,'A')")
        for all_groups, count, read_count in [(False, 3, 0), (True, 4, 1)]:
            self.mode(all_groups)
            self.assertEqual(count, len(self.chapters()))
            self.assertEqual(count, self.library()["totalCount"])
            self.assertEqual(read_count, self.library()["readCount"])
        self.db.execute("INSERT INTO excluded_scanlators VALUES (1,'B')")
        self.assertEqual(2, len(self.chapters()))
        self.assertEqual(2, self.library()["totalCount"])
        self.assertEqual(1, self.library()["unavailableCount"])

    def test_legacy_combination_exclusions_and_uploader_fallback(self):
        self.db.execute("INSERT INTO excluded_scanlators VALUES (1,'A & B')")
        self.mode(True)
        self.assertEqual(2, len(self.chapters()))
        self.chapter(5, 1, 4, "Uploader")
        self.db.execute("INSERT INTO excluded_scanlators VALUES (1,'Uploader')")
        self.assertEqual(2, len(self.chapters()))

    def test_merged_parent_uses_its_own_exclusions_and_filled_gaps(self):
        self.manga(2)
        self.manga(3, 6969)
        self.chapter(5, 2, 2, "Other")
        for child in [1, 2]:
            self.db.execute(
                "INSERT INTO merged(info_manga,get_chapter_updates,chapter_sort_mode,chapter_priority,download_chapters,merge_id,merge_url,manga_id,manga_url,manga_source) VALUES (1,1,0,0,1,3,'3',?,?,1)",
                (child, str(child)),
            )
        self.assertEqual(0, self.library(3)["missingCount"])
        self.db.execute("INSERT INTO excluded_scanlators VALUES (3,'A')")
        for all_groups, count in [(False, 4), (True, 5)]:
            self.mode(all_groups)
            self.assertEqual(count, len(self.chapters(3, merged=True)))
            self.assertEqual(count, self.library(3)["totalCount"])
            self.assertEqual(4, self.library(1)["totalCount"])

    def test_empty_titles_have_no_known_gap_or_unavailable_chapter(self):
        self.manga(2)
        self.assertEqual(0, self.library(2)["missingCount"])
        self.assertEqual(0, self.library(2)["unavailableCount"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
