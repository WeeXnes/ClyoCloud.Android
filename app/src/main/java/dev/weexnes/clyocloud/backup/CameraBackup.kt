package dev.weexnes.clyocloud.backup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.weexnes.clyocloud.MainActivity
import dev.weexnes.clyocloud.data.ApiException
import dev.weexnes.clyocloud.data.Session
import dev.weexnes.clyocloud.data.UniqueNamer
import dev.weexnes.clyocloud.data.Uploader
import kotlinx.coroutines.sync.Mutex
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Automatic upload of new photos and videos.
 *
 * Three ways a backup run starts, all running [CameraBackupWorker]:
 *  - a content trigger: Android wakes the app shortly after the photo library changes;
 *  - an hourly periodic run, as a safety net;
 *  - "Back up now" from the app.
 * Only one run uploads at a time; the others see the lock and leave the work to it.
 */
object CameraBackup {
    private const val WORK_PERIODIC = "camera_backup_periodic"
    private const val WORK_TRIGGER = "camera_backup_on_new_media"
    private const val WORK_NOW = "camera_backup_now"
    const val TRIGGER_TAG = "camera_backup_trigger_job"
    const val CHANNEL_ID = "camera_backup"
    const val NOTIFICATION_ID = 7301

    private fun constraints(context: Context, network: Boolean = true) = Constraints.Builder().apply {
        if (network) setRequiredNetworkType(if (BackupPrefs.wifiOnly(context)) NetworkType.UNMETERED else NetworkType.CONNECTED)
        setRequiresCharging(BackupPrefs.chargingOnly(context))
        setRequiresBatteryNotLow(true)
    }.build()

    /** Turns backup on or off. [fromNow]: only photos taken from now on (false = also the existing ones). */
    fun setEnabled(context: Context, enabled: Boolean, fromNow: Boolean = true) {
        if (enabled) {
            if (!BackupPrefs.enabled(context)) BackupPrefs.setSince(context, if (fromNow) System.currentTimeMillis() else 0L)
            BackupPrefs.setEnabled(context, true)
            schedule(context)
            runNow(context)
        } else {
            BackupPrefs.setEnabled(context, false)
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(WORK_PERIODIC)
            wm.cancelUniqueWork(WORK_TRIGGER)
            wm.cancelUniqueWork(WORK_NOW)
        }
    }

