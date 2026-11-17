package dev.weexnes.clyocloud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.MediaItem
import dev.weexnes.clyocloud.data.UiPrefs
import dev.weexnes.clyocloud.data.formatFileSize
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.Overlay
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoIconButton
import dev.weexnes.clyocloud.ui.components.ClyoTextField
import dev.weexnes.clyocloud.ui.components.EmptyState
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.MicroLabel
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.RadiusCard
import dev.weexnes.clyocloud.ui.components.Segmented
import dev.weexnes.clyocloud.ui.components.ServerImage
import dev.weexnes.clyocloud.ui.components.Spinner
import dev.weexnes.clyocloud.ui.components.VideoPlayer
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoType
import kotlinx.coroutines.delay

private const val PAGE_SIZE = 30

@Composable
fun MediaScreen() {
    val c = Clyo.colors
    var mode by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Media", style = ClyoType.title.copy(fontWeight = FontWeight.Medium), color = c.text, modifier = Modifier.weight(1f))
                Segmented(listOf("Library", "Shorts"), mode, { mode = it })
            }
            Hairline()
        }
        Box(Modifier.weight(1f)) {
            if (mode == 0) MediaLibrary() else ShortsFeed()
        }
    }
}

/** Videos and slideshows, searchable, loaded page by page. */
@Composable
private fun MediaLibrary() {
    val state = LocalAppState.current
    val c = Clyo.colors
    var query by rememberSaveable { mutableStateOf("") }
    var appliedQuery by rememberSaveable { mutableStateOf("") }
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(appliedQuery, page, reload) {
        loading = true
        try {
            val result = state.api.videos(appliedQuery.ifBlank { null }, page, PAGE_SIZE)
            // The list can shift between pages; never show (or key) the same item twice
            items = if (page == 1) result.items.distinctBy { it.path } else (items + result.items).distinctBy { it.path }
            total = result.total
        } catch (_: Exception) {
            state.toast("Could not load media")
        }
        loading = false
    }

    LazyVerticalGrid(
        GridCells.Adaptive(160.dp),
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClyoTextField(query, { query = it }, Modifier.weight(1f), placeholder = "Search media", leadingIcon = Icons.Outlined.Search,
                    onImeAction = { appliedQuery = query; page = 1 })
                ClyoIconButton(Icons.Outlined.Refresh, "Refresh", { page = 1; reload++ })
            }
        }
        if (total > 0) item(span = { GridItemSpan(maxLineSpan) }) { MicroLabel("$total items") }
        items(items, key = { it.path }) { m -> MediaCard(m) { openMedia(state, m) } }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                when {
                    loading -> Spinner()
                    items.isEmpty() -> EmptyState(if (appliedQuery.isBlank()) "No videos or slideshows yet" else "Nothing matches \"$appliedQuery\"")
                    items.size < total -> ClyoButton("Load more", { page++ }, small = true)
                }
            }
        }
    }
}

private fun openMedia(state: dev.weexnes.clyocloud.ui.AppState, m: MediaItem) {
    if (m.type == "slideshow") {
        val images = m.images.map { CloudFile(it.substringAfterLast('/'), it, 0, "", 0, false) }
        if (images.isNotEmpty()) state.push(Overlay.Viewer(images, 0))
    } else {
        state.push(Overlay.Viewer(listOf(CloudFile(m.name, m.path, m.size, "", m.lastModified, false)), 0))
    }
}

@Composable
private fun MediaCard(m: MediaItem, onClick: () -> Unit) {
    val state = LocalAppState.current
    val c = Clyo.colors
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RadiusCard).border(1.dp, c.line, RadiusCard)) {
            val thumb = if (m.type == "slideshow") m.images.firstOrNull() else m.path
            if (thumb != null) ServerImage(state.api.thumbnailUrl(thumb), state.api.connection.token, Modifier.fillMaxSize(), decodeSize = 480)
            if (m.type == "slideshow") {
                Text(
                    "${m.images.size} photos", style = ClyoType.micro, color = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), dev.weexnes.clyocloud.ui.components.RadiusTag).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(m.name, style = ClyoType.small, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumText(formatFileSize(m.size), color = c.text3)
            m.tags.take(2).forEach { NumText(it.name, color = c.text3) }
        }
    }
}

/** Full-screen vertical feed of random videos and slideshows. */
@Composable
private fun ShortsFeed() {
    val state = LocalAppState.current
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(UiPrefs.shortsMuted(context)) }

    LaunchedEffect(reload) {
        loading = true
        items = try { state.api.videos(page = 1, limit = 200, shuffle = true).items } catch (_: Exception) { emptyList() }
        loading = false
    }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner() }
        items.isEmpty() -> EmptyState("Nothing in your Shorts feed yet. Choose folders under Settings → Media on the website.", actionText = "Reload", onAction = { reload++ })
        else -> {
            val pager = rememberPagerState { items.size }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    val m = items[page]
                    val active = page == pager.currentPage
                    Box(Modifier.fillMaxSize()) {
                        if (m.type == "slideshow") Slideshow(m.images, active)
                        else if (active) VideoPlayer(state.api.streamUrl(m.path), state.api.connection.token, Modifier.fillMaxSize(), autoPlay = true, muted = muted, loop = true, showControls = false)
                        else ServerImage(state.api.thumbnailUrl(m.path), state.api.connection.token, Modifier.fillMaxSize(), decodeSize = 720, contentScale = ContentScale.Fit, placeholder = {})

                        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.45f)).padding(16.dp)) {
                            Text(m.name, style = ClyoType.title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            NumText(m.path.substringBeforeLast('/', ""), color = Color(0xFFA3A3A3))
                        }
                    }
                }
                Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    ClyoIconButton(Icons.Outlined.FolderOpen, "Open folder", { state.openFolder(items[pager.currentPage].path.substringBeforeLast('/', "")) }, tint = Color.White)
                    ClyoIconButton(
                        if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                        if (muted) "Unmute" else "Mute",
                        { muted = !muted; UiPrefs.setShortsMuted(context, muted) },
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

/** Shows each image for 5 seconds while the page is visible. */
@Composable
private fun Slideshow(images: List<String>, active: Boolean) {
    val state = LocalAppState.current
    if (images.isEmpty()) return
    var index by remember(images) { mutableIntStateOf(0) }
    LaunchedEffect(images, active) {
        while (active) {
            delay(5000)
            index = (index + 1) % images.size
        }
    }
    ServerImage(state.api.fileUrl(images[index]), state.api.connection.token, Modifier.fillMaxSize(), decodeSize = 1600, contentScale = ContentScale.Fit, diskCache = false, placeholder = {})
}

