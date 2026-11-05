package dev.weexnes.clyocloud.wallpaper

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.weexnes.clyocloud.data.ApiException
import dev.weexnes.clyocloud.data.ClyoApi
import dev.weexnes.clyocloud.data.Session
import dev.weexnes.clyocloud.data.isImageFile
import java.util.concurrent.TimeUnit

/**
 * Automatic wallpaper changes.
 *
 * Earlier versions kept a foreground service alive with a Handler timer. That timer stops while
 * the phone sleeps, Android 15 stops "data sync" services after 6 hours a day, and nothing brought
 * it back after a reboot. Here every change is a WorkManager job that schedules the next one:
 * it survives sleep, reboots and app updates, needs no permanent notification, and waits for a
 * network connection instead of failing.
 */
object WallpaperCycle {
    private const val WORK_TIMER = "wallpaper_cycle_timer"
    private const val WORK_NOW = "wallpaper_cycle_now"
    const val TAG = "WallpaperCycle"

    private fun constraints(context: Context) = Constraints.Builder()
        .setRequiredNetworkType(if (WallpaperPrefs.unmeteredOnly(context)) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .build()

    /** Turns automatic changes on or off and (re)starts the timer. */
    fun setEnabled(context: Context, enabled: Boolean) {
        WallpaperPrefs.setCycleEnabled(context, enabled)
        if (enabled) scheduleNext(context) else cancel(context)
        WallpaperWidget.refresh(context)
    }

    /** Call after changing the interval or network setting: restarts the countdown with the new values. */
    fun reschedule(context: Context) {
        if (WallpaperPrefs.cycleEnabled(context)) scheduleNext(context)
        WallpaperWidget.refresh(context)
    }

    /**
     * Schedules the next automatic change one interval from now (replacing any pending one).
     * [fromTimerJob]: called by the timer job itself; it appends instead of replacing, because
     * replacing a unique job that is still running would cancel that very job.
     */
    fun scheduleNext(context: Context, fromTimerJob: Boolean = false) {
        val minutes = WallpaperPrefs.intervalMinutes(context).toLong()
        val request = OneTimeWorkRequestBuilder<WallpaperCycleWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .setConstraints(constraints(context))
            .addTag(TAG)
            .addTag(TIMER_TAG)
            .build()
        val policy = if (fromTimerJob) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_TIMER, policy, request)
        WallpaperPrefs.setNextRun(context, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes))
    }

    /** Changes the wallpaper right away (and restarts the countdown if the cycle is on). */
    fun changeNow(context: Context, specificPath: String? = null) {
        val request = OneTimeWorkRequestBuilder<WallpaperCycleWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(KEY_PATH to specificPath, KEY_MANUAL to true))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TIMER)
        WallpaperPrefs.setNextRun(context, 0L)
    }

    /** Re-creates the timer if it was lost (e.g. the app was force-stopped). Called on app start. */
    fun ensureScheduled(context: Context) {
        if (!WallpaperPrefs.cycleEnabled(context)) return
        val infos = runCatching { WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_TIMER).get() }.getOrNull().orEmpty()
        val pending = infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
        if (!pending) scheduleNext(context)
    }

    internal const val TIMER_TAG = "wallpaper_cycle_timer_job"
    internal const val KEY_PATH = "path"
    internal const val KEY_MANUAL = "manual"
}

class WallpaperCycleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val manual = inputData.getBoolean(WallpaperCycle.KEY_MANUAL, false)
        val specificPath = inputData.getString(WallpaperCycle.KEY_PATH)

        // A timer run that fires after the cycle was turned off does nothing
        if (!manual && !WallpaperPrefs.cycleEnabled(context)) return Result.success()

        val api = Session.api(context)
        if (api == null) {
            WallpaperPrefs.setLastError(context, "Not signed in")
            return finish(context, Result.success())
        }

        return try {
            val applied = if (specificPath != null) {
                applyPath(context, api, specificPath)
                specificPath
            } else {
                applyNext(context, api)
            }
            if (applied == null) WallpaperPrefs.setLastError(context, "No wallpapers to show. Mark images as wallpapers or add them to the playlist.")
            finish(context, Result.success())
        } catch (e: ApiException) {
            Log.w(WallpaperCycle.TAG, "Server error: ${e.message}")
            WallpaperPrefs.setLastError(context, e.message)
            // 401: the session is gone; retrying won't help. Other server errors: try again later.
            if (e.code == 401 || runAttemptCount >= 3) finish(context, Result.success()) else Result.retry()
        } catch (e: Exception) {
            Log.w(WallpaperCycle.TAG, "Wallpaper change failed", e)
            WallpaperPrefs.setLastError(context, e.message ?: "Wallpaper change failed")
            if (runAttemptCount >= 3) finish(context, Result.success()) else Result.retry()
        }
    }

    /** Always schedule the following change, so one failure never ends the cycle. */
    private fun finish(context: Context, result: Result): Result {
        if (WallpaperPrefs.cycleEnabled(context)) {
            WallpaperCycle.scheduleNext(context, fromTimerJob = WallpaperCycle.TIMER_TAG in tags)
        } else {
            WallpaperPrefs.setNextRun(context, 0L)
        }
        WallpaperWidget.refresh(context)
        return result
    }

    /** Picks the next candidate and applies it, skipping files that are gone from the server. */
    private suspend fun applyNext(context: Context, api: ClyoApi): String? {
        var candidates = when (WallpaperPrefs.source(context)) {
            CycleSource.All -> api.wallpapers().filter { !it.isFolder && isImageFile(it.name) }.map { it.path }
            CycleSource.Playlist -> WallpaperPrefs.playlist(context)
        }
        if (candidates.isEmpty()) return null

        repeat(minOf(5, candidates.size)) {
            val next = pick(context, candidates) ?: return null
            try {
                applyPath(context, api, next)
                return next
            } catch (e: WallpaperMissingException) {
                // Deleted or renamed on the server: drop it from the playlist and try the next one
                if (WallpaperPrefs.source(context) == CycleSource.Playlist) {
                    WallpaperPrefs.setPlaylist(context, WallpaperPrefs.playlist(context) - next)
                }
                candidates = candidates - next
                if (candidates.isEmpty()) return null
            }
        }
        return null
    }

    private suspend fun applyPath(context: Context, api: ClyoApi, path: String) {
        val file = WallpaperApplier.download(context, api, path)
        try {
            WallpaperApplier.apply(context, file, WallpaperPrefs.framing(context, path) ?: Framing(), WallpaperPrefs.target(context))
        } finally {
            file.delete()
        }
        WallpaperPrefs.recordApplied(context, path)
        val recent = (listOf(path) + WallpaperPrefs.recent(context)).distinct().take(200)
        WallpaperPrefs.setRecent(context, recent)
    }

    /** Shuffle: random among the ones not shown recently. In order: the next one after the current. */
    private fun pick(context: Context, candidates: List<String>): String? {
        if (candidates.isEmpty()) return null
        val current = WallpaperPrefs.current(context)
        if (WallpaperPrefs.shuffle(context)) {
            if (candidates.size == 1) return candidates.first()
            // Avoid the most recent ~70% of the list (and never the current one)
            val avoid = WallpaperPrefs.recent(context).take((candidates.size * 0.7).toInt().coerceAtLeast(1)).toSet() + listOfNotNull(current)
            val fresh = candidates.filter { it !in avoid }
            return (fresh.ifEmpty { candidates.filter { it != current } }).random()
        }
        // In order: continue after the current wallpaper's position, even if the list changed meanwhile
        val currentIndex = candidates.indexOf(current)
        val index = if (currentIndex >= 0) (currentIndex + 1) % candidates.size else WallpaperPrefs.cursor(context) % candidates.size
        WallpaperPrefs.setCursor(context, (index + 1) % candidates.size)
        return candidates[index]
    }
}