    /** (Re)applies the network and charging conditions; call after changing them. */
    fun schedule(context: Context) {
        if (!BackupPrefs.enabled(context)) return
        val wm = WorkManager.getInstance(context)
        val periodic = PeriodicWorkRequestBuilder<CameraBackupWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints(context))
            .build()
        wm.enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        watchForNewMedia(context, ExistingWorkPolicy.REPLACE)
    }

    /**
     * Wakes up when the photo library changes. A content trigger fires once, so every
     * backup run re-arms it (KEEP: don't reset a trigger that is already waiting).
     */
    fun watchForNewMedia(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
        if (!BackupPrefs.enabled(context)) return
        val triggers = Constraints.Builder()
            .setRequiredNetworkType(if (BackupPrefs.wifiOnly(context)) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(BackupPrefs.chargingOnly(context))
            .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .setTriggerContentUpdateDelay(20, TimeUnit.SECONDS)
            .setTriggerContentMaxDelay(2, TimeUnit.MINUTES)
            .build()
        val request = OneTimeWorkRequestBuilder<CameraBackupWorker>().setConstraints(triggers).addTag(TRIGGER_TAG).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_TRIGGER, policy, request)
    }

    /** Starts a run now (still honouring Wi-Fi only). */
    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<CameraBackupWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(if (BackupPrefs.wifiOnly(context)) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
            .setInputData(workDataOf("manual" to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.KEEP, request)
    }

    /** Called on app start: re-creates the schedule if it was lost (e.g. after a force-stop). */
    fun ensureScheduled(context: Context) {
        if (BackupPrefs.enabled(context)) {
            schedule(context)
        }
    }

    /** Folder for one item: base, or base/2026/11 when sorting by month. */
    fun folderFor(context: Context, takenAt: Long): String {
        val base = BackupPrefs.folder(context)
        if (!BackupPrefs.byMonth(context)) return base
        val cal = Calendar.getInstance().apply { timeInMillis = takenAt }
        return "%s/%04d/%02d".format(base, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    internal val running = Mutex()

    /** Live progress for the settings screen: (done, total) of the current run, or null. */
    @Volatile var progress: Pair<Int, Int>? = null
        internal set
}

class CameraBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo("Backing up photos", 0, 0)

    override suspend fun doWork(): Result {
        val context = applicationContext
        // Re-arm the "new photo" trigger first, whatever happens below. From inside the trigger job
        // itself, append: KEEP would see this still-running job and arm nothing.
        CameraBackup.watchForNewMedia(context, if (CameraBackup.TRIGGER_TAG in tags) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP)
        val manual = inputData.getBoolean("manual", false)

        if (!BackupPrefs.enabled(context)) return Result.success()
        val api = Session.api(context) ?: return Result.success().also { BackupPrefs.recordRun(context, "Not signed in") }
        if (!MediaLibrary.hasPermission(context, BackupPrefs.includeVideos(context)) && !MediaLibrary.hasOnlySelectedAccess(context)) {
            BackupPrefs.recordRun(context, "Allow access to photos and videos to back them up")
            return Result.success()
        }
        // Another run is uploading: it will also pick up whatever triggered this one
        if (!CameraBackup.running.tryLock()) return Result.success()

        try {
            val pending = MediaLibrary.pending(context)
            if (pending.isEmpty()) {
                BackupPrefs.recordRun(context, "Up to date")
                return Result.success()
            }

            // Long runs show a progress notification; Android only allows that for runs started
            // while the app was open, so background runs just work in smaller steps instead
            if (manual) {
                try { setForeground(foregroundInfo("Backing up photos", 0, pending.size)) } catch (_: Exception) {}
            }

            val namer = UniqueNamer(api)
            var uploaded = 0
            var failed = 0
            pending.forEachIndexed { index, item ->
                if (isStopped) {
                    BackupPrefs.recordRun(context, "Paused after $uploaded of ${pending.size}; continues automatically")
                    return Result.success()
                }
                CameraBackup.progress = index to pending.size
                val folder = CameraBackup.folderFor(context, item.takenAt)
                try {
                    when (val d = namer.decide(folder, item.name, item.size)) {
                        UniqueNamer.Decision.AlreadyThere -> {}
                        is UniqueNamer.Decision.UploadAs -> {
                            Uploader.upload(api.connection, folder, d.name, item.mime, item.size, open = { context.contentResolver.openInputStream(item.uri) })
                            namer.uploaded(folder, d.name, item.size)
                            uploaded++
                        }
                    }
                    BackupRegistry.add(context, item.key)
                } catch (e: ApiException) {
                    if (e.code == 401) {
                        BackupPrefs.recordRun(context, "Signed out; sign in again to continue the backup")
                        return Result.success()
                    }
                    if (e.code == 413 || e.code == 507) {
                        BackupPrefs.recordRun(context, e.message ?: "Storage full")
                        return Result.success()
                    }
                    failed++
                    Log.w(TAG, "Upload of ${item.name} failed", e)
                } catch (e: java.io.FileNotFoundException) {
                    // Deleted from the phone meanwhile
                    BackupRegistry.add(context, item.key)
                } catch (e: Exception) {
                    failed++
                    Log.w(TAG, "Upload of ${item.name} failed", e)
                    // Probably the network: stop and let WorkManager retry later
                    if (failed >= 3) {
                        BackupPrefs.recordRun(context, "Backed up $uploaded, waiting for the connection")
                        return Result.retry()
                    }
                }
                if (manual && index % 5 == 0) {
                    try { setForeground(foregroundInfo("Backing up photos", index + 1, pending.size)) } catch (_: Exception) {}
                }
            }
            BackupPrefs.recordRun(context, if (failed == 0) "Backed up $uploaded new item${if (uploaded == 1) "" else "s"}" else "Backed up $uploaded, $failed failed")
            return Result.success()
        } finally {
            CameraBackup.progress = null
            CameraBackup.running.unlock()
        }
    }

    private fun foregroundInfo(title: String, done: Int, total: Int): ForegroundInfo {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CameraBackup.CHANNEL_ID, "Camera backup", NotificationManager.IMPORTANCE_LOW))
        val n: Notification = NotificationCompat.Builder(applicationContext, CameraBackup.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (total > 0) "$done of $total" else "Checking for new photos")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total, done, total == 0)
            .setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build()
        return ForegroundInfo(CameraBackup.NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private companion object { const val TAG = "CameraBackup" }
}
