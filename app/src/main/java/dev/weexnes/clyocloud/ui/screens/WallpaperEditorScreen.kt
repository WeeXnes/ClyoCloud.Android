package dev.weexnes.clyocloud.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.ImageLoader
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.components.ButtonKind
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.MicroLabel
import dev.weexnes.clyocloud.ui.components.RadiusCard
import dev.weexnes.clyocloud.ui.components.Segmented
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.components.Spinner
import dev.weexnes.clyocloud.ui.components.TopBar
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.wallpaper.Framing
import dev.weexnes.clyocloud.wallpaper.WallpaperApplier
import dev.weexnes.clyocloud.wallpaper.WallpaperCycle
import dev.weexnes.clyocloud.wallpaper.WallpaperPrefs
import dev.weexnes.clyocloud.wallpaper.WallpaperTarget
import kotlin.math.roundToInt

/** Frame a wallpaper for this phone's screen: drag to move, pinch or slide to zoom. */
@Composable
fun WallpaperEditorScreen(path: String, onClose: () -> Unit) {
    val state = LocalAppState.current
    val context = LocalContext.current
    val c = Clyo.colors
    val (screenW, screenH) = remember { WallpaperApplier.screenSize(context) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var framing by remember { mutableStateOf(WallpaperPrefs.framing(context, path) ?: Framing()) }
    var target by remember { mutableStateOf(WallpaperPrefs.target(context)) }

    LaunchedEffect(path) {
        bitmap = ImageLoader.load(context, state.api.fileUrl(path), state.api.connection.token, 2048, diskCache = false)
        failed = bitmap == null
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar("Frame wallpaper", subtitle = path.substringAfterLast('/'), onBack = onClose)
        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            val b = bitmap
            when {
                b != null -> FramePreview(b, screenW, screenH, framing) { framing = it }
                failed -> SmallText("Could not load this image")
                else -> Spinner()
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicroLabel("Zoom", Modifier.padding(end = 12.dp))
                Slider(
                    framing.scale, { framing = framing.copy(scale = it) }, valueRange = 1f..4f, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = c.invert, activeTrackColor = c.invert, inactiveTrackColor = c.lineStrong),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicroLabel("Apply to", Modifier.weight(1f))
                Segmented(listOf("Both", "Home", "Lock"), target.ordinal, { target = WallpaperTarget.entries[it] })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClyoButton("Reset", { framing = Framing() }, kind = ButtonKind.Ghost)
                Spacer(Modifier.weight(1f))
                ClyoButton("Save framing", {
                    WallpaperPrefs.setFraming(context, path, framing)
                    state.toast("Framing saved for the wallpaper cycle")
                })
                ClyoButton("Set wallpaper", {
                    WallpaperPrefs.setFraming(context, path, framing)
                    WallpaperPrefs.setTarget(context, target)
                    WallpaperCycle.changeNow(context, path)
                    state.toast("Setting wallpaper…")
                    onClose()
                }, kind = ButtonKind.Primary, enabled = bitmap != null)
            }
        }
    }
}

/** Draws exactly the region WallpaperApplier.frame() will crop, in a screen-shaped frame. */
@Composable
private fun FramePreview(bitmap: Bitmap, screenW: Int, screenH: Int, framing: Framing, onChange: (Framing) -> Unit) {
    val c = Clyo.colors
    val image: ImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    // The gesture handler outlives recompositions; read the latest framing through this
    val latest by rememberUpdatedState(framing)
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val ratio = screenW.toFloat() / screenH
        Box(
            Modifier
                .aspectRatio(ratio, matchHeightConstraintsFirst = maxWidth / maxHeight > ratio)
                .clip(RadiusCard)
                .border(1.dp, c.lineStrong, RadiusCard)
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val boxW = size.width.toFloat()
                        val framing = latest
                        val scale = (framing.scale * zoom).coerceIn(1f, 4f)
                        val cover = maxOf(screenW.toFloat() / bitmap.width, screenH.toFloat() / bitmap.height) * scale
                        val cropW = screenW / cover
                        val cropH = screenH / cover
                        val maxX = ((bitmap.width - cropW) / 2f).coerceAtLeast(0.0001f)
                        val maxY = ((bitmap.height - cropH) / 2f).coerceAtLeast(0.0001f)
                        val srcPerPx = cropW / boxW // source pixels per preview pixel
                        onChange(
                            Framing(
                                scale,
                                (framing.panX - pan.x * srcPerPx / maxX).coerceIn(-1f, 1f),
                                (framing.panY - pan.y * srcPerPx / maxY).coerceIn(-1f, 1f),
                            ),
                        )
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val scale = framing.scale.coerceIn(1f, 4f)
                val cover = maxOf(screenW.toFloat() / bitmap.width, screenH.toFloat() / bitmap.height) * scale
                val cropW = screenW / cover
                val cropH = screenH / cover
                val maxX = (bitmap.width - cropW) / 2f
                val maxY = (bitmap.height - cropH) / 2f
                val left = (bitmap.width / 2f + framing.panX * maxX - cropW / 2f).coerceIn(0f, (bitmap.width - cropW).coerceAtLeast(0f))
                val top = (bitmap.height / 2f + framing.panY * maxY - cropH / 2f).coerceIn(0f, (bitmap.height - cropH).coerceAtLeast(0f))
                drawImage(
                    image,
                    srcOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    srcSize = IntSize(cropW.roundToInt().coerceAtMost(bitmap.width), cropH.roundToInt().coerceAtMost(bitmap.height)),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                )
            }
        }
    }
}

