package dev.weexnes.clyocloud.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.TagItem
import dev.weexnes.clyocloud.data.canGenerateWallpaper
import dev.weexnes.clyocloud.data.formatDate
import dev.weexnes.clyocloud.data.formatFileSize
import dev.weexnes.clyocloud.data.hasThumbnail
import dev.weexnes.clyocloud.data.isImageFile
import dev.weexnes.clyocloud.data.isVideoFile
import dev.weexnes.clyocloud.ui.AppState
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.Overlay
import dev.weexnes.clyocloud.ui.components.ButtonKind
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoDialog
import dev.weexnes.clyocloud.ui.components.ClyoIconButton
import dev.weexnes.clyocloud.ui.components.ClyoSheet
import dev.weexnes.clyocloud.ui.components.ClyoSwitch
import dev.weexnes.clyocloud.ui.components.ClyoTextField
import dev.weexnes.clyocloud.ui.components.Dot
import dev.weexnes.clyocloud.ui.components.EmptyState
import dev.weexnes.clyocloud.ui.components.FileTypeIcon
import dev.weexnes.clyocloud.ui.components.HairList
import dev.weexnes.clyocloud.ui.components.HairRow
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.MicroLabel
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.RadiusCard
import dev.weexnes.clyocloud.ui.components.ServerImage
import dev.weexnes.clyocloud.ui.components.SheetAction
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.components.Spinner
import dev.weexnes.clyocloud.ui.components.TagChip
import dev.weexnes.clyocloud.ui.components.parseHexColor
import dev.weexnes.clyocloud.ui.components.pixels
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Opens a file: images and videos in the viewer (with their neighbours), anything else as a download. */
fun AppState.openFile(file: CloudFile, siblings: List<CloudFile>) {
    if (isImageFile(file.name) || isVideoFile(file.name)) {
        val media = siblings.filter { !it.isFolder && (isImageFile(it.name) || isVideoFile(it.name)) }
        push(Overlay.Viewer(media, media.indexOfFirst { it.path == file.path }.coerceAtLeast(0)))
    } else {
        download(api.fileUrl(file.path), file.name)
    }
}

// ─── Rows and tiles ──────────────────────────────────────────────────────────

