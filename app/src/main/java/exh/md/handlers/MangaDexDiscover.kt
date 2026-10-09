package exh.md.handlers

import exh.md.dto.MangaDataDto
import exh.md.dto.MangaListDto
import exh.md.dto.SeasonalDto
import exh.md.service.MangaDexFeatureRequests
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class MangaDexDiscoverRow(
    val key: String,
    val title: String,
    val manga: List<MangaDataDto> = emptyList(),
    val error: String? = null,
)

class MangaDexDiscover(private val features: MangaDexFeatures) {
    fun rows(signedIn: Boolean) = flow {
        val loaders = listOf<Pair<String, suspend () -> MangaDexDiscoverRow>>(
            "Seasonal" to {
                val season = features.request<SeasonalDto>(MangaDexFeatureRequests.seasonal())
                MangaDexDiscoverRow("seasonal", season.name, list(season.id))
            },
            "Staff picks" to { MangaDexDiscoverRow("staff", "Staff picks", list(MangaDexFeatureRequests.STAFF_PICKS)) },
            "Neko picks" to { MangaDexDiscoverRow("neko", "Neko picks", list(MangaDexFeatureRequests.NEKO_PICKS)) },
            "Popular new titles" to {
                val since = LocalDateTime.now(ZoneOffset.UTC).minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                MangaDexDiscoverRow("popular", "Popular new titles", features.request<MangaListDto>(MangaDexFeatureRequests.popularNew(features.ratings, since)).data)
            },
            "Recently added" to {
                MangaDexDiscoverRow("recent", "Recently added", features.request<MangaListDto>(MangaDexFeatureRequests.recentlyAdded(features.ratings)).data)
            },
            "Latest chapters" to {
                MangaDexDiscoverRow("latest", "Latest chapters", features.feed(0, followed = false).entries.distinctBy { it.manga.id }.take(20).map { it.manga })
            },
        ) + if (signedIn) listOf("Your follows" to suspend {
            MangaDexDiscoverRow("follows", "Your follows", features.feed(0).entries.distinctBy { it.manga.id }.take(20).map { it.manga })
        }) else emptyList()
        if (features.ratings.isEmpty()) return@flow
        for ((title, load) in loaders) {
            val row = try {
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MangaDexDiscoverRow(title, title, error = e.message ?: "MangaDex could not load this shelf")
            }
            emit(row)
        }
    }

    private suspend fun list(id: String): List<MangaDataDto> = features.manga(features.list(id).mangaIds).shuffled().take(20)
}
