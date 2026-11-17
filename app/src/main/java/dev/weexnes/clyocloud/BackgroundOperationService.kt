package dev.weexnes.clyocloud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.weexnes.clyocloud.data.ClyoApi
import dev.weexnes.clyocloud.data.CloudConnection
import dev.weexnes.clyocloud.data.UniqueNamer
import dev.weexnes.clyocloud.data.Uploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

enum class OperationStatus { PROCESSING, COMPLETED, ERROR }

data class ProgressOperation(
    val id: String,
    val title: String,
    val current: Int,
    val total: Int,
    val status: OperationStatus,
)

/**
 * Uploads chosen or shared files in the foreground, one after another, with a progress notification.
 * Files are copied to app storage first, so they still upload if the app that shared them goes away.
 */
class BackgroundOperationService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Batches started while one is running queue behind it
    private val queue = Mutex()
    private val running = java.util.concurrent.atomic.AtomicInteger(0)

    companion object {
        private const val TAG = "ClyoUpload"
        private const val CHANNEL_ID = "clyocloud_bg_operations"
        private const val NOTIFICATION_ID = 9001
        private const val ACTION_UPLOAD = "dev.weexnes.clyocloud.ACTION_UPLOAD"

        private val _operations = MutableStateFlow<List<ProgressOperation>>(emptyList())
        val activeOperations: StateFlow<List<ProgressOperation>> = _operations.asStateFlow()

        /** Upload [uris] (content:// or file://) into [targetPath]. */
        fun startUpload(context: Context, serverUrl: String, token: String, targetPath: String, uris: ArrayList<Uri>) {
            val intent = Intent(context, BackgroundOperationService::class.java).apply {
                action = ACTION_UPLOAD
                putExtra("serverUrl", serverUrl)
                putExtra("token", token)
                putExtra("targetPath", targetPath)
                putParcelableArrayListExtra("uris", uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startForegroundService(intent)
        }

        private fun update(op: ProgressOperation) {
            _operations.value = _operations.value.filterNot { it.id == op.id } + op
        }

        private fun removeLater(id: String) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(4000)
                _operations.value = _operations.value.filterNot { it.id == id }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "Uploads", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Progress of uploads to ClyoCloud"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_UPLOAD) return START_NOT_STICKY
        val conn = CloudConnection(intent.getStringExtra("serverUrl") ?: "", "", intent.getStringExtra("token") ?: "")
        val target = intent.getStringExtra("targetPath") ?: ""
        val uris = intent.getParcelableArrayListExtra("uris", Uri::class.java) ?: arrayListOf()
        if (uris.isEmpty() || conn.serverUrl.isBlank()) {
            if (running.get() == 0) stopSelf(startId)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, notification("Uploading", "Preparing ${uris.size} file(s)…", 0, 0, ongoing = true), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        running.incrementAndGet()
        scope.launch {
            // Copy right away (off the main thread), while the read permission for picked or
            // shared content is still valid; the upload itself may wait behind another batch
            val staged = stage(uris)
            queue.withLock { uploadAll(conn, target, staged) }
            if (running.decrementAndGet() == 0) {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private data class Staged(val file: File?, val uri: Uri, val name: String, val size: Long, val mime: String)

    private fun stage(uris: List<Uri>): List<Staged> {
        val dir = File(cacheDir, "upload_staging").apply { mkdirs() }
        return uris.mapIndexed { i, uri ->
            var name = if (uri.scheme == "file") uri.lastPathSegment ?: "file_${i + 1}" else "file_${i + 1}"
            var size = -1L
            try {
                contentResolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { idx -> c.getString(idx)?.takeIf { it.isNotBlank() }?.let { name = it } }
                        c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { idx -> if (!c.isNull(idx)) size = c.getLong(idx) }
                    }
                }
            } catch (_: Exception) {
                if (uri.scheme == "file") uri.lastPathSegment?.let { name = it }
            }
            val mime = (try { contentResolver.getType(uri) } catch (_: Exception) { null })
                ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
                ?: "application/octet-stream"
            val copy = if (uri.scheme == "file") null else try {
                val f = File(dir, "${System.nanoTime()}_$i")
                contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it, 64 * 1024) } }
                f.takeIf { it.exists() }
            } catch (e: Exception) {
                Log.w(TAG, "Could not stage $name, will read it directly", e)
                null
            }
            val localSize = if (uri.scheme == "file") uri.path?.let { File(it).length() } ?: size else size
            Staged(copy, uri, name, copy?.length() ?: localSize, mime)
        }
    }

    private suspend fun uploadAll(conn: CloudConnection, target: String, files: List<Staged>) {
        val opId = "upload-${System.nanoTime()}"
        val namer = UniqueNamer(ClyoApi(conn))
        var ok = 0
        var skipped = 0
        var failed = 0
        update(ProgressOperation(opId, "Uploading", 0, files.size, OperationStatus.PROCESSING))

        files.forEachIndexed { index, f ->
            val number = index + 1
            try {
                when (val decision = namer.decide(target, f.name, f.size)) {
                    UniqueNamer.Decision.AlreadyThere -> skipped++
                    is UniqueNamer.Decision.UploadAs -> {
                        Uploader.upload(conn, target, decision.name, f.mime, f.size, open = {
                            f.file?.inputStream() ?: contentResolver.openInputStream(f.uri)
                        }) { percent ->
                            notify("Uploading $number of ${files.size}", "${decision.name} · $percent%", number, files.size)
                        }
                        namer.uploaded(target, decision.name, f.size)
                        ok++
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Upload failed for ${f.name}", e)
                failed++
            } finally {
                f.file?.delete()
                // Copies made by the share screen live in our cache; remove them (and their folder)
                if (f.uri.scheme == "file") f.uri.path?.let { p ->
                    val local = File(p)
                    if (local.canonicalPath.startsWith(cacheDir.canonicalPath)) {
                        local.delete()
                        local.parentFile?.takeIf { it.name.all(Char::isDigit) }?.delete()
                    }
                }
            }
            update(ProgressOperation(opId, "Uploading", number, files.size, OperationStatus.PROCESSING))
        }

        update(ProgressOperation(opId, "Uploading", files.size, files.size, if (failed == 0) OperationStatus.COMPLETED else OperationStatus.ERROR))
        removeLater(opId)
        val summary = buildList {
            if (ok > 0) add("$ok uploaded")
            if (skipped > 0) add("$skipped already there")
            if (failed > 0) add("$failed failed")
        }.joinToString(" · ").ifEmpty { "Nothing to upload" }
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID + 1, notification(if (failed == 0) "Upload finished" else "Upload finished with errors", summary, 0, 0, ongoing = false))
    }

    private fun notify(title: String, text: String, progress: Int, total: Int) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(title, text, progress, total, ongoing = true))
    }

    private fun notification(title: String, text: String, progress: Int, total: Int, ongoing: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(if (ongoing) android.R.drawable.stat_sys_upload else android.R.drawable.stat_sys_upload_done)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .apply { if (ongoing) setProgress(total, progress, total == 0) }
            .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
