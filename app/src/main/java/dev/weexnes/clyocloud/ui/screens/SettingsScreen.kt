package dev.weexnes.clyocloud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.BuildConfigInfo
import dev.weexnes.clyocloud.data.ImageLoader
import dev.weexnes.clyocloud.data.TrashItem
import dev.weexnes.clyocloud.data.formatDate
import dev.weexnes.clyocloud.data.formatFileSize
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.Overlay
import dev.weexnes.clyocloud.ui.components.ButtonKind
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoDialog
import dev.weexnes.clyocloud.ui.components.ClyoIconButton
import dev.weexnes.clyocloud.ui.components.HairList
import dev.weexnes.clyocloud.ui.components.HairRow
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.PageTitle
import dev.weexnes.clyocloud.ui.components.SectionHeader
import dev.weexnes.clyocloud.ui.components.Segmented
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.components.TopBar
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ThemePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
    val state = LocalAppState.current
    val context = LocalContext.current
    val c = Clyo.colors
    val dark by ThemePrefs.isDark
    var cacheSize by remember { mutableLongStateOf(-1L) }
    var confirmSignOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { cacheSize = withContext(Dispatchers.IO) { ImageLoader.diskUsage(context) } }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar("Settings")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            PageTitle("Settings")
            Spacer(Modifier.height(32.dp))

            SectionHeader("Account")
            HairList {
                HairRow(state.api.connection.username, hint = "Signed in") {}
                Hairline()
                HairRow("Server") { NumText(state.api.connection.serverUrl.removePrefix("https://").removePrefix("http://")) }
            }
            Spacer(Modifier.height(32.dp))

            SectionHeader("Appearance")
            HairList {
                HairRow("Theme", hint = "Light is the same design, inverted") {
                    Segmented(listOf("Dark", "Light"), if (dark) 0 else 1, { ThemePrefs.set(context, it == 0) })
                }
            }
            Spacer(Modifier.height(32.dp))

            SectionHeader("Phone")
            HairList {
                HairRow("Camera backup", hint = if (dev.weexnes.clyocloud.backup.BackupPrefs.enabled(context)) "On · ${dev.weexnes.clyocloud.backup.BackupPrefs.folder(context)}" else "Off", onClick = { state.push(Overlay.Backup) }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = c.text3, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(32.dp))

            SectionHeader("Storage")
            HairList {
                HairRow("Recycle bin", hint = "Restore or permanently delete items", onClick = { state.push(Overlay.RecycleBin) }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = c.text3, modifier = Modifier.size(18.dp))
                }
                Hairline()
                HairRow("Image cache", hint = "Thumbnails kept on this phone") {
                    NumText(if (cacheSize < 0) "…" else formatFileSize(cacheSize))
                    Spacer(Modifier.size(12.dp))
                    ClyoButton("Clear", {
                        ImageLoader.clear(context)
                        cacheSize = 0
                        state.toast("Image cache cleared")
                    }, small = true)
                }
            }
            Spacer(Modifier.height(32.dp))

            ClyoButton("Sign out", { confirmSignOut = true }, kind = ButtonKind.Danger)
            Spacer(Modifier.height(32.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            SmallText("ClyoCloud for Android ${BuildConfigInfo.versionName(context)}")
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmSignOut) {
        ClyoDialog("Sign out?", { confirmSignOut = false }, confirmText = "Sign out", confirmKind = ButtonKind.Danger, onConfirm = {
            confirmSignOut = false
            state.signOut()
        }) {
            SmallText("Automatic wallpaper changes and the photo widget pause until you sign in again.")
        }
    }
}

@Composable
fun RecycleBinScreen(onClose: () -> Unit) {
    val state = LocalAppState.current
    val c = Clyo.colors
    var items by remember { mutableStateOf<List<TrashItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf<TrashItem?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    LaunchedEffect(reload) {
        loading = true
        items = try { state.api.trash() } catch (_: Exception) { state.toast("Could not load the recycle bin"); emptyList() }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar("Recycle bin", subtitle = if (items.isEmpty()) null else "${items.size} items · ${formatFileSize(items.sumOf { it.size })}", onBack = onClose) {
            if (items.isNotEmpty()) ClyoButton("Empty", { confirmEmpty = true }, kind = ButtonKind.Danger, small = true)
        }
        ListState(loading, items.isEmpty(), "The recycle bin is empty") {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { item ->
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.isFolder) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile, null, tint = c.text2, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            androidx.compose.material3.Text(item.originalPath.substringAfterLast('/'), style = dev.weexnes.clyocloud.ui.theme.ClyoType.title, color = c.text, maxLines = 1)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumText(formatFileSize(item.size), color = c.text3)
                                NumText("deleted ${formatDate(item.trashedAt).substringBefore(' ')}", color = c.text3)
                            }
                            if (item.originalPath.contains('/')) SmallText(item.originalPath.substringBeforeLast('/'), maxLines = 1)
                        }
                        ClyoIconButton(Icons.Outlined.Restore, "Restore", {
                            state.run("Restored", { reload++; state.filesChanged() }) { state.api.restoreFromTrash(item.id) }
                        })
                        ClyoIconButton(Icons.Outlined.Delete, "Delete permanently", { confirm = item }, tint = c.danger)
                    }
                    Hairline()
                }
            }
        }
    }

    confirm?.let { item ->
        ClyoDialog("Delete permanently?", { confirm = null }, confirmText = "Delete", confirmKind = ButtonKind.Danger, onConfirm = {
            state.run("Deleted permanently", { confirm = null; reload++ }) { state.api.deleteFromTrash(item.id) }
        }) { SmallText("\"${item.originalPath.substringAfterLast('/')}\" can't be restored afterwards.") }
    }
    if (confirmEmpty) {
        ClyoDialog("Empty the recycle bin?", { confirmEmpty = false }, confirmText = "Empty", confirmKind = ButtonKind.Danger, onConfirm = {
            state.run("Recycle bin emptied", { confirmEmpty = false; reload++ }) { state.api.emptyTrash() }
        }) { SmallText("All ${items.size} items are deleted permanently.") }
    }
}

