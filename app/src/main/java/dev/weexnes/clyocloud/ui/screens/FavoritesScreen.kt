package dev.weexnes.clyocloud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.ApiException
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.components.ClyoIconButton
import dev.weexnes.clyocloud.ui.components.Spinner
import dev.weexnes.clyocloud.ui.components.TopBar
import dev.weexnes.clyocloud.ui.theme.Clyo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen() {
    val state = LocalAppState.current
    val c = Clyo.colors
    val filesVersion by state.filesVersion
    var files by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var hideFolders by rememberSaveable { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<FileDialog?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(filesVersion, reload) {
        try {
            files = state.api.favorites(sync = syncing)
            if (syncing) state.toast("Favorites are up to date")
        } catch (e: ApiException) {
            if (e.code == 401) state.signOut() else state.toast(e.message ?: "Could not load favorites")
        } catch (_: Exception) {
            state.toast("Could not reach the server")
        }
        loading = false
        refreshing = false
        syncing = false
    }

    val visible = remember(files, hideFolders) { sortFiles(files.filter { !hideFolders || !it.isFolder }, "name") }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar("Favorites", subtitle = if (files.isEmpty()) null else "${files.size} items") {
            ClyoIconButton(if (hideFolders) Icons.Outlined.FolderOff else Icons.Outlined.Folder, if (hideFolders) "Show folders" else "Hide folders", { hideFolders = !hideFolders })
            if (syncing) Box(Modifier) { Spinner() }
            else ClyoIconButton(Icons.Outlined.Refresh, "Sync with storage", { syncing = true; reload++ })
        }
        PullToRefreshBox(refreshing, { refreshing = true; reload++ }, Modifier.weight(1f)) {
            ListState(loading, visible.isEmpty(), "No favorites yet. Star files to find them here.") {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(visible, key = { it.path }) { f ->
                        FileRow(
                            f, selected = false, selectionMode = false,
                            onClick = { if (f.isFolder) state.openFolder(f.path) else state.openFile(f, visible) },
                            onLongClick = { dialog = FileDialog.Actions(f) },
                            onMore = { dialog = FileDialog.Actions(f) },
                            showPath = true,
                        )
                    }
                }
            }
        }
    }

    FileDialogs(dialog, onDismiss = { dialog = null }, onOpen = { dialog = it }, currentFolder = null, siblings = visible)
}

