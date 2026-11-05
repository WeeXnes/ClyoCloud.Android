package dev.weexnes.clyocloud.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.ApiException
import dev.weexnes.clyocloud.data.CloudFile
import dev.weexnes.clyocloud.data.PipelineJob
import dev.weexnes.clyocloud.data.ScenePreset
import dev.weexnes.clyocloud.data.UpscaleModel
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.components.ButtonKind
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoSheet
import dev.weexnes.clyocloud.ui.components.ClyoTextField
import dev.weexnes.clyocloud.ui.components.HairList
import dev.weexnes.clyocloud.ui.components.HairRow
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.MicroLabel
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.ProgressBar
import dev.weexnes.clyocloud.ui.components.RadiusCard
import dev.weexnes.clyocloud.ui.components.ServerImage
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.components.Spinner
import dev.weexnes.clyocloud.ui.components.StatusLine
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoType
import dev.weexnes.clyocloud.wallpaper.WallpaperCycle
import dev.weexnes.clyocloud.wallpaper.WallpaperPrefs
import kotlinx.coroutines.delay

// Shown until the server's list arrives (and if it can't be reached), same as the web UI
private val FALLBACK_MODELS = listOf(
    UpscaleModel("4x-UltraSharp.pth", "4x-UltraSharp (Recommended)", 4),
    UpscaleModel("RealESRGAN_x2plus.pth", "RealESRGAN 2x (Fast)", 2),
    UpscaleModel("4x_foolhardy_Remacri.pth", "4x-Remacri (Smooth)", 4),
)

private val SCENES = listOf(
    Triple("indoor", "Indoor", "Room walls, bedding, ambient light. No curtains or clutter."),
    Triple("outdoor", "Outdoor", "Natural scenery and soft bokeh. Avoids paths and roads."),
    Triple("studio", "Studio", "Seamless backdrop, smooth gradient, neutral light."),
)

