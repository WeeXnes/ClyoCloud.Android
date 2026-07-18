package dev.weexnes.clyocloudandroid

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.ExistingWorkPolicy
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class WallpaperCycleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        Log.d("WallpaperCycleWorker", "Worker execution triggered")

        val prefs = context.getSharedPreferences("clyo_wallpaper_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("cycle_enabled", false)
        if (!isEnabled) {
            Log.d("WallpaperCycleWorker", "Auto-cycle is disabled, exiting worker")
            return Result.success()
        }

        val serverUrl = prefs.getString("server_url", null)
        val token = prefs.getString("token", null)
        if (serverUrl.isNullOrBlank() || token.isNullOrBlank()) {
            Log.e("WallpaperCycleWorker", "Missing connection credentials, cycle stopped")
            return Result.failure()
        }

        val source = prefs.getString("cycle_source", "Playlist") ?: "Playlist"
        val isShuffle = prefs.getBoolean("cycle_shuffle", false)
        var currentIndex = prefs.getInt("cycle_index", 0)

        // 1. Determine target wallpaper paths list
        val targetList = mutableListOf<String>()
        if (source == "All") {
            try {
                Log.d("WallpaperCycleWorker", "Fetching all wallpapers from server")
                val wallpapers = fetchWallpapersList(serverUrl, token)
                wallpapers.forEach {
                    targetList.add(it.name)
                }
            } catch (e: Exception) {
                Log.e("WallpaperCycleWorker", "Failed to fetch wallpapers list from server: ${e.message}")
            }
        } else {
            val playlistJson = prefs.getString("playlist", "[]") ?: "[]"
            try {
                val arr = JSONArray(playlistJson)
                for (i in 0 until arr.length()) {
                    targetList.add(arr.getString(i))
                }
            } catch (e: Exception) {
                Log.e("WallpaperCycleWorker", "Failed to parse local playlist: ${e.message}")
            }
        }

        if (targetList.isEmpty()) {
            Log.w("WallpaperCycleWorker", "No wallpapers found to cycle")
            scheduleNext(context, prefs)
            return Result.success()
        }

        // 2. Select next wallpaper path
        val nextPath = if (isShuffle) {
            val randomIdx = (targetList.indices).random()
            targetList[randomIdx]
        } else {
            if (currentIndex >= targetList.size) {
                currentIndex = 0
            }
            val path = targetList[currentIndex]
            currentIndex = (currentIndex + 1) % targetList.size
            prefs.edit().putInt("cycle_index", currentIndex).apply()
            path
        }

        Log.d("WallpaperCycleWorker", "Picked wallpaper to set: $nextPath")

        // 3. Download and set wallpaper
        try {
            val bitmap = fetchBitmapFromUrl(serverUrl, token, nextPath, isThumbnail = false)
            if (bitmap != null) {
                val destString = prefs.getString("cycle_destination", "Both") ?: "Both"
                val destination = when (destString) {
                    "Home" -> WallpaperDestination.Home
                    "Lock" -> WallpaperDestination.Lock
                    else -> WallpaperDestination.Both
                }
                
                val savedArr = PrefsManager.getWallpaperArrangement(context, nextPath)
                val scale = savedArr?.scale ?: 1.0f
                val panX = savedArr?.panX ?: 0.0f
                val panY = savedArr?.panY ?: 0.0f

                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
                
                val applied = applyCroppedWallpaper(
                    context = context,
                    source = bitmap,
                    scale = scale,
                    panX = panX,
                    panY = panY,
                    screenAspectRatio = screenAspectRatio,
                    destination = destination
                )
                Log.d("WallpaperCycleWorker", "Wallpaper applied: $applied (cropped = true)")
                if (applied) {
                    PrefsManager.saveCurrentWallpaper(context, nextPath)
                    
                    val upcomingPath = if (isShuffle) {
                        "Random (Shuffle Mode)"
                    } else {
                        if (currentIndex < targetList.size) {
                            targetList[currentIndex]
                        } else {
                            targetList.firstOrNull() ?: ""
                        }
                    }
                    PrefsManager.saveNextWallpaper(context, upcomingPath)
                }
            } else {
                Log.e("WallpaperCycleWorker", "Failed to load bitmap stream for: $nextPath")
            }
        } catch (e: Exception) {
            Log.e("WallpaperCycleWorker", "Error updating wallpaper: ${e.message}")
        }

        // 4. Schedule next run
        scheduleNext(context, prefs)

        return Result.success()
    }

    private fun scheduleNext(context: Context, prefs: android.content.SharedPreferences) {
        // Restart the foreground service to schedule the next cycle
        WallpaperCycleService.start(context)
    }
}
