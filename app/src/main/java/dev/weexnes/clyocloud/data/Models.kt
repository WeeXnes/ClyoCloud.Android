package dev.weexnes.clyocloud.data

import org.json.JSONArray
import org.json.JSONObject

data class CloudConnection(
    val serverUrl: String,
    val username: String,
    val token: String,
)

data class TagItem(val name: String, val color: String)

data class CloudFile(
    /** Display name (last path segment) */
    val name: String,
    /** Path relative to the user's storage root, e.g. "Photos/2024/a.jpg" */
    val path: String,
    val size: Long,
    val type: String,
    val lastModified: Long,
    val isFolder: Boolean,
    val isPublic: Boolean = false,
    val isFavourite: Boolean = false,
    val isWallpaper: Boolean = false,
    val isPhotoWidget: Boolean = false,
    val description: String? = null,
    val tags: List<TagItem> = emptyList(),
    val previewImage: String? = null,
)

data class MediaItem(
    /** "video" or "slideshow" */
    val type: String,
    val name: String,
    val path: String,
    val images: List<String>,
    val size: Long,
    val lastModified: Long,
    val isPublic: Boolean,
    val isFavourite: Boolean,
    val description: String?,
    val tags: List<TagItem>,
)

data class MediaPage(val items: List<MediaItem>, val total: Int)

data class TrashItem(
    val id: String,
    val originalPath: String,
    val trashedAt: Long,
    val expiresAt: Long,
    val size: Long,
    val isFolder: Boolean,
)

sealed class LoginResult {
    data class Success(val token: String) : LoginResult()
    data class Requires2Fa(val tempToken: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

// ─── ComfyUI wallpaper pipeline ──────────────────────────────────────────────

data class UpscaleModel(val id: String, val name: String, val scale: Int)

data class ScenePreset(val id: String, val name: String, val description: String, val positive: String, val negative: String)

data class PipelineJob(
    val id: String,
    val status: String,
    val progress: Int,
    val message: String,
    val outputName: String?,
    val outputPath: String?,
    val error: String?,
) {
    val isFinished get() = status == "completed" || status == "failed"
}

data class ComfyConnectionResult(val ok: Boolean, val version: String?, val devices: List<String>, val error: String?)

// ─── JSON parsing ────────────────────────────────────────────────────────────

internal fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotEmpty() } else null

internal fun parseTags(arr: JSONArray?): List<TagItem> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val name = o.optString("name")
        if (name.isBlank()) null else TagItem(name, o.optString("color", "#808080"))
    }
}

/** `name` is either a bare file name (inside [parentPath]) or a full relative path (when [parentPath] is null). */
internal fun parseCloudFile(obj: JSONObject, parentPath: String?): CloudFile {
    val meta = obj.optJSONObject("meta")
    val rawName = obj.optString("name")
    val path = if (parentPath == null) rawName else if (parentPath.isEmpty()) rawName else "$parentPath/$rawName"
    return CloudFile(
        name = rawName.substringAfterLast('/'),
        path = path,
        size = obj.optLong("size", 0L),
        type = obj.optString("type", ""),
        lastModified = obj.optLong("lastModified", 0L),
        isFolder = obj.optBoolean("isFolder", false),
        isPublic = meta?.optBoolean("isPublic", false) ?: false,
        isFavourite = meta?.optBoolean("isFavourite", false) ?: false,
        isWallpaper = meta?.optBoolean("isWallpaper", false) ?: false,
        isPhotoWidget = meta?.optBoolean("isPhotoWidget", false) ?: false,
        description = meta?.optStringOrNull("description"),
        tags = parseTags(meta?.optJSONArray("tags")),
        previewImage = meta?.optStringOrNull("previewImage"),
    )
}

internal fun parseMediaItem(obj: JSONObject): MediaItem {
    val meta = obj.optJSONObject("meta")
    val images = obj.optJSONArray("images")
    return MediaItem(
        type = obj.optString("type", "video"),
        name = obj.optString("name"),
        path = obj.optString("fullPath"),
        images = if (images == null) emptyList() else (0 until images.length()).map { images.getString(it) },
        size = obj.optLong("size", 0L),
        lastModified = obj.optLong("lastModified", 0L),
        isPublic = meta?.optBoolean("isPublic", false) ?: false,
        isFavourite = meta?.optBoolean("isFavourite", false) ?: false,
        description = meta?.optStringOrNull("description"),
        tags = parseTags(meta?.optJSONArray("tags")),
    )
}

internal fun parsePipelineJob(obj: JSONObject) = PipelineJob(
    id = obj.optString("id"),
    status = obj.optString("status", "starting"),
    progress = obj.optInt("progress", 0),
    message = obj.optString("message", ""),
    outputName = obj.optStringOrNull("outputName"),
    outputPath = obj.optStringOrNull("outputPath"),
    error = obj.optStringOrNull("error"),
)

// ─── File type helpers (same lists as the web UI) ────────────────────────────

private val VIDEO_EXT = setOf("mp4", "webm", "mov", "mkv", "3gp", "avi", "m4v")
private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
private val THUMB_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "mp4", "webm", "mov", "mkv")
private val WALLPAPER_GEN_EXT = setOf("jpg", "jpeg", "png", "webp")

fun extensionOf(name: String) = name.substringAfterLast('.', "").lowercase()
fun isVideoFile(name: String) = extensionOf(name) in VIDEO_EXT
fun isImageFile(name: String) = extensionOf(name) in IMAGE_EXT
fun hasThumbnail(name: String) = extensionOf(name) in THUMB_EXT
/** Formats the ComfyUI wallpaper pipeline accepts. */
fun canGenerateWallpaper(file: CloudFile) = !file.isFolder && extensionOf(file.name) in WALLPAPER_GEN_EXT

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val group = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = size / Math.pow(1024.0, group.toDouble())
    return if (group == 0) "$size B" else String.format(java.util.Locale.US, "%.1f %s", value, units[group])
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "—"
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}
