package dev.weexnes.clyocloud.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.WindowManager
import dev.weexnes.clyocloud.data.ClyoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Downloads a wallpaper, frames it for this screen and sets it. */
object WallpaperApplier {

    /** Full screen size in pixels (including system bars), portrait-oriented. */
    fun screenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(WindowManager::class.java)
        val b = wm.maximumWindowMetrics.bounds
        val w = minOf(b.width(), b.height())
        val h = maxOf(b.width(), b.height())
        return w to h
    }

    /** Downloads the original file to the cache and returns it. */
    suspend fun download(context: Context, api: ClyoApi, path: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "wallpaper").apply { mkdirs() }
        val out = File(dir, "source.tmp")
        val conn = URL(api.fileUrl(path)).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 120_000
        conn.setRequestProperty("Authorization", "Bearer ${api.connection.token}")
        try {
            val code = conn.responseCode
            if (code == 404) throw WallpaperMissingException(path)
            if (code !in 200..299) throw IOException("Server returned HTTP $code for $path")
            conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it, 64 * 1024) } }
        } finally {
            conn.disconnect()
        }
        out
    }

    /**
     * Decodes [file] at a size that still covers the screen at the given zoom, crops it to the
     * screen's aspect ratio using [framing], scales it to exactly the screen size and sets it.
     */
    suspend fun apply(context: Context, file: File, framing: Framing, target: WallpaperTarget): Unit = withContext(Dispatchers.Default) {
        val (screenW, screenH) = screenSize(context)
        val bitmap = decodeForScreen(file, screenW, screenH, framing.scale) ?: throw IOException("This file is not an image Android can read")
        try {
            val cropped = frame(bitmap, screenW, screenH, framing)
            val wm = WallpaperManager.getInstance(context)
            val flags = when (target) {
                WallpaperTarget.Home -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.Lock -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.Both -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            // allowBackup = true; the bitmap is exactly screen-sized, so the launcher shows it as framed
            wm.setBitmap(cropped, null, true, flags)
            if (cropped !== bitmap) cropped.recycle()
        } finally {
            bitmap.recycle()
        }
    }

    /** Never decodes more pixels than needed: at most ~2x the screen's long edge at the chosen zoom. */
    private fun decodeForScreen(file: File, screenW: Int, screenH: Int, scale: Float): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        // Size needed so that the cropped region still has at least screen resolution
        val cover = maxOf(screenW.toFloat() / bounds.outWidth, screenH.toFloat() / bounds.outHeight) * scale.coerceAtLeast(1f)
        var sample = 1
        while (cover * sample * 2 <= 1f) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }

    /**
     * Centre-crop to the screen aspect ratio, zoomed by framing.scale and moved by framing.pan
     * (-1 = far left/top, 1 = far right/bottom). Matches the preview in the framing editor.
     */
    fun frame(src: Bitmap, screenW: Int, screenH: Int, framing: Framing): Bitmap {
        val scale = framing.scale.coerceIn(1f, 5f)
        val cover = maxOf(screenW.toFloat() / src.width, screenH.toFloat() / src.height) * scale
        val cropW = screenW / cover
        val cropH = screenH / cover
        val maxX = (src.width - cropW) / 2f
        val maxY = (src.height - cropH) / 2f
        val cx = src.width / 2f + framing.panX.coerceIn(-1f, 1f) * maxX
        val cy = src.height / 2f + framing.panY.coerceIn(-1f, 1f) * maxY
        val left = (cx - cropW / 2f).coerceIn(0f, (src.width - cropW).coerceAtLeast(0f))
        val top = (cy - cropH / 2f).coerceIn(0f, (src.height - cropH).coerceAtLeast(0f))
        val srcRect = Rect(left.toInt(), top.toInt(), (left + cropW).toInt().coerceAtMost(src.width), (top + cropH).toInt().coerceAtMost(src.height))
        val out = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, srcRect, RectF(0f, 0f, screenW.toFloat(), screenH.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }
}

class WallpaperMissingException(path: String) : IOException("$path is no longer on the server")
