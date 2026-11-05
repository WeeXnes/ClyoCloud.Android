package dev.weexnes.clyocloud.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import dev.weexnes.clyocloud.data.ApiException
import dev.weexnes.clyocloud.data.ClyoApi
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class Tab(val label: String) { Files("Files"), Media("Media"), Wallpapers("Wallpapers"), Favorites("Favorites"), Settings("Settings") }

/** Full-screen layers above the tabs. */
sealed class Overlay {
    data class Viewer(val files: List<CloudFile>, val startIndex: Int) : Overlay()
    data class WallpaperEditor(val path: String) : Overlay()
    data object RecycleBin : Overlay()
}

/** App-wide state shared by every screen. */
class AppState(val context: Context, val scope: CoroutineScope, val api: ClyoApi) {
    val snackbar = SnackbarHostState()
    val tab = mutableStateOf(Tab.Files)
    val overlays = mutableStateOf<List<Overlay>>(emptyList())

    /** Set to open the ComfyUI wallpaper generator for this file. */
    val generateFor = mutableStateOf<CloudFile?>(null)

    /** Set to make the Files tab open this folder. */
    val pendingFolder = mutableStateOf<String?>(null)
    fun openFolder(path: String) {
        pendingFolder.value = path
        tab.value = Tab.Files
    }

    /** Bumped whenever files change, so lists can reload. */
    val filesVersion = mutableStateOf(0)
    fun filesChanged() { filesVersion.value++ }

    fun push(overlay: Overlay) { overlays.value = overlays.value + overlay }
    fun pop() { overlays.value = overlays.value.dropLast(1) }

    fun toast(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    /**
     * Runs a server call; shows [success] when it worked or the server's message when it didn't.
     * A rejected session signs out.
     */
    fun run(success: String? = null, onDone: () -> Unit = {}, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                success?.let { toast(it) }
                onDone()
            } catch (e: ApiException) {
                if (e.code == 401) signOut() else toast(e.message ?: "Something went wrong")
            } catch (e: Exception) {
                toast("Could not reach the server")
            }
        }
    }

    fun signOut() {
        Session.clear(context)
    }

    /** Downloads through the system download manager, so it continues when the app is closed. */
    fun download(url: String, fileName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .addRequestHeader("Authorization", "Bearer ${api.connection.token}")
                .setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ClyoCloud/$fileName")
            context.getSystemService(DownloadManager::class.java).enqueue(request)
            toast("Downloading $fileName")
        } catch (e: Exception) {
            toast("Download failed: ${e.message}")
        }
    }

    fun shareText(text: String, title: String = "Share link") {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun copy(text: String, label: String = "ClyoCloud link") {
        val cm = context.getSystemService(android.content.ClipboardManager::class.java)
        cm.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
    }
}

val LocalAppState = staticCompositionLocalOf<AppState> { error("AppState not provided") }
