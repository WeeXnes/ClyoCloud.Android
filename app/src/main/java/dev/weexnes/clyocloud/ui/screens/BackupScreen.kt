package dev.weexnes.clyocloud.ui.screens

import android.Manifest
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.backup.Album
import dev.weexnes.clyocloud.backup.BackupPrefs
import dev.weexnes.clyocloud.backup.BackupRegistry
import dev.weexnes.clyocloud.backup.CameraBackup
import dev.weexnes.clyocloud.backup.MediaLibrary
import dev.weexnes.clyocloud.data.formatDate
import dev.weexnes.clyocloud.ui.LocalAppState
import dev.weexnes.clyocloud.ui.components.ButtonKind
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoDialog
import dev.weexnes.clyocloud.ui.components.ClyoSwitch
import dev.weexnes.clyocloud.ui.components.ClyoTextField
import dev.weexnes.clyocloud.ui.components.HairList
import dev.weexnes.clyocloud.ui.components.HairRow
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.HeroStat
import dev.weexnes.clyocloud.ui.components.NumText
import dev.weexnes.clyocloud.ui.components.ProgressBar
import dev.weexnes.clyocloud.ui.components.SectionHeader
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.components.StatusLine
import dev.weexnes.clyocloud.ui.components.TopBar
import dev.weexnes.clyocloud.ui.theme.Clyo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val MEDIA_PERMISSIONS = arrayOf(
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.READ_MEDIA_VIDEO,
    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
)

@Composable
fun BackupScreen(onClose: () -> Unit) {
    val state = LocalAppState.current
    val context = LocalContext.current
    val c = Clyo.colors

    // Re-read settings whenever they change (also from the background job)
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        BackupPrefs.register(context, l)
        onDispose { BackupPrefs.unregister(context, l) }
    }
    val enabled = remember(version) { BackupPrefs.enabled(context) }
    var folder by remember { mutableStateOf(BackupPrefs.folder(context)) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var selected by remember(version) { mutableStateOf(MediaLibrary.selectedAlbums(context)) }
    var pending by remember { mutableIntStateOf(-1) }
    var permitted by remember { mutableStateOf(MediaLibrary.hasPermission(context) || MediaLibrary.hasOnlySelectedAccess(context)) }
    var askStart by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(CameraBackup.progress) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = MediaLibrary.hasPermission(context) || MediaLibrary.hasOnlySelectedAccess(context)
        if (permitted) askStart = true else state.toast("Photo access is needed for camera backup")
    }

    LaunchedEffect(permitted, version) {
        if (!permitted) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            albums = MediaLibrary.albums(context)
            pending = MediaLibrary.pending(context).size
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            progress = CameraBackup.progress
            delay(1000)
        }
    }

    fun turnOn() {
        if (!permitted) permissionLauncher.launch(MEDIA_PERMISSIONS) else askStart = true
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar("Camera backup", onBack = onClose)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                HeroStat(if (pending < 0) "…" else pending.toString(), "Waiting", Modifier.weight(1f))
                HeroStat(BackupRegistry.count(context).toString(), "Backed up", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            val p = progress
            if (p != null) {
                SmallText("Uploading ${p.first + 1} of ${p.second}", color = c.text2)
                Spacer(Modifier.height(6.dp))
                ProgressBar(p.first.toFloat() / p.second.coerceAtLeast(1))
            } else if (enabled) {
                val last = BackupPrefs.lastRun(context)
                StatusLine(BackupPrefs.lastResult(context) ?: "Waiting for the first run", if (BackupPrefs.lastResult(context)?.startsWith("Backed up") == true || BackupPrefs.lastResult(context) == "Up to date") c.success else c.text3)
                if (last > 0) NumText("Last check ${formatDate(last)}", color = c.text3)
            } else {
                SmallText("Backup is off.")
            }
            if (MediaLibrary.hasOnlySelectedAccess(context)) {
                Spacer(Modifier.height(8.dp))
                StatusLine("Only the photos you selected are accessible. Allow all photos to back up new ones.", c.warning)
            }
            Spacer(Modifier.height(16.dp))
            if (enabled) {
                ClyoButton("Back up now", { CameraBackup.runNow(context); state.toast("Backup started") }, Modifier.fillMaxWidth(), kind = ButtonKind.Primary, icon = Icons.Outlined.Backup)
            } else {
                ClyoButton("Turn on camera backup", ::turnOn, Modifier.fillMaxWidth(), kind = ButtonKind.Primary, icon = Icons.Outlined.Backup)
            }
            Spacer(Modifier.height(32.dp))

            SectionHeader("Backup")
            HairList {
                HairRow("Camera backup", hint = "New photos upload automatically, in the background") {
                    ClyoSwitch(enabled, { on -> if (on) turnOn() else CameraBackup.setEnabled(context, false) })
                }
                Hairline()
                HairRow("Include videos") {
                    ClyoSwitch(BackupPrefs.includeVideos(context), { BackupPrefs.setIncludeVideos(context, it); version++ })
                }
                Hairline()
                HairRow("Only on Wi-Fi", hint = "Never use mobile data") {
                    ClyoSwitch(BackupPrefs.wifiOnly(context), { BackupPrefs.setWifiOnly(context, it); CameraBackup.schedule(context) })
                }
                Hairline()
                HairRow("Only while charging") {
                    ClyoSwitch(BackupPrefs.chargingOnly(context), { BackupPrefs.setChargingOnly(context, it); CameraBackup.schedule(context) })
                }
            }
            Spacer(Modifier.height(24.dp))

            SectionHeader("Where")
            ClyoTextField(folder, { folder = it }, label = "Folder in ClyoCloud", mono = true, onImeAction = { BackupPrefs.setFolder(context, folder) })
            if (folder.trim('/') != BackupPrefs.folder(context)) {
                Spacer(Modifier.height(8.dp))
                ClyoButton("Save folder", { BackupPrefs.setFolder(context, folder); state.toast("Saved") }, small = true, enabled = folder.isNotBlank())
            }
            Spacer(Modifier.height(12.dp))
            HairList {
                HairRow("Sort into year and month", hint = CameraBackup.folderFor(context, System.currentTimeMillis())) {
                    ClyoSwitch(BackupPrefs.byMonth(context), { BackupPrefs.setByMonth(context, it); version++ })
                }
            }
            Spacer(Modifier.height(24.dp))

            SectionHeader("Albums")
            when {
                !permitted -> SmallText("Allow photo access to choose albums.")
                albums.isEmpty() -> SmallText("No photos found on this phone.")
                else -> HairList {
                    albums.forEachIndexed { i, a ->
                        if (i > 0) Hairline()
                        HairRow(a.name, hint = "${a.count} items · ${a.path.trimEnd('/')}") {
                            ClyoSwitch(a.id in selected, { on ->
                                selected = if (on) selected + a.id else selected - a.id
                                BackupPrefs.setAlbums(context, selected)
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SmallText("Files already in the backup folder with the same name and size are skipped; different files with the same name get a number added. Nothing is deleted from your phone.")
            Spacer(Modifier.height(24.dp))
        }
    }

    if (askStart) {
        ClyoDialog(
            "Back up which photos?",
            { askStart = false },
            confirmText = "Only new ones",
            onConfirm = { askStart = false; CameraBackup.setEnabled(context, true, fromNow = true); version++ },
            dismissText = "Cancel",
        ) {
            SmallText("Start with photos taken from now on, or also upload everything already in the chosen albums.")
            ClyoButton("Everything, including existing", {
                askStart = false
                CameraBackup.setEnabled(context, true, fromNow = false)
                version++
            }, Modifier.fillMaxWidth())
        }
    }
}
