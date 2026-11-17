package dev.weexnes.clyocloud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.ClyoApi
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.Session
import dev.weexnes.clyocloud.data.formatFileSize
import dev.weexnes.clyocloud.ui.components.ButtonKind
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoDialog
import dev.weexnes.clyocloud.ui.components.ClyoIconButton
import dev.weexnes.clyocloud.ui.components.ClyoTextField
import dev.weexnes.clyocloud.ui.components.EmptyState
import dev.weexnes.clyocloud.ui.components.HairRow
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.MicroLabel
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.components.Spinner
import dev.weexnes.clyocloud.ui.components.StatusLine
import dev.weexnes.clyocloud.ui.components.TopBar
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoCloudTheme
import dev.weexnes.clyocloud.ui.theme.ThemePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One item someone shared with ClyoCloud. */
private data class SharedItem(val uri: Uri?, val text: String?, val name: String, val size: Long)

/**
 * "Share to ClyoCloud" from any app: choose a folder, then the files upload in the background.
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePrefs.load(this)
        Session.load(this)
        enableEdgeToEdge(
            statusBarStyle = if (ThemePrefs.isDark.value) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val items = readIntent(intent)
        setContent {
            ClyoCloudTheme {
                ShareScreen(items, onDone = { finish() }, onOpenApp = {
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                })
            }
        }
    }

    private fun readIntent(intent: Intent): List<SharedItem> {
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
        if (uris.isNotEmpty()) return uris.map { describe(it) }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!text.isNullOrBlank()) {
            val stamp = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault()).format(Date())
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.replace(Regex("[\\\\/:*?\"<>|]"), " ")?.trim()?.take(60)
            return listOf(SharedItem(null, text, "${subject?.ifBlank { null } ?: "Shared note"} $stamp.txt", text.toByteArray().size.toLong()))
        }
        return emptyList()
    }

    private fun describe(uri: Uri): SharedItem {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        var size = -1L
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { i -> c.getString(i)?.takeIf { it.isNotBlank() }?.let { name = it } }
                    c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { i -> if (!c.isNull(i)) size = c.getLong(i) }
                }
            }
        } catch (_: Exception) {}
        return SharedItem(uri, null, name, size)
    }
}

private const val PREFS = "clyo_share_prefs"

// Stored with a leading "/" so the root folder ("") is a valid entry
private fun recentFolders(activity: ComponentActivity): List<String> =
    activity.getSharedPreferences(PREFS, 0).getString("recent", "")!!.split('\n')
        .filter { it.startsWith("/") }.map { it.removePrefix("/") }.distinct().take(5)

private fun rememberFolder(activity: ComponentActivity, path: String) {
    val list = (listOf(path) + recentFolders(activity).filter { it != path }).take(5)
    activity.getSharedPreferences(PREFS, 0).edit().putString("recent", list.joinToString("\n") { "/$it" }).apply()
}

@Composable
private fun ShareScreen(items: List<SharedItem>, onDone: () -> Unit, onOpenApp: () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    val c = Clyo.colors
    val connection = Session.current.value
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar("Save to ClyoCloud", subtitle = when (items.size) {
            0 -> null
            1 -> items.first().name
            else -> "${items.size} files · ${formatFileSize(items.sumOf { it.size.coerceAtLeast(0) })}"
        }, onBack = onDone)

        when {
            connection == null -> EmptyState("Sign in to ClyoCloud first.", actionText = "Open ClyoCloud", onAction = onOpenApp)
            items.isEmpty() -> EmptyState("Nothing to save. Share files, photos or text.", actionText = "Close", onAction = onDone)
            else -> {
                val api = remember { ClyoApi(connection) }
                var path by remember { mutableStateOf(recentFolders(activity).firstOrNull() ?: "") }
                var folders by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
                var loading by remember { mutableStateOf(true) }
                var error by remember { mutableStateOf<String?>(null) }
                var reload by remember { mutableStateOf(0) }
                var newFolder by remember { mutableStateOf<String?>(null) }
                var sending by remember { mutableStateOf(false) }

                LaunchedEffect(path, reload) {
                    loading = true
                    try {
                        folders = api.files(path).filter { it.isFolder }.sortedBy { it.name.lowercase() }
                        error = null
                    } catch (e: Exception) {
                        if (path.isNotEmpty()) path = "" else error = "Could not reach the server"
                    }
                    loading = false
                }

                // Current folder and navigation
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (path.isNotEmpty()) ClyoIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Up", { path = path.substringBeforeLast('/', "") }, tint = c.text)
                    else Spacer(Modifier.size(12.dp))
                    NumText("/$path", color = c.text, modifier = Modifier.weight(1f))
                    ClyoIconButton(Icons.Outlined.CreateNewFolder, "New folder", { newFolder = "" })
                }
                Hairline(Modifier.padding(top = 8.dp))

                Box(Modifier.weight(1f)) {
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner() }
                        error != null -> EmptyState(error!!, actionText = "Retry", onAction = { reload++ })
                        else -> LazyColumn(Modifier.fillMaxSize()) {
                            val recent = recentFolders(activity).filter { it != path }
                            if (path.isEmpty() && recent.isNotEmpty()) {
                                item { MicroLabel("Recent", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)) }
                                items(recent) { r ->
                                    HairRow(r.substringAfterLast('/'), hint = "/$r", leading = { Icon(Icons.Outlined.History, null, tint = c.text3, modifier = Modifier.size(20.dp)) }, onClick = { path = r })
                                    Hairline()
                                }
                                item { MicroLabel("Folders", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)) }
                            }
                            if (folders.isEmpty()) item { SmallText("No subfolders", Modifier.padding(16.dp)) }
                            items(folders, key = { it.path }) { f ->
                                HairRow(f.name, leading = { Icon(Icons.Outlined.Folder, null, tint = c.text2, modifier = Modifier.size(20.dp)) }, onClick = { path = f.path })
                                Hairline()
                            }
                        }
                    }
                }

                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (items.size > 1) StatusLine("${items.size} files", c.text3)
                    ClyoButton(
                        if (path.isEmpty()) "Save to root" else "Save to ${path.substringAfterLast('/')}",
                        {
                            sending = true
                            scope.launch {
                                // Copy while the sharing app's permission is valid, then upload in the background
                                val staged = withContext(Dispatchers.IO) { stage(activity, items) }
                                if (staged.isEmpty()) {
                                    sending = false
                                    error = "Could not read the shared files"
                                    return@launch
                                }
                                rememberFolder(activity, path)
                                BackgroundOperationService.startUpload(activity, connection.serverUrl, connection.token, path, ArrayList(staged))
                                android.widget.Toast.makeText(activity, "Uploading to ClyoCloud", android.widget.Toast.LENGTH_SHORT).show()
                                onDone()
                            }
                        },
                        Modifier.fillMaxWidth(), kind = ButtonKind.Primary, icon = Icons.Outlined.Upload, loading = sending,
                    )
                }

                newFolder?.let { name ->
                    ClyoDialog("New folder", { newFolder = null }, confirmText = "Create", confirmEnabled = name.isNotBlank() && '/' !in name, onConfirm = {
                        scope.launch {
                            try {
                                api.createFolder(path, name.trim())
                                path = if (path.isEmpty()) name.trim() else "$path/${name.trim()}"
                            } catch (_: Exception) {
                                error = "Could not create the folder"
                            }
                            newFolder = null
                        }
                    }) {
                        ClyoTextField(name, { newFolder = it }, label = "Name")
                    }
                }
            }
        }
    }
}

/** Copies shared content into app storage (keeping its name) and returns file:// URIs. */
private fun stage(activity: ComponentActivity, items: List<SharedItem>): List<Uri> {
    val inbox = File(activity.cacheDir, "share_inbox").apply { mkdirs() }
    return items.mapNotNull { item ->
        try {
            val dir = File(inbox, System.nanoTime().toString()).apply { mkdirs() }
            val out = File(dir, item.name.replace('/', '_'))
            if (item.text != null) {
                out.writeText(item.text)
            } else {
                activity.contentResolver.openInputStream(item.uri!!)?.use { input -> out.outputStream().use { input.copyTo(it, 64 * 1024) } } ?: return@mapNotNull null
            }
            Uri.fromFile(out)
        } catch (_: Exception) {
            null
        }
    }
}