@Composable
fun FileThumb(file: CloudFile, modifier: Modifier = Modifier, iconSize: androidx.compose.ui.unit.Dp = 22.dp) {
    val state = LocalAppState.current
    val c = Clyo.colors
    val thumbPath = when {
        file.isFolder && file.previewImage != null -> "${file.path}/${file.previewImage}"
        !file.isFolder && hasThumbnail(file.name) -> file.path
        else -> null
    }
    Box(modifier.background(c.surface), contentAlignment = Alignment.Center) {
        if (thumbPath != null) {
            ServerImage(state.api.thumbnailUrl(thumbPath), state.api.connection.token, Modifier.fillMaxSize(), decodeSize = 320,
                placeholder = { FileTypeIcon(file, size = iconSize) })
        } else {
            FileTypeIcon(file, size = iconSize)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    file: CloudFile,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    showPath: Boolean = false,
) {
    val c = Clyo.colors
    val bg by animateColorAsState(if (selected) c.surface2 else Color.Transparent, androidx.compose.animation.core.tween(120), label = "row")
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            FileThumb(file, Modifier.size(40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)))
            if (selectionMode) SelectMark(selected, Modifier.align(Alignment.TopStart).padding(2.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(file.name, style = ClyoType.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (file.isFavourite) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.Star, "Favorite", tint = c.text2, modifier = Modifier.size(13.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showPath && file.path.contains('/')) {
                    SmallText(file.path.substringBeforeLast('/'), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                } else {
                    NumText(if (file.isFolder) "Folder" else formatFileSize(file.size), color = c.text3)
                    NumText(formatDate(file.lastModified).substringBefore(' '), color = c.text3)
                }
                file.tags.take(3).forEach { Dot(parseHexColor(it.color) ?: c.text3) }
            }
        }
        if (!selectionMode) ClyoIconButton(Icons.Outlined.MoreVert, "More actions for ${file.name}", onMore)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTile(file: CloudFile, selected: Boolean, selectionMode: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = Clyo.colors
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RadiusCard)
                .border(1.dp, if (selected) c.text else c.line, RadiusCard),
        ) {
            FileThumb(file, Modifier.fillMaxSize(), iconSize = 32.dp)
            if (selectionMode) SelectMark(selected, Modifier.align(Alignment.TopStart).padding(8.dp))
            if (file.isFavourite) Icon(Icons.Outlined.Star, "Favorite", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(file.name, style = ClyoType.small, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        NumText(if (file.isFolder) "Folder" else formatFileSize(file.size), color = c.text3)
    }
}

@Composable
private fun SelectMark(selected: Boolean, modifier: Modifier = Modifier) {
    val c = Clyo.colors
    Box(
        modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) c.invert else c.bg.copy(alpha = 0.6f))
            .border(1.dp, if (selected) c.invert else c.lineStrong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Outlined.Check, null, tint = c.onInvert, modifier = Modifier.size(14.dp))
    }
}

// ─── Actions for one file ────────────────────────────────────────────────────

/** Which sheet or dialog is open for which file. */
sealed class FileDialog {
    data class Actions(val file: CloudFile) : FileDialog()
    data class Details(val file: CloudFile) : FileDialog()
    data class Rename(val file: CloudFile) : FileDialog()
    data class Delete(val files: List<CloudFile>) : FileDialog()
    data class Move(val files: List<CloudFile>) : FileDialog()
    data class Tags(val files: List<CloudFile>) : FileDialog()
    data class BulkRename(val files: List<CloudFile>) : FileDialog()
}

/**
 * Renders whichever [FileDialog] is open. [currentFolder] is the folder being browsed
 * (null outside the file browser), used for "Use as folder thumbnail".
 */
@Composable
fun FileDialogs(dialog: FileDialog?, onDismiss: () -> Unit, onOpen: (FileDialog) -> Unit, currentFolder: String?, siblings: List<CloudFile>) {
    when (dialog) {
        null -> {}
        is FileDialog.Actions -> FileActionsSheet(dialog.file, currentFolder, siblings, onDismiss, onOpen)
        is FileDialog.Details -> FileDetailsSheet(dialog.file, onDismiss, onOpen)
        is FileDialog.Rename -> RenameDialog(dialog.file, onDismiss)
        is FileDialog.Delete -> DeleteDialog(dialog.files, onDismiss)
        is FileDialog.Move -> MoveDialog(dialog.files, onDismiss)
        is FileDialog.Tags -> TagsSheet(dialog.files, onDismiss)
        is FileDialog.BulkRename -> BulkRenameDialog(dialog.files, onDismiss)
    }
}

@Composable
private fun FileActionsSheet(file: CloudFile, currentFolder: String?, siblings: List<CloudFile>, onDismiss: () -> Unit, onOpen: (FileDialog) -> Unit) {
    val state = LocalAppState.current
    val api = state.api
    ClyoSheet(onDismiss) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            FileThumb(file, Modifier.size(44.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = ClyoType.title, color = Clyo.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                NumText(if (file.isFolder) "Folder" else formatFileSize(file.size), color = Clyo.colors.text3)
            }
        }
        Hairline(Modifier.padding(top = 8.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (!file.isFolder) SheetAction(Icons.Outlined.OpenInNew, "Open", { onDismiss(); state.openFile(file, siblings) })
            SheetAction(Icons.Outlined.Info, "Details, notes and tags", { onOpen(FileDialog.Details(file)) })
            if (canGenerateWallpaper(file)) {
                SheetAction(Icons.Outlined.AutoAwesome, "Create wallpaper", { onDismiss(); state.generateFor.value = file }, hint = "Extend and upscale to 2880×1800 with ComfyUI")
            }
            SheetAction(
                if (file.isFavourite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                if (file.isFavourite) "Remove from favorites" else "Add to favorites",
                { onDismiss(); state.run(if (file.isFavourite) "Removed from favorites" else "Added to favorites", state::filesChanged) { api.updateMeta(file.path, isFavourite = !file.isFavourite) } },
            )
            if (!file.isFolder && isImageFile(file.name)) {
                SheetAction(
                    Icons.Outlined.Wallpaper,
                    if (file.isWallpaper) "Remove from wallpapers" else "Add to wallpapers",
                    { onDismiss(); state.run(if (file.isWallpaper) "Removed from wallpapers" else "Added to wallpapers", state::filesChanged) { api.updateMeta(file.path, isWallpaper = !file.isWallpaper) } },
                )
                if (currentFolder != null && currentFolder.isNotEmpty()) {
                    SheetAction(Icons.Outlined.FolderSpecial, "Use as folder thumbnail", {
                        onDismiss()
                        state.run("Folder thumbnail set", state::filesChanged) { api.updateMeta(currentFolder, previewImage = file.name) }
                    })
                }
            }
            if (file.isFolder && file.previewImage != null) {
                SheetAction(Icons.Outlined.Folder, "Reset folder thumbnail", { onDismiss(); state.run("Folder thumbnail reset", state::filesChanged) { api.updateMeta(file.path, clearPreviewImage = true) } })
            }
            if (file.isPublic) {
                SheetAction(Icons.Outlined.Share, "Share public link", { onDismiss(); state.shareText(api.publicUrl(file.path)) })
                SheetAction(Icons.Outlined.ContentCopy, "Copy public link", { onDismiss(); state.copy(api.publicUrl(file.path)); state.toast("Link copied") })
            }
            SheetAction(Icons.Outlined.Download, if (file.isFolder) "Download as ZIP" else "Download", {
                onDismiss()
                if (file.isFolder) state.download(api.zipUrl(file.path), "${file.name}.zip") else state.download(api.fileUrl(file.path), file.name)
            })
            SheetAction(Icons.Outlined.Edit, "Rename", { onOpen(FileDialog.Rename(file)) })
            SheetAction(Icons.Outlined.DriveFileMove, "Move", { onOpen(FileDialog.Move(listOf(file))) })
            SheetAction(Icons.Outlined.Delete, "Delete", { onOpen(FileDialog.Delete(listOf(file))) }, danger = true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileDetailsSheet(file: CloudFile, onDismiss: () -> Unit, onOpen: (FileDialog) -> Unit) {
    val state = LocalAppState.current
    val api = state.api
    val c = Clyo.colors
    var description by remember(file.path) { mutableStateOf(file.description ?: "") }
    var isPublic by remember(file.path) { mutableStateOf(file.isPublic) }
    var isWallpaper by remember(file.path) { mutableStateOf(file.isWallpaper) }
    var isPhotoWidget by remember(file.path) { mutableStateOf(file.isPhotoWidget) }

    fun toggle(success: String, block: suspend () -> Unit) = state.run(success, state::filesChanged, block)

    ClyoSheet(onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (!file.isFolder && (isImageFile(file.name) || isVideoFile(file.name))) {
                FileThumb(file, Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RadiusCard).border(1.dp, c.line, RadiusCard))
                Spacer(Modifier.height(16.dp))
            }
            Text(file.name, style = ClyoType.sectionTitle, color = c.text)
            if (file.path.contains('/')) SmallText(file.path.substringBeforeLast('/'))
            Spacer(Modifier.height(16.dp))

            // Stats with hairlines between them, no boxes
            Hairline()
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Column(Modifier.weight(1f)) { MicroLabel("Size"); NumText(if (file.isFolder) "—" else formatFileSize(file.size), color = c.text) }
                Column(Modifier.weight(1f)) { MicroLabel("Modified"); NumText(formatDate(file.lastModified), color = c.text) }
                Column(Modifier.weight(0.6f)) { MicroLabel("Type"); NumText(if (file.isFolder) "Folder" else file.type.trimStart('.').ifBlank { "—" }.uppercase(), color = c.text) }
            }
            Hairline()
            Spacer(Modifier.height(20.dp))

            MicroLabel("Notes", Modifier.padding(bottom = 6.dp))
            ClyoTextField(description, { description = it }, placeholder = "Add a description", singleLine = false, minLines = 2)
            if (description != (file.description ?: "")) {
                Spacer(Modifier.height(8.dp))
                ClyoButton("Save notes", { toggle("Notes saved") { api.updateMeta(file.path, description = description) } }, small = true)
            }
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                MicroLabel("Tags", Modifier.weight(1f))
                ClyoButton("Edit", { onOpen(FileDialog.Tags(listOf(file))) }, kind = ButtonKind.Ghost, small = true)
            }
            if (file.tags.isEmpty()) SmallText("No tags")
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                file.tags.forEach { TagChip(it.name, it.color) }
            }
            Spacer(Modifier.height(20.dp))

            HairList {
                HairRow("Public link", hint = "Anyone with the link can download") {
                    ClyoSwitch(isPublic, { v -> isPublic = v; toggle(if (v) "Public link on" else "Public link off") { api.updateMeta(file.path, isPublic = v) } })
                }
                if (isPublic) {
                    Hairline()
                    HairRow("Share link", onClick = { state.shareText(api.publicUrl(file.path)) }) {
                        Icon(Icons.Outlined.Share, null, tint = c.text2, modifier = Modifier.size(18.dp))
                    }
                }
                if (!file.isFolder && isImageFile(file.name)) {
                    Hairline()
                    HairRow("Wallpaper", hint = "Used by the wallpaper cycle") {
                        ClyoSwitch(isWallpaper, { v -> isWallpaper = v; toggle(if (v) "Added to wallpapers" else "Removed from wallpapers") { api.updateMeta(file.path, isWallpaper = v) } })
                    }
                    Hairline()
                    HairRow("Photo widget", hint = "Shown in the home-screen photo widget") {
                        ClyoSwitch(isPhotoWidget, { v -> isPhotoWidget = v; toggle(if (v) "Added to photo widget" else "Removed from photo widget") { api.updateMeta(file.path, isPhotoWidget = v) } })
                    }
                }
            }
            if (canGenerateWallpaper(file)) {
                Spacer(Modifier.height(16.dp))
                ClyoButton("Create wallpaper", { onDismiss(); state.generateFor.value = file }, Modifier.fillMaxWidth(), icon = Icons.Outlined.AutoAwesome)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Dialogs ─────────────────────────────────────────────────────────────────

@Composable
private fun RenameDialog(file: CloudFile, onDismiss: () -> Unit) {
    val state = LocalAppState.current
    var name by remember { mutableStateOf(file.name) }
    var busy by remember { mutableStateOf(false) }
    ClyoDialog("Rename", onDismiss, confirmText = "Rename", confirmEnabled = name.isNotBlank() && name != file.name && '/' !in name, confirmLoading = busy, onConfirm = {
        busy = true
        state.run("Renamed", { state.filesChanged(); onDismiss() }) { state.api.rename(file.path, name.trim()) }
    }) {
        ClyoTextField(name, { name = it }, label = "Name")
    }
}

@Composable
private fun DeleteDialog(files: List<CloudFile>, onDismiss: () -> Unit) {
    val state = LocalAppState.current
    var busy by remember { mutableStateOf(false) }
    val label = if (files.size == 1) "\"${files.first().name}\"" else "${files.size} items"
    ClyoDialog("Delete $label?", onDismiss, confirmText = "Move to recycle bin", confirmKind = ButtonKind.Danger, confirmLoading = busy, onConfirm = {
        busy = true
        state.run(if (files.size == 1) "Moved to recycle bin" else "${files.size} items moved to recycle bin", { state.filesChanged(); onDismiss() }) {
            files.forEach { state.api.delete(it.path) }
        }
    }) {
        SmallText("Items stay in the recycle bin for 30 days.")
    }
}

/** Bulk rename: base name plus a running number, keeping each file's extension. */
@Composable
private fun BulkRenameDialog(files: List<CloudFile>, onDismiss: () -> Unit) {
    val state = LocalAppState.current
    var base by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("1") }
    var busy by remember { mutableStateOf(false) }
    val first = start.toIntOrNull() ?: 1
    val example = files.firstOrNull()?.let { f -> "$base$first" + if (f.isFolder || !f.name.contains('.')) "" else "." + f.name.substringAfterLast('.') }
    ClyoDialog("Rename ${files.size} items", onDismiss, confirmText = "Rename all", confirmEnabled = base.isNotBlank() && '/' !in base, confirmLoading = busy, onConfirm = {
        busy = true
        state.run("Renamed ${files.size} items", { state.filesChanged(); onDismiss() }) {
            files.sortedBy { it.name.lowercase() }.forEachIndexed { i, f ->
                val ext = if (f.isFolder || !f.name.contains('.')) "" else "." + f.name.substringAfterLast('.')
                state.api.rename(f.path, "$base${first + i}$ext")
            }
        }
    }) {
        ClyoTextField(base, { base = it }, label = "Base name", placeholder = "Holiday ")
        ClyoTextField(start, { start = it.filter(Char::isDigit).take(6) }, label = "Start number", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, mono = true)
        if (example != null && base.isNotBlank()) SmallText("First item: $example")
    }
}

/** Folder picker for moving: browse with breadcrumbs, create folders, move here. */
@Composable
private fun MoveDialog(files: List<CloudFile>, onDismiss: () -> Unit) {
    val state = LocalAppState.current
    val c = Clyo.colors
    var path by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newFolder by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val moving = files.map { it.path }.toSet()

    LaunchedEffect(path, reload) {
        loading = true
        folders = try { state.api.files(path).filter { it.isFolder && it.path !in moving }.sortedBy { it.name.lowercase() } } catch (_: Exception) { emptyList() }
        loading = false
    }

    ClyoDialog(
        "Move ${if (files.size == 1) files.first().name else "${files.size} items"}",
        onDismiss,
        confirmText = "Move here",
        confirmLoading = busy,
        confirmEnabled = files.none { it.path.substringBeforeLast('/', "") == path },
        onConfirm = {
            busy = true
            state.run("Moved", { state.filesChanged(); onDismiss() }) { state.api.move(files.map { it.path }, path) }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (path.isNotEmpty()) ClyoIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Up", { path = path.substringBeforeLast('/', "") }, tint = c.text, size = 32.dp)
            NumText("/" + path, color = c.text, modifier = Modifier.weight(1f))
            ClyoIconButton(Icons.Outlined.CreateNewFolder, "New folder", { newFolder = "" }, size = 32.dp)
        }
        HairList(Modifier.heightIn(max = 320.dp)) {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Spinner() }
                folders.isEmpty() -> SmallText("No folders here", Modifier.padding(16.dp))
                else -> LazyColumn {
                    items(folders, key = { it.path }) { f ->
                        HairRow(f.name, leading = { Icon(Icons.Outlined.Folder, null, tint = c.text2, modifier = Modifier.size(20.dp)) }, onClick = { path = f.path })
                        Hairline()
                    }
                }
            }
        }
    }

    newFolder?.let { name ->
        ClyoDialog("New folder", { newFolder = null }, confirmText = "Create", confirmEnabled = name.isNotBlank() && '/' !in name, onConfirm = {
            state.run("Folder created", { newFolder = null; reload++ }) { state.api.createFolder(path, name.trim()) }
        }) {
            ClyoTextField(name, { newFolder = it }, label = "Name")
        }
    }
}

private val TAG_COLORS = listOf("#f87171", "#fb923c", "#fbbf24", "#4ade80", "#2dd4bf", "#60a5fa", "#a78bfa", "#f472b6", "#a3a3a3")

/** Pick from the saved tag library or create a new tag; applies to one or many files. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSheet(files: List<CloudFile>, onDismiss: () -> Unit) {
    val state = LocalAppState.current
    val c = Clyo.colors
    val single = files.size == 1
    var library by remember { mutableStateOf<List<TagItem>>(emptyList()) }
    var selected by remember { mutableStateOf(if (single) files.first().tags else emptyList()) }
    var newName by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf(TAG_COLORS[5]) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { library = try { state.api.tags() } catch (_: Exception) { emptyList() } }

    fun isSelected(t: TagItem) = selected.any { it.name.equals(t.name, ignoreCase = true) }

    ClyoSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Text(if (single) "Tags" else "Add tags to ${files.size} items", style = ClyoType.sectionTitle, color = c.text)
            Spacer(Modifier.height(16.dp))

            MicroLabel("Selected", Modifier.padding(bottom = 6.dp))
            if (selected.isEmpty()) SmallText("None")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selected.forEach { t ->
                    TagChip(t.name, t.color, onClick = { selected = selected.filterNot { it.name == t.name } }, trailing = {
                        Icon(Icons.Outlined.Close, "Remove", tint = c.text3, modifier = Modifier.size(12.dp))
                    })
                }
            }
            Spacer(Modifier.height(20.dp))

            MicroLabel("Library", Modifier.padding(bottom = 6.dp))
            val available = library.filterNot(::isSelected)
            if (available.isEmpty()) SmallText("No other saved tags")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                available.forEach { t -> TagChip(t.name, t.color, onClick = { selected = selected + t }) }
            }
            Spacer(Modifier.height(20.dp))

            MicroLabel("New tag", Modifier.padding(bottom = 6.dp))
            ClyoTextField(newName, { newName = it }, placeholder = "Name")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TAG_COLORS.forEach { hex ->
                    val col = parseHexColor(hex)!!
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(col)
                            .border(2.dp, if (hex == newColor) c.text else Color.Transparent, CircleShape)
                            .clickable { newColor = hex },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            ClyoButton("Add tag", {
                val tag = TagItem(newName.trim(), newColor)
                if (!isSelected(tag)) selected = selected + tag
                if (library.none { it.name.equals(tag.name, true) }) {
                    library = library + tag
                    state.scope.launchSafely { state.api.saveTags(library) }
                }
                newName = ""
            }, enabled = newName.isNotBlank(), small = true)

            Spacer(Modifier.height(24.dp))
            ClyoButton(if (single) "Save tags" else "Add to ${files.size} items", {
                busy = true
                state.run("Tags saved", { state.filesChanged(); onDismiss() }) {
                    files.forEach { f ->
                        val tags = if (single) selected else (f.tags + selected.filter { s -> f.tags.none { it.name.equals(s.name, true) } })
                        state.api.updateMeta(f.path, tags = tags)
                    }
                }
            }, Modifier.fillMaxWidth(), kind = ButtonKind.Primary, loading = busy, enabled = single || selected.isNotEmpty())
        }
    }
}

/** Fire-and-forget launch that swallows errors (for non-critical background saves). */
fun CoroutineScope.launchSafely(block: suspend () -> Unit) {
    launch { try { block() } catch (_: Exception) {} }
}

/** Shared empty / loading states for file lists. */
@Composable
fun ListState(loading: Boolean, empty: Boolean, emptyText: String, content: @Composable () -> Unit) {
    when {
        loading && empty -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Spinner() }
        empty -> EmptyState(emptyText)
        else -> content()
    }
}
