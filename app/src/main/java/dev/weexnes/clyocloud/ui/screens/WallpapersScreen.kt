package dev.weexnes.clyocloud.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.canGenerateWallpaper
import dev.weexnes.clyocloud.data.formatDate
import dev.weexnes.clyocloud.data.isImageFile
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.Overlay
import dev.weexnes.clyocloud.ui.Tab
import dev.weexnes.clyocloud.ui.components.ButtonKind
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoIconButton
import dev.weexnes.clyocloud.ui.components.ClyoSheet
import dev.weexnes.clyocloud.ui.components.ClyoSwitch
import dev.weexnes.clyocloud.ui.components.EmptyState
import dev.weexnes.clyocloud.ui.components.HairList
import dev.weexnes.clyocloud.ui.components.HairRow
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.HeroStat
import dev.weexnes.clyocloud.ui.components.MicroLabel
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.RadiusCard
import dev.weexnes.clyocloud.ui.components.SectionHeader
import dev.weexnes.clyocloud.ui.components.Segmented
import dev.weexnes.clyocloud.ui.components.ServerImage
import dev.weexnes.clyocloud.ui.components.SheetAction
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.components.Spinner
import dev.weexnes.clyocloud.ui.components.StatusLine
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoType
import dev.weexnes.clyocloud.wallpaper.CycleSource
import dev.weexnes.clyocloud.wallpaper.PhotoWidget
import dev.weexnes.clyocloud.wallpaper.PhotoWidgetCycle
import dev.weexnes.clyocloud.wallpaper.WallpaperCycle
import dev.weexnes.clyocloud.wallpaper.WallpaperPrefs
import dev.weexnes.clyocloud.wallpaper.WallpaperTarget
import kotlinx.coroutines.delay

/** Bumps whenever a wallpaper preference changes (also from the background job). */
@Composable
private fun rememberPrefsVersion(): Int {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        WallpaperPrefs.register(context, listener)
        onDispose { WallpaperPrefs.unregister(context, listener) }
    }
    return version
}

@Composable
fun WallpapersScreen() {
    val c = Clyo.colors
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Wallpapers", style = ClyoType.title.copy(fontWeight = FontWeight.Medium), color = c.text, modifier = Modifier.weight(1f))
            }
            Segmented(listOf("Library", "Auto change", "Photo widget"), section, { section = it }, Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
            Hairline()
        }
        Box(Modifier.weight(1f)) {
            when (section) {
                0 -> WallpaperLibrary()
                1 -> AutoChange()
                else -> PhotoWidgetSettings()
            }
        }
    }
}

