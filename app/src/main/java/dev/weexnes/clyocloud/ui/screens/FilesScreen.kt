package dev.weexnes.clyocloud.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.BackgroundOperationService
import dev.weexnes.clyocloud.OperationStatus
import dev.weexnes.clyocloud.data.ApiException
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.UiPrefs
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoDialog
import dev.weexnes.clyocloud.ui.components.ClyoIconButton
import dev.weexnes.clyocloud.ui.components.ClyoTextField
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.MicroLabel
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.ProgressBar
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoType

fun sortFiles(files: List<CloudFile>, sort: String): List<CloudFile> {
    val byKind = compareBy<CloudFile> { !it.isFolder }
    return when (sort) {
        "size" -> files.sortedWith(byKind.thenByDescending { it.size })
        "date" -> files.sortedWith(byKind.thenByDescending { it.lastModified })
        else -> files.sortedWith(byKind.thenBy { it.name.lowercase() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen() {
    val state = LocalAppState.current
    val context = LocalContext.current
    val c = Clyo.colors
    val filesVersion by state.filesVersion

    var path by rememberSaveable { mutableStateOf("") }
    var files by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var grid by remember { mutableStateOf(UiPrefs.gridView(context)) }
    var sort by remember { mutableStateOf(UiPrefs.sort(context)) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dialog by remember { mutableStateOf<FileDialog?>(null) }
    var newFolder by remember { mutableStateOf<String?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(path, filesVersion, reloadKey) {
        if (!refreshing) loading = true
        try {
            files = state.api.files(path)
            error = null
        } catch (e: ApiException) {
            if (e.code == 401) state.signOut()
            error = e.message
            if (e.code == 404 && path.isNotEmpty()) path = path.substringBeforeLast('/', "")
        } catch (_: Exception) {
            error = "Could not reach the server"
        }
        loading = false
        refreshing = false
    }
    LaunchedEffect(path) { selection = emptySet() }

    // Another tab (Favorites, Wallpapers) asked to show a folder
    val pendingFolder by state.pendingFolder
    LaunchedEffect(pendingFolder) {
        pendingFolder?.let { path = it; state.pendingFolder.value = null }
    }

    // Reload when an upload finishes
    val operations by BackgroundOperationService.activeOperations.collectAsState()
    val upload = operations.firstOrNull()
    LaunchedEffect(upload?.status, upload?.current) {
        if (upload != null && upload.status != OperationStatus.PROCESSING) state.filesChanged()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            BackgroundOperationService.startUpload(context, state.api.connection.serverUrl, state.api.connection.token, path, ArrayList(uris))
            state.toast("Uploading ${uris.size} file(s)")
        }
    }

    val visible = remember(files, query, favoritesOnly, sort) {
        val q = query.trim().lowercase()
        sortFiles(files.filter { f ->
            (!favoritesOnly || f.isFavourite) &&
                (q.isEmpty() || f.name.lowercase().contains(q) || f.tags.any { it.name.lowercase().contains(q) })
        }, sort)
    }
    val selectionMode = selection.isNotEmpty()
    val selectedFiles = visible.filter { it.path in selection }

    fun toggle(f: CloudFile) { selection = if (f.path in selection) selection - f.path else selection + f.path }
    fun open(f: CloudFile) {
        when {
            selectionMode -> toggle(f)
            f.isFolder -> path = f.path
            else -> state.openFile(f, visible)
        }
    }

    BackHandler(enabled = selectionMode || path.isNotEmpty() || searching) {
        when {
            selectionMode -> selection = emptySet()
            searching -> { searching = false; query = "" }
            else -> path = path.substringBeforeLast('/', "")
        }
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        // Header: title or selection bar, then breadcrumbs
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    ClyoIconButton(Icons.Outlined.Close, "Clear selection", { selection = emptySet() }, tint = c.text)
                    NumText("${selection.size} selected", color = c.text, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    ClyoIconButton(Icons.Outlined.SelectAll, "Select all", { selection = if (selection.size == visible.size) emptySet() else visible.map { it.path }.toSet() })
                } else if (searching) {
                    ClyoTextField(query, { query = it }, Modifier.weight(1f), placeholder = "Search name or tag", leadingIcon = Icons.Outlined.Search)
                    ClyoIconButton(Icons.Outlined.Close, "Close search", { searching = false; query = "" })
                } else {
                    Text("Files", style = ClyoType.title.copy(fontWeight = FontWeight.Medium), color = c.text, modifier = Modifier.weight(1f))
                    ClyoIconButton(Icons.Outlined.Search, "Search", { searching = true })
                    ClyoIconButton(if (favoritesOnly) Icons.Outlined.Star else Icons.Outlined.StarOutline, "Favorites only", { favoritesOnly = !favoritesOnly }, tint = if (favoritesOnly) c.text else c.text2)
                    Box {
                        ClyoIconButton(Icons.Outlined.Sort, "Sort", { sortMenu = true })
                        DropdownMenu(sortMenu, { sortMenu = false }, containerColor = c.bg) {
                            listOf("name" to "Name", "date" to "Date modified", "size" to "Size").forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, style = ClyoType.title, color = if (sort == key) c.text else c.text2) },
                                    onClick = { sort = key; UiPrefs.setSort(context, key); sortMenu = false },
                                )
                            }
                        }
                    }
                    ClyoIconButton(if (grid) Icons.Outlined.ViewList else Icons.Outlined.GridView, if (grid) "List view" else "Grid view", { grid = !grid; UiPrefs.setGridView(context, grid) })
                }
            }
            Breadcrumbs(path) { path = it }
            Hairline()
        }

        // Upload progress
        upload?.let { op ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallText(
                        when (op.status) {
                            OperationStatus.PROCESSING -> "Uploading"
                            OperationStatus.COMPLETED -> "Upload finished"
                            OperationStatus.ERROR -> "Some uploads failed"
                        },
                        color = if (op.status == OperationStatus.ERROR) c.danger else c.text2,
                        modifier = Modifier.weight(1f),
                    )
                    NumText("${op.current} / ${op.total}")
                }
                Spacer(Modifier.height(6.dp))
                ProgressBar(if (op.total > 0) op.current.toFloat() / op.total else 0f)
            }
            Hairline()
        }

        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; reloadKey++ }, modifier = Modifier.weight(1f)) {
            when {
                error != null && files.isEmpty() -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SmallText(error!!)
                    Spacer(Modifier.height(12.dp))
                    ClyoButton("Try again", { reloadKey++ }, small = true)
                }
                else -> ListState(loading, visible.isEmpty(), if (query.isNotBlank() || favoritesOnly) "Nothing matches" else "This folder is empty") {
                    if (grid) {
                        LazyVerticalGrid(
                            GridCells.Adaptive(104.dp),
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(visible, key = { it.path }) { f ->
                                FileTile(f, f.path in selection, selectionMode, onClick = { open(f) }, onLongClick = { if (selectionMode) toggle(f) else dialog = FileDialog.Actions(f) })
                            }
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                            items(visible, key = { it.path }) { f ->
                                FileRow(f, f.path in selection, selectionMode, onClick = { open(f) }, onLongClick = { toggle(f) }, onMore = { dialog = FileDialog.Actions(f) })
                            }
                        }
                    }
                }
            }

            // Actions: a bulk bar while selecting, otherwise upload + new folder
            Row(
                Modifier.align(Alignment.BottomCenter).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selectionMode) {
                    BulkBar(
                        count = selection.size,
                        onDownload = {
                            val names = selectedFiles.map { it.name }
                            state.download(state.api.bulkDownloadUrl(path, names), "ClyoCloud-${System.currentTimeMillis() / 1000}.zip")
                            selection = emptySet()
                        },
                        onFavorite = {
                            val makeFav = selectedFiles.any { !it.isFavourite }
                            state.run(if (makeFav) "Added to favorites" else "Removed from favorites", { selection = emptySet(); state.filesChanged() }) {
                                selectedFiles.forEach { state.api.updateMeta(it.path, isFavourite = makeFav) }
                            }
                        },
                        onTags = { dialog = FileDialog.Tags(selectedFiles) },
                        onRename = { dialog = FileDialog.BulkRename(selectedFiles) },
                        onMove = { dialog = FileDialog.Move(selectedFiles) },
                        onDelete = { dialog = FileDialog.Delete(selectedFiles) },
                    )
                } else {
                    ClyoButton("New folder", { newFolder = "" }, icon = Icons.Outlined.CreateNewFolder, modifier = Modifier.background(c.bg, androidx.compose.foundation.shape.RoundedCornerShape(50)))
                    ClyoButton("Upload", { picker.launch("*/*") }, kind = dev.weexnes.clyocloud.ui.components.ButtonKind.Primary, icon = Icons.Outlined.Upload)
                }
            }
        }
    }

    FileDialogs(
        dialog,
        onDismiss = { dialog = null; selection = emptySet() },
        onOpen = { dialog = it },
        currentFolder = path,
        siblings = visible,
    )

    newFolder?.let { name ->
        ClyoDialog("New folder", { newFolder = null }, confirmText = "Create", confirmEnabled = name.isNotBlank() && '/' !in name, onConfirm = {
            state.run("Folder created", { newFolder = null; state.filesChanged() }) { state.api.createFolder(path, name.trim()) }
        }) {
            ClyoTextField(name, { newFolder = it }, label = "Name", onImeAction = {})
        }
    }
}