/** ComfyUI Wallpaper V2: outpaint and upscale an image to 2880×1800 on the user's GPU workstation. */
@Composable
fun GenerateWallpaperSheet(file: CloudFile, onDismiss: () -> Unit) {
    val state = LocalAppState.current
    val context = LocalContext.current
    val c = Clyo.colors

    var models by remember { mutableStateOf(FALLBACK_MODELS) }
    var presets by remember { mutableStateOf<List<ScenePreset>>(emptyList()) }
    var model by remember { mutableStateOf(FALLBACK_MODELS.first().id) }
    var scene by remember { mutableStateOf("indoor") }
    var customPrompts by remember { mutableStateOf(false) }
    var positive by remember { mutableStateOf("") }
    var negative by remember { mutableStateOf("") }
    var modelMenu by remember { mutableStateOf(false) }

    var job by remember { mutableStateOf<PipelineJob?>(null) }
    var starting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }

    fun syncPrompts(id: String) {
        presets.firstOrNull { it.id == id }?.let { positive = it.positive; negative = it.negative }
    }

    // Options and the user's defaults from the server
    LaunchedEffect(Unit) {
        try {
            val (m, p) = state.api.pipelineOptions()
            if (m.isNotEmpty()) models = m
            presets = p
        } catch (_: Exception) {}
        try {
            val settings = state.api.userSettings()
            settings.optString("defaultUpscaleModel").takeIf { id -> models.any { it.id == id } }?.let { model = it }
            settings.optString("defaultScenePreset").takeIf { it in SCENES.map { s -> s.first } }?.let { scene = it }
        } catch (_: Exception) {}
        if (models.none { it.id == model }) model = models.first().id
        syncPrompts(scene)
    }

    // Start, then follow the job until it finishes
    LaunchedEffect(attempt) {
        if (attempt == 0) return@LaunchedEffect
        starting = true
        error = null
        job = null
        try {
            var current = state.api.startWallpaperJob(
                file.path, model, scene,
                if (customPrompts) positive else null,
                if (customPrompts) negative else null,
            )
            job = current
            starting = false
            while (!current.isFinished) {
                delay(1500)
                current = try { state.api.pipelineJob(current.id) } catch (e: ApiException) {
                    if (e.code == 404) current.copy(status = "failed", error = "The server restarted and lost this job.") else current
                }
                job = current
            }
            if (current.status == "completed") state.filesChanged()
        } catch (e: ApiException) {
            error = e.message
        } catch (_: Exception) {
            error = "Could not reach the server"
        }
        starting = false
    }

    val running = starting || (job != null && job?.isFinished == false)

    ClyoSheet(onDismiss = {
        if (running) state.toast("Still generating on the server; it will appear in Wallpapers when done")
        onDismiss()
    }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Create wallpaper", style = ClyoType.sectionTitle, color = c.text)
                    SmallText(file.name, maxLines = 1)
                }
                NumText("2880×1800", color = c.text2)
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RadiusCard).border(1.dp, c.line, RadiusCard).background(Color.Black)) {
                ServerImage(state.api.thumbnailUrl(file.path), state.api.connection.token, Modifier.fillMaxSize(), decodeSize = 720, contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.height(20.dp))

            val j = job
            when {
                // Finished
                j != null && j.status == "completed" -> {
                    StatusLine("Saved as ${j.outputName}", c.success)
                    SmallText("It's next to the original, tagged and added to your wallpapers.")
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClyoButton("Add to playlist", {
                            j.outputPath?.let { if (it !in WallpaperPrefs.playlist(context)) WallpaperPrefs.togglePlaylist(context, it) }
                            state.toast("Added to playlist")
                        })
                        ClyoButton("Set as wallpaper", {
                            j.outputPath?.let { WallpaperCycle.changeNow(context, it) }
                            state.toast("Setting wallpaper…")
                            onDismiss()
                        }, kind = ButtonKind.Primary)
                    }
                }
                // Running
                running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spinner(size = 14.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(j?.message ?: "Connecting to ComfyUI…", style = ClyoType.small, color = c.text, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        NumText("${j?.progress ?: 5}%")
                    }
                    Spacer(Modifier.height(8.dp))
                    ProgressBar((j?.progress ?: 5) / 100f)
                    Spacer(Modifier.height(8.dp))
                    SmallText("This takes about a minute on the GPU. You can close this; the wallpaper is saved either way.")
                }
                // Setup (and after a failure)
                else -> {
                    val failure = error ?: j?.error
                    if (failure != null) {
                        StatusLine(failure, c.danger)
                        SmallText("Check that ComfyUI is running and the URL under Settings → AI Wallpaper Pipeline on the website is right.")
                        Spacer(Modifier.height(16.dp))
                    }

                    MicroLabel("Scene", Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SCENES.forEach { (id, label, hint) ->
                            val active = id == scene
                            val border by animateColorAsState(if (active) c.text else c.line, label = "scene")
                            Row(
                                Modifier.fillMaxWidth().clip(RadiusCard).border(1.dp, border, RadiusCard)
                                    .background(if (active) c.surface2 else Color.Transparent)
                                    .clickable { scene = id; syncPrompts(id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(label, style = ClyoType.title, color = c.text)
                                    SmallText(hint)
                                }
                                if (active) Icon(Icons.Outlined.Check, null, tint = c.text, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    MicroLabel("Upscale model", Modifier.padding(bottom = 8.dp))
                    Box {
                        HairList {
                            HairRow(models.firstOrNull { it.id == model }?.name ?: model, onClick = { modelMenu = true }) {
                                Icon(Icons.Outlined.ExpandMore, null, tint = c.text3)
                            }
                        }
                        DropdownMenu(modelMenu, { modelMenu = false }, containerColor = c.bg) {
                            models.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name, style = ClyoType.title, color = if (m.id == model) c.text else c.text2) },
                                    onClick = { model = m.id; modelMenu = false },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    HairList {
                        HairRow("Custom prompts", hint = if (customPrompts) "In use" else "Optional", onClick = { customPrompts = !customPrompts }) {
                            Icon(if (customPrompts) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = c.text3)
                        }
                        if (customPrompts) {
                            Hairline()
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ClyoTextField(positive, { positive = it.take(4000) }, label = "Positive prompt", singleLine = false, minLines = 3, mono = true)
                                ClyoTextField(negative, { negative = it.take(4000) }, label = "Negative prompt", singleLine = false, minLines = 3, mono = true)
                                ClyoButton("Reset to preset", { syncPrompts(scene) }, kind = ButtonKind.Ghost, small = true)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    ClyoButton(if (failure != null) "Try again" else "Generate", { attempt++ }, Modifier.fillMaxWidth(), kind = ButtonKind.Primary, icon = Icons.Outlined.AutoAwesome)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
