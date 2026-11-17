package dev.weexnes.clyocloud.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Streams one file to /api/upload as multipart/form-data.
 *
 * The body is sent in chunks (never buffered whole), so any file size works. Note the server
 * overwrites a file of the same name in the target folder; use [UniqueNamer] to avoid that.
 */
object Uploader {
    private const val CHUNK = 64 * 1024

    suspend fun upload(
        connection: CloudConnection,
        folder: String,
        fileName: String,
        mimeType: String,
        size: Long,
        open: () -> InputStream?,
        onProgress: (Int) -> Unit = {},
    ): Unit = withContext(Dispatchers.IO) {
        val boundary = "ClyoBoundary${System.nanoTime()}"
        val conn = URL("${connection.serverUrl}/api/upload").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 120_000
        conn.setChunkedStreamingMode(CHUNK)
        conn.setRequestProperty("Authorization", "Bearer ${connection.token}")
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        try {
            val safeName = fileName.replace("\"", "_").replace("\r", "").replace("\n", "").ifBlank { "file" }
            val input = open() ?: throw java.io.IOException("Could not read $fileName")
            input.use { src ->
                conn.outputStream.use { out ->
                    out.write(
                        ("--$boundary\r\nContent-Disposition: form-data; name=\"path\"\r\n\r\n$folder\r\n" +
                            "--$boundary\r\nContent-Disposition: form-data; name=\"files\"; filename=\"$safeName\"\r\n" +
                            "Content-Type: $mimeType\r\n\r\n").toByteArray(Charsets.UTF_8),
                    )
                    val buffer = ByteArray(CHUNK)
                    var sent = 0L
                    var lastPercent = -1
                    while (true) {
                        val n = src.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        sent += n
                        if (size > 0) {
                            val percent = ((sent * 100) / size).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) { lastPercent = percent; onProgress(percent) }
                        }
                    }
                    out.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
                }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val message = try {
                    org.json.JSONObject(conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "").optString("statusMessage")
                } catch (_: Exception) { "" }
                throw ApiException(code, message.ifBlank { "Upload failed (HTTP $code)" })
            }
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Picks upload names that don't overwrite different files already in a folder.
 * Lists each folder once. A file with the same name and size is treated as already uploaded.
 */
class UniqueNamer(private val api: ClyoApi) {
    private val folders = HashMap<String, MutableMap<String, Long>>()

    private suspend fun existing(folder: String): MutableMap<String, Long> = folders.getOrPut(folder) {
        try {
            api.files(folder).filter { !it.isFolder }.associate { it.name.lowercase() to it.size }.toMutableMap()
        } catch (e: ApiException) {
            if (e.code == 404) mutableMapOf() else throw e
        }
    }

    sealed class Decision {
        data object AlreadyThere : Decision()
        data class UploadAs(val name: String) : Decision()
    }

    suspend fun decide(folder: String, name: String, size: Long): Decision {
        val files = existing(folder)
        val sameSize = files[name.lowercase()]
        if (sameSize != null && size > 0 && sameSize == size) return Decision.AlreadyThere
        if (sameSize == null) return Decision.UploadAs(name)
        val base = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        var i = 2
        while (true) {
            val candidate = "$base ($i)$ext"
            val other = files[candidate.lowercase()]
            if (other == null) return Decision.UploadAs(candidate)
            if (size > 0 && other == size) return Decision.AlreadyThere
            i++
        }
    }

    /** Record a finished upload so later decisions in this run see it. */
    suspend fun uploaded(folder: String, name: String, size: Long) {
        existing(folder)[name.lowercase()] = size
    }
}
