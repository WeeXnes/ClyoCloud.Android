package dev.weexnes.clyocloudandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy

class WallpaperCycleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WallpaperCycleReceiver", "Alarm received, triggering wallpaper cycle worker")
        
        val workRequest = OneTimeWorkRequest.Builder(WallpaperCycleWorker::class.java)
            .addTag("WallpaperCycleWork")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "WallpaperCycleWorkName",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