// ─── Library ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallpaperLibrary() {
    val state = LocalAppState.current
    val context = LocalContext.current
    val c = Clyo.colors
    val filesVersion by state.filesVersion
    val prefsVersion = rememberPrefsVersion()
    var items by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var syncing by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<CloudFile?>(null) }

    LaunchedEffect(filesVersion, reload) {
        try {
            items = state.api.wallpapers(sync = syncing).filter { !it.isFolder && isImageFile(it.name) }
            if (syncing) state.toast("Wallpapers are up to date")
        } catch (_: Exception) {
            state.toast("Could not load wallpapers")
        }
        loading = false
        syncing = false
    }

    val playlist = remember(prefsVersion) { WallpaperPrefs.playlist(context).toSet() }
    val current = remember(prefsVersion) { WallpaperPrefs.current(context) }

    LazyVerticalGrid(
        GridCells.Adaptive(110.dp),
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicroLabel(if (items.isEmpty()) "Library" else "${items.size} wallpapers · ${playlist.size} in playlist", Modifier.weight(1f))
                if (syncing) Spinner(size = 16.dp)
                else ClyoIconButton(Icons.Outlined.Refresh, "Sync with storage", { syncing = true; reload++ }, size = 36.dp)
            }
        }
        if (loading) {
            item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Spinner() } }
        } else if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("No wallpapers yet. Mark images as wallpapers in Files, or create one with ComfyUI.", actionText = "Open Files", onAction = { state.tab.value = Tab.Files })
            }
        }
        items(items, key = { it.path }) { f ->
            Column(Modifier.combinedClickable(onClick = { selected = f }, onLongClick = { selected = f })) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RadiusCard)
                        .border(1.dp, if (f.path == current) c.text else c.line, RadiusCard),
                ) {
                    ServerImage(state.api.thumbnailUrl(f.path), state.api.connection.token, Modifier.fillMaxSize(), decodeSize = 360)
                    if (f.path in playlist) {
                        MarkBadge("Playlist", Modifier.align(Alignment.BottomStart).padding(6.dp))
                    }
                    if (f.path == current) MarkBadge("Current", Modifier.align(Alignment.TopStart).padding(6.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(f.name, style = ClyoType.small, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 8.dp)) {
                Hairline()
                Spacer(Modifier.height(12.dp))
                SmallText("To create a 2880×1800 wallpaper from any photo, open it in Files and choose Create wallpaper.")
            }
        }
    }

    selected?.let { f ->
        val inPlaylist = f.path in playlist
        ClyoSheet({ selected = null }) {
            Text(f.name, style = ClyoType.title, color = c.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Hairline()
            SheetAction(Icons.Outlined.Wallpaper, "Set as wallpaper now", {
                selected = null
                WallpaperCycle.changeNow(context, f.path)
                state.toast("Setting wallpaper…")
            }, hint = "Uses the framing saved for this image")
            SheetAction(Icons.Outlined.Crop, "Frame and set", { selected = null; state.push(Overlay.WallpaperEditor(f.path)) })
            SheetAction(
                if (inPlaylist) Icons.Outlined.PlaylistRemove else Icons.Outlined.PlaylistAdd,
                if (inPlaylist) "Remove from playlist" else "Add to playlist",
                { selected = null; WallpaperPrefs.togglePlaylist(context, f.path) },
            )
            if (canGenerateWallpaper(f)) SheetAction(Icons.Outlined.AutoAwesome, "Create wallpaper from this", { selected = null; state.generateFor.value = f })
            SheetAction(Icons.Outlined.FolderOpen, "Show in Files", { selected = null; state.openFolder(f.path.substringBeforeLast('/', "")) })
            SheetAction(Icons.Outlined.Delete, "Remove from wallpapers", {
                selected = null
                state.run("Removed from wallpapers", { reload++ }) { state.api.updateMeta(f.path, isWallpaper = false) }
                if (inPlaylist) WallpaperPrefs.togglePlaylist(context, f.path)
            }, danger = true)
        }
    }
}

@Composable
private fun MarkBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), style = ClyoType.micro, color = Color.White,
        modifier = modifier.background(Color.Black.copy(alpha = 0.65f), dev.weexnes.clyocloud.ui.components.RadiusTag).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ─── Auto change ─────────────────────────────────────────────────────────────

private val INTERVALS = listOf(15, 30, 60, 180, 360, 720, 1440)

private fun intervalLabel(minutes: Int) = when {
    minutes < 60 -> "$minutes min"
    minutes % 1440 == 0 -> if (minutes == 1440) "1 day" else "${minutes / 1440} days"
    minutes % 60 == 0 -> if (minutes == 60) "1 hour" else "${minutes / 60} hours"
    else -> "$minutes min"
}

