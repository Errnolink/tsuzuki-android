package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.sync.SyncNotifier
import eu.kanade.tachiyomi.data.sync.selectSyncDelta
import eu.kanade.tachiyomi.data.sync.selectSyncSections
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.util.system.logcat
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.http.HttpStatus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class SyncYomiSyncService(
    context: Context,
    json: Json,
    syncPreferences: SyncPreferences,
    private val notifier: SyncNotifier,

    private val protoBuf: ProtoBuf = Injekt.get(),
) : SyncService(context, json, syncPreferences) {

    // KMK -->
    companion object {
        private val client by lazy { OkHttpClient() }

        private val FULL_SYNC_INTERVAL_MS = TimeUnit.HOURS.toMillis(24)
    }
    // KMK <--

    private class SyncYomiException(message: String?) : Exception(message)

    @Serializable
    private data class SyncEvent(
        val event: SyncEventStatus,
        @SerialName("device_id")
        val deviceId: String? = null,
        @SerialName("device_name")
        val deviceName: String? = null,
        val message: String? = null,
    )

    @Serializable
    private enum class SyncEventStatus {
        SYNC_STARTED,
        SYNC_SUCCESS,
        SYNC_FAILED,
        SYNC_ERROR,
        SYNC_CANCELLED,
    }

    @Serializable
    private data class Capabilities(
        val version: Int = 0,
        val merge: String? = null,
        val snapshot: String? = null,
    )

    override suspend fun doSync(syncData: SyncData): SyncResult {
        reportSyncEvent(SyncEventStatus.SYNC_STARTED)

        try {
            syncPreferences.prepareSyncIdentity()
            requireV2Capabilities()

            val backup = requireNotNull(syncData.backup) { "Local backup snapshot is missing" }
            val cursor = syncPreferences.syncCursor().get()
            val fullUpload = cursor == 0L ||
                syncPreferences.fullSyncRequested().get() ||
                System.currentTimeMillis() - syncPreferences.lastFullSync().get() > FULL_SYNC_INTERVAL_MS
            val deletedCategories = syncPreferences.categoryState.deleted()

            val payload: Backup
            val sectionDigests: Set<String>
            if (fullUpload) {
                payload = backup
                sectionDigests = selectSyncSections(backup, emptySet(), full = true).digests
            } else {
                val delta = selectSyncDelta(backup, syncPreferences.lastPushedAt().get())
                val sections = selectSyncSections(delta, syncPreferences.sectionDigests().get(), full = false)
                payload = sections.backup
                sectionDigests = sections.digests
            }

            val host = clientHost()
            val headers = Headers.Builder()
                .add("X-API-Token", syncPreferences.clientAPIKey().get())
                .add("X-Device-ID", syncData.deviceId)
                .add("X-Device-Name", android.os.Build.MODEL)
                .add("X-Sync-Cursor", cursor.toString())
                .add("X-Sync-Full", fullUpload.toString())
                .apply {
                    if (deletedCategories.isNotEmpty()) {
                        add("X-Sync-Deleted-Categories", deletedCategories.joinToString(","))
                    }
                }
                .build()

            val request = POST(
                url = "$host/api/sync/v2/merge",
                headers = headers,
                body = BackupRequestBody(payload, protoBuf),
            )

            val responseBackup: Backup
            val responseCursor: Long
            val changed: Boolean
            val fullRequested: Boolean
            client.newCall(request).await().use { response ->
                if (response.code == HttpStatus.SC_UNAUTHORIZED) {
                    throw SyncYomiException("SyncYomi authentication failed (401): check the API key")
                }
                if (!response.isSuccessful) {
                    val responseBody = response.body.string()
                    notifier.showSyncError("Failed to sync with SyncYomi: $responseBody")
                    logcat(LogPriority.ERROR) { "SyncError: $responseBody" }
                    throw SyncYomiException("SyncYomi v2 merge failed (${response.code})")
                }

                responseCursor = response.headers["X-Sync-Cursor"]?.toLongOrNull()
                    ?: throw SyncYomiException("Missing X-Sync-Cursor in merge response")
                changed = response.headers["X-Sync-Changed"]?.equals("true", ignoreCase = true) == true
                fullRequested = response.headers["X-Sync-Full-Requested"]?.equals("true", ignoreCase = true) == true

                val bytes = response.body.byteStream().use { it.readBytes() }
                responseBackup = if (bytes.isEmpty()) {
                    Backup()
                } else {
                    try {
                        protoBuf.decodeFromByteArray(Backup.serializer(), bytes)
                    } catch (e: SerializationException) {
                        logcat(LogPriority.ERROR, throwable = e) { "Bad content responded from server" }
                        throw SyncYomiException("SyncYomi returned an undecodable backup")
                    }
                }
            }

            logcat(LogPriority.DEBUG) {
                "SyncYomi v2 merge completed: cursor=$responseCursor changed=$changed fullRequested=$fullRequested"
            }

            return SyncResult(
                backup = responseBackup,
                changed = changed,
                acknowledgement = SyncAcknowledgement(
                    cursor = responseCursor,
                    fullRequested = fullRequested,
                    fullUpload = fullUpload,
                    deletedCategories = deletedCategories,
                    snapshotStartedAt = syncData.snapshotStartedAt,
                    sectionDigests = sectionDigests,
                ),
            )
        } catch (e: Exception) {
            if (e is CancellationException) {
                reportSyncEvent(SyncEventStatus.SYNC_CANCELLED, e.message)
                throw e
            }
            logcat(LogPriority.ERROR) { "Error syncing: ${e.message}" }
            notifier.showSyncError(e.message)
            reportSyncEvent(SyncEventStatus.SYNC_ERROR, e.message)
            throw e
        }
    }

    suspend fun reportSyncSuccess() = reportSyncEvent(SyncEventStatus.SYNC_SUCCESS)

    suspend fun reportSyncFailed(message: String? = null) = reportSyncEvent(SyncEventStatus.SYNC_FAILED, message)

    private fun clientHost(): String {
        return syncPreferences.clientHost().get().trim().trimEnd('/')
    }

    private suspend fun requireV2Capabilities() {
        if (syncPreferences.capabilities().get() == 2) return

        val request = GET(
            url = "${clientHost()}/api/sync/v2/capabilities",
            headers = Headers.Builder().add("X-API-Token", syncPreferences.clientAPIKey().get()).build(),
        )

        client.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw SyncYomiException("SyncYomi v2 capabilities probe failed (HTTP ${response.code})")
            }
            val capabilities = json.decodeFromString(Capabilities.serializer(), response.body.string())
            if (capabilities.version < 2) {
                throw SyncYomiException("SyncYomi server speaks protocol v${capabilities.version}, v2 required")
            }
            syncPreferences.capabilities().set(2)
        }
    }

    private suspend fun reportSyncEvent(event: SyncEventStatus, message: String? = null) {
        withContext(NonCancellable) {
            try {
                val headers = Headers.Builder()
                    .add("X-API-Token", syncPreferences.clientAPIKey().get())
                    .add("X-Device-ID", syncPreferences.uniqueDeviceID())
                    .build()

                val bodyObj = SyncEvent(
                    event = event,
                    deviceId = syncPreferences.uniqueDeviceID(),
                    deviceName = android.os.Build.MODEL,
                    message = message,
                )

                val jsonBody = json.encodeToString(SyncEvent.serializer(), bodyObj)
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = POST(
                    url = "${clientHost()}/api/sync/event",
                    headers = headers,
                    body = requestBody,
                )

                client.newCall(request).await().close()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Failed to report sync event: ${e.message}" }
            }
        }
    }
}
