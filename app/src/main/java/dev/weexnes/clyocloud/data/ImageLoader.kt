package dev.weexnes.clyocloud.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Loads server images with a memory cache (by bytes, not entries) and a size-capped disk cache.
 *
 * Images are always decoded downsampled to the size they are shown at, so a 48 MP photo in a
 * grid cell costs a few hundred KB instead of ~190 MB and can't crash the app.
 */
object ImageLoader {
    private const val DISK_CACHE_MAX_BYTES = 200L * 1024 * 1024

    private val memory = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    // One download per URL at a time; concurrent callers wait and then hit the cache
    private val locks = HashMap<String, Mutex>()
    private fun lockFor(key: String) = synchronized(locks) { locks.getOrPut(key) { Mutex() } }

    private fun diskDir(context: Context) = File(context.cacheDir, "images").apply { mkdirs() }

    private fun keyOf(url: String, maxSize: Int): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + "_$maxSize"
    }

    fun cached(url: String, maxSize: Int): Bitmap? = memory.get(keyOf(url, maxSize))

    /**
     * @param maxSize longest edge to decode to, in pixels
     * @param diskCache keep the downsampled result on disk (thumbnails and previews)
     */
    suspend fun load(context: Context, url: String, token: String?, maxSize: Int, diskCache: Boolean = true): Bitmap? = withContext(Dispatchers.IO) {
        val key = keyOf(url, maxSize)
        memory.get(key)?.let { return@withContext it }

        lockFor(key).withLock {
            memory.get(key)?.let { return@withLock it }

            val diskFile = File(diskDir(context), key)
            if (diskCache && diskFile.exists()) {
                decodeFile(diskFile, maxSize)?.let {
                    diskFile.setLastModified(System.currentTimeMillis())
                    memory.put(key, it)
                    return@withLock it
                }
            }

            // Download to a temp file first: decoding twice (bounds, then pixels) needs the bytes twice
            val tmp = File.createTempFile("img", ".tmp", context.cacheDir)
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
                try {
                    if (conn.responseCode !in 200..299) return@withLock null
                    conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it, 64 * 1024) } }
                } finally {
                    conn.disconnect()
                }
                val bitmap = decodeFile(tmp, maxSize) ?: return@withLock null
                memory.put(key, bitmap)
                if (diskCache) {
                    try {
                        diskFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, it) }
                        trimDisk(context)
                    } catch (_: Exception) {}
                }
                bitmap
            } catch (_: Exception) {
                null
            } finally {
                tmp.delete()
            }
        }
    }

    /** Decodes with inSampleSize so the longest edge is close to (and not below) maxSize. */
    fun decodeFile(file: File, maxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= maxSize) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun trimDisk(context: Context) {
        val files = diskDir(context).listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= DISK_CACHE_MAX_BYTES) return
        for (f in files.sortedBy { it.lastModified() }) {
            total -= f.length()
            f.delete()
            if (total <= DISK_CACHE_MAX_BYTES * 3 / 4) break
        }
    }

    fun diskUsage(context: Context): Long = diskDir(context).listFiles()?.sumOf { it.length() } ?: 0L

    fun clear(context: Context) {
        memory.evictAll()
        diskDir(context).listFiles()?.forEach { it.delete() }
    }
}
