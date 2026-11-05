package dev.weexnes.clyocloud.data

import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiException(val code: Int, message: String) : IOException(message)

/** Adds a scheme, removes a trailing slash and maps localhost on the emulator. */
fun normalizeServerUrl(input: String): String {
    var url = input.trim()
    if (url.isEmpty()) return url
    if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
    url = url.trimEnd('/')
    val isEmulator = Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for x86")
    if (isEmulator) url = url.replace("://localhost", "://10.0.2.2").replace("://127.0.0.1", "://10.0.2.2")
    return url
}

/** Encodes each segment of a storage path for use in a URL path. */
fun encodePath(path: String) = path.split('/').filter { it.isNotEmpty() }.joinToString("/") { Uri.encode(it) }

private val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

/** Low-level request helper shared by every endpoint. */
internal object Http {
    fun open(url: String, method: String, token: String?, readTimeoutMs: Int = 30_000): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15_000
        conn.readTimeout = readTimeoutMs
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("X-Device-Model", deviceModel)
        if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
        return conn
    }

    /** Sends an optional JSON body, returns the response text; throws [ApiException] on non-2xx. */
    fun send(conn: HttpURLConnection, body: Any? = null): String {
        try {
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setFixedLengthStreamingMode(bytes.size)
                conn.outputStream.use { it.write(bytes) }
            }
            val code = conn.responseCode
            if (code in 200..299) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
            val errText = try { conn.errorStream?.bufferedReader()?.use { it.readText() } } catch (_: Exception) { null }
            val message = try {
                val obj = JSONObject(errText ?: "")
                obj.optString("statusMessage").ifBlank { obj.optString("message") }
            } catch (_: Exception) { "" }
            throw ApiException(code, message.ifBlank {
                when (code) {
                    401 -> "Your session has expired. Sign in again."
                    403 -> "Access denied"
                    404 -> "Not found"
                    else -> "Server returned HTTP $code"
                }
            })
        } finally {
            conn.disconnect()
        }
    }
}

// ─── Unauthenticated calls ───────────────────────────────────────────────────

object AuthApi {
    suspend fun login(serverUrl: String, username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            val text = Http.send(
                Http.open("$serverUrl/api/login", "POST", null),
                JSONObject().put("username", username).put("password", password),
            )
            val json = JSONObject(text)
            if (json.optBoolean("requires2fa", false)) LoginResult.Requires2Fa(json.getString("tempToken"))
            else LoginResult.Success(json.getString("token"))
        } catch (e: ApiException) {
            LoginResult.Error(if (e.code == 401) "Invalid username or password" else e.message ?: "Sign-in failed")
        } catch (e: Exception) {
            LoginResult.Error("Could not reach the server: ${e.message ?: "connection error"}")
        }
    }

    suspend fun verify2fa(serverUrl: String, tempToken: String, code: String): String? = withContext(Dispatchers.IO) {
        try {
            JSONObject(Http.send(Http.open("$serverUrl/api/2fa/verify-login", "POST", null), JSONObject().put("tempToken", tempToken).put("code", code))).getString("token")
        } catch (_: Exception) {
            null
        }
    }

    /** True when the token is accepted; false when it was rejected; null when the server could not be reached. */
    suspend fun verifyToken(serverUrl: String, token: String): Boolean? = withContext(Dispatchers.IO) {
        try {
            Http.send(Http.open("$serverUrl/api/verify", "GET", token, 8_000))
            true
        } catch (e: ApiException) {
            if (e.code == 401 || e.code == 403) false else null
        } catch (_: Exception) {
            null
        }
    }
}

// ─── Authenticated calls ─────────────────────────────────────────────────────

class ClyoApi(val connection: CloudConnection) {
    private val base = connection.serverUrl
    private val token = connection.token

    private suspend fun get(path: String, readTimeoutMs: Int = 30_000): String = withContext(Dispatchers.IO) {
        Http.send(Http.open("$base$path", "GET", token, readTimeoutMs))
    }

