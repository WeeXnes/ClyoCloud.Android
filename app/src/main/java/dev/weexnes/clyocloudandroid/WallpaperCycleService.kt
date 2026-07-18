package dev.weexnes.clyocloudandroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.json.JSONArray

class WallpaperCycleService : Service() {

    companion object {
        const val TAG = "WallpaperCycleService"
        const val CHANNEL_ID = "wallpaper_cycle_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "dev.weexnes.clyocloudandroid.START_CYCLE"
        const val ACTION_STOP = "dev.weexnes.clyocloudandroid.STOP_CYCLE"
        const val ACTION_FORCE_CYCLE = "dev.weexnes.clyocloudandroid.FORCE_CYCLE"

        fun start(context: Context) {
            val intent = Intent(context, WallpaperCycleService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WallpaperCycleService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun forceCycle(context: Context) {
            val intent = Intent(context, WallpaperCycleService::class.java).apply {
                action = ACTION_FORCE_CYCLE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var cycleRunnable: Runnable? = null
    private var notificationUpdateRunnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stopping cycle service")
                cancelScheduledCycle()
                PrefsManager.saveCycleNextExecution(this, 0L)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_FORCE_CYCLE -> {
                Log.d(TAG, "Force cycle requested")
                startForeground(NOTIFICATION_ID, buildNotification("Changing wallpaper..."))
                cancelScheduledCycle()
                executeCycleNow()
                return START_STICKY
            }
            ACTION_START, null -> {
                Log.d(TAG, "Starting/resuming cycle service")
                val prefs = getSharedPreferences("clyo_wallpaper_prefs", Context.MODE_PRIVATE)
                val isEnabled = prefs.getBoolean("cycle_enabled", false)
                if (!isEnabled) {
                    Log.d(TAG, "Cycle is disabled, stopping")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification("Wallpaper cycle active"))
                scheduleNextCycle()
                startNotificationUpdates()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private fun scheduleNextCycle() {
        cancelScheduledCycle()
        val prefs = getSharedPreferences("clyo_wallpaper_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("cycle_interval", 15).coerceAtLeast(1)
        val delayMs = intervalMinutes * 60 * 1000L
        val nextTime = System.currentTimeMillis() + delayMs
        PrefsManager.saveCycleNextExecution(this, nextTime)

        Log.d(TAG, "Scheduling next cycle in $intervalMinutes minutes ($delayMs ms)")
        cycleRunnable = Runnable {
            executeCycleNow()
        }
        handler.postDelayed(cycleRunnable!!, delayMs)
    }

    private fun cancelScheduledCycle() {
        cycleRunnable?.let { handler.removeCallbacks(it) }
        cycleRunnable = null
    }

    private fun executeCycleNow() {
        Log.d(TAG, "Executing wallpaper cycle now")
        // Acquire a wake lock to keep CPU on during network+wallpaper operations
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ClyoCloudAndroid::CycleWakeLock"
        ).apply {
            acquire(60 * 1000L) // 60 second timeout
        }

        Thread {
            try {
                performWallpaperChange()
            } catch (e: Exception) {
                Log.e(TAG, "Error during wallpaper cycle: ${e.message}", e)
            } finally {
                wakeLock?.let {
                    if (it.isHeld) it.release()
                }
                wakeLock = null
                // Schedule next on the main thread
                handler.post {
                    val prefs = getSharedPreferences("clyo_wallpaper_prefs", Context.MODE_PRIVATE)
                    val isEnabled = prefs.getBoolean("cycle_enabled", false)
                    if (isEnabled) {
                        scheduleNextCycle()
                        updateNotification()
                    } else {
                        PrefsManager.saveCycleNextExecution(this, 0L)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }.start()
    }

    private fun performWallpaperChange() = runBlocking {
        val context = applicationContext
        val prefs = context.getSharedPreferences("clyo_wallpaper_prefs", Context.MODE_PRIVATE)

        val serverUrl = prefs.getString("server_url", null)
        val token = prefs.getString("token", null)
        if (serverUrl.isNullOrBlank() || token.isNullOrBlank()) {
            Log.e(TAG, "Missing connection credentials")
            return@runBlocking
        }

        val source = prefs.getString("cycle_source", "Playlist") ?: "Playlist"
        val isShuffle = prefs.getBoolean("cycle_shuffle", false)
        var currentIndex = prefs.getInt("cycle_index", 0)

        // 1. Determine target wallpaper paths list
        val targetList = mutableListOf<String>()
        if (source == "All") {
            try {
                Log.d(TAG, "Fetching all wallpapers from server")
                val wallpapers = fetchWallpapersList(serverUrl, token)
                wallpapers.forEach { targetList.add(it.name) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch wallpapers list: ${e.message}")
            }
        } else {
            val playlistJson = prefs.getString("playlist", "[]") ?: "[]"
            try {
                val arr = JSONArray(playlistJson)
                for (i in 0 until arr.length()) {
                    targetList.add(arr.getString(i))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse local playlist: ${e.message}")
            }
        }

        if (targetList.isEmpty()) {
            Log.w(TAG, "No wallpapers found to cycle")
            return@runBlocking
        }

        // 2. Select next wallpaper path
        val nextPath = if (isShuffle) {
            targetList[(targetList.indices).random()]
        } else {
            if (currentIndex >= targetList.size) currentIndex = 0
            val path = targetList[currentIndex]
            currentIndex = (currentIndex + 1) % targetList.size
            prefs.edit().putInt("cycle_index", currentIndex).apply()
            path
        }

        Log.d(TAG, "Picked wallpaper to set: $nextPath")

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

                Log.d(TAG, "Wallpaper applied: $applied (cropped = true)")
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
                Log.e(TAG, "Failed to load bitmap for: $nextPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wallpaper: ${e.message}", e)
        }
    }

    // Notification channel + builder
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wallpaper Cycle",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for wallpaper auto-cycling"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Clyo Wallpaper")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nextTime = PrefsManager.getCycleNextExecution(this)
        val remaining = if (nextTime > 0) {
            val secs = ((nextTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            val mins = secs / 60
            val s = secs % 60
            "Next change in ${mins}:${String.format("%02d", s)}"
        } else {
            "Wallpaper cycle active"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(remaining))
    }

    private fun startNotificationUpdates() {
        stopNotificationUpdates()
        notificationUpdateRunnable = object : Runnable {
            override fun run() {
                updateNotification()
                handler.postDelayed(this, 30_000) // update every 30s
            }
        }
        handler.post(notificationUpdateRunnable!!)
    }

    private fun stopNotificationUpdates() {
        notificationUpdateRunnable?.let { handler.removeCallbacks(it) }
        notificationUpdateRunnable = null
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        cancelScheduledCycle()
        stopNotificationUpdates()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