/** Root / a / b as tappable mono segments, scrolling horizontally when long. */
@Composable
private fun Breadcrumbs(path: String, onNavigate: (String) -> Unit) {
    val c = Clyo.colors
    val parts = path.split('/').filter { it.isNotEmpty() }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crumb("Root", parts.isEmpty()) { onNavigate("") }
        parts.forEachIndexed { i, part ->
            Text("/", style = ClyoType.num, color = c.text3, modifier = Modifier.padding(horizontal = 6.dp))
            Crumb(part, i == parts.lastIndex) { onNavigate(parts.take(i + 1).joinToString("/")) }
        }
    }
}

@Composable
private fun Crumb(label: String, current: Boolean, onClick: () -> Unit) {
    val c = Clyo.colors
    Text(
        label,
        style = ClyoType.num,
        color = if (current) c.text else c.text2,
        maxLines = 1,
        modifier = Modifier.then(if (current) Modifier else Modifier.clickableNoRipple(onClick)),
    )
}

@Composable
private fun BulkBar(count: Int, onDownload: () -> Unit, onFavorite: () -> Unit, onTags: () -> Unit, onRename: () -> Unit, onMove: () -> Unit, onDelete: () -> Unit) {
    val c = Clyo.colors
    Row(
        Modifier
            .background(c.bg, dev.weexnes.clyocloud.ui.components.Pill)
            .then(Modifier.border1(c.lineStrong))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClyoIconButton(Icons.Outlined.Download, "Download $count as ZIP", onDownload)
        ClyoIconButton(Icons.Outlined.StarOutline, "Favorite", onFavorite)
        ClyoIconButton(Icons.Outlined.Label, "Tags", onTags)
        ClyoIconButton(Icons.Outlined.Edit, "Rename", onRename)
        ClyoIconButton(Icons.Outlined.DriveFileMove, "Move", onMove)
        Spacer(Modifier.width(4.dp))
        ClyoIconButton(Icons.Outlined.Delete, "Delete", onDelete, tint = c.danger)
    }
}

private fun Modifier.border1(color: androidx.compose.ui.graphics.Color) = this.border(1.dp, color, dev.weexnes.clyocloud.ui.components.Pill)

fun Modifier.clickableNoRipple(onClick: () -> Unit) = this.clickable(onClick = onClick)
