package dev.weexnes.clyocloud.backup

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import java.io.File

/** Camera backup settings. */
object BackupPrefs {
    private const val PREFS = "clyo_backup_prefs"
    private fun p(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(c: Context) = p(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("enabled", v).apply()

    /** Server folder the backup goes into. */
    fun folder(c: Context) = p(c).getString("folder", "Camera Backup")!!.trim('/').ifBlank { "Camera Backup" }
    fun setFolder(c: Context, v: String) = p(c).edit().putString("folder", v.trim().trim('/')).apply()

    /** Put files into Year/Month subfolders. */
    fun byMonth(c: Context) = p(c).getBoolean("by_month", true)
    fun setByMonth(c: Context, v: Boolean) = p(c).edit().putBoolean("by_month", v).apply()

    fun includeVideos(c: Context) = p(c).getBoolean("videos", true)
    fun setIncludeVideos(c: Context, v: Boolean) = p(c).edit().putBoolean("videos", v).apply()

    fun wifiOnly(c: Context) = p(c).getBoolean("wifi_only", true)
    fun setWifiOnly(c: Context, v: Boolean) = p(c).edit().putBoolean("wifi_only", v).apply()

    fun chargingOnly(c: Context) = p(c).getBoolean("charging_only", false)
    fun setChargingOnly(c: Context, v: Boolean) = p(c).edit().putBoolean("charging_only", v).apply()

    /** Only items added to the phone after this moment (0 = everything). */
    fun since(c: Context) = p(c).getLong("since", 0L)
    fun setSince(c: Context, v: Long) = p(c).edit().putLong("since", v).apply()

    /** Album (bucket) IDs to back up; null = the default, every album under DCIM (camera). */
    fun albums(c: Context): Set<String>? = if (p(c).contains("albums")) p(c).getStringSet("albums", emptySet())!!.toSet() else null
    fun setAlbums(c: Context, v: Set<String>) = p(c).edit().putStringSet("albums", v).apply()

    fun lastRun(c: Context) = p(c).getLong("last_run", 0L)
    fun lastResult(c: Context): String? = p(c).getString("last_result", null)
    fun recordRun(c: Context, result: String) = p(c).edit().putLong("last_run", System.currentTimeMillis()).putString("last_result", result).apply()

    fun register(c: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) = p(c).registerOnSharedPreferenceChangeListener(l)
    fun unregister(c: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) = p(c).unregisterOnSharedPreferenceChangeListener(l)
}

/**
 * Items already backed up, one key per line in app storage, so nothing is uploaded twice.
 * The key includes the modification time: an edited photo is backed up again.
 */
object BackupRegistry {
    private fun file(c: Context) = File(c.filesDir, "camera_backup_done.txt")
    private var cache: MutableSet<String>? = null

    private fun load(c: Context): MutableSet<String> = cache ?: run {
        val f = file(c)
        val set = if (f.exists()) f.readLines().filter { it.isNotBlank() }.toHashSet() else HashSet()
        cache = set
        set
    }

    /** A copy, safe to read while the backup job adds keys. */
    @Synchronized
    fun keys(c: Context): Set<String> = HashSet(load(c))

    @Synchronized
    fun add(c: Context, key: String) {
        if (load(c).add(key)) file(c).appendText(key + "\n")
    }

    @Synchronized
    fun count(c: Context) = load(c).size

    @Synchronized
    fun clear(c: Context) {
        file(c).delete()
        cache = HashSet()
    }
}

data class LocalMedia(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime: String,
    val takenAt: Long,
    val bucketId: String,
    val isVideo: Boolean,
    val modified: Long,
) {
    val key get() = "${if (isVideo) "v" else "i"}:${uri.lastPathSegment}:$modified:$size"
}

data class Album(val id: String, val name: String, val path: String, val count: Int)

/** Reads the phone's photo and video library through MediaStore. */
object MediaLibrary {

    fun hasPermission(c: Context, videos: Boolean = true): Boolean {
        val images = c.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val vids = !videos || c.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        return images && vids
    }

    /** Android 14+ "Select photos" access: only some items are visible. */
    fun hasOnlySelectedAccess(c: Context): Boolean =
        !hasPermission(c, videos = false) &&
            c.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED

    private val collections = listOf(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) to false,
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) to true,
    )

    fun albums(c: Context): List<Album> {
        val map = LinkedHashMap<String, Album>()
        for ((collection, _) in collections) {
            c.contentResolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns.BUCKET_ID, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH),
                null, null, null,
            )?.use { cur ->
                while (cur.moveToNext()) {
                    val id = cur.getString(0) ?: continue
                    val prev = map[id]
                    map[id] = prev?.copy(count = prev.count + 1) ?: Album(id, cur.getString(1) ?: "Unnamed", cur.getString(2) ?: "", 1)
                }
            }
        }
        return map.values.sortedWith(compareBy<Album> { !it.path.startsWith("DCIM/") }.thenByDescending { it.count })
    }

    /** Album IDs used when the user hasn't picked any: everything under DCIM (camera apps). */
    fun defaultAlbums(c: Context): Set<String> = albums(c).filter { it.path.startsWith("DCIM/") }.map { it.id }.toSet()

    /** Items in the chosen albums added since [sinceMillis], oldest first. */
    fun scan(c: Context, albums: Set<String>, includeVideos: Boolean, sinceMillis: Long): List<LocalMedia> {
        val out = ArrayList<LocalMedia>()
        for ((collection, isVideo) in collections) {
            if (isVideo && !includeVideos) continue
            c.contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.BUCKET_ID, MediaStore.MediaColumns.DATE_MODIFIED,
                ),
                "${MediaStore.MediaColumns.DATE_ADDED} >= ? AND ${MediaStore.MediaColumns.IS_PENDING} = 0",
                arrayOf((sinceMillis / 1000).toString()),
                "${MediaStore.MediaColumns.DATE_ADDED} ASC",
            )?.use { cur ->
                while (cur.moveToNext()) {
                    val bucket = cur.getString(6) ?: ""
                    if (bucket !in albums) continue
                    val added = cur.getLong(5) * 1000
                    val taken = if (cur.isNull(4)) added else cur.getLong(4).takeIf { it > 0 } ?: added
                    out += LocalMedia(
                        uri = ContentUris.withAppendedId(collection, cur.getLong(0)),
                        name = cur.getString(1) ?: "media_${cur.getLong(0)}",
                        size = cur.getLong(2),
                        mime = cur.getString(3) ?: if (isVideo) "video/mp4" else "image/jpeg",
                        takenAt = taken,
                        bucketId = bucket,
                        isVideo = isVideo,
                        modified = cur.getLong(7),
                    )
                }
            }
        }
        return out.sortedBy { it.takenAt }
    }

    fun selectedAlbums(c: Context): Set<String> = BackupPrefs.albums(c) ?: defaultAlbums(c)

    /** Items that still need uploading. */
    fun pending(c: Context): List<LocalMedia> {
        if (!hasPermission(c, BackupPrefs.includeVideos(c)) && !hasOnlySelectedAccess(c)) return emptyList()
        val done = BackupRegistry.keys(c)
        return scan(c, selectedAlbums(c), BackupPrefs.includeVideos(c), BackupPrefs.since(c)).filter { it.key !in done }
    }
}
