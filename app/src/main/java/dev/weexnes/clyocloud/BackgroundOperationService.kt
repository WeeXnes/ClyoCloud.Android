package dev.weexnes.clyocloud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class OperationStatus {
    PROCESSING, COMPLETED, ERROR
}

data class ProgressOperation(
    val id: String,
    val title: String,
    val current: Int,
    val total: Int,
    val status: OperationStatus
)

class BackgroundOperationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "ClyoCloudBgService"
        private const val CHANNEL_ID = "clyocloud_bg_operations"
        private const val NOTIFICATION_ID = 9001

        private const val ACTION_UPLOAD = "dev.weexnes.clyocloud.ACTION_UPLOAD"

        private val _operations = MutableStateFlow<List<ProgressOperation>>(emptyList())
        val activeOperations: StateFlow<List<ProgressOperation>> = _operations.asStateFlow()

        fun startUpload(
            context: Context,
            serverUrl: String,
            token: String,
            targetPath: String,
            uris: ArrayList<Uri>
        ) {
            val intent = Intent(context, BackgroundOperationService::class.java).apply {
                action = ACTION_UPLOAD
                putExtra("serverUrl", serverUrl)
                putExtra("token", token)
                putExtra("targetPath", targetPath)
                putParcelableArrayListExtra("uris", uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            for (uri in uris) {
                try {
                    context.grantUriPermission(
                        context.packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(id: String, title: String, current: Int, total: Int, status: OperationStatus) {
            val list = _operations.value.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            val op = ProgressOperation(id, title, current, total, status)
            if (idx >= 0) {
                list[idx] = op
            } else {
                list.add(op)
            }
            _operations.value = list
        }

        fun removeOperationDelayed(id: String, delayMs: Long = 4000L) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(delayMs)
                val list = _operations.value.toMutableList()
                list.removeAll { it.id == id }
                _operations.value = list
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        cleanStagingDirectory()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_UPLOAD -> {
                val serverUrl = intent.getStringExtra("serverUrl") ?: ""
                val token = intent.getStringExtra("token") ?: ""
                val targetPath = intent.getStringExtra("targetPath") ?: ""
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra("uris", Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra("uris")
                } ?: arrayListOf()

                if (uris.isNotEmpty()) {
                    startForegroundNotification("Uploading Files", "Preparing ${uris.size} files...")
                    serviceScope.launch {
                        processUploadQueue(serverUrl, token, targetPath, uris)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ClyoCloud Background Operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress for uploads and bulk actions in ClyoCloud"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun cleanStagingDirectory() {
        try {
            val stagingDir = File(cacheDir, "upload_staging")
            if (stagingDir.exists()) {
                stagingDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean staging directory", e)
        }
    }

    private fun startForegroundNotification(title: String, contentText: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, 0, true)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateSystemNotification(title: String, contentText: String, progress: Int, total: Int) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(total, progress, false)
            .setOnlyAlertOnce(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(title: String, contentText: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 1, notification)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private suspend fun processUploadQueue(
        serverUrl: String,
        token: String,
        targetPath: String,
        uris: List<Uri>
    ) {
        val total = uris.size
        var successCount = 0
        var failCount = 0
        val opId = "upload-queue"
        val opTitle = "Uploading Files"

        updateProgress(opId, opTitle, 0, total, OperationStatus.PROCESSING)

        for ((index, uri) in uris.withIndex()) {
            var fileName = "file_${index + 1}"
            var fileSize = 0L

            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val nameVal = cursor.getString(nameIndex)
                            if (!nameVal.isNull_or_empty()) {
                                fileName = nameVal
                            }
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve metadata for $uri", e)
            }

            // Stage URI content to private cache file to insulate against ContentResolver permission revocation when app reopens
            var stagedFile: File? = null
            try {
                val stagingDir = File(cacheDir, "upload_staging").apply { mkdirs() }
                val tempFile = File(stagingDir, "upload_${System.currentTimeMillis()}_$index.tmp")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    stagedFile = tempFile
                    if (fileSize <= 0) {
                        fileSize = tempFile.length()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Staging to cache failed for $fileName, will attempt direct stream", e)
            }

            val currentFileNum = index + 1
            updateSystemNotification(
                "Uploading Files ($currentFileNum/$total)",
                "$fileName (${(currentFileNum * 100) / total}%)",
                currentFileNum,
                total
            )

            val success = performFileUpload(
                serverUrl = serverUrl,
                token = token,
                targetPath = targetPath,
                fileUri = uri,
                stagedFile = stagedFile,
                fileName = fileName,
                fileSize = fileSize,
                onProgressPercent = { percent ->
                    updateSystemNotification(
                        "Uploading Files ($currentFileNum/$total)",
                        "$fileName - $percent%",
                        currentFileNum,
                        total
                    )
                }
            )

            try {
                stagedFile?.delete()
            } catch (_: Exception) {}

            if (success) {
                successCount++
            } else {
                failCount++
                Log.w(TAG, "Failed to upload $fileName")
            }

            updateProgress(opId, opTitle, currentFileNum, total, OperationStatus.PROCESSING)
        }

        val finalStatus = if (failCount == 0) OperationStatus.COMPLETED else OperationStatus.ERROR
        updateProgress(opId, opTitle, total, total, finalStatus)
        removeOperationDelayed(opId)

        val completionText = if (failCount == 0) {
            "Successfully uploaded all $successCount file(s)"
        } else {
            "Uploaded $successCount file(s), $failCount failed"
        }
        showCompletionNotification("Upload Finished", completionText)

        cleanStagingDirectory()
        stopSelf()
    }

    private suspend fun performFileUpload(
        serverUrl: String,
        token: String,
        targetPath: String,
        fileUri: Uri,
        stagedFile: File?,
        fileName: String,
        fileSize: Long,
        onProgressPercent: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val boundary = "Boundary-${System.currentTimeMillis()}"
        val url = URL("$serverUrl/api/upload")
        var conn: HttpURLConnection? = null

        try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Authorization", "Bearer $token")
            }

            val outputStream = conn.outputStream
            val writer = OutputStreamWriter(outputStream, "UTF-8")

            // Write path field
            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"path\"\r\n\r\n")
            writer.write(targetPath)
            writer.write("\r\n")
            writer.flush()

            // Write files header
            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"files\"; filename=\"$fileName\"\r\n")
            val mimeType = try {
                contentResolver.getType(fileUri) ?: "application/octet-stream"
            } catch (_: Exception) {
                "application/octet-stream"
            }
            writer.write("Content-Type: $mimeType\r\n\r\n")
            writer.flush()

            // Open input stream from private staged file or fallback to ContentResolver
            val inputStream = if (stagedFile != null && stagedFile.exists()) {
                stagedFile.inputStream()
            } else {
                contentResolver.openInputStream(fileUri)
            }

            inputStream?.use { input ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalBytesSent = 0L
                var lastProgressReportTime = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesSent += bytesRead

                    val now = System.currentTimeMillis()
                    if (fileSize > 0 && now - lastProgressReportTime > 250) {
                        lastProgressReportTime = now
                        val percent = ((totalBytesSent * 100) / fileSize).toInt().coerceIn(0, 100)
                        onProgressPercent(percent)
                    }
                }
                outputStream.flush()
            } ?: return@withContext false

            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for $fileName", e)
            false
        } finally {
            conn?.disconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanStagingDirectory()
        serviceScope.cancel()
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