private fun countdown(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

@Composable
private fun AutoChange() {
    val state = LocalAppState.current
    val context = LocalContext.current
    val c = Clyo.colors
    val prefsVersion = rememberPrefsVersion()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    // Re-read on every preference change, so the background job's updates show up live
    val enabled = remember(prefsVersion) { WallpaperPrefs.cycleEnabled(context) }
    val interval = remember(prefsVersion) { WallpaperPrefs.intervalMinutes(context) }
    val shuffle = remember(prefsVersion) { WallpaperPrefs.shuffle(context) }
    val source = remember(prefsVersion) { WallpaperPrefs.source(context) }
    val target = remember(prefsVersion) { WallpaperPrefs.target(context) }
    val unmetered = remember(prefsVersion) { WallpaperPrefs.unmeteredOnly(context) }
    val current = remember(prefsVersion) { WallpaperPrefs.current(context) }
    val nextRun = remember(prefsVersion) { WallpaperPrefs.nextRun(context) }
    val lastChanged = remember(prefsVersion) { WallpaperPrefs.lastChanged(context) }
    val lastError = remember(prefsVersion) { WallpaperPrefs.lastError(context) }
    val playlist = remember(prefsVersion) { WallpaperPrefs.playlist(context) }
    var intervalMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Status: big numbers, hairlines between, no boxes
        Row(Modifier.fillMaxWidth()) {
            HeroStat(
                when {
                    !enabled -> "Off"
                    nextRun > now -> countdown(nextRun - now)
                    else -> "Soon"
                },
                if (enabled) "Next change" else "Auto change",
                Modifier.weight(1f),
            )
            HeroStat(if (source == CycleSource.Playlist) playlist.size.toString() else "All", if (source == CycleSource.Playlist) "In playlist" else "Wallpapers", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Hairline()
        Spacer(Modifier.height(12.dp))
        SmallText("Current", color = c.text3)
        Text(current?.substringAfterLast('/') ?: "None yet", style = ClyoType.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (lastChanged > 0) NumText("Changed ${formatDate(lastChanged)}", color = c.text3)
        if (enabled && lastError != null) {
            Spacer(Modifier.height(8.dp))
            StatusLine(lastError, c.warning)
        }
        Spacer(Modifier.height(16.dp))
        ClyoButton("Change now", { WallpaperCycle.changeNow(context); state.toast("Changing wallpaper…") }, Modifier.fillMaxWidth(), kind = ButtonKind.Primary, icon = Icons.Outlined.Wallpaper)
        Spacer(Modifier.height(32.dp))

        SectionHeader("Schedule")
        HairList {
            HairRow("Change automatically", hint = "Keeps running after restarts; no notification needed") {
                ClyoSwitch(enabled, { WallpaperCycle.setEnabled(context, it) })
            }
            Hairline()
            Box {
                HairRow("Every", onClick = { intervalMenu = true }) { NumText(intervalLabel(interval), color = c.text) }
                DropdownMenu(intervalMenu, { intervalMenu = false }, containerColor = c.bg) {
                    INTERVALS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(intervalLabel(m), style = ClyoType.title, color = if (m == interval) c.text else c.text2) },
                            onClick = { intervalMenu = false; WallpaperPrefs.setIntervalMinutes(context, m); WallpaperCycle.reschedule(context) },
                        )
                    }
                }
            }
            Hairline()
            HairRow("Only on Wi-Fi", hint = "Wait for an unmetered network") {
                ClyoSwitch(unmetered, { WallpaperPrefs.setUnmeteredOnly(context, it); WallpaperCycle.reschedule(context) })
            }
        }
        Spacer(Modifier.height(24.dp))

        SectionHeader("What to show")
        HairList {
            HairRow("Source") {
                Segmented(CycleSource.entries.map { if (it == CycleSource.All) "All" else "Playlist" }, source.ordinal, { WallpaperPrefs.setSource(context, CycleSource.entries[it]) })
            }
            Hairline()
            HairRow("Order") {
                Segmented(listOf("Shuffle", "In order"), if (shuffle) 0 else 1, { WallpaperPrefs.setShuffle(context, it == 0) })
            }
            Hairline()
            HairRow("Apply to") {
                Segmented(listOf("Both", "Home", "Lock"), target.ordinal, { WallpaperPrefs.setTarget(context, WallpaperTarget.entries[it]) })
            }
        }
        Spacer(Modifier.height(24.dp))

        SectionHeader("Playlist", action = { if (playlist.isNotEmpty()) ClyoButton("Clear", { WallpaperPrefs.setPlaylist(context, emptyList()) }, kind = ButtonKind.Ghost, small = true) })
        if (playlist.isEmpty()) {
            SmallText("Empty. Add wallpapers from the Library.")
        } else {
            HairList {
                playlist.forEachIndexed { i, path ->
                    if (i > 0) Hairline()
                    HairRow(
                        path.substringAfterLast('/'),
                        hint = path.substringBeforeLast('/', "").ifBlank { null },
                        leading = {
                            ServerImage(state.api.thumbnailUrl(path), state.api.connection.token, Modifier.size(36.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)), decodeSize = 120)
                        },
                    ) {
                        ClyoIconButton(Icons.Outlined.PlaylistRemove, "Remove from playlist", { WallpaperPrefs.togglePlaylist(context, path) }, size = 36.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SmallText("Android may delay a change by a few minutes to save battery. Intervals start at 15 minutes.")
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Photo widget ────────────────────────────────────────────────────────────

@Composable
private fun PhotoWidgetSettings() {
    val state = LocalAppState.current
    val context = LocalContext.current
    val c = Clyo.colors
    val prefsVersion = rememberPrefsVersion()
    val filesVersion by state.filesVersion
    var photos by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var intervalMenu by remember { mutableStateOf(false) }

    val enabled = remember(prefsVersion) { WallpaperPrefs.widgetEnabled(context) }
    val interval = remember(prefsVersion) { WallpaperPrefs.widgetIntervalMinutes(context) }
    val current = remember(prefsVersion) { WallpaperPrefs.widgetCurrent(context) }
    val hasWidget = remember(prefsVersion) { PhotoWidget.hasWidgets(context) }

    LaunchedEffect(filesVersion, reload) {
        photos = try { state.api.photoWidgets().filter { !it.isFolder && isImageFile(it.name) } } catch (_: Exception) { emptyList() }
        loading = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (!hasWidget) {
            StatusLine("Add the ClyoCloud photo widget from your home screen's widget list.", c.text3)
            Spacer(Modifier.height(16.dp))
        }
        HairList {
            HairRow("Rotate photos", hint = "Show the next photo automatically") {
                ClyoSwitch(enabled, { PhotoWidgetCycle.setEnabled(context, it) })
            }
            Hairline()
            Box {
                HairRow("Every", onClick = { intervalMenu = true }) { NumText(intervalLabel(interval), color = c.text) }
                DropdownMenu(intervalMenu, { intervalMenu = false }, containerColor = c.bg) {
                    INTERVALS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(intervalLabel(m), style = ClyoType.title, color = if (m == interval) c.text else c.text2) },
                            onClick = {
                                intervalMenu = false
                                WallpaperPrefs.setWidgetIntervalMinutes(context, m)
                                if (WallpaperPrefs.widgetEnabled(context)) PhotoWidgetCycle.scheduleNext(context)
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ClyoButton("Next photo now", { PhotoWidgetCycle.nextNow(context); state.toast("Updating the widget…") }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))

        SectionHeader("Photos (${photos.size})", action = { ClyoIconButton(Icons.Outlined.Refresh, "Reload", { reload++ }, size = 36.dp) })
        when {
            loading -> Spinner()
            photos.isEmpty() -> SmallText("No photos yet. In Files, open a photo's details and turn on Photo widget.")
            else -> HairList {
                photos.forEachIndexed { i, f ->
                    if (i > 0) Hairline()
                    HairRow(
                        f.name,
                        hint = if (f.path == current) "Showing now" else f.path.substringBeforeLast('/', "").ifBlank { null },
                        leading = { ServerImage(state.api.thumbnailUrl(f.path), state.api.connection.token, Modifier.size(40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)), decodeSize = 120) },
                    ) {
                        ClyoIconButton(Icons.Outlined.Delete, "Remove from photo widget", {
                            state.run("Removed from photo widget", { reload++ }) { state.api.updateMeta(f.path, isPhotoWidget = false) }
                        }, size = 36.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

