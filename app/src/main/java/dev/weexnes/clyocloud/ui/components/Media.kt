package dev.weexnes.clyocloud.ui.components

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.ImageLoader
import dev.weexnes.clyocloud.data.extensionOf
import dev.weexnes.clyocloud.data.isImageFile
import dev.weexnes.clyocloud.data.isVideoFile
import dev.weexnes.clyocloud.ui.theme.Clyo

/**
 * Image from the server, downsampled to [decodeSize] pixels on its long edge.
 * Shows [placeholder] while loading or when the image can't be loaded.
 */
@Composable
fun ServerImage(
    url: String,
    token: String?,
    modifier: Modifier = Modifier,
    decodeSize: Int = 512,
    contentScale: ContentScale = ContentScale.Crop,
    diskCache: Boolean = true,
    contentDescription: String? = null,
    placeholder: @Composable () -> Unit = { Box(Modifier.fillMaxSize().background(Clyo.colors.surface)) },
) {
    val context = LocalContext.current
    var bitmap by remember(url, decodeSize) { mutableStateOf<Bitmap?>(ImageLoader.cached(url, decodeSize)) }
    LaunchedEffect(url, decodeSize) {
        if (bitmap == null) bitmap = ImageLoader.load(context, url, token, decodeSize, diskCache)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription, Modifier.fillMaxSize(), contentScale = contentScale)
        } else {
            placeholder()
        }
    }
}

/** Decode size in pixels for something drawn [size] large. */
@Composable
fun pixels(size: Dp): Int = with(LocalDensity.current) { size.roundToPx() }

fun fileIcon(file: CloudFile): ImageVector = when {
    file.isFolder -> Icons.Outlined.Folder
    isImageFile(file.name) -> Icons.Outlined.Image
    isVideoFile(file.name) -> Icons.Outlined.Movie
    else -> when (extensionOf(file.name)) {
        "pdf" -> Icons.Outlined.PictureAsPdf
        "zip", "rar", "7z", "tar", "gz" -> Icons.Outlined.FolderZip
        "mp3", "flac", "wav", "ogg", "m4a" -> Icons.Outlined.AudioFile
        "txt", "md", "doc", "docx", "odt", "rtf" -> Icons.Outlined.Article
        "js", "ts", "kt", "java", "py", "json", "xml", "html", "css", "sh" -> Icons.Outlined.Code
        else -> Icons.Outlined.InsertDriveFile
    }
}

/** Monochrome file-type icon; colour is reserved for meaning. */
@Composable
fun FileTypeIcon(file: CloudFile, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Icon(fileIcon(file), contentDescription = null, tint = if (file.isFolder) Clyo.colors.text else Clyo.colors.text2, modifier = modifier.size(size))
}

/** Exoplayer for a server stream, sending the login token. Released when it leaves the screen. */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    token: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    muted: Boolean = false,
    loop: Boolean = false,
    showControls: Boolean = true,
) {
    val context = LocalContext.current
    val player = remember(url) {
        val dataSource = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(ProgressiveMediaSource.Factory(dataSource).createMediaSource(MediaItem.fromUri(url)))
            prepare()
            playWhenReady = autoPlay
        }
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(loop) { player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
    LaunchedEffect(autoPlay) { player.playWhenReady = autoPlay }
    DisposableEffect(player) { onDispose { player.release() } }

    AndroidView(
        modifier = modifier.background(androidx.compose.ui.graphics.Color.Black),
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = showControls
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { it.useController = showControls },
    )
}