    private suspend fun call(method: String, path: String, body: Any?, readTimeoutMs: Int = 30_000): String = withContext(Dispatchers.IO) {
        Http.send(Http.open("$base$path", method, token, readTimeoutMs), body)
    }

    // URLs used directly by image loaders, players and the download manager
    fun fileUrl(path: String) = "$base/file/${encodePath(path)}"
    fun thumbnailUrl(path: String) = "$base/thumbnail/${encodePath(path)}"
    fun previewUrl(path: String) = "$base/preview/${encodePath(path)}"
    fun streamUrl(path: String) = "$base/stream/${encodePath(path)}"
    fun publicUrl(path: String) = "$base/p/${Uri.encode(connection.username)}/${Uri.encode(path)}"
    fun zipUrl(folderPath: String) = "$base/api/zip?path=${Uri.encode(folderPath)}"
    fun bulkDownloadUrl(folderPath: String, names: List<String>) =
        "$base/api/bulk-download?files=${Uri.encode(JSONArray(names).toString())}&path=${Uri.encode(folderPath)}"

    // Files
    suspend fun files(path: String): List<CloudFile> {
        val arr = JSONArray(get("/api/files?path=${Uri.encode(path)}"))
        return (0 until arr.length()).map { parseCloudFile(arr.getJSONObject(it), path) }
    }

    suspend fun favorites(sync: Boolean = false): List<CloudFile> {
        val arr = JSONArray(get("/api/favorites" + if (sync) "?sync=true" else "", 120_000))
        return (0 until arr.length()).map { parseCloudFile(arr.getJSONObject(it), null) }
    }

    suspend fun wallpapers(sync: Boolean = false): List<CloudFile> {
        val arr = JSONArray(get("/api/wallpapers" + if (sync) "?sync=true" else "", 120_000))
        return (0 until arr.length()).map { parseCloudFile(arr.getJSONObject(it), null) }
    }

    suspend fun photoWidgets(): List<CloudFile> {
        val arr = JSONArray(get("/api/photo-widgets", 120_000))
        return (0 until arr.length()).map { parseCloudFile(arr.getJSONObject(it), null) }
    }

    suspend fun createFolder(parentPath: String, name: String) {
        call("POST", "/api/folder", JSONObject().put("name", name).put("path", parentPath))
    }

    suspend fun delete(path: String) {
        call("DELETE", "/api/files", JSONObject().put("path", path))
    }

    suspend fun rename(path: String, newName: String) {
        call("POST", "/api/rename", JSONObject().put("oldPath", path).put("newName", newName))
    }

    suspend fun move(paths: List<String>, destination: String) {
        call("POST", "/api/move", JSONObject().put("paths", JSONArray(paths)).put("destination", destination))
    }

    /** Changes only the fields that are not null. */
    suspend fun updateMeta(
        path: String,
        isPublic: Boolean? = null,
        isFavourite: Boolean? = null,
        isWallpaper: Boolean? = null,
        isPhotoWidget: Boolean? = null,
        description: String? = null,
        tags: List<TagItem>? = null,
        previewImage: String? = null,
        clearPreviewImage: Boolean = false,
    ) {
        val body = JSONObject().put("fullPath", path)
        isPublic?.let { body.put("isPublic", it) }
        isFavourite?.let { body.put("isFavourite", it) }
        isWallpaper?.let { body.put("isWallpaper", it) }
        isPhotoWidget?.let { body.put("isPhotoWidget", it) }
        description?.let { body.put("description", it) }
        if (clearPreviewImage) body.put("previewImage", JSONObject.NULL) else previewImage?.let { body.put("previewImage", it) }
        tags?.let { list -> body.put("tags", JSONArray(list.map { JSONObject().put("name", it.name).put("color", it.color) })) }
        call("POST", "/api/file-meta", body)
    }

