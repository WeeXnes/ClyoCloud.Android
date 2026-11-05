package dev.weexnes.clyocloud.wallpaper

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.weexnes.clyocloud.R
import dev.weexnes.clyocloud.data.ImageLoader
import dev.weexnes.clyocloud.data.Session
import dev.weexnes.clyocloud.data.isImageFile
import java.io.File
import java.util.concurrent.TimeUnit

// ─── Wallpaper widget: current wallpaper, countdown, tap to change ───────────

class WallpaperWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refresh(context)
            ACTION_CHANGE_NOW -> WallpaperCycle.changeNow(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "dev.weexnes.clyocloud.WALLPAPER_WIDGET_REFRESH"
        const val ACTION_CHANGE_NOW = "dev.weexnes.clyocloud.WALLPAPER_WIDGET_CHANGE"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, WallpaperWidget::class.java)).forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_wallpaper)
            val current = WallpaperPrefs.current(context)
            views.setTextViewText(R.id.widget_title, current?.substringAfterLast('/') ?: "No wallpaper yet")

            val next = WallpaperPrefs.nextRun(context)
            val now = System.currentTimeMillis()
            if (WallpaperPrefs.cycleEnabled(context) && next > now) {
                views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                views.setViewVisibility(R.id.widget_status, View.GONE)
                views.setChronometerCountDown(R.id.widget_chronometer, true)
                views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime() + (next - now), null, true)
            } else {
                views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                views.setViewVisibility(R.id.widget_status, View.VISIBLE)
                views.setTextViewText(R.id.widget_status, if (WallpaperPrefs.cycleEnabled(context)) "Waiting for network" else "Auto change off")
            }

            val tap = PendingIntent.getBroadcast(
                context, 10,
                Intent(context, WallpaperWidget::class.java).setAction(ACTION_CHANGE_NOW),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, tap)
            manager.updateAppWidget(id, views)
        }
    }
}

// ─── Photo widget: a rotating photo from the "photo widget" images ───────────

class PhotoWidget : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        PhotoWidgetCycle.ensureScheduled(context)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
        if (!File(context.filesDir, PhotoWidgetCycle.IMAGE_FILE).exists()) PhotoWidgetCycle.nextNow(context)
    }

    override fun onDisabled(context: Context) {
        PhotoWidgetCycle.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refresh(context)
            ACTION_NEXT -> PhotoWidgetCycle.nextNow(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "dev.weexnes.clyocloud.PHOTO_WIDGET_REFRESH"
        const val ACTION_NEXT = "dev.weexnes.clyocloud.PHOTO_WIDGET_NEXT"

        fun hasWidgets(context: Context) =
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, PhotoWidget::class.java)).isNotEmpty()

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, PhotoWidget::class.java)).forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_photo)
            val file = File(context.filesDir, PhotoWidgetCycle.IMAGE_FILE)
            val bitmap = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.photo_image, bitmap)
                views.setViewVisibility(R.id.photo_image, View.VISIBLE)
                views.setViewVisibility(R.id.photo_placeholder, View.GONE)
            } else {
                views.setViewVisibility(R.id.photo_image, View.GONE)
                views.setViewVisibility(R.id.photo_placeholder, View.VISIBLE)
            }
            val tap = PendingIntent.getBroadcast(
                context, 11,
                Intent(context, PhotoWidget::class.java).setAction(ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.photo_root, tap)
            manager.updateAppWidget(id, views)
        }
    }
}

object PhotoWidgetCycle {
    private const val WORK_TIMER = "photo_widget_timer"
    private const val WORK_NOW = "photo_widget_now"
    internal const val TIMER_TAG = "photo_widget_timer_job"
    const val IMAGE_FILE = "photo_widget_current.png"

    /** Widget images are kept small: RemoteViews bitmaps are limited by the binder transaction size. */
    private const val MAX_EDGE = 900

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun scheduleNext(context: Context, fromTimerJob: Boolean = false) {
        val minutes = WallpaperPrefs.widgetIntervalMinutes(context).toLong()
        val request = OneTimeWorkRequestBuilder<PhotoWidgetWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .setConstraints(network)
            .addTag(TIMER_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_TIMER, if (fromTimerJob) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, request,
        )
        WallpaperPrefs.setWidgetNextRun(context, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes))
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        WallpaperPrefs.setWidgetEnabled(context, enabled)
        if (enabled) scheduleNext(context) else cancel(context)
    }

    fun nextNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<PhotoWidgetWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(network)
            .setInputData(workDataOf("manual" to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TIMER)
        WallpaperPrefs.setWidgetNextRun(context, 0L)
    }

    fun ensureScheduled(context: Context) {
        if (!WallpaperPrefs.widgetEnabled(context)) return
        val infos = runCatching { WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_TIMER).get() }.getOrNull().orEmpty()
        if (infos.none { !it.state.isFinished }) scheduleNext(context)
    }

    internal fun saveImage(context: Context, bitmap: Bitmap) {
        val tmp = File(context.filesDir, "$IMAGE_FILE.tmp")
        tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tmp.renameTo(File(context.filesDir, IMAGE_FILE))
    }

    internal const val MAX_EDGE_PX = MAX_EDGE
}

class PhotoWidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val manual = inputData.getBoolean("manual", false)
        if (!manual && !WallpaperPrefs.widgetEnabled(context)) return Result.success()

        val result = try {
            val api = Session.api(context) ?: return done(context, Result.success())
            val photos = api.photoWidgets().filter { !it.isFolder && isImageFile(it.name) }.map { it.path }
            if (photos.isNotEmpty()) {
                val current = WallpaperPrefs.widgetCurrent(context)
                val index = photos.indexOf(current).let { if (it >= 0) (it + 1) % photos.size else WallpaperPrefs.widgetCursor(context) % photos.size }
                val path = photos[index]
                WallpaperPrefs.setWidgetCursor(context, (index + 1) % photos.size)
                val bitmap = ImageLoader.load(context, api.fileUrl(path), api.connection.token, PhotoWidgetCycle.MAX_EDGE_PX, diskCache = false)
                if (bitmap != null) {
                    PhotoWidgetCycle.saveImage(context, bitmap)
                    WallpaperPrefs.setWidgetCurrent(context, path)
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= 3) Result.success() else return Result.retry()
        }
        return done(context, result)
    }

    private fun done(context: Context, result: Result): Result {
        PhotoWidget.refresh(context)
        if (WallpaperPrefs.widgetEnabled(context)) {
            PhotoWidgetCycle.scheduleNext(context, fromTimerJob = PhotoWidgetCycle.TIMER_TAG in tags)
        }
        return result
    }
}
