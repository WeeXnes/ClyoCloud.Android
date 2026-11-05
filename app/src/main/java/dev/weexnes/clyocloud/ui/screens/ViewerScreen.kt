package dev.weexnes.clyocloud.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.canGenerateWallpaper
import dev.weexnes.clyocloud.data.isVideoFile
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.components.ClyoIconButton
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.ServerImage
import dev.weexnes.clyocloud.ui.components.Spinner
import dev.weexnes.clyocloud.ui.components.VideoPlayer
import dev.weexnes.clyocloud.ui.theme.ClyoType

/** Full-screen viewer; always black, whatever the theme, like a photo app. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(files: List<CloudFile>, startIndex: Int, onClose: () -> Unit) {
    val state = LocalAppState.current
    if (files.isEmpty()) { onClose(); return }
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, files.lastIndex)) { files.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed, beyondViewportPageCount = 1) { page ->
            val file = files[page]
            if (isVideoFile(file.name)) {
                VideoPlayer(state.api.streamUrl(file.path), state.api.connection.token, Modifier.fillMaxSize(), autoPlay = page == pager.currentPage)
            } else {
                ZoomableImage(file, onTap = { chrome = !chrome }, onZoomChange = { zoomed = it })
            }
        }

        if (chrome) {
            val file = files[pager.currentPage]
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClyoIconButton(Icons.Outlined.Close, "Close", onClose, tint = Color.White)
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = ClyoType.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        NumText("${pager.currentPage + 1} / ${files.size}", color = Color(0xFFA3A3A3))
                    }
                    if (canGenerateWallpaper(file)) {
                        ClyoIconButton(Icons.Outlined.AutoAwesome, "Create wallpaper", { state.generateFor.value = file }, tint = Color.White)
                    }
                    ClyoIconButton(Icons.Outlined.Download, "Download", { state.download(state.api.fileUrl(file.path), file.name) }, tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomableImage(file: CloudFile, onTap: () -> Unit, onZoomChange: (Boolean) -> Unit) {
    val state = LocalAppState.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset = if (scale > 1f) offset + pan else Offset.Zero
        onZoomChange(scale > 1.01f)
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(file.path) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                        onZoomChange(scale > 1f)
                    },
                )
            }
            .transformable(transform, canPan = { scale > 1f }),
        contentAlignment = Alignment.Center,
    ) {
        // Full-size image decoded to ~2.5k px: sharp when zoomed, far below the memory limit
        ServerImage(
            state.api.fileUrl(file.path), state.api.connection.token,
            Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
            decodeSize = 2560, contentScale = ContentScale.Fit, diskCache = false,
            contentDescription = file.name,
            placeholder = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ServerImage(state.api.thumbnailUrl(file.path), state.api.connection.token, Modifier.fillMaxSize(), decodeSize = 320, contentScale = ContentScale.Fit, placeholder = { Spinner() })
                }
            },
        )
    }
}