    // Media
    suspend fun videos(search: String? = null, page: Int = 0, limit: Int = 0, shuffle: Boolean = false): MediaPage {
        val params = buildList {
            if (shuffle) add("shuffle=true")
            if (page > 0) add("page=$page")
            if (limit > 0) add("limit=$limit")
            if (!search.isNullOrBlank()) add("search=${Uri.encode(search)}")
        }
        val text = get("/api/videos" + if (params.isEmpty()) "" else "?" + params.joinToString("&"), 120_000).trim()
        return if (text.startsWith("{")) {
            val root = JSONObject(text)
            val items = root.optJSONArray("items") ?: JSONArray()
            MediaPage((0 until items.length()).map { parseMediaItem(items.getJSONObject(it)) }, root.optInt("total", items.length()))
        } else {
            val arr = JSONArray(text)
            val items = (0 until arr.length()).map { parseMediaItem(arr.getJSONObject(it)) }
            MediaPage(items, items.size)
        }
    }

    // Tags
    suspend fun tags(): List<TagItem> = parseTags(JSONArray(get("/api/tags")))

    suspend fun saveTags(tags: List<TagItem>) {
        call("POST", "/api/tags", JSONObject().put("tags", JSONArray(tags.map { JSONObject().put("name", it.name).put("color", it.color) })))
    }

    // Recycle bin
    suspend fun trash(): List<TrashItem> {
        val arr = JSONArray(get("/api/trash"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            TrashItem(o.getString("id"), o.optString("originalPath"), o.optLong("trashedAt"), o.optLong("expiresAt"), o.optLong("size"), o.optBoolean("isFolder"))
        }.sortedByDescending { it.trashedAt }
    }

    suspend fun restoreFromTrash(id: String) { call("POST", "/api/trash-restore", JSONObject().put("id", id)) }
    suspend fun deleteFromTrash(id: String) { call("DELETE", "/api/trash", JSONObject().put("id", id)) }
    suspend fun emptyTrash() { call("DELETE", "/api/trash", JSONObject().put("emptyAll", true)) }

    // Settings
    suspend fun userSettings(): JSONObject = JSONObject(get("/api/user-settings"))

    // ComfyUI wallpaper pipeline (server/api/pipeline)
    suspend fun pipelineOptions(): Pair<List<UpscaleModel>, List<ScenePreset>> {
        val root = JSONObject(get("/api/pipeline/models", 15_000))
        val models = root.optJSONArray("models") ?: JSONArray()
        val presets = root.optJSONArray("presets") ?: JSONArray()
        return (0 until models.length()).map {
            val o = models.getJSONObject(it)
            UpscaleModel(o.getString("id"), o.optString("name", o.getString("id")), o.optInt("scale", 4))
        } to (0 until presets.length()).map {
            val o = presets.getJSONObject(it)
            ScenePreset(o.getString("id"), o.optString("name"), o.optString("description"), o.optString("positive"), o.optString("negative"))
        }
    }

    suspend fun testComfyConnection(url: String?): ComfyConnectionResult {
        val body = JSONObject()
        if (!url.isNullOrBlank()) body.put("url", url)
        val o = JSONObject(call("POST", "/api/pipeline/test-connection", body, 20_000))
        val devices = o.optJSONArray("devices")
        return ComfyConnectionResult(
            o.optBoolean("ok"), o.optStringOrNull("version"),
            if (devices == null) emptyList() else (0 until devices.length()).map { devices.getString(it) },
            o.optStringOrNull("error"),
        )
    }

    suspend fun startWallpaperJob(path: String, modelName: String?, scenePreset: String?, positive: String?, negative: String?): PipelineJob {
        val body = JSONObject().put("filePath", path)
        modelName?.let { body.put("modelName", it) }
        scenePreset?.let { body.put("scenePreset", it) }
        positive?.takeIf { it.isNotBlank() }?.let { body.put("promptOverride", it) }
        negative?.takeIf { it.isNotBlank() }?.let { body.put("negativeOverride", it) }
        return parsePipelineJob(JSONObject(call("POST", "/api/pipeline/wallpaper-v2", body)).getJSONObject("job"))
    }

    suspend fun pipelineJob(id: String): PipelineJob =
        parsePipelineJob(JSONObject(get("/api/pipeline/status?jobId=${Uri.encode(id)}", 15_000)).getJSONObject("job"))
}
