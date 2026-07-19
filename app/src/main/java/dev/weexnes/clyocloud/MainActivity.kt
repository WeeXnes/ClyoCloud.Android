package dev.weexnes.clyocloud

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import dev.weexnes.clyocloud.ui.theme.ClyoCloudTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode


val QrCodeIcon: ImageVector
    get() = ImageVector.Builder(
        name = "QrCode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(3f, 3f)
        lineTo(3f, 9f)
        lineTo(9f, 9f)
        lineTo(9f, 3f)
        lineTo(3f, 3f)
        close()
        moveTo(7f, 7f)
        lineTo(5f, 7f)
        lineTo(5f, 5f)
        lineTo(7f, 5f)
        lineTo(7f, 7f)
        close()
        
        moveTo(3f, 15f)
        lineTo(3f, 21f)
        lineTo(9f, 21f)
        lineTo(9f, 15f)
        lineTo(3f, 15f)
        close()
        moveTo(7f, 19f)
        lineTo(5f, 19f)
        lineTo(5f, 17f)
        lineTo(7f, 17f)
        lineTo(7f, 19f)
        close()
        
        moveTo(15f, 3f)
        lineTo(15f, 9f)
        lineTo(21f, 9f)
        lineTo(21f, 3f)
        lineTo(15f, 3f)
        close()
        moveTo(19f, 7f)
        lineTo(17f, 7f)
        lineTo(17f, 5f)
        lineTo(19f, 5f)
        lineTo(19f, 7f)
        close()
        
        moveTo(15f, 15f)
        lineTo(15f, 17f)
        lineTo(17f, 17f)
        lineTo(17f, 15f)
        lineTo(15f, 15f)
        close()
        
        moveTo(17f, 17f)
        lineTo(17f, 19f)
        lineTo(19f, 19f)
        lineTo(19f, 17f)
        lineTo(17f, 17f)
        close()
        
        moveTo(19f, 19f)
        lineTo(19f, 21f)
        lineTo(21f, 21f)
        lineTo(21f, 19f)
        lineTo(19f, 19f)
        close()
        
        moveTo(21f, 15f)
        lineTo(21f, 17f)
        lineTo(19f, 17f)
        lineTo(19f, 15f)
        lineTo(21f, 15f)
        close()
        
        moveTo(15f, 19f)
        lineTo(15f, 21f)
        lineTo(17f, 21f)
        lineTo(17f, 19f)
        lineTo(15f, 19f)
        close()
    }.build()

val DeleteIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Delete",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(6f, 19f)
        curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
        lineTo(16f, 21f)
        curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
        lineTo(18f, 7f)
        lineTo(6f, 7f)
        close()
        moveTo(19f, 4f)
        lineTo(15.5f, 4f)
        lineTo(14.5f, 3f)
        lineTo(9.5f, 3f)
        lineTo(8.5f, 4f)
        lineTo(5f, 4f)
        lineTo(5f, 6f)
        lineTo(19f, 6f)
        close()
    }.build()

val VolumeOffIcon: ImageVector
    get() = ImageVector.Builder(
        name = "VolumeOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(16.5f, 12f)
        curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 8f)
        lineTo(14f, 10.27f)
        lineTo(16.25f, 12.52f)
        curveTo(16.41f, 12.35f, 16.5f, 12.18f, 16.5f, 12f)
        close()
        moveTo(19f, 12f)
        curveTo(19f, 12.94f, 18.8f, 13.82f, 18.46f, 14.64f)
        lineTo(19.97f, 16.15f)
        curveTo(20.63f, 14.91f, 21f, 13.5f, 21f, 12f)
        curveTo(21f, 7.72f, 18f, 4.14f, 14f, 3.23f)
        lineTo(14f, 5.29f)
        curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f)
        close()
        moveTo(4.27f, 3f)
        lineTo(3f, 4.27f)
        lineTo(7.73f, 9f)
        lineTo(3f, 9f)
        lineTo(3f, 15f)
        lineTo(7f, 15f)
        lineTo(12f, 20f)
        lineTo(12f, 13.27f)
        lineTo(16.25f, 17.52f)
        curveTo(15.58f, 18.04f, 14.83f, 18.44f, 14f, 18.67f)
        lineTo(14f, 20.73f)
        curveTo(15.38f, 20.48f, 16.63f, 19.82f, 17.68f, 18.96f)
        lineTo(19.73f, 21f)
        lineTo(21f, 19.73f)
        lineTo(4.27f, 3f)
        close()
        moveTo(12f, 4f)
        lineTo(9.91f, 6.09f)
        lineTo(12f, 8.18f)
        close()
    }.build()

val VolumeUpIcon: ImageVector
    get() = ImageVector.Builder(
        name = "VolumeUp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(3f, 9f)
        lineTo(3f, 15f)
        lineTo(7f, 15f)
        lineTo(12f, 20f)
        lineTo(12f, 4f)
        lineTo(7f, 9f)
        close()
        moveTo(16.5f, 12f)
        curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 8f)
        lineTo(14f, 16f)
        curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f)
        close()
        moveTo(14f, 3.23f)
        lineTo(14f, 5.29f)
        curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f)
        curveTo(19f, 15.17f, 16.89f, 17.85f, 14f, 18.71f)
        lineTo(14f, 20.77f)
        curveTo(18f, 19.86f, 21f, 16.28f, 21f, 12f)
        curveTo(21f, 7.72f, 18f, 4.14f, 14f, 3.23f)
        close()
    }.build()

val RefreshIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Refresh",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(17.65f, 6.35f)
        curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
        curveTo(7.58f, 4f, 4.01f, 7.58f, 4.01f, 12f)
        curveTo(4.01f, 16.42f, 7.58f, 20f, 12f, 20f)
        curveTo(15.73f, 20f, 18.84f, 17.45f, 19.73f, 14f)
        lineTo(17.65f, 14f)
        curveTo(16.83f, 16.33f, 14.61f, 18f, 12f, 18f)
        curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
        curveTo(6f, 8.69f, 8.69f, 6f, 12f, 6f)
        curveTo(13.66f, 6f, 15.14f, 6.69f, 16.22f, 7.78f)
        lineTo(13f, 11f)
        lineTo(20f, 11f)
        lineTo(20f, 4f)
        close()
    }.build()

val WarningIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Warning",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(1f, 21f)
        lineTo(23f, 21f)
        lineTo(12f, 2f)
        close()
        moveTo(12f, 18f)
        curveTo(11.45f, 18f, 11f, 17.55f, 11f, 17f)
        curveTo(11f, 16.45f, 11.45f, 16f, 12f, 16f)
        curveTo(12.55f, 16f, 13f, 16.45f, 13f, 17f)
        curveTo(13f, 17.55f, 12.55f, 18f, 12f, 18f)
        close()
        moveTo(11f, 10f)
        lineTo(13f, 10f)
        lineTo(13f, 14f)
        lineTo(11f, 14f)
        close()
    }.build()

val GridViewIcon: ImageVector
    get() = ImageVector.Builder(
        name = "GridView",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(4f, 4f)
        lineTo(11f, 4f)
        lineTo(11f, 11f)
        lineTo(4f, 11f)
        close()
        moveTo(4f, 13f)
        lineTo(11f, 13f)
        lineTo(11f, 20f)
        lineTo(4f, 20f)
        close()
        moveTo(13f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 11f)
        lineTo(13f, 11f)
        close()
        moveTo(13f, 13f)
        lineTo(20f, 13f)
        lineTo(20f, 20f)
        lineTo(13f, 20f)
        close()
    }.build()

val ListViewIcon: ImageVector
    get() = ImageVector.Builder(
        name = "ListView",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(4f, 18f)
        lineTo(20f, 18f)
        lineTo(20f, 20f)
        lineTo(4f, 20f)
        close()
        moveTo(4f, 11f)
        lineTo(20f, 11f)
        lineTo(20f, 13f)
        lineTo(4f, 13f)
        close()
        moveTo(4f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 6f)
        lineTo(4f, 6f)
        close()
    }.build()

val AutoplayIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Autoplay",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(10f, 16.5f)
        lineTo(10f, 7.5f)
        lineTo(16f, 12f)
        close()
    }.build()

val UploadIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Upload",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(9f, 16f)
        lineTo(15f, 16f)
        lineTo(15f, 10f)
        lineTo(19f, 10f)
        lineTo(12f, 3f)
        lineTo(5f, 10f)
        lineTo(9f, 10f)
        close()
        moveTo(5f, 18f)
        lineTo(19f, 18f)
        lineTo(19f, 20f)
        lineTo(5f, 20f)
        close()
    }.build()

val FolderIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Folder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(10f, 4f)
        lineTo(4f, 4f)
        curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
        lineTo(2f, 18f)
        curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
        lineTo(20f, 20f)
        curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
        lineTo(22f, 6f)
        curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
        lineTo(12f, 4f)
        close()
    }.build()

val FileIcon: ImageVector
    get() = ImageVector.Builder(
        name = "File",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(14f, 2f)
        lineTo(6f, 2f)
        curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
        lineTo(4f, 20f)
        curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
        lineTo(18f, 22f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
        lineTo(20f, 8f)
        lineTo(14f, 2f)
        close()
        moveTo(13f, 9f)
        lineTo(13f, 3.5f)
        lineTo(18.5f, 9f)
        lineTo(13f, 9f)
        close()
    }.build()

val ShareIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Share",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(18f, 16.08f)
        curveTo(17.24f, 16.08f, 16.56f, 16.38f, 16.04f, 16.85f)
        lineTo(8.91f, 12.7f)
        curveTo(8.96f, 12.47f, 9f, 12.24f, 9f, 12f)
        curveTo(9f, 11.76f, 8.96f, 11.53f, 8.91f, 11.3f)
        lineTo(15.96f, 7.19f)
        curveTo(16.5f, 7.69f, 17.21f, 8f, 18f, 8f)
        curveTo(19.66f, 8f, 21f, 6.66f, 21f, 5f)
        curveTo(21f, 3.34f, 19.66f, 2f, 18f, 2f)
        curveTo(16.34f, 2f, 15f, 3.34f, 15f, 5f)
        curveTo(15f, 5.24f, 15.04f, 5.47f, 15.09f, 5.7f)
        lineTo(8.04f, 9.81f)
        curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
        curveTo(4.34f, 9f, 3f, 10.34f, 3f, 12f)
        curveTo(3f, 13.66f, 4.34f, 15f, 6f, 15f)
        curveTo(6.79f, 15f, 7.5f, 14.69f, 8.04f, 14.19f)
        lineTo(15.16f, 18.35f)
        curveTo(15.11f, 18.56f, 15.08f, 18.77f, 15.08f, 19f)
        curveTo(15.08f, 20.61f, 16.39f, 21.92f, 18f, 21.92f)
        curveTo(19.61f, 21.92f, 20.92f, 20.61f, 20.92f, 19f)
        curveTo(20.92f, 17.39f, 19.61f, 16.08f, 18f, 16.08f)
        close()
    }.build()

val DownloadIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Download",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19.35f, 10.04f)
        curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
        curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
        curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
        curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
        lineTo(19f, 20f)
        curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
        curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
        close()
        moveTo(17f, 13f)
        lineTo(12f, 18f)
        lineTo(7f, 13f)
        lineTo(10f, 13f)
        lineTo(10f, 9f)
        lineTo(14f, 9f)
        lineTo(14f, 13f)
        lineTo(17f, 13f)
        close()
    }.build()

val StarIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Star",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 17.27f)
        lineTo(18.18f, 21f)
        lineTo(16.54f, 13.97f)
        lineTo(22f, 9.24f)
        lineTo(14.81f, 8.63f)
        lineTo(12f, 2f)
        lineTo(9.19f, 8.63f)
        lineTo(2f, 9.24f)
        lineTo(7.46f, 13.97f)
        lineTo(5.82f, 21f)
        lineTo(12f, 17.27f)
        close()
    }.build()

val StarBorderIcon: ImageVector
    get() = ImageVector.Builder(
        name = "StarBorder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(22f, 9.24f)
        lineTo(14.81f, 8.62f)
        lineTo(12f, 2f)
        lineTo(9.19f, 8.63f)
        lineTo(2f, 9.24f)
        lineTo(7.46f, 13.97f)
        lineTo(5.82f, 21f)
        lineTo(12f, 17.27f)
        lineTo(18.18f, 21f)
        lineTo(16.54f, 13.97f)
        lineTo(22f, 9.24f)
        close()
        moveTo(12f, 15.4f)
        lineTo(8.24f, 17.67f)
        lineTo(9.24f, 13.39f)
        lineTo(5.92f, 10.51f)
        lineTo(10.3f, 10.13f)
        lineTo(12f, 6.1f)
        lineTo(13.7f, 10.13f)
        lineTo(18.08f, 10.51f)
        lineTo(14.76f, 13.39f)
        lineTo(15.76f, 17.67f)
        lineTo(12f, 15.4f)
        close()
    }.build()

val SearchIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Search",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(15.5f, 14f)
        lineTo(14.71f, 14f)
        lineTo(14.43f, 13.73f)
        curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
        curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
        curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
        lineTo(14f, 14.71f)
        lineTo(14f, 15.5f)
        lineTo(19f, 20.49f)
        lineTo(20.49f, 19f)
        lineTo(15.5f, 14f)
        close()
        moveTo(9.5f, 14f)
        curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
        curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
        curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
        close()
    }.build()

val PlayIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Play",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(8f, 5f)
        lineTo(8f, 19f)
        lineTo(19f, 12f)
        close()
    }.build()

val CloseIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Close",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }.build()

val SettingsIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19.14f, 12.94f)
        curveTo(19.18f, 12.63f, 19.2f, 12.32f, 19.2f, 12f)
        curveTo(19.2f, 11.68f, 19.18f, 11.37f, 19.14f, 11.06f)
        lineTo(21.1f, 9.53f)
        curveTo(21.28f, 9.39f, 21.33f, 9.14f, 21.21f, 8.94f)
        lineTo(19.35f, 5.72f)
        curveTo(19.23f, 5.51f, 18.98f, 5.43f, 18.77f, 5.51f)
        lineTo(16.46f, 6.44f)
        curveTo(15.98f, 6.07f, 15.46f, 5.76f, 14.89f, 5.53f)
        lineTo(14.54f, 3.07f)
        curveTo(14.5f, 2.85f, 14.31f, 2.69f, 14.09f, 2.69f)
        lineTo(10.37f, 2.69f)
        curveTo(10.15f, 2.69f, 9.96f, 2.85f, 9.92f, 3.07f)
        lineTo(9.57f, 5.53f)
        curveTo(9f, 5.76f, 8.48f, 6.07f, 8f, 6.44f)
        lineTo(5.69f, 5.51f)
        curveTo(5.48f, 5.43f, 5.23f, 5.51f, 5.11f, 5.72f)
        lineTo(3.25f, 8.94f)
        curveTo(3.13f, 9.14f, 3.18f, 9.39f, 3.36f, 9.53f)
        lineTo(5.32f, 11.06f)
        curveTo(5.28f, 11.37f, 5.26f, 11.68f, 5.26f, 12f)
        curveTo(5.26f, 12.32f, 5.28f, 12.63f, 5.32f, 12.94f)
        lineTo(3.36f, 14.47f)
        curveTo(3.18f, 14.61f, 3.13f, 14.86f, 3.25f, 15.06f)
        lineTo(5.11f, 18.28f)
        curveTo(5.23f, 18.49f, 5.48f, 18.57f, 5.69f, 18.49f)
        lineTo(8f, 17.56f)
        curveTo(8.48f, 17.93f, 9f, 18.24f, 9.57f, 18.47f)
        lineTo(9.92f, 20.93f)
        curveTo(9.96f, 21.15f, 10.15f, 21.31f, 10.37f, 21.31f)
        lineTo(14.09f, 21.31f)
        curveTo(14.31f, 21.31f, 14.5f, 21.15f, 14.54f, 20.93f)
        lineTo(14.89f, 18.47f)
        curveTo(15.46f, 18.24f, 15.98f, 17.93f, 16.46f, 17.56f)
        lineTo(18.77f, 18.49f)
        curveTo(18.98f, 18.57f, 19.23f, 18.49f, 19.35f, 18.28f)
        lineTo(21.21f, 15.06f)
        curveTo(21.33f, 14.86f, 21.28f, 14.61f, 21.1f, 14.47f)
        lineTo(19.14f, 12.94f)
        close()
        moveTo(12.23f, 15.62f)
        curveTo(10.23f, 15.62f, 8.61f, 14f, 8.61f, 12f)
        curveTo(8.61f, 10f, 10.23f, 8.38f, 12.23f, 8.38f)
        curveTo(14.23f, 8.38f, 15.85f, 10f, 15.85f, 12f)
        curveTo(15.85f, 14f, 14.23f, 15.62f, 12.23f, 15.62f)
        close()
    }.build()

val ArrowBackIcon: ImageVector
    get() = ImageVector.Builder(
        name = "ArrowBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(20f, 11f)
        lineTo(7.83f, 11f)
        lineTo(13.41f, 5.41f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(13.41f, 18.59f)
        lineTo(7.83f, 13f)
        lineTo(20f, 13f)
        close()
    }.build()

val AddIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Add",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19f, 13f)
        lineTo(13f, 13f)
        lineTo(13f, 19f)
        lineTo(11f, 19f)
        lineTo(11f, 13f)
        lineTo(5f, 13f)
        lineTo(5f, 11f)
        lineTo(11f, 11f)
        lineTo(11f, 5f)
        lineTo(13f, 5f)
        lineTo(13f, 11f)
        lineTo(19f, 11f)
        close()
    }.build()

val ExitIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Exit",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(10.09f, 15.59f)
        lineTo(11.5f, 17f)
        lineTo(16.5f, 12f)
        lineTo(11.5f, 7f)
        lineTo(10.09f, 8.41f)
        lineTo(12.67f, 11f)
        lineTo(3f, 11f)
        lineTo(3f, 13f)
        lineTo(12.67f, 13f)
        close()
        moveTo(19f, 3f)
        lineTo(5f, 3f)
        curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
        lineTo(3f, 9f)
        lineTo(5f, 9f)
        lineTo(5f, 5f)
        lineTo(19f, 5f)
        lineTo(19f, 19f)
        lineTo(5f, 19f)
        lineTo(5f, 15f)
        lineTo(3f, 15f)
        lineTo(3f, 19f)
        curveTo(3.89f, 21f, 5f, 21f, 5f, 21f)
        lineTo(19f, 21f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        lineTo(21f, 5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
    }.build()

// Data Models
data class CloudConnection(
    val serverUrl: String,
    val username: String,
    val token: String
)

data class TagItem(
    val name: String,
    val color: String
)

data class CloudFile(
    val name: String,
    val size: Long,
    val type: String,
    val lastModified: Long,
    val isFolder: Boolean,
    var isPublic: Boolean,
    var isFavourite: Boolean,
    val description: String?,
    val tags: List<TagItem>,
    val previewImage: String? = null,
    val fullPath: String? = null,
    var isWallpaper: Boolean = false
)

data class MediaItem(
    val type: String, // "video" or "slideshow"
    val name: String,
    val fullPath: String,
    val images: List<String>,
    val size: Long,
    val lastModified: Long,
    val isPublic: Boolean,
    val isFavourite: Boolean,
    val description: String?,
    val tags: List<TagItem>
)

enum class AppScreen {
    Login,
    Dashboard
}

data class MediaListResult(
    val items: List<MediaItem>,
    val total: Int
)

enum class DashboardTab {
    Files,
    Shorts,
    Media,
    Favorites,
    Settings
}

// SharedPreferences Manager
object PrefsManager {
    private const val PREFS_NAME = "clyo_cloud_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_TOKEN = "token"

    fun getCloudConnection(context: Context): CloudConnection? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val url = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val user = prefs.getString(KEY_USERNAME, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return CloudConnection(url, user, token)
    }

    fun saveCloudConnection(context: Context, connection: CloudConnection) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SERVER_URL, connection.serverUrl)
            putString(KEY_USERNAME, connection.username)
            putString(KEY_TOKEN, connection.token)
            apply()
        }
    }

    fun clearCloudConnection(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

// ─── Theme System ─────────────────────────────────────────────────────────────

data class ClyoTheme(
    val name: String,
    val displayName: String,
    val accent1: Color,       // primary accent (nav, buttons, highlights)
    val accent2: Color,       // secondary accent (teal-ish, confirmations)
    val bgColors: List<Color>, // vertical gradient background
    val glow1: Color,         // top-start glow blob
    val glow2: Color,         // bottom-end glow blob
    val glow3: Color,         // center glow blob
    val swatchColors: List<Color> // preview swatch gradient
)

object ClyoThemes {
    val Obsidian = ClyoTheme(
        name = "obsidian",
        displayName = "Obsidian",
        accent1 = Color(0xFFBB86FC),
        accent2 = Color(0xFF03DAC6),
        bgColors = listOf(Color(0xFF080612), Color(0xFF140D26), Color(0xFF05030A)),
        glow1 = Color(0xFF8A2BE2),
        glow2 = Color(0xFF00E5FF),
        glow3 = Color(0xFFFF007F),
        swatchColors = listOf(Color(0xFF8A2BE2), Color(0xFFBB86FC))
    )
    val Neon = ClyoTheme(
        name = "neon",
        displayName = "Neon",
        accent1 = Color(0xFF39FF14),
        accent2 = Color(0xFFFF00FF),
        bgColors = listOf(Color(0xFF030803), Color(0xFF0A0F0A), Color(0xFF020302)),
        glow1 = Color(0xFF39FF14),
        glow2 = Color(0xFFFF00FF),
        glow3 = Color(0xFFFFFF00),
        swatchColors = listOf(Color(0xFF39FF14), Color(0xFFFF00FF))
    )
    val Sunrise = ClyoTheme(
        name = "sunrise",
        displayName = "Sunrise",
        accent1 = Color(0xFFFFB347),
        accent2 = Color(0xFFFF6B6B),
        bgColors = listOf(Color(0xFF0A0A1A), Color(0xFF1A0A1F), Color(0xFF100518)),
        glow1 = Color(0xFFFFB347),
        glow2 = Color(0xFFFF6B6B),
        glow3 = Color(0xFFFF4081),
        swatchColors = listOf(Color(0xFFFFB347), Color(0xFFFF6B6B))
    )
    val Arctic = ClyoTheme(
        name = "arctic",
        displayName = "Arctic",
        accent1 = Color(0xFF74D7F7),
        accent2 = Color(0xFFB2F5EA),
        bgColors = listOf(Color(0xFF050C14), Color(0xFF0C1825), Color(0xFF040810)),
        glow1 = Color(0xFF74D7F7),
        glow2 = Color(0xFFB2F5EA),
        glow3 = Color(0xFFE0F7FA),
        swatchColors = listOf(Color(0xFF74D7F7), Color(0xFFB2F5EA))
    )
    val Forest = ClyoTheme(
        name = "forest",
        displayName = "Forest",
        accent1 = Color(0xFF6BCB77),
        accent2 = Color(0xFFFFD93D),
        bgColors = listOf(Color(0xFF040A04), Color(0xFF0A140A), Color(0xFF020602)),
        glow1 = Color(0xFF6BCB77),
        glow2 = Color(0xFFFFD93D),
        glow3 = Color(0xFF00C853),
        swatchColors = listOf(Color(0xFF6BCB77), Color(0xFFFFD93D))
    )

    val all = listOf(Obsidian, Neon, Sunrise, Arctic, Forest)
    fun byName(name: String) = all.firstOrNull { it.name == name } ?: Obsidian
}

object ThemeManager {
    private const val PREFS_NAME = "clyo_cloud_prefs"
    private const val KEY_THEME = "app_theme"

    fun save(context: Context, themeName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, themeName).apply()
    }

    fun load(context: Context): ClyoTheme {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "obsidian") ?: "obsidian"
        return ClyoThemes.byName(name)
    }
}

val LocalClyoTheme = staticCompositionLocalOf { ClyoThemes.Obsidian }

// ──────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.setProperty("http.agent", "ClyoCloudApp")
        enableEdgeToEdge()
        setContent {
            ClyoCloudTheme {
                MainAppRouter()
            }
        }
    }
}

@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    val theme = LocalClyoTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = theme.bgColors))
    ) {
        // Glowing backdrops
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopStart)
                .graphicsLayer {
                    translationX = -120f
                    translationY = -120f
                    alpha = 0.18f
                }
                .background(Brush.radialGradient(listOf(theme.glow1, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    translationX = 180f
                    translationY = 180f
                    alpha = 0.15f
                }
                .background(Brush.radialGradient(listOf(theme.glow2, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = 0.08f
                }
                .background(Brush.radialGradient(listOf(theme.glow3, Color.Transparent)))
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalClyoTheme.current
    Box(
        modifier = modifier.clip(shape)
    ) {
        // Modern clean semi-transparent dark background gradient
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF151221).copy(alpha = 0.90f),
                            Color(0xFF0C0916).copy(alpha = 0.96f)
                        )
                    )
                )
        )
        // Glaze layer — subtle theme-tinted gradient overlay for vibrancy
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            theme.accent1.copy(alpha = 0.06f),
                            theme.accent2.copy(alpha = 0.02f)
                        )
                    )
                )
        )
        // Border ring
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            theme.accent1.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = shape
                )
        )
        // Sharp content
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    val theme = LocalClyoTheme.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFFA0A0C0)) },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFFF3F3F7),
            unfocusedTextColor = Color(0xFFF3F3F7),
            focusedBorderColor = theme.accent1,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedLabelColor = theme.accent1,
            unfocusedLabelColor = Color(0xFFA0A0C0),
            cursorColor = theme.accent1
        )
    )
}

@Composable
fun MainAppRouter() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.Login) }
    var connection by remember { mutableStateOf<CloudConnection?>(null) }
    var activeTheme by remember { mutableStateOf(ThemeManager.load(context)) }

    LaunchedEffect(Unit) {
        val saved = PrefsManager.getCloudConnection(context)
        if (saved != null) {
            connection = saved
            currentScreen = AppScreen.Dashboard
        } else {
            currentScreen = AppScreen.Login
        }
    }

    CompositionLocalProvider(LocalClyoTheme provides activeTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                AppScreen.Login -> {
                    LoginScreen(
                        onLoginSuccess = { conn ->
                            connection = conn
                            PrefsManager.saveCloudConnection(context, conn)
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                }
                AppScreen.Dashboard -> {
                    DashboardScreen(
                        connection = connection,
                        onDisconnect = {
                            PrefsManager.clearCloudConnection(context)
                            connection = null
                            currentScreen = AppScreen.Login
                        },
                        onThemeChange = { theme ->
                            ThemeManager.save(context, theme.name)
                            activeTheme = theme
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (CloudConnection) -> Unit) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var is2faMode by remember { mutableStateOf(false) }
    var tempToken by remember { mutableStateOf("") }
    var code2fa by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showScanConfirmDialog by remember { mutableStateOf(false) }
    var scannedConnection by remember { mutableStateOf<CloudConnection?>(null) }

    GlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "CLYO",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = LocalClyoTheme.current.accent1,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "CLOUD BROWSER",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = LocalClyoTheme.current.accent2,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (!is2faMode) {
                        Text(
                            text = "Connect to Cloud",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3F3F7)
                        )

                        Text(
                            text = "Enter your server credentials to browse files and view media.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA0A0C0),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GlassTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it; errorMessage = null },
                            label = "Server URL (e.g. http://10.0.2.2:3000)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )

                        GlassTextField(
                            value = username,
                            onValueChange = { username = it; errorMessage = null },
                            label = "Username",
                            modifier = Modifier.fillMaxWidth()
                        )

                        GlassTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = "Password",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF5252),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all fields"
                                    return@Button
                                }
                                var formattedUrl = serverUrl.trim()
                                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                    formattedUrl = "http://$formattedUrl"
                                }
                                formattedUrl = formattedUrl.removeSuffix("/")

                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        when (val result = loginToCloud(formattedUrl, username.trim(), password)) {
                                            is LoginResponse.Success -> {
                                                onLoginSuccess(CloudConnection(formattedUrl, username.trim(), result.token))
                                            }
                                            is LoginResponse.Requires2Fa -> {
                                                tempToken = result.tempToken
                                                is2faMode = true
                                            }
                                            is LoginResponse.Error -> {
                                                errorMessage = result.message
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error connecting: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LocalClyoTheme.current.accent1,
                                contentColor = Color(0xFF0F0C1B),
                                disabledContainerColor = LocalClyoTheme.current.accent1.copy(alpha = 0.3f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color(0xFF0F0C1B), modifier = Modifier.size(24.dp))
                            } else {
                                Text("Connect Server", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val context = LocalContext.current
                        Button(
                            onClick = {
                                val options = GmsBarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                    .build()
                                val scanner = GmsBarcodeScanning.getClient(context, options)
                                errorMessage = null
                                scanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        val rawValue = barcode.rawValue
                                        if (rawValue != null) {
                                            try {
                                                val json = JSONObject(rawValue)
                                                val serverUrlVal = json.getString("serverUrl")
                                                val usernameVal = json.getString("username")
                                                val tokenVal = json.getString("token")
                                                
                                                val mappedUrl = mapLocalhostForEmulator(serverUrlVal)
                                                scannedConnection = CloudConnection(mappedUrl, usernameVal, tokenVal)
                                                showScanConfirmDialog = true
                                            } catch (e: Exception) {
                                                errorMessage = "Failed to parse QR code content"
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Scanning failed: ${e.localizedMessage}"
                                    }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = LocalClyoTheme.current.accent1,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = LocalClyoTheme.current.accent1.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LocalClyoTheme.current.accent1.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = QrCodeIcon,
                                    contentDescription = "Scan QR",
                                    tint = LocalClyoTheme.current.accent1,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Scan QR Code to Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        if (showScanConfirmDialog && scannedConnection != null) {
                            Dialog(onDismissRequest = { showScanConfirmDialog = false; scannedConnection = null }) {
                                GlassCard(modifier = Modifier.padding(16.dp)) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text("Confirm Connection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Connect to:\n${scannedConnection!!.serverUrl}\nas user ${scannedConnection!!.username}?", color = Color(0xFFA0A0C0), fontSize = 13.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { showScanConfirmDialog = false; scannedConnection = null }) {
                                                Text("Cancel", color = Color(0xFFA0A0C0))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    val conn = scannedConnection!!
                                                    showScanConfirmDialog = false
                                                    scannedConnection = null
                                                    coroutineScope.launch {
                                                        isLoading = true
                                                        try {
                                                            val isValid = testToken(conn.serverUrl, conn.token)
                                                            if (isValid) {
                                                                onLoginSuccess(conn)
                                                            } else {
                                                                errorMessage = "Invalid connection settings or expired token"
                                                            }
                                                        } catch (e: Exception) {
                                                            errorMessage = "Error validating credentials: ${e.message}"
                                                        } finally {
                                                            isLoading = false
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                                            ) {
                                                Text("OK", color = Color(0xFF0F0C1B), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Two-Factor Auth",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3F3F7)
                        )

                        Text(
                            text = "Enter the 6-digit verification code from your authenticator app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA0A0C0),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GlassTextField(
                            value = code2fa,
                            onValueChange = { code2fa = it; errorMessage = null },
                            label = "6-Digit Code",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF5252),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (code2fa.isBlank() || code2fa.length < 6) {
                                    errorMessage = "Please enter a valid 6-digit code"
                                    return@Button
                                }
                                var formattedUrl = serverUrl.trim()
                                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                    formattedUrl = "http://$formattedUrl"
                                }
                                formattedUrl = formattedUrl.removeSuffix("/")

                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val token = verify2fa(formattedUrl, tempToken, code2fa.trim())
                                        if (token != null) {
                                            onLoginSuccess(CloudConnection(formattedUrl, username.trim(), token))
                                        } else {
                                            errorMessage = "Invalid 2FA code"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Verification failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LocalClyoTheme.current.accent1,
                                contentColor = Color(0xFF0F0C1B),
                                disabledContainerColor = LocalClyoTheme.current.accent1.copy(alpha = 0.3f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color(0xFF0F0C1B), modifier = Modifier.size(24.dp))
                            } else {
                                Text("Verify Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                is2faMode = false
                                code2fa = ""
                                errorMessage = null
                            },
                            enabled = !isLoading
                        ) {
                            Text("Back to Login", color = LocalClyoTheme.current.accent2)
                        }
                    }
                }
            }
        }
    }
}

// Main Dashboard Composable
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    connection: CloudConnection?,
    onDisconnect: () -> Unit,
    onThemeChange: (ClyoTheme) -> Unit = {}
) {
    if (connection == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(DashboardTab.Files) }
    var searchQuery by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    val sharedPref = remember { context.getSharedPreferences("ClyoCloudPrefs", Context.MODE_PRIVATE) }
    var isGridView by remember { mutableStateOf(sharedPref.getBoolean("is_grid_view", false)) }
    var autoplayPreviews by remember { mutableStateOf(sharedPref.getBoolean("autoplay_previews", true)) }
    var sortOption by remember { mutableStateOf(sharedPref.getString("sort_option", "name") ?: "name") }

    LaunchedEffect(isGridView) {
        sharedPref.edit().putBoolean("is_grid_view", isGridView).apply()
    }

    LaunchedEffect(autoplayPreviews) {
        sharedPref.edit().putBoolean("autoplay_previews", autoplayPreviews).apply()
    }

    LaunchedEffect(sortOption) {
        sharedPref.edit().putString("sort_option", sortOption).apply()
    }

    // Files Tab state
    var currentPath by remember { mutableStateOf("") }
    val pathStack = remember { mutableStateListOf<String>() }
    var filesList by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var isFilesLoading by remember { mutableStateOf(false) }
    var isFilesRefreshing by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Favorites Tab state
    var favoritesList by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var isFavoritesLoading by remember { mutableStateOf(false) }
    var isFavoritesRefreshing by remember { mutableStateOf(false) }
    var hideFolders by remember { mutableStateOf(false) }

    // Multi-Selection State
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateListOf<String>() }

    // Dialog state variables
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTargetFile by remember { mutableStateOf<CloudFile?>(null) }
    var renameNewName by remember { mutableStateOf("") }

    var showBulkRenameDialog by remember { mutableStateOf(false) }
    var bulkRenameBaseName by remember { mutableStateOf("") }
    var bulkRenameExtension by remember { mutableStateOf("") }
    var bulkRenameStartNumber by remember { mutableStateOf(1) }

    var showMoveDialog by remember { mutableStateOf(false) }
    var moveTargetFiles by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var pickerCurrentPath by remember { mutableStateOf("") }
    var pickerFoldersList by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var isPickerLoading by remember { mutableStateOf(false) }
    var showPickerCreateFolderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isBulkActionFavourite by remember { mutableStateOf(true) }
    var showBulkTagDialog by remember { mutableStateOf(false) }

    // Media Tab state
    var mediaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isMediaLoading by remember { mutableStateOf(false) }
    var mediaPage by remember { mutableStateOf(1) }
    var mediaTotalCount by remember { mutableStateOf(0) }
    val mediaPageSize = 30
    var allMediaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isAllMediaLoading by remember { mutableStateOf(false) }
    var activeDetailMedia by remember { mutableStateOf<MediaItem?>(null) }

    // Shorts Tab state
    var shortsList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isShortsLoading by remember { mutableStateOf(false) }

    // Global Predefined Tags state
    var predefinedTags by remember { mutableStateOf<List<TagItem>>(emptyList()) }

    // Tag / Details editing dialog state
    var showTagEditDialog by remember { mutableStateOf(false) }
    var fileForTagEdit by remember { mutableStateOf<CloudFile?>(null) }

    // Immersive viewer state
    var activeViewerFile by remember { mutableStateOf<CloudFile?>(null) }
    var activeViewerMediaList by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var activeViewerMedia by remember { mutableStateOf<MediaItem?>(null) }

    var uploadProgressText by remember { mutableStateOf<String?>(null) }

    val selectFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var successCount = 0
                val total = uris.size
                
                for ((index, uri) in uris.withIndex()) {
                    val resolver = context.contentResolver
                    var displayName = "file_${index + 1}"
                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                displayName = cursor.getString(nameIndex)
                            }
                        }
                    }

                    uploadProgressText = "Uploading $displayName (${index + 1}/$total) - 0%"
                    
                    val success = uploadFileToServer(
                        context = context,
                        serverUrl = connection.serverUrl,
                        token = connection.token,
                        targetPath = currentPath,
                        fileUri = uri,
                        fileName = displayName,
                        fileIndex = index + 1,
                        totalFiles = total,
                        onProgress = { name, fileIdx, totalF, percent ->
                            uploadProgressText = "Uploading $name ($fileIdx/$totalF) - $percent%"
                        }
                    )
                    if (success) {
                        successCount++
                    }
                }
                
                uploadProgressText = null
                Toast.makeText(context, "Uploaded $successCount/$total files", Toast.LENGTH_SHORT).show()
                
                isFilesLoading = true
                try {
                    filesList = fetchFilesList(connection.serverUrl, connection.token, currentPath)
                } catch (e: Exception) {
                    Log.e("ClyoCloudAndroid", "Upload refresh error", e)
                } finally {
                    isFilesLoading = false
                }
            }
        }
    }

    // Fetch predefined tags once
    LaunchedEffect(Unit) {
        predefinedTags = fetchPredefinedTags(connection.serverUrl, connection.token)
    }

    // Navigation Backpress handling
    BackHandler(enabled = isSelectionMode || pathStack.isNotEmpty() || activeViewerFile != null || activeViewerMedia != null || activeDetailMedia != null || currentTab == DashboardTab.Shorts || currentTab == DashboardTab.Favorites) {
        if (isSelectionMode) {
            selectedFiles.clear()
            isSelectionMode = false
        } else if (activeDetailMedia != null) {
            activeDetailMedia = null
        } else if (activeViewerFile != null) {
            activeViewerFile = null
        } else if (activeViewerMedia != null) {
            activeViewerMedia = null
        } else if (currentTab == DashboardTab.Shorts || currentTab == DashboardTab.Favorites) {
            currentTab = DashboardTab.Files
        } else if (pathStack.isNotEmpty()) {
            pathStack.removeAt(pathStack.size - 1)
            currentPath = pathStack.joinToString("/")
        }
    }

    // Refresh Files List when path changes
    LaunchedEffect(currentPath) {
        selectedFiles.clear()
        isSelectionMode = false
        isFilesLoading = true
        try {
            filesList = fetchFilesList(connection.serverUrl, connection.token, currentPath)
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                Toast.makeText(context, "Error loading files: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            isFilesLoading = false
        }
    }

    // Load paginated media and all media for recommendations
    LaunchedEffect(currentTab, mediaPage, searchQuery) {
        if (currentTab == DashboardTab.Media) {
            isMediaLoading = true
            try {
                val res = fetchMediaList(
                    serverUrl = connection.serverUrl,
                    token = connection.token,
                    search = searchQuery.takeIf { it.isNotBlank() },
                    page = mediaPage,
                    limit = mediaPageSize
                )
                mediaList = res.items
                mediaTotalCount = res.total
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Toast.makeText(context, "Error loading media: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isMediaLoading = false
            }
        }
    }

    // Fetch all media once for details overlay recommended section
    LaunchedEffect(currentTab) {
        if (currentTab == DashboardTab.Media) {
            isAllMediaLoading = true
            try {
                val res = fetchMediaList(
                    serverUrl = connection.serverUrl,
                    token = connection.token
                )
                allMediaList = res.items
            } catch (e: Exception) {
                Log.e("ClyoCloudAndroid", "Error prefetching all media", e)
            } finally {
                isAllMediaLoading = false
            }
        }
    }

    // Refresh Shorts Feed when entering Shorts tab
    LaunchedEffect(currentTab) {
        if (currentTab == DashboardTab.Shorts) {
            isShortsLoading = true
            try {
                shortsList = fetchShortsFeed(connection.serverUrl, connection.token)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Toast.makeText(context, "Error loading Shorts: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isShortsLoading = false
            }
        }
    }

    // Load Favorites when Favorites tab is active
    LaunchedEffect(currentTab) {
        if (currentTab == DashboardTab.Favorites) {
            isFavoritesLoading = true
            try {
                favoritesList = fetchFavoritesList(connection.serverUrl, connection.token)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Toast.makeText(context, "Error loading favorites: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isFavoritesLoading = false
            }
        }
    }

    val triggerFavoritesRefresh = {
        scope.launch {
            try {
                favoritesList = fetchFavoritesList(connection.serverUrl, connection.token)
            } catch (e: Exception) {
                Log.e("ClyoCloudAndroid", "Favorites Refresh error", e)
            }
        }
    }

    val triggerFileRefresh = {
        scope.launch {
            try {
                filesList = fetchFilesList(connection.serverUrl, connection.token, currentPath)
            } catch (e: Exception) {
                Log.e("ClyoCloudAndroid", "Refresh error", e)
            }
        }
    }

    val triggerMediaRefresh = {
        scope.launch {
            try {
                val res = fetchMediaList(
                    serverUrl = connection.serverUrl,
                    token = connection.token,
                    search = searchQuery.takeIf { it.isNotBlank() },
                    page = mediaPage,
                    limit = mediaPageSize
                )
                mediaList = res.items
                mediaTotalCount = res.total
                
                // Also refresh allMediaList in background
                val allRes = fetchMediaList(connection.serverUrl, connection.token)
                allMediaList = allRes.items
            } catch (e: Exception) {
                Log.e("ClyoCloudAndroid", "Refresh error", e)
            }
        }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header / Toolbar
            if (currentTab == DashboardTab.Files && isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            selectedFiles.clear()
                            isSelectionMode = false
                        }) {
                            Icon(CloseIcon, contentDescription = "Cancel Selection", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${selectedFiles.size} selected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle Select All / Deselect All
                        IconButton(onClick = {
                            if (selectedFiles.size == filesList.size) {
                                selectedFiles.clear()
                                isSelectionMode = false
                            } else {
                                selectedFiles.clear()
                                filesList.forEach { selectedFiles.add(it.name) }
                            }
                        }) {
                            Icon(
                                imageVector = CheckIcon,
                                contentDescription = "Select All Toggle",
                                tint = if (selectedFiles.size == filesList.size) LocalClyoTheme.current.accent1 else Color.White
                            )
                        }

                        // Bulk Actions Trigger Dropdown Menu
                        var showBulkActionsMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showBulkActionsMenu = true }) {
                                Icon(MoreVertIcon, contentDescription = "Bulk Actions", tint = LocalClyoTheme.current.accent2)
                            }
                            DropdownMenu(
                                expanded = showBulkActionsMenu,
                                onDismissRequest = { showBulkActionsMenu = false },
                                modifier = Modifier.background(Color(0xFF1E1E2C))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Favorite", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showBulkActionsMenu = false
                                        scope.launch {
                                            var count = 0
                                            selectedFiles.forEach { name ->
                                                val file = filesList.find { it.name == name }
                                                if (file != null && !file.isFavourite) {
                                                    val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                                    if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isFavourite = true)) {
                                                        count++
                                                    }
                                                }
                                            }
                                            Toast.makeText(context, "Added $count items to favorites", Toast.LENGTH_SHORT).show()
                                            triggerFileRefresh()
                                            selectedFiles.clear()
                                            isSelectionMode = false
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Unfavorite", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showBulkActionsMenu = false
                                        scope.launch {
                                            var count = 0
                                            selectedFiles.forEach { name ->
                                                val file = filesList.find { it.name == name }
                                                if (file != null && file.isFavourite) {
                                                    val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                                    if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isFavourite = false)) {
                                                        count++
                                                    }
                                                }
                                            }
                                            Toast.makeText(context, "Removed $count items from favorites", Toast.LENGTH_SHORT).show()
                                            triggerFileRefresh()
                                            selectedFiles.clear()
                                            isSelectionMode = false
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add Tags", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showBulkActionsMenu = false
                                        showBulkTagDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Move", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showBulkActionsMenu = false
                                        val selectedItemsList = filesList.filter { selectedFiles.contains(it.name) }
                                        moveTargetFiles = selectedItemsList
                                        pickerCurrentPath = ""
                                        showMoveDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (selectedFiles.size == 1) "Rename" else "Bulk Rename", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showBulkActionsMenu = false
                                        if (selectedFiles.size == 1) {
                                            val file = filesList.find { it.name == selectedFiles.first() }
                                            if (file != null) {
                                                renameTargetFile = file
                                                renameNewName = file.name
                                                showRenameDialog = true
                                            }
                                        } else {
                                            bulkRenameBaseName = ""
                                            bulkRenameExtension = ""
                                            bulkRenameStartNumber = 1
                                            showBulkRenameDialog = true
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Download ZIP", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showBulkActionsMenu = false
                                        val filesQuery = selectedFiles.joinToString(",")
                                        val bulkDownloadUrl = "${connection.serverUrl}/api/bulk-download?files=${Uri.encode(filesQuery)}&path=${Uri.encode(currentPath)}"
                                        val zipName = "bulk_download.zip"
                                        scope.launch {
                                            Toast.makeText(context, "Downloading zip archive...", Toast.LENGTH_SHORT).show()
                                            val success = downloadUrlToDevice(context, connection.token, bulkDownloadUrl, zipName)
                                            if (success) {
                                                Toast.makeText(context, "Download complete: $zipName", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                            }
                                            selectedFiles.clear()
                                            isSelectionMode = false
                                        }
                                    }
                                )
                                
                                val selectedObjects = filesList.filter { selectedFiles.contains(it.name) }
                                if (selectedObjects.isNotEmpty() && selectedObjects.all { it.isFolder }) {
                                    DropdownMenuItem(
                                        text = { Text("Toggle Shorts", color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            showBulkActionsMenu = false
                                            scope.launch {
                                                val settings = fetchUserSettings(connection.serverUrl, connection.token)
                                                if (settings != null) {
                                                    val selectedShortsPaths = mutableListOf<String>()
                                                    val pathArr = settings.optJSONArray("selectedShortsPaths")
                                                    if (pathArr != null) {
                                                        for (j in 0 until pathArr.length()) {
                                                            selectedShortsPaths.add(pathArr.getString(j))
                                                        }
                                                    }
                                                    var toggledCount = 0
                                                    selectedObjects.forEach { folder ->
                                                        val relPath = if (currentPath.isEmpty()) folder.name else "$currentPath/${folder.name}"
                                                        if (selectedShortsPaths.contains(relPath)) {
                                                            selectedShortsPaths.remove(relPath)
                                                        } else {
                                                            selectedShortsPaths.add(relPath)
                                                        }
                                                        toggledCount++
                                                    }
                                                    val updatedArr = JSONArray()
                                                    selectedShortsPaths.forEach { updatedArr.put(it) }
                                                    settings.put("selectedShortsPaths", updatedArr)
                                                    if (saveUserSettings(connection.serverUrl, connection.token, settings)) {
                                                        Toast.makeText(context, "Toggled Shorts for $toggledCount folders", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to save settings", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Failed to fetch settings", Toast.LENGTH_SHORT).show()
                                                }
                                                selectedFiles.clear()
                                                isSelectionMode = false
                                            }
                                        }
                                    )
                                }
                                
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                DropdownMenuItem(
                                    text = { Text("Delete", color = Color(0xFFFF5252), fontSize = 13.sp) },
                                    onClick = {
                                        showBulkActionsMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (currentTab != DashboardTab.Shorts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (currentTab) {
                                DashboardTab.Files -> "Cloud Storage"
                                DashboardTab.Shorts -> "Shorts Feed"
                                DashboardTab.Media -> "Media Library"
                                DashboardTab.Favorites -> "Favorites"
                                DashboardTab.Settings -> "Connection"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = connection.username,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A0C0)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentTab != DashboardTab.Settings && currentTab != DashboardTab.Favorites) {
                            IconButton(onClick = { showFavoritesOnly = !showFavoritesOnly }) {
                                Icon(
                                    imageVector = if (showFavoritesOnly) StarIcon else StarBorderIcon,
                                    contentDescription = "Favorites Only",
                                    tint = if (showFavoritesOnly) Color(0xFFFFD700) else Color(0xFFA0A0C0)
                                )
                            }
                        }

                        if (currentTab == DashboardTab.Files || currentTab == DashboardTab.Favorites) {
                            var showSortMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = SortIcon,
                                        contentDescription = "Display and Sort Options",
                                        tint = Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier.background(Color(0xFF151221))
                                ) {
                                    // View Mode Toggle
                                    DropdownMenuItem(
                                        text = { Text(if (isGridView) "Switch to List View" else "Switch to Grid View", color = Color.White, fontSize = 13.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isGridView) ListViewIcon else GridViewIcon,
                                                contentDescription = null,
                                                tint = LocalClyoTheme.current.accent1,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = { isGridView = !isGridView; showSortMenu = false }
                                    )

                                    // Hide Folders Toggle
                                    DropdownMenuItem(
                                        text = { Text(if (hideFolders) "Show Folders" else "Hide Folders", color = Color.White, fontSize = 13.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = FolderIcon,
                                                contentDescription = null,
                                                tint = if (hideFolders) Color(0xFFFF5252) else LocalClyoTheme.current.accent1,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = { hideFolders = !hideFolders; showSortMenu = false }
                                    )

                                    // Autoplay Previews Toggle (only show if Files tab has video previews)
                                    DropdownMenuItem(
                                        text = { Text(if (autoplayPreviews) "Disable Autoplay" else "Enable Autoplay", color = Color.White, fontSize = 13.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = AutoplayIcon,
                                                contentDescription = null,
                                                tint = if (autoplayPreviews) LocalClyoTheme.current.accent2 else Color(0xFFA0A0C0),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = { autoplayPreviews = !autoplayPreviews; showSortMenu = false }
                                    )

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 4.dp))

                                    // Sort Options Title
                                    Text(
                                        text = "  Sort by:",
                                        color = Color(0xFFA0A0C0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Name", color = if (sortOption == "name") LocalClyoTheme.current.accent1 else Color.White, fontSize = 13.sp) },
                                        onClick = { sortOption = "name"; showSortMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Size", color = if (sortOption == "size") LocalClyoTheme.current.accent1 else Color.White, fontSize = 13.sp) },
                                        onClick = { sortOption = "size"; showSortMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Date", color = if (sortOption == "date") LocalClyoTheme.current.accent1 else Color.White, fontSize = 13.sp) },
                                        onClick = { sortOption = "date"; showSortMenu = false }
                                    )
                                }
                            }
                        }

                        if (currentTab == DashboardTab.Files) {
                            IconButton(onClick = { selectFilesLauncher.launch("*/*") }) {
                                Icon(
                                    imageVector = UploadIcon,
                                    contentDescription = "Upload File",
                                    tint = LocalClyoTheme.current.accent1
                                )
                            }
                            IconButton(onClick = { showCreateFolderDialog = true }) {
                                Icon(
                                    imageVector = AddIcon,
                                    contentDescription = "Create Folder",
                                    tint = LocalClyoTheme.current.accent2
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar (Files & Media)
            if (currentTab != DashboardTab.Settings && currentTab != DashboardTab.Shorts) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Icon(SearchIcon, contentDescription = "Search", tint = Color(0xFFA0A0C0), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search files...", color = Color(0xFFA0A0C0).copy(alpha = 0.6f), fontSize = 14.sp)
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(CloseIcon, contentDescription = "Clear", tint = Color(0xFFA0A0C0))
                                }
                            }
                        }
                    }
                }
            }

            // Main Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    DashboardTab.Files -> {
                        PullToRefreshBox(
                            isRefreshing = isFilesRefreshing,
                            onRefresh = {
                                isFilesRefreshing = true
                                scope.launch {
                                    try {
                                        filesList = fetchFilesList(connection.serverUrl, connection.token, currentPath)
                                    } catch (e: Exception) {
                                        if (e !is kotlinx.coroutines.CancellationException) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isFilesRefreshing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            FilesTabContent(
                                hideFolders = hideFolders,
                                predefinedTags = predefinedTags,
                                serverUrl = connection.serverUrl,
                                token = connection.token,
                                currentPath = currentPath,
                                pathStack = pathStack,
                                files = filesList,
                                isLoading = isFilesLoading,
                                searchQuery = searchQuery,
                                showFavoritesOnly = showFavoritesOnly,
                                isGridView = isGridView,
                                autoplayPreviews = autoplayPreviews,
                                sortOption = sortOption,
                                isSelectionMode = isSelectionMode,
                                selectedFiles = selectedFiles.toSet(),
                                onPathChanged = { newPath ->
                                    currentPath = newPath
                                    pathStack.clear()
                                    if (newPath.isNotEmpty()) {
                                        pathStack.addAll(newPath.split("/"))
                                    }
                                },
                                onFileClick = { file, list ->
                                    activeViewerFile = file
                                    activeViewerMediaList = list
                                },
                                onToggleFavorite = { file, isFav ->
                                     scope.launch {
                                         val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                         if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isFavourite = isFav)) {
                                             triggerFileRefresh()
                                             triggerFavoritesRefresh()
                                         }
                                     }
                                 },
                                onTogglePublic = { file, isPub ->
                                    scope.launch {
                                        val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                        if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isPublic = isPub)) {
                                            triggerFileRefresh()
                                            if (isPub) {
                                                val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                                                shareText(context, publicUrl, "Share public link")
                                            }
                                        }
                                    }
                                },
                                onCopyLink = { file ->
                                    val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                    val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("ClyoCloud Link", publicUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onDownload = { file ->
                                    val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                    scope.launch {
                                        Toast.makeText(context, "Downloading ${file.name}...", Toast.LENGTH_SHORT).show()
                                        val success = downloadFileToDevice(context, connection.serverUrl, connection.token, fullFilePath, file.name)
                                        if (success) {
                                            Toast.makeText(context, "Download complete: ${file.name}", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDelete = { file ->
                                    val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                    scope.launch {
                                        if (deleteFileOrFolder(connection.serverUrl, connection.token, fullFilePath)) {
                                            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                                            triggerFileRefresh()
                                            triggerFavoritesRefresh()
                                        } else {
                                            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onEditTags = { file ->
                                    fileForTagEdit = file
                                    showTagEditDialog = true
                                },
                                onToggleSelection = { file ->
                                    if (selectedFiles.contains(file.name)) {
                                        selectedFiles.remove(file.name)
                                        if (selectedFiles.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    } else {
                                        selectedFiles.add(file.name)
                                    }
                                },
                                onLongClick = { file ->
                                    isSelectionMode = true
                                    selectedFiles.clear()
                                    selectedFiles.add(file.name)
                                },
                                onRename = { file ->
                                    renameTargetFile = file
                                    renameNewName = file.name
                                    showRenameDialog = true
                                },
                                onMove = { file ->
                                    moveTargetFiles = listOf(file)
                                    pickerCurrentPath = ""
                                    showMoveDialog = true
                                },
                                onDownloadZip = { file ->
                                    val fileRelativePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                    val zipUrl = "${connection.serverUrl}/api/zip?path=${Uri.encode(fileRelativePath)}"
                                    val zipName = "${file.name}.zip"
                                    scope.launch {
                                        Toast.makeText(context, "Downloading zip archive...", Toast.LENGTH_SHORT).show()
                                        val success = downloadUrlToDevice(context, connection.token, zipUrl, zipName)
                                        if (success) {
                                            Toast.makeText(context, "Download complete: $zipName", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                    DashboardTab.Shorts -> {
                        ShortsTabContent(
                            serverUrl = connection.serverUrl,
                            token = connection.token,
                            username = connection.username,
                            shortsList = shortsList,
                            isLoading = isShortsLoading,
                            predefinedTags = predefinedTags,
                            onPredefinedTagsChanged = { predefinedTags = it },
                            onRefreshFeed = {
                                isShortsLoading = true
                                scope.launch {
                                    try {
                                        shortsList = fetchShortsFeed(connection.serverUrl, connection.token)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isShortsLoading = false
                                    }
                                }
                            }
                        )
                    }
                    DashboardTab.Media -> {
                        MediaTabContent(
                            serverUrl = connection.serverUrl,
                            token = connection.token,
                            mediaList = mediaList,
                            isLoading = isMediaLoading,
                            searchQuery = searchQuery,
                            showFavoritesOnly = showFavoritesOnly,
                            mediaPage = mediaPage,
                            mediaPageSize = mediaPageSize,
                            mediaTotalCount = mediaTotalCount,
                            onPageChanged = { mediaPage = it },
                            onMediaClick = { media -> activeDetailMedia = media },
                            onMediaLongClick = { media ->
                                val mappedFile = CloudFile(
                                    name = media.name,
                                    size = media.size,
                                    type = if (media.type == "video") "video/mp4" else "image/jpeg",
                                    lastModified = media.lastModified,
                                    isFolder = false,
                                    isPublic = media.isPublic,
                                    isFavourite = media.isFavourite,
                                    description = media.description,
                                    tags = media.tags,
                                    previewImage = null,
                                    fullPath = media.fullPath
                                )
                                fileForTagEdit = mappedFile
                                showTagEditDialog = true
                            }
                        )
                    }
                    DashboardTab.Favorites -> {
                        PullToRefreshBox(
                            isRefreshing = isFavoritesRefreshing,
                            onRefresh = {
                                isFavoritesRefreshing = true
                                scope.launch {
                                    try {
                                        favoritesList = fetchFavoritesList(connection.serverUrl, connection.token)
                                    } catch (e: Exception) {
                                        if (e !is kotlinx.coroutines.CancellationException) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isFavoritesRefreshing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            FavoritesTabContent(
                                hideFolders = hideFolders,
                                predefinedTags = predefinedTags,
                                serverUrl = connection.serverUrl,
                                token = connection.token,
                                files = favoritesList,
                                isLoading = isFavoritesLoading,
                                searchQuery = searchQuery,
                                isGridView = isGridView,
                                autoplayPreviews = autoplayPreviews,
                                sortOption = sortOption,
                                onFolderClick = { folder ->
                                    val folderFullPath = folder.fullPath ?: folder.name
                                    currentPath = folderFullPath
                                    pathStack.clear()
                                    if (folderFullPath.isNotEmpty()) {
                                        pathStack.addAll(folderFullPath.split("/"))
                                    }
                                    currentTab = DashboardTab.Files
                                },
                                onFileClick = { file, list ->
                                    activeViewerFile = file
                                    activeViewerMediaList = list
                                },
                                onToggleFavorite = { file, isFav ->
                                    scope.launch {
                                        val fullFilePath = file.fullPath ?: file.name
                                        if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isFavourite = isFav)) {
                                            triggerFileRefresh()
                                            triggerFavoritesRefresh()
                                        }
                                    }
                                },
                                onTogglePublic = { file, isPub ->
                                    scope.launch {
                                        val fullFilePath = file.fullPath ?: file.name
                                        if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isPublic = isPub)) {
                                            triggerFileRefresh()
                                            triggerFavoritesRefresh()
                                            if (isPub) {
                                                val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                                                shareText(context, publicUrl, "Share public link")
                                            }
                                        }
                                    }
                                },
                                onCopyLink = { file ->
                                    val fullFilePath = file.fullPath ?: file.name
                                    val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("ClyoCloud Link", publicUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onDownload = { file ->
                                    val fullFilePath = file.fullPath ?: file.name
                                    scope.launch {
                                        Toast.makeText(context, "Downloading ${file.name}...", Toast.LENGTH_SHORT).show()
                                        val success = downloadFileToDevice(context, connection.serverUrl, connection.token, fullFilePath, file.name)
                                        if (success) {
                                            Toast.makeText(context, "Download complete: ${file.name}", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDelete = { file ->
                                    val fullFilePath = file.fullPath ?: file.name
                                    scope.launch {
                                        if (deleteFileOrFolder(connection.serverUrl, connection.token, fullFilePath)) {
                                            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                                            triggerFileRefresh()
                                            triggerFavoritesRefresh()
                                        } else {
                                            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onEditTags = { file ->
                                    fileForTagEdit = file
                                    showTagEditDialog = true
                                },
                                onRename = { file ->
                                    renameTargetFile = file
                                    renameNewName = file.name
                                    showRenameDialog = true
                                },
                                onMove = { file ->
                                    moveTargetFiles = listOf(file)
                                    pickerCurrentPath = ""
                                    showMoveDialog = true
                                }
                            )
                        }
                    }
                    DashboardTab.Settings -> {
                        SettingsTabContent(
                            connection = connection,
                            onDisconnect = onDisconnect,
                            onThemeChange = onThemeChange
                        )
                    }
                }
            }

            // Bottom Navigation Bar — modern clean style with theme accents
            val theme = LocalClyoTheme.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                // Modern clean semi-transparent dark background gradient
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF151221).copy(alpha = 0.90f),
                                    Color(0xFF0C0916).copy(alpha = 0.96f)
                                )
                            )
                        )
                )
                // Theme tint
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    theme.accent1.copy(alpha = 0.06f),
                                    theme.accent2.copy(alpha = 0.02f)
                                )
                            )
                        )
                )
                // Top edge border
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(theme.accent1.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                )
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == DashboardTab.Files,
                        onClick = { currentTab = DashboardTab.Files },
                        icon = { Icon(FolderIcon, contentDescription = "Files", modifier = Modifier.size(22.dp)) },
                        label = { Text("Files") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.accent1,
                            selectedTextColor = theme.accent1,
                            unselectedIconColor = Color(0xFFA0A0C0),
                            unselectedTextColor = Color(0xFFA0A0C0),
                            indicatorColor = theme.accent1.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == DashboardTab.Shorts,
                        onClick = { currentTab = DashboardTab.Shorts },
                        icon = { Icon(AutoplayIcon, contentDescription = "Shorts", modifier = Modifier.size(22.dp)) },
                        label = { Text("Shorts") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.accent1,
                            selectedTextColor = theme.accent1,
                            unselectedIconColor = Color(0xFFA0A0C0),
                            unselectedTextColor = Color(0xFFA0A0C0),
                            indicatorColor = theme.accent1.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == DashboardTab.Media,
                        onClick = { currentTab = DashboardTab.Media },
                        icon = { Icon(PlayIcon, contentDescription = "Media", modifier = Modifier.size(22.dp)) },
                        label = { Text("Media") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.accent1,
                            selectedTextColor = theme.accent1,
                            unselectedIconColor = Color(0xFFA0A0C0),
                            unselectedTextColor = Color(0xFFA0A0C0),
                            indicatorColor = theme.accent1.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == DashboardTab.Favorites,
                        onClick = { currentTab = DashboardTab.Favorites },
                        icon = { Icon(if (currentTab == DashboardTab.Favorites) StarIcon else StarBorderIcon, contentDescription = "Favorites", modifier = Modifier.size(22.dp)) },
                        label = { Text("Favorites") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.accent1,
                            selectedTextColor = theme.accent1,
                            unselectedIconColor = Color(0xFFA0A0C0),
                            unselectedTextColor = Color(0xFFA0A0C0),
                            indicatorColor = theme.accent1.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == DashboardTab.Settings,
                        onClick = { currentTab = DashboardTab.Settings },
                        icon = { Icon(SettingsIcon, contentDescription = "Settings", modifier = Modifier.size(22.dp)) },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.accent1,
                            selectedTextColor = theme.accent1,
                            unselectedIconColor = Color(0xFFA0A0C0),
                            unselectedTextColor = Color(0xFFA0A0C0),
                            indicatorColor = theme.accent1.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }

        // Folder Creation Dialog
        if (showCreateFolderDialog) {
            var newFolderName by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showCreateFolderDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Create Folder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        GlassTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = "Folder name",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCreateFolderDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newFolderName.isNotBlank()) {
                                        scope.launch {
                                            if (createFolder(connection.serverUrl, connection.token, newFolderName.trim(), currentPath)) {
                                                showCreateFolderDialog = false
                                                triggerFileRefresh()
                                            } else {
                                                Toast.makeText(context, "Failed to create folder", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                            ) {
                                Text("Create", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Upload Progress Dialog
        if (uploadProgressText != null) {
            Dialog(onDismissRequest = {}) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Uploading Files", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = uploadProgressText ?: "",
                            color = Color(0xFFA0A0C0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(color = LocalClyoTheme.current.accent1)
                    }
                }
            }
        }

        // Tag & Share Settings / File Details Dialog
        if (showTagEditDialog && fileForTagEdit != null) {
            val file = fileForTagEdit!!
            val selectedTags = remember { mutableStateListOf<TagItem>().apply { addAll(file.tags) } }
            var isPublicState by remember { mutableStateOf(file.isPublic) }
            var isWallpaperState by remember { mutableStateOf(file.isWallpaper) }
            var descriptionState by remember { mutableStateOf(file.description ?: "") }
            
            // Tag creation states
            var newTagName by remember { mutableStateOf("") }
            val tagColors = listOf("#4B5EAA", "#BB86FC", "#03DAC6", "#FFB347", "#FF6B6B", "#6BCB77", "#FFD93D")
            var selectedTagColor by remember { mutableStateOf(tagColors.first()) }
            
            val fullFilePath = file.fullPath ?: (if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}")
            val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
            
            var activeTab by remember { mutableStateOf("Details") }
            val dialogTabs = listOf("Details", "Access", "Tags")
            val theme = LocalClyoTheme.current

            Dialog(onDismissRequest = { showTagEditDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "File Settings",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        // Tab Segmented Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            dialogTabs.forEach { tabName ->
                                val isTabSelected = activeTab == tabName
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isTabSelected) theme.accent1.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { activeTab = tabName }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabName,
                                        color = if (isTabSelected) theme.accent1 else Color(0xFFA0A0C0),
                                        fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Tab Contents
                        if (activeTab == "Details") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // File properties list
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Name: ${file.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Path: /$fullFilePath", color = Color(0xFFA0A0C0), fontSize = 11.sp)
                                    if (!file.isFolder) {
                                        Text("Size: ${formatFileSize(file.size)}", color = Color(0xFFA0A0C0), fontSize = 11.sp)
                                        Text("Type: ${file.type}", color = Color(0xFFA0A0C0), fontSize = 11.sp)
                                    } else {
                                        Text("Type: Folder", color = Color(0xFFA0A0C0), fontSize = 11.sp)
                                    }
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified))
                                    Text("Modified: $dateStr", color = Color(0xFFA0A0C0), fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Description Editor
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Notes / Description", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    GlassTextField(
                                        value = descriptionState,
                                        onValueChange = { descriptionState = it },
                                        label = "Add description notes...",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        if (activeTab == "Access") {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Public Access Section
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Public Sharing", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("Allow anyone to view/download", color = Color(0xFFA0A0C0), fontSize = 11.sp)
                                        }
                                        androidx.compose.material3.Switch(
                                            checked = isPublicState,
                                            onCheckedChange = { isPublicState = it },
                                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                                checkedThumbColor = theme.accent2,
                                                checkedTrackColor = theme.accent2.copy(alpha = 0.4f)
                                            )
                                        )
                                    }
                                    if (isPublicState) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Public link is active",
                                                color = theme.accent2,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = { shareText(context, publicUrl, "Share link") },
                                                colors = ButtonDefaults.buttonColors(containerColor = theme.accent2),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Share", color = Color(0xFF0F0C1B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Wallpaper / Folder Icon Tools
                                if (!file.isFolder && isImageFile(file.name)) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Set as Wallpaper", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text("Show as option for wallpaper", color = Color(0xFFA0A0C0), fontSize = 11.sp)
                                            }
                                            androidx.compose.material3.Switch(
                                                checked = isWallpaperState,
                                                onCheckedChange = { isWallpaperState = it },
                                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                                    checkedThumbColor = theme.accent1,
                                                    checkedTrackColor = theme.accent1.copy(alpha = 0.4f)
                                                )
                                            )
                                        }

                                        if (currentPath.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        if (updateFileMeta(connection.serverUrl, connection.token, currentPath, previewImage = file.name)) {
                                                            Toast.makeText(context, "Set folder icon successfully", Toast.LENGTH_SHORT).show()
                                                            showTagEditDialog = false
                                                            triggerFileRefresh()
                                                        } else {
                                                            Toast.makeText(context, "Failed to set folder icon", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = theme.accent2)
                                            ) {
                                                Text("Use as Folder Thumbnail", color = Color(0xFF0F0C1B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (file.isFolder && file.previewImage != null) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, clearPreviewImage = true)) {
                                                    Toast.makeText(context, "Reset folder icon successfully", Toast.LENGTH_SHORT).show()
                                                    showTagEditDialog = false
                                                    triggerFileRefresh()
                                                } else {
                                                    Toast.makeText(context, "Failed to reset folder icon", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                                    ) {
                                        Text("Reset Folder Icon", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (activeTab == "Tags") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Select Tags", color = theme.accent1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        predefinedTags.forEach { tag ->
                                            val isChecked = selectedTags.any { it.name == tag.name }
                                            val tagColor = Color(android.graphics.Color.parseColor(tag.color))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(if (isChecked) tagColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isChecked) tagColor else Color.White.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable {
                                                        if (isChecked) {
                                                            selectedTags.removeAll { it.name == tag.name }
                                                        } else {
                                                            selectedTags.add(tag)
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .background(tagColor, CircleShape)
                                                    )
                                                    Text(tag.name, color = Color.White, fontSize = 12.sp)
                                                    if (isChecked) {
                                                        Icon(
                                                            imageVector = CheckIcon,
                                                            contentDescription = "Selected",
                                                            tint = theme.accent2,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Add new tag textbox
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Create New Tag", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            GlassTextField(
                                                value = newTagName,
                                                onValueChange = { newTagName = it },
                                                label = "Tag name...",
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        var showColorsMenu by remember { mutableStateOf(false) }
                                        Box {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color(android.graphics.Color.parseColor(selectedTagColor)), CircleShape)
                                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                                    .clickable { showColorsMenu = true }
                                            )
                                            DropdownMenu(
                                                expanded = showColorsMenu,
                                                onDismissRequest = { showColorsMenu = false }
                                            ) {
                                                tagColors.forEach { colorStr ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Box(modifier = Modifier.size(20.dp).background(Color(android.graphics.Color.parseColor(colorStr)), CircleShape))
                                                        },
                                                        onClick = { selectedTagColor = colorStr; showColorsMenu = false }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                if (newTagName.isNotBlank()) {
                                                    val newTagObj = TagItem(newTagName.trim(), selectedTagColor)
                                                    if (!predefinedTags.any { it.name.lowercase() == newTagObj.name.lowercase() }) {
                                                        scope.launch {
                                                            val updatedPredefined = predefinedTags + newTagObj
                                                            if (savePredefinedTags(connection.serverUrl, connection.token, updatedPredefined)) {
                                                                predefinedTags = fetchPredefinedTags(connection.serverUrl, connection.token)
                                                            }
                                                        }
                                                    }
                                                    if (!selectedTags.any { it.name.lowercase() == newTagObj.name.lowercase() }) {
                                                        selectedTags.add(newTagObj)
                                                    }
                                                    newTagName = ""
                                                }
                                            },
                                            modifier = Modifier
                                                .background(theme.accent1.copy(alpha = 0.12f), CircleShape)
                                                .size(36.dp)
                                        ) {
                                            Icon(AddIcon, contentDescription = "Add Tag", tint = theme.accent1)
                                        }
                                    }
                                }
                            }
                        }

                        // Save & Cancel Actions
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showTagEditDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (updateFileMeta(
                                                serverUrl = connection.serverUrl,
                                                token = connection.token,
                                                filePath = fullFilePath,
                                                isPublic = isPublicState,
                                                isWallpaper = isWallpaperState,
                                                description = descriptionState,
                                                tags = selectedTags.toList()
                                            )) {
                                            showTagEditDialog = false
                                            triggerFileRefresh()
                                            triggerFavoritesRefresh()
                                            triggerMediaRefresh()
                                        } else {
                                            Toast.makeText(context, "Failed to update settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = theme.accent1)
                            ) {
                                Text("Save", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Single Rename Dialog
        if (showRenameDialog && renameTargetFile != null) {
            Dialog(onDismissRequest = { showRenameDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Rename File/Folder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        GlassTextField(
                            value = renameNewName,
                            onValueChange = { renameNewName = it },
                            label = "New name",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showRenameDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (renameNewName.isNotBlank() && renameNewName != renameTargetFile!!.name) {
                                        scope.launch {
                                            val oldName = renameTargetFile!!.name
                                            val oldPath = if (currentPath.isEmpty()) oldName else "$currentPath/$oldName"
                                            if (renameFileOrFolder(connection.serverUrl, connection.token, oldPath, renameNewName.trim())) {
                                                showRenameDialog = false
                                                triggerFileRefresh()
                                            } else {
                                                Toast.makeText(context, "Failed to rename", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        showRenameDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                            ) {
                                Text("Rename", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Bulk Rename Dialog
        if (showBulkRenameDialog) {
            Dialog(onDismissRequest = { showBulkRenameDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Bulk Rename Selected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${selectedFiles.size} files will be renamed sequentially.", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                        GlassTextField(
                            value = bulkRenameBaseName,
                            onValueChange = { bulkRenameBaseName = it },
                            label = "Base Name (e.g. vacation)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        GlassTextField(
                            value = bulkRenameExtension,
                            onValueChange = { bulkRenameExtension = it },
                            label = "Extension override (optional, e.g. png)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        GlassTextField(
                            value = bulkRenameStartNumber.toString(),
                            onValueChange = { bulkRenameStartNumber = it.toIntOrNull() ?: 1 },
                            label = "Start Index Number",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showBulkRenameDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (bulkRenameBaseName.isNotBlank()) {
                                        scope.launch {
                                            var success = true
                                            val sortedSelected = selectedFiles.sorted()
                                            sortedSelected.forEachIndexed { index, oldName ->
                                                val file = filesList.find { it.name == oldName }
                                                if (file != null) {
                                                    val oldPath = if (currentPath.isEmpty()) oldName else "$currentPath/$oldName"
                                                    val originalExt = if (oldName.contains('.')) ".${oldName.split('.').last()}" else ""
                                                    val newExt = if (bulkRenameExtension.isNotBlank()) {
                                                        if (bulkRenameExtension.startsWith('.')) bulkRenameExtension else ".$bulkRenameExtension"
                                                    } else {
                                                        originalExt
                                                    }
                                                    val countNum = bulkRenameStartNumber + index
                                                    val newName = "${bulkRenameBaseName.trim()}_$countNum$newExt"
                                                    if (!renameFileOrFolder(connection.serverUrl, connection.token, oldPath, newName)) {
                                                        success = false
                                                    }
                                                }
                                            }
                                            if (success) {
                                                Toast.makeText(context, "Bulk rename complete", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Bulk rename finished with some failures", Toast.LENGTH_SHORT).show()
                                            }
                                            showBulkRenameDialog = false
                                            selectedFiles.clear()
                                            isSelectionMode = false
                                            triggerFileRefresh()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                            ) {
                                Text("Rename All", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Move Directory Picker Dialog
        if (showMoveDialog) {
            // Load directories dynamically
            LaunchedEffect(pickerCurrentPath) {
                isPickerLoading = true
                try {
                    val allItems = fetchFilesList(connection.serverUrl, connection.token, pickerCurrentPath)
                    // Filter out moving files to prevent circular reference
                    val targetNames = moveTargetFiles.map { it.name }
                    pickerFoldersList = allItems.filter { it.isFolder && (pickerCurrentPath.isNotEmpty() || !targetNames.contains(it.name)) }
                } catch (e: Exception) {
                    Log.e("ClyoCloudAndroid", "Picker fetch error", e)
                } finally {
                    isPickerLoading = false
                }
            }

            Dialog(onDismissRequest = { showMoveDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Move Items", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Moving ${moveTargetFiles.size} item(s)", color = LocalClyoTheme.current.accent1, fontSize = 12.sp)
                        
                        // Current Path Label with breadcrumb actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    if (pickerCurrentPath.isNotEmpty()) {
                                        val parts = pickerCurrentPath.split('/')
                                        pickerCurrentPath = if (parts.size <= 1) "" else parts.dropLast(1).joinToString("/")
                                    }
                                },
                                enabled = pickerCurrentPath.isNotEmpty()
                            ) {
                                Icon(ArrowBackIcon, contentDescription = "Up Directory", tint = if (pickerCurrentPath.isNotEmpty()) Color.White else Color.Gray)
                            }
                            Text(
                                text = if (pickerCurrentPath.isEmpty()) "Destination: /Root" else "Destination: /$pickerCurrentPath",
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showPickerCreateFolderDialog = true }) {
                                Icon(AddIcon, contentDescription = "Create Directory Here", tint = LocalClyoTheme.current.accent2)
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        if (isPickerLoading) {
                            Box(modifier = Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = LocalClyoTheme.current.accent1)
                            }
                        } else if (pickerFoldersList.isEmpty()) {
                            Box(modifier = Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No subdirectories here", color = Color(0xFFA0A0C0))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.height(150.dp)) {
                                items(pickerFoldersList) { folder ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                pickerCurrentPath = if (pickerCurrentPath.isEmpty()) folder.name else "$pickerCurrentPath/${folder.name}"
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(FolderIcon, contentDescription = null, tint = LocalClyoTheme.current.accent1, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(folder.name, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showMoveDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val fullPaths = moveTargetFiles.map { if (currentPath.isEmpty()) it.name else "$currentPath/${it.name}" }
                                        if (moveItems(connection.serverUrl, connection.token, fullPaths, pickerCurrentPath)) {
                                            Toast.makeText(context, "Items moved successfully", Toast.LENGTH_SHORT).show()
                                            showMoveDialog = false
                                            selectedFiles.clear()
                                            isSelectionMode = false
                                            triggerFileRefresh()
                                        } else {
                                            Toast.makeText(context, "Move failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                            ) {
                                Text("Move Here", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Subfolder creation inside Move Picker Dialog
        if (showPickerCreateFolderDialog) {
            var pickerNewFolderName by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showPickerCreateFolderDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Create Folder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        GlassTextField(
                            value = pickerNewFolderName,
                            onValueChange = { pickerNewFolderName = it },
                            label = "Folder name",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showPickerCreateFolderDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (pickerNewFolderName.isNotBlank()) {
                                        scope.launch {
                                            if (createFolder(connection.serverUrl, connection.token, pickerNewFolderName.trim(), pickerCurrentPath)) {
                                                showPickerCreateFolderDialog = false
                                                // Refresh folders list in picker
                                                val allItems = fetchFilesList(connection.serverUrl, connection.token, pickerCurrentPath)
                                                val targetNames = moveTargetFiles.map { it.name }
                                                pickerFoldersList = allItems.filter { it.isFolder && (pickerCurrentPath.isNotEmpty() || !targetNames.contains(it.name)) }
                                            } else {
                                                Toast.makeText(context, "Failed to create folder", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                            ) {
                                Text("Create", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Confirm Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Are you sure you want to delete the selected ${selectedFiles.size} item(s)? This action cannot be undone.", color = Color(0xFFA0A0C0), fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        var count = 0
                                        selectedFiles.forEach { name ->
                                            val oldPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                                            if (deleteFileOrFolder(connection.serverUrl, connection.token, oldPath)) {
                                                count++
                                            }
                                        }
                                        Toast.makeText(context, "Deleted $count items", Toast.LENGTH_SHORT).show()
                                        showDeleteConfirmDialog = false
                                        selectedFiles.clear()
                                        isSelectionMode = false
                                        triggerFileRefresh()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                            ) {
                                Text("Delete", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Bulk Tag Dialog
        if (showBulkTagDialog) {
            val bulkSelectedTags = remember { mutableStateListOf<TagItem>() }
            var bulkNewTagName by remember { mutableStateOf("") }
            val tagColors = listOf("#4B5EAA", "#BB86FC", "#03DAC6", "#FFB347", "#FF6B6B", "#6BCB77", "#FFD93D")
            var bulkSelectedTagColor by remember { mutableStateOf(tagColors.first()) }

            Dialog(onDismissRequest = { showBulkTagDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Add Tags to Selection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Applying tags to ${selectedFiles.size} items", color = Color(0xFFA0A0C0), fontSize = 12.sp)

                        LazyColumn(modifier = Modifier.height(110.dp)) {
                            items(predefinedTags) { tag ->
                                val isChecked = bulkSelectedTags.any { it.name == tag.name }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                bulkSelectedTags.removeAll { it.name == tag.name }
                                            } else {
                                                bulkSelectedTags.add(tag)
                                            }
                                        }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(Color(android.graphics.Color.parseColor(tag.color)), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(tag.name, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    if (isChecked) {
                                        Icon(CheckIcon, contentDescription = "Selected", tint = LocalClyoTheme.current.accent2, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Create Custom Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                GlassTextField(
                                    value = bulkNewTagName,
                                    onValueChange = { bulkNewTagName = it },
                                    label = "New tag...",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            var showColorsMenu by remember { mutableStateOf(false) }
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(android.graphics.Color.parseColor(bulkSelectedTagColor)), CircleShape)
                                        .clickable { showColorsMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showColorsMenu,
                                    onDismissRequest = { showColorsMenu = false }
                                ) {
                                    tagColors.forEach { colorStr ->
                                        DropdownMenuItem(
                                            text = {
                                                Box(modifier = Modifier.size(20.dp).background(Color(android.graphics.Color.parseColor(colorStr)), CircleShape))
                                            },
                                            onClick = { bulkSelectedTagColor = colorStr; showColorsMenu = false }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                if (bulkNewTagName.isNotBlank()) {
                                    val newTagObj = TagItem(bulkNewTagName.trim(), bulkSelectedTagColor)
                                    if (!predefinedTags.any { it.name.lowercase() == newTagObj.name.lowercase() }) {
                                        scope.launch {
                                            val updatedPredefined = predefinedTags + newTagObj
                                            if (savePredefinedTags(connection.serverUrl, connection.token, updatedPredefined)) {
                                                predefinedTags = fetchPredefinedTags(connection.serverUrl, connection.token)
                                            }
                                        }
                                    }
                                    if (!bulkSelectedTags.any { it.name.lowercase() == newTagObj.name.lowercase() }) {
                                        bulkSelectedTags.add(newTagObj)
                                    }
                                    bulkNewTagName = ""
                                }
                            }) {
                                Icon(AddIcon, contentDescription = "Add Tag", tint = LocalClyoTheme.current.accent2)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showBulkTagDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (bulkSelectedTags.isNotEmpty()) {
                                        scope.launch {
                                            var count = 0
                                            selectedFiles.forEach { name ->
                                                val file = filesList.find { it.name == name }
                                                if (file != null) {
                                                    val currentTags = file.tags
                                                    val tagsToActuallyAdd = bulkSelectedTags.filter { newTag ->
                                                        !currentTags.any { it.name.lowercase() == newTag.name.lowercase() }
                                                    }
                                                    val updatedTags = currentTags + tagsToActuallyAdd
                                                    val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                                    if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, tags = updatedTags)) {
                                                        count++
                                                    }
                                                }
                                            }
                                            Toast.makeText(context, "Added tags to $count items", Toast.LENGTH_SHORT).show()
                                            showBulkTagDialog = false
                                            selectedFiles.clear()
                                            isSelectionMode = false
                                            triggerFileRefresh()
                                        }
                                    } else {
                                        showBulkTagDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                            ) {
                                Text("Apply", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Immersive viewer for files
        if (activeViewerFile != null) {
            val mediaFiles = remember(currentTab, filesList, favoritesList) {
                if (currentTab == DashboardTab.Favorites) {
                    favoritesList.filter { !it.isFolder && (isImageFile(it.name) || isVideoFile(it.name)) }
                } else {
                    filesList.filter { !it.isFolder && (isImageFile(it.name) || isVideoFile(it.name)) }
                }
            }
            val initialIndex = mediaFiles.indexOfFirst { (it.fullPath ?: it.name) == (activeViewerFile?.fullPath ?: activeViewerFile?.name) }.coerceAtLeast(0)
            ImmersiveMediaViewer(
                serverUrl = connection.serverUrl,
                token = connection.token,
                mediaFiles = mediaFiles,
                initialIndex = initialIndex,
                currentPath = currentPath,
                onClose = { activeViewerFile = null },
                onToggleFavorite = { file, isFav ->
                    scope.launch {
                        val fullFilePath = file.fullPath ?: (if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}")
                        if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isFavourite = isFav)) {
                            file.isFavourite = isFav
                            triggerFileRefresh()
                            triggerFavoritesRefresh()
                        }
                    }
                },
                onDownload = { file ->
                    val fullFilePath = file.fullPath ?: (if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}")
                    scope.launch {
                        Toast.makeText(context, "Downloading ${file.name}...", Toast.LENGTH_SHORT).show()
                        val success = downloadFileToDevice(context, connection.serverUrl, connection.token, fullFilePath, file.name)
                        if (success) {
                            Toast.makeText(context, "Download complete: ${file.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShare = { file ->
                    val fullFilePath = file.fullPath ?: (if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}")
                    val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                    shareText(context, publicUrl, "Share link")
                }
            )
        }

        // Immersive viewer for Media Tab
        if (activeViewerMedia != null) {
            val mappedCloudFiles = mediaList.map { media ->
                CloudFile(
                    name = media.name,
                    size = media.size,
                    type = if (media.type == "video") "video/mp4" else "image/jpeg",
                    lastModified = media.lastModified,
                    isFolder = false,
                    isPublic = media.isPublic,
                    isFavourite = media.isFavourite,
                    description = media.description,
                    tags = media.tags,
                    previewImage = null,
                    fullPath = media.fullPath
                )
            }
            val initialIndex = mediaList.indexOfFirst { it.fullPath == activeViewerMedia?.fullPath }.coerceAtLeast(0)

            ImmersiveMediaViewer(
                serverUrl = connection.serverUrl,
                token = connection.token,
                mediaFiles = mappedCloudFiles,
                initialIndex = initialIndex,
                currentPath = "",
                onClose = { activeViewerMedia = null },
                onToggleFavorite = { file, isFav ->
                    scope.launch {
                        val fullPath = file.fullPath ?: file.name
                        if (updateFileMeta(connection.serverUrl, connection.token, fullPath, isFavourite = isFav)) {
                            file.isFavourite = isFav
                            triggerMediaRefresh()
                        }
                    }
                },
                onDownload = { file ->
                    val fullPath = file.fullPath ?: file.name
                    scope.launch {
                        Toast.makeText(context, "Downloading ${file.name}...", Toast.LENGTH_SHORT).show()
                        val success = downloadFileToDevice(context, connection.serverUrl, connection.token, fullPath, file.name)
                        if (success) {
                            Toast.makeText(context, "Download complete: ${file.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShare = { file ->
                    val fullPath = file.fullPath ?: file.name
                    val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullPath)}"
                    shareText(context, publicUrl, "Share link")
                }
            )
        }

        // Detailed Player overlay for Media Tab
        if (activeDetailMedia != null) {
            val media = activeDetailMedia!!
            MediaDetailsView(
                serverUrl = connection.serverUrl,
                token = connection.token,
                username = connection.username,
                media = media,
                allVideos = allMediaList,
                predefinedTags = predefinedTags,
                onPredefinedTagsChanged = { predefinedTags = it },
                onClose = { activeDetailMedia = null },
                onToggleFavorite = { item, isFav ->
                    scope.launch {
                        if (updateFileMeta(connection.serverUrl, connection.token, item.fullPath, isFavourite = isFav)) {
                            activeDetailMedia = item.copy(isFavourite = isFav)
                            triggerMediaRefresh()
                        }
                    }
                },
                onTogglePublic = { item, isPub ->
                    scope.launch {
                        if (updateFileMeta(connection.serverUrl, connection.token, item.fullPath, isPublic = isPub)) {
                            activeDetailMedia = item.copy(isPublic = isPub)
                            triggerMediaRefresh()
                        }
                    }
                },
                onDownload = { item ->
                    scope.launch {
                        Toast.makeText(context, "Downloading ${item.name}...", Toast.LENGTH_SHORT).show()
                        val success = downloadFileToDevice(context, connection.serverUrl, connection.token, item.fullPath, item.name)
                        if (success) {
                            Toast.makeText(context, "Download complete: ${item.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShare = { item ->
                    val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(item.fullPath)}"
                    shareText(context, publicUrl, "Share public link")
                },
                onExcludeParentFolder = { item ->
                    scope.launch {
                        val pathParts = item.fullPath.split('/')
                        if (pathParts.size > 1) {
                            val parentFolder = pathParts.dropLast(1).joinToString("/")
                            val settings = fetchUserSettings(connection.serverUrl, connection.token)
                            if (settings != null) {
                                val excluded = mutableListOf<String>()
                                val arr = settings.optJSONArray("excludedMediaPaths")
                                if (arr != null) {
                                    for (j in 0 until arr.length()) {
                                        excluded.add(arr.getString(j))
                                    }
                                }
                                if (!excluded.contains(parentFolder)) {
                                    excluded.add(parentFolder)
                                    val updatedArr = JSONArray()
                                    excluded.forEach { updatedArr.put(it) }
                                    settings.put("excludedMediaPaths", updatedArr)
                                    if (saveUserSettings(connection.serverUrl, connection.token, settings)) {
                                        Toast.makeText(context, "Folder excluded successfully", Toast.LENGTH_SHORT).show()
                                        activeDetailMedia = null
                                        triggerMediaRefresh()
                                    } else {
                                        Toast.makeText(context, "Failed to update settings", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Folder already excluded", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Failed to fetch settings", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Cannot exclude root folder", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onVideoSelected = { video ->
                    activeDetailMedia = video
                },
                onSaveDescription = { item, newDesc ->
                    scope.launch {
                        if (updateFileMeta(connection.serverUrl, connection.token, item.fullPath, description = newDesc)) {
                            activeDetailMedia = item.copy(description = newDesc)
                            triggerMediaRefresh()
                            Toast.makeText(context, "Description saved", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to save description", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onUpdateTags = { item, updatedTags ->
                    scope.launch {
                        if (updateFileMeta(connection.serverUrl, connection.token, item.fullPath, tags = updatedTags)) {
                            activeDetailMedia = item.copy(tags = updatedTags)
                            triggerMediaRefresh()
                        }
                    }
                }
            )
        }
    }
}

// Files Tab content Composable
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesTabContent(
    hideFolders: Boolean,
    predefinedTags: List<TagItem>,
    serverUrl: String,
    token: String,
    currentPath: String,
    pathStack: List<String>,
    files: List<CloudFile>,
    isLoading: Boolean,
    searchQuery: String,
    showFavoritesOnly: Boolean,
    isGridView: Boolean,
    autoplayPreviews: Boolean,
    sortOption: String,
    isSelectionMode: Boolean,
    selectedFiles: Set<String>,
    onPathChanged: (String) -> Unit,
    onFileClick: (CloudFile, List<CloudFile>) -> Unit,
    onToggleFavorite: (CloudFile, Boolean) -> Unit,
    onTogglePublic: (CloudFile, Boolean) -> Unit,
    onCopyLink: (CloudFile) -> Unit,
    onDownload: (CloudFile) -> Unit,
    onDelete: (CloudFile) -> Unit,
    onEditTags: (CloudFile) -> Unit,
    onToggleSelection: (CloudFile) -> Unit,
    onLongClick: (CloudFile) -> Unit,
    onRename: (CloudFile) -> Unit,
    onMove: (CloudFile) -> Unit,
    onDownloadZip: (CloudFile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTagFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPath) {
        selectedTagFilter = null
    }

    val folderScrollStates = remember<MutableMap<String, Pair<Int, Int>>> { mutableMapOf() }

    val gridState = remember(currentPath, isGridView) {
        val saved = folderScrollStates[currentPath] ?: Pair(0, 0)
        androidx.compose.foundation.lazy.grid.LazyGridState(firstVisibleItemIndex = saved.first, firstVisibleItemScrollOffset = saved.second)
    }

    val listState = remember(currentPath, isGridView) {
        val saved = folderScrollStates[currentPath] ?: Pair(0, 0)
        androidx.compose.foundation.lazy.LazyListState(firstVisibleItemIndex = saved.first, firstVisibleItemScrollOffset = saved.second)
    }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        if (isGridView) {
            folderScrollStates[currentPath] = Pair(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (!isGridView) {
            folderScrollStates[currentPath] = Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
    }

    val sortedFiles = remember(files, searchQuery, showFavoritesOnly, sortOption, selectedTagFilter, hideFolders) {
        files.filter { file ->
            val matchesSearch = file.name.contains(searchQuery, ignoreCase = true) ||
                                file.tags.any { it.name.contains(searchQuery, ignoreCase = true) }
            val matchesFav = !showFavoritesOnly || file.isFavourite
            val matchesTag = selectedTagFilter == null || file.tags.any { it.name.equals(selectedTagFilter, ignoreCase = true) }
            val matchesFolderHide = !hideFolders || !file.isFolder
            matchesSearch && matchesFav && matchesTag && matchesFolderHide
        }.sortedWith { a, b ->
            if (a.isFolder != b.isFolder) {
                if (a.isFolder) -1 else 1
            } else {
                when (sortOption) {
                    "size" -> b.size.compareTo(a.size)
                    "date" -> b.lastModified.compareTo(a.lastModified)
                    else -> a.name.compareTo(b.name, ignoreCase = true)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Breadcrumb Trail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Root",
                color = if (currentPath.isEmpty()) LocalClyoTheme.current.accent1 else Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onPathChanged("") }
                    .padding(vertical = 4.dp)
            )

            pathStack.forEachIndexed { index, segment ->
                Text(" > ", color = Color(0xFFA0A0C0), modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = segment,
                    color = if (index == pathStack.lastIndex) LocalClyoTheme.current.accent1 else Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            val newPath = pathStack.take(index + 1).joinToString("/")
                            onPathChanged(newPath)
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Horizontal Tag Filters
        if (predefinedTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All" chip
                val isAllSelected = selectedTagFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAllSelected) LocalClyoTheme.current.accent1.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = if (isAllSelected) LocalClyoTheme.current.accent1 else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTagFilter = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All",
                        color = if (isAllSelected) LocalClyoTheme.current.accent1 else Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                predefinedTags.forEach { tag ->
                    val isSelected = selectedTagFilter == tag.name
                    val tagColor = Color(android.graphics.Color.parseColor(tag.color))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) tagColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) tagColor else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedTagFilter = if (isSelected) null else tag.name
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(tagColor, CircleShape)
                            )
                            Text(
                                text = tag.name,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        if (isLoading && files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LocalClyoTheme.current.accent1)
            }
        } else if (sortedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files found", color = Color(0xFFA0A0C0))
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = sortedFiles, key = { it.name }) { file ->
                        var isMenuExpanded by remember { mutableStateOf(false) }
                        val fileRelativePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                        val isSelected = selectedFiles.contains(file.name)

                        GlassCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isSelected) Modifier.border(
                                        width = 2.dp,
                                        color = LocalClyoTheme.current.accent1,
                                        shape = RoundedCornerShape(16.dp)
                                    ) else Modifier
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelection(file)
                                        } else {
                                            if (file.isFolder) {
                                                val newPath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                                onPathChanged(newPath)
                                            } else {
                                                onFileClick(file, sortedFiles.filter { !it.isFolder && (isImageFile(it.name) || isVideoFile(it.name)) })
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        onLongClick(file)
                                    }
                                )
                        ) {
                            Column {
                                // Thumbnail / Preview Area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(Color.Black.copy(alpha = 0.2f))
                                ) {
                                    if (file.isFolder) {
                                        if (file.previewImage != null) {
                                            AsyncServerImage(
                                                serverUrl = serverUrl,
                                                token = token,
                                                relativePath = "$fileRelativePath/${file.previewImage}",
                                                isThumbnail = true,
                                                contentDescription = file.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = FolderIcon,
                                                contentDescription = null,
                                                tint = LocalClyoTheme.current.accent1,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    } else {
                                        if (hasThumbnail(file.name)) {
                                            if (isVideoFile(file.name) && autoplayPreviews) {
                                                RemoteGifPlayer(
                                                    serverUrl = serverUrl,
                                                    token = token,
                                                    relativePath = fileRelativePath,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                AsyncServerImage(
                                                    serverUrl = serverUrl,
                                                    token = token,
                                                    relativePath = fileRelativePath,
                                                    isThumbnail = true,
                                                    contentDescription = file.name,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (isVideoFile(file.name)) PlayIcon else FileIcon,
                                                contentDescription = null,
                                                tint = LocalClyoTheme.current.accent2,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }

                                    // Selection status checkbox overlay (top-left)
                                    if (isSelectionMode) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .background(
                                                    if (isSelected) LocalClyoTheme.current.accent1 else Color.Black.copy(alpha = 0.4f),
                                                    CircleShape
                                                )
                                                .clickable { onToggleSelection(file) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CheckIcon,
                                                contentDescription = "Selected",
                                                tint = if (isSelected) Color(0xFF0F0C1B) else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    // Star/Favorite top-right overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            .clickable { onToggleFavorite(file, !file.isFavourite) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (file.isFavourite) StarIcon else StarBorderIcon,
                                            contentDescription = "Favorite",
                                            tint = if (file.isFavourite) Color(0xFFFFD700) else Color(0xFFA0A0C0),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Quick Copy Link overlay if shared
                                    if (file.isPublic && !isSelectionMode) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                                .clickable { onCopyLink(file) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = ShareIcon,
                                                contentDescription = "Copy Link",
                                                tint = LocalClyoTheme.current.accent2,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Metadata Area
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (file.isFolder) "Folder" else formatFileSize(file.size),
                                            color = Color(0xFFA0A0C0),
                                            fontSize = 9.sp
                                        )
                                    }
                                    Box {
                                        IconButton(
                                            onClick = { isMenuExpanded = true },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = MoreVertIcon,
                                                contentDescription = "Actions",
                                                tint = Color(0xFFA0A0C0),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = isMenuExpanded,
                                            onDismissRequest = { isMenuExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1E1E2C))
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(if (file.isPublic) "Make Private" else "Make Public", color = LocalClyoTheme.current.accent1, fontSize = 13.sp) },
                                                onClick = { onTogglePublic(file, !file.isPublic); isMenuExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Details & Tags", color = LocalClyoTheme.current.accent2, fontSize = 13.sp) },
                                                onClick = { onEditTags(file); isMenuExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Rename", color = Color.White, fontSize = 13.sp) },
                                                onClick = { onRename(file); isMenuExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Move", color = Color.White, fontSize = 13.sp) },
                                                onClick = { onMove(file); isMenuExpanded = false }
                                            )
                                            if (!file.isFolder) {
                                                DropdownMenuItem(
                                                    text = { Text("Download", color = Color.White, fontSize = 13.sp) },
                                                    onClick = { onDownload(file); isMenuExpanded = false }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text("Download ZIP", color = Color.White, fontSize = 13.sp) },
                                                    onClick = { onDownloadZip(file); isMenuExpanded = false }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = Color(0xFFFF5252), fontSize = 13.sp) },
                                                onClick = { onDelete(file); isMenuExpanded = false }
                                            )
                                        }
                                    }
                                }

                                // Tags display (mini color dots)
                                if (file.tags.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        file.tags.take(4).forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(android.graphics.Color.parseColor(tag.color)), CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = sortedFiles, key = { it.name }) { file ->
                        var isMenuExpanded by remember { mutableStateOf(false) }
                        val isSelected = selectedFiles.contains(file.name)

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isSelected) Modifier.border(
                                        width = 1.5.dp,
                                        color = LocalClyoTheme.current.accent1,
                                        shape = RoundedCornerShape(12.dp)
                                    ) else Modifier
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelection(file)
                                        } else {
                                            if (file.isFolder) {
                                                val newPath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                                onPathChanged(newPath)
                                            } else {
                                                onFileClick(file, sortedFiles.filter { !it.isFolder && (isImageFile(it.name) || isVideoFile(it.name)) })
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        onLongClick(file)
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelectionMode) {
                                        IconButton(onClick = { onToggleSelection(file) }, modifier = Modifier.size(32.dp)) {
                                            Icon(
                                                imageVector = CheckIcon,
                                                contentDescription = "Selected",
                                                tint = if (isSelected) LocalClyoTheme.current.accent1 else Color(0xFFA0A0C0),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Icon(
                                            imageVector = if (file.isFolder) FolderIcon else FileIcon,
                                            contentDescription = null,
                                            tint = if (file.isFolder) LocalClyoTheme.current.accent1 else LocalClyoTheme.current.accent2,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (file.isFolder) "Folder" else formatFileSize(file.size),
                                                color = Color(0xFFA0A0C0),
                                                fontSize = 11.sp
                                            )
                                            if (file.isPublic) {
                                                Text(
                                                    text = "Shared",
                                                    color = LocalClyoTheme.current.accent2,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Toggle Favorite icon directly
                                    IconButton(onClick = { onToggleFavorite(file, !file.isFavourite) }) {
                                        Icon(
                                            imageVector = if (file.isFavourite) StarIcon else StarBorderIcon,
                                            contentDescription = "Favorite",
                                            tint = if (file.isFavourite) Color(0xFFFFD700) else Color(0xFFA0A0C0),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Quick share/link button
                                    if (file.isPublic) {
                                        IconButton(onClick = { onCopyLink(file) }) {
                                            Icon(ShareIcon, contentDescription = "Copy Link", tint = LocalClyoTheme.current.accent2, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // Options menu expander
                                    IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                                        Icon(MoreVertIcon, contentDescription = "Actions", tint = Color(0xFFA0A0C0), modifier = Modifier.size(20.dp))
                                    }
                                }

                                // Tags Display
                                if (file.tags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        file.tags.forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .border(1.dp, Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(tag.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Expanded Actions drawer
                                if (isMenuExpanded) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        TextButton(onClick = { onTogglePublic(file, !file.isPublic); isMenuExpanded = false }) {
                                            Text(if (file.isPublic) "Make Private" else "Make Public", color = LocalClyoTheme.current.accent1, fontSize = 12.sp)
                                        }
                                        TextButton(onClick = { onEditTags(file); isMenuExpanded = false }) {
                                            Text("Details", color = LocalClyoTheme.current.accent2, fontSize = 12.sp)
                                        }
                                        TextButton(onClick = { onRename(file); isMenuExpanded = false }) {
                                            Text("Rename", color = Color.White, fontSize = 12.sp)
                                        }
                                        TextButton(onClick = { onMove(file); isMenuExpanded = false }) {
                                            Text("Move", color = Color.White, fontSize = 12.sp)
                                        }
                                        TextButton(onClick = {
                                            if (!file.isFolder) {
                                                onDownload(file)
                                            } else {
                                                onDownloadZip(file)
                                            }
                                            isMenuExpanded = false
                                        }) {
                                            Text(if (file.isFolder) "Zip" else "Download", color = Color.White, fontSize = 12.sp)
                                        }
                                        TextButton(onClick = { onDelete(file); isMenuExpanded = false }) {
                                            Text("Delete", color = Color(0xFFFF5252), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Favorites Tab content Composable
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesTabContent(
    hideFolders: Boolean,
    predefinedTags: List<TagItem>,
    serverUrl: String,
    token: String,
    files: List<CloudFile>,
    isLoading: Boolean,
    searchQuery: String,
    isGridView: Boolean,
    autoplayPreviews: Boolean,
    sortOption: String,
    onFolderClick: (CloudFile) -> Unit,
    onFileClick: (CloudFile, List<CloudFile>) -> Unit,
    onToggleFavorite: (CloudFile, Boolean) -> Unit,
    onTogglePublic: (CloudFile, Boolean) -> Unit,
    onCopyLink: (CloudFile) -> Unit,
    onDownload: (CloudFile) -> Unit,
    onDelete: (CloudFile) -> Unit,
    onEditTags: (CloudFile) -> Unit,
    onRename: (CloudFile) -> Unit,
    onMove: (CloudFile) -> Unit
) {
    val context = LocalContext.current
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }

    val sortedFiles = remember(files, searchQuery, sortOption, selectedTagFilter, hideFolders) {
        files.filter { file ->
            val matchesSearch = file.name.contains(searchQuery, ignoreCase = true) ||
                                file.tags.any { it.name.contains(searchQuery, ignoreCase = true) }
            val matchesTag = selectedTagFilter == null || file.tags.any { it.name.equals(selectedTagFilter, ignoreCase = true) }
            val matchesFolderHide = !hideFolders || !file.isFolder
            matchesSearch && matchesTag && matchesFolderHide
        }.sortedWith { a, b ->
            if (a.isFolder != b.isFolder) {
                if (a.isFolder) -1 else 1
            } else {
                when (sortOption) {
                    "size" -> b.size.compareTo(a.size)
                    "date" -> b.lastModified.compareTo(a.lastModified)
                    else -> a.name.compareTo(b.name, ignoreCase = true)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Tag Filters
        if (predefinedTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All" chip
                val isAllSelected = selectedTagFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAllSelected) LocalClyoTheme.current.accent1.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = if (isAllSelected) LocalClyoTheme.current.accent1 else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTagFilter = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All",
                        color = if (isAllSelected) LocalClyoTheme.current.accent1 else Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                predefinedTags.forEach { tag ->
                    val isSelected = selectedTagFilter == tag.name
                    val tagColor = Color(android.graphics.Color.parseColor(tag.color))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) tagColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) tagColor else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedTagFilter = if (isSelected) null else tag.name
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(tagColor, CircleShape)
                            )
                            Text(
                                text = tag.name,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        if (isLoading && files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LocalClyoTheme.current.accent1)
            }
        } else if (sortedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites found", color = Color(0xFFA0A0C0))
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = sortedFiles, key = { it.fullPath ?: it.name }) { file ->
                        var isMenuExpanded by remember { mutableStateOf(false) }
                        val fileRelativePath = file.fullPath ?: file.name

                        GlassCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (file.isFolder) {
                                        onFolderClick(file)
                                    } else {
                                        onFileClick(file, sortedFiles.filter { !it.isFolder && (isImageFile(it.name) || isVideoFile(it.name)) })
                                    }
                                }
                        ) {
                            Column {
                                // Thumbnail / Preview Area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(Color.Black.copy(alpha = 0.2f))
                                ) {
                                    if (file.isFolder) {
                                        if (file.previewImage != null) {
                                            AsyncServerImage(
                                                serverUrl = serverUrl,
                                                token = token,
                                                relativePath = "$fileRelativePath/${file.previewImage}",
                                                isThumbnail = true,
                                                contentDescription = file.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = FolderIcon,
                                                contentDescription = null,
                                                tint = LocalClyoTheme.current.accent1,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    } else {
                                        if (hasThumbnail(file.name)) {
                                            if (isVideoFile(file.name) && autoplayPreviews) {
                                                RemoteGifPlayer(
                                                    serverUrl = serverUrl,
                                                    token = token,
                                                    relativePath = fileRelativePath,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                AsyncServerImage(
                                                    serverUrl = serverUrl,
                                                    token = token,
                                                    relativePath = fileRelativePath,
                                                    isThumbnail = true,
                                                    contentDescription = file.name,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (isVideoFile(file.name)) PlayIcon else FileIcon,
                                                contentDescription = null,
                                                tint = LocalClyoTheme.current.accent2,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }

                                    // Star/Favorite top-right overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            .clickable { onToggleFavorite(file, !file.isFavourite) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (file.isFavourite) StarIcon else StarBorderIcon,
                                            contentDescription = "Favorite",
                                            tint = if (file.isFavourite) Color(0xFFFFD700) else Color(0xFFA0A0C0),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Quick Copy Link overlay if shared
                                    if (file.isPublic) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                                .clickable { onCopyLink(file) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = ShareIcon,
                                                contentDescription = "Copy Link",
                                                tint = LocalClyoTheme.current.accent2,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Metadata Area
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (file.isFolder) "Folder" else formatFileSize(file.size),
                                            color = Color(0xFFA0A0C0),
                                            fontSize = 9.sp
                                        )
                                    }

                                    Box {
                                        IconButton(
                                            onClick = { isMenuExpanded = true },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = MoreVertIcon,
                                                contentDescription = "More Actions",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = isMenuExpanded,
                                            onDismissRequest = { isMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Details & Tags") },
                                                onClick = { isMenuExpanded = false; onEditTags(file) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (file.isPublic) "Disable Link" else "Share Link") },
                                                onClick = { isMenuExpanded = false; onTogglePublic(file, !file.isPublic) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Download") },
                                                onClick = { isMenuExpanded = false; onDownload(file) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Rename") },
                                                onClick = { isMenuExpanded = false; onRename(file) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Move") },
                                                onClick = { isMenuExpanded = false; onMove(file) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Remove Favorite", color = Color(0xFFFF5252)) },
                                                onClick = { isMenuExpanded = false; onToggleFavorite(file, false) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = Color(0xFFFF5252)) },
                                                onClick = { isMenuExpanded = false; onDelete(file) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = sortedFiles, key = { it.fullPath ?: it.name }) { file ->
                        var isMenuExpanded by remember { mutableStateOf(false) }
                        val fileRelativePath = file.fullPath ?: file.name

                        GlassCard(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (file.isFolder) {
                                        onFolderClick(file)
                                    } else {
                                        onFileClick(file, sortedFiles.filter { !it.isFolder && (isImageFile(it.name) || isVideoFile(it.name)) })
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini Preview
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                ) {
                                    if (file.isFolder) {
                                        if (file.previewImage != null) {
                                            AsyncServerImage(
                                                serverUrl = serverUrl,
                                                token = token,
                                                relativePath = "$fileRelativePath/${file.previewImage}",
                                                isThumbnail = true,
                                                contentDescription = file.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = FolderIcon,
                                                contentDescription = null,
                                                tint = LocalClyoTheme.current.accent1,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    } else {
                                        if (hasThumbnail(file.name)) {
                                            AsyncServerImage(
                                                serverUrl = serverUrl,
                                                token = token,
                                                relativePath = fileRelativePath,
                                                isThumbnail = true,
                                                contentDescription = file.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isVideoFile(file.name)) PlayIcon else FileIcon,
                                                contentDescription = null,
                                                tint = LocalClyoTheme.current.accent2,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Meta Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (file.isFolder) "Folder" else formatFileSize(file.size),
                                            color = Color(0xFFA0A0C0),
                                            fontSize = 9.sp
                                        )
                                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified))
                                        Text(
                                            text = dateStr,
                                            color = Color(0xFFA0A0C0),
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                // Quick Favorite Toggle
                                IconButton(
                                    onClick = { onToggleFavorite(file, !file.isFavourite) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (file.isFavourite) StarIcon else StarBorderIcon,
                                        contentDescription = "Favorite",
                                        tint = if (file.isFavourite) Color(0xFFFFD700) else Color(0xFFA0A0C0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { isMenuExpanded = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = MoreVertIcon,
                                            contentDescription = "More",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = isMenuExpanded,
                                        onDismissRequest = { isMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Details & Tags") },
                                            onClick = { isMenuExpanded = false; onEditTags(file) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (file.isPublic) "Disable Link" else "Share Link") },
                                            onClick = { isMenuExpanded = false; onTogglePublic(file, !file.isPublic) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Download") },
                                            onClick = { isMenuExpanded = false; onDownload(file) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename") },
                                            onClick = { isMenuExpanded = false; onRename(file) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Move") },
                                            onClick = { isMenuExpanded = false; onMove(file) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Remove Favorite", color = Color(0xFFFF5252)) },
                                            onClick = { isMenuExpanded = false; onToggleFavorite(file, false) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = Color(0xFFFF5252)) },
                                            onClick = { isMenuExpanded = false; onDelete(file) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Media Tab content Composable
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaTabContent(
    serverUrl: String,
    token: String,
    mediaList: List<MediaItem>,
    isLoading: Boolean,
    searchQuery: String,
    showFavoritesOnly: Boolean,
    mediaPage: Int,
    mediaPageSize: Int,
    mediaTotalCount: Int,
    onPageChanged: (Int) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit
) {
    val filteredMedia = mediaList.filter { media ->
        val matchesFav = !showFavoritesOnly || media.isFavourite
        matchesFav
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LocalClyoTheme.current.accent1)
            }
        } else if (filteredMedia.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No media found", color = Color(0xFFA0A0C0))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = filteredMedia, key = { it.fullPath }) { media ->
                    // Thumbnail loading path:
                    // For video: use fullPath
                    // For slideshow: use the first image in list if available
                    val thumbnailPath = if (media.type == "slideshow" && media.images.isNotEmpty()) {
                        media.images[0]
                    } else {
                        media.fullPath
                    }

                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onMediaClick(media) },
                                onLongClick = { onMediaLongClick(media) }
                            )
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f)
                                    .background(Color.Black)
                            ) {
                                AsyncServerImage(
                                    serverUrl = serverUrl,
                                    token = token,
                                    relativePath = thumbnailPath,
                                    isThumbnail = true,
                                    contentDescription = media.name,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Play overlay badge for video
                                if (media.type == "video") {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .align(Alignment.Center)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = PlayIcon,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }

                                // Slideshow indicator badge
                                if (media.type == "slideshow") {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(LocalClyoTheme.current.accent1, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("SLIDESHOW", color = Color(0xFF0F0C1B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = media.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (media.type == "slideshow") "Slideshow" else formatFileSize(media.size),
                                        color = Color(0xFFA0A0C0),
                                        fontSize = 10.sp
                                    )
                                    if (media.isFavourite) {
                                        Icon(StarIcon, contentDescription = "Favorite", tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pagination controls footer
        if (mediaTotalCount > mediaPageSize) {
            val totalPages = (mediaTotalCount + mediaPageSize - 1) / mediaPageSize
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onPageChanged((mediaPage - 1).coerceAtLeast(1)) },
                    enabled = mediaPage > 1,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.03f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Prev", fontSize = 12.sp)
                }

                Text(
                    text = "Page $mediaPage of $totalPages",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = { onPageChanged((mediaPage + 1).coerceAtMost(totalPages)) },
                    enabled = mediaPage < totalPages,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.03f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Next", fontSize = 12.sp)
                }
            }
        }
    }
}

// Settings Tab Composable
@Composable
fun SettingsTabContent(
    connection: CloudConnection,
    onDisconnect: () -> Unit,
    onThemeChange: (ClyoTheme) -> Unit = {}
) {
    val theme = LocalClyoTheme.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User info card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(theme.accent1.copy(alpha = 0.18f), CircleShape)
                            .border(2.dp, theme.accent1.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            connection.username.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = theme.accent1
                        )
                    }
                    Text(connection.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("SERVER", color = Color(0xFFA0A0C0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(connection.serverUrl, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // Theme picker card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(theme.accent1.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎨", fontSize = 18.sp)
                        }
                        Column {
                            Text("App Theme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Choose your colour palette", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClyoThemes.all.forEach { t ->
                            val isSelected = t.name == theme.name
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onThemeChange(t) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(t.swatchColors)
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(2.5.dp, Color.White, CircleShape)
                                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text("✓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    t.displayName,
                                    color = if (isSelected) theme.accent1 else Color(0xFFA0A0C0),
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Disconnect card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(ExitIcon, contentDescription = "Logout", modifier = Modifier.size(18.dp))
                            Text("Disconnect Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Immersive Media Viewer Overlay
@Composable
fun ImmersiveMediaViewer(
    serverUrl: String,
    token: String,
    mediaFiles: List<CloudFile>,
    initialIndex: Int,
    currentPath: String,
    onClose: () -> Unit,
    onToggleFavorite: (CloudFile, Boolean) -> Unit,
    onDownload: (CloudFile) -> Unit,
    onShare: (CloudFile) -> Unit
) {
    if (mediaFiles.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = initialIndex) { mediaFiles.size }
    val currentFile = mediaFiles.getOrNull(pagerState.currentPage) ?: return
    val isVideo = MimeTypeMap.getFileExtensionFromUrl(currentFile.name).lowercase() in listOf("mp4", "webm", "mov", "mkv")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        // Immersive Player / Image view inside Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val file = mediaFiles[pageIndex]
            val path = file.fullPath ?: (if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}")
            val isPageVideo = MimeTypeMap.getFileExtensionFromUrl(file.name).lowercase() in listOf("mp4", "webm", "mov", "mkv")

            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            // Reset zoom scale on page change
            LaunchedEffect(pageIndex) {
                scale = 1f
                offset = Offset.Zero
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isPageVideo) {
                    val encodedSegments = path.split('/').map { Uri.encode(it) }
                    val cleanPath = encodedSegments.joinToString("/")
                    val streamUrl = "$serverUrl/file/$cleanPath"
                    VideoPlayer(
                        videoUrl = streamUrl,
                        token = token,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                    )
                } else {
                    var localBitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
                    var isImageLoading by remember(path) { mutableStateOf(true) }

                    LaunchedEffect(path) {
                        isImageLoading = true
                        try {
                            val bitmapObj = fetchBitmapFromUrl(serverUrl, token, path, false)
                            localBitmap = bitmapObj?.asImageBitmap()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isImageLoading = false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectZoomPanGestures(
                                    scaleProvider = { scale },
                                    onGesture = { pan, zoom ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        offset = if (scale > 1f) offset + pan else Offset.Zero
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImageLoading) {
                            CircularProgressIndicator(color = LocalClyoTheme.current.accent1)
                        } else if (localBitmap != null) {
                            Image(
                                bitmap = localBitmap!!,
                                contentDescription = file.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red)
                        }
                    }
                }
            }
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(CloseIcon, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Text(
                text = currentFile.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            Row {
                IconButton(onClick = { onToggleFavorite(currentFile, !currentFile.isFavourite) }) {
                    Icon(
                        imageVector = if (currentFile.isFavourite) StarIcon else StarBorderIcon,
                        contentDescription = "Favorite",
                        tint = if (currentFile.isFavourite) Color(0xFFFFD700) else Color.White
                    )
                }
                IconButton(onClick = { onDownload(currentFile) }) {
                    Icon(DownloadIcon, contentDescription = "Download", tint = Color.White)
                }
                if (currentFile.isPublic) {
                    IconButton(onClick = { onShare(currentFile) }) {
                        Icon(ShareIcon, contentDescription = "Share", tint = LocalClyoTheme.current.accent2)
                    }
                }
            }
        }
    }
}

// Custom gesture detector helper to resolve Zoom vs Pager Swipe conflicts
suspend fun PointerInputScope.detectZoomPanGestures(
    scaleProvider: () -> Float,
    onGesture: (pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val scale = scaleProvider()
                    val isPinch = event.changes.size > 1
                    
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(panChange, zoomChange)
                    }
                    
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            // Only consume the pointer event if we are actively pinching (zooming)
                            // or if the image is already zoomed in (scale > 1f).
                            // This allows normal 1-finger swipes to bubble up to the HorizontalPager!
                            if (isPinch || scale > 1f) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

// Custom Media3 ExoPlayer wrapper with custom headers
@Composable
fun VideoPlayer(
    videoUrl: String,
    token: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
        }
    }

    LaunchedEffect(videoUrl, token) {
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("ClyoCloudApp")
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
        val mediaSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(androidx.media3.common.MediaItem.fromUri(videoUrl))
        
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier
    )
}

// --- Composable helper: Async Server Image ---
@Composable
fun AsyncServerImage(
    serverUrl: String,
    token: String,
    relativePath: String,
    isThumbnail: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(relativePath) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(relativePath) { mutableStateOf(true) }

    LaunchedEffect(relativePath, token) {
        isLoading = true
        try {
            val loaded = fetchBitmapFromUrl(serverUrl, token, relativePath, isThumbnail)
            bitmap = loaded?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = LocalClyoTheme.current.accent1)
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFFF5252).copy(alpha = 0.5f)
            )
        }
    }
}

// --- Composable helper: Remote GIF Player for video preview animations ---
@Composable
fun RemoteGifPlayer(
    serverUrl: String,
    token: String,
    relativePath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var drawableState by remember(relativePath) { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(relativePath, token) {
        withContext(Dispatchers.IO) {
            val encodedSegments = relativePath.split('/').map { Uri.encode(it) }
            val cleanPath = encodedSegments.joinToString("/")
            val url = URL("$serverUrl/preview/$cleanPath")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            try {
                if (conn.responseCode == 200) {
                    val cacheFile = File(context.cacheDir, "gif_preview_${relativePath.hashCode()}.gif")
                    conn.inputStream.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(cacheFile)
                        val drawable = ImageDecoder.decodeDrawable(source)
                        drawableState = drawable
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn.disconnect()
            }
        }
    }

    if (drawableState != null) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            },
            update = { imageView ->
                imageView.setImageDrawable(drawableState)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val d = imageView.drawable
                    if (d is AnimatedImageDrawable) {
                        d.start()
                    }
                }
            },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = LocalClyoTheme.current.accent1)
        }
    }
}

// File classification helpers matching the website
fun isVideoFile(fileName: String): Boolean {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return listOf("mp4", "webm", "mov", "mkv", "3gp", "avi").contains(ext)
}

fun isImageFile(fileName: String): Boolean {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return listOf("jpg", "jpeg", "png", "webp", "gif", "bmp").contains(ext)
}

fun hasThumbnail(fileName: String): Boolean {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return listOf("jpg", "jpeg", "png", "gif", "mp4", "webm", "mov", "mkv").contains(ext)
}

// --- Networking & Helpers ---

sealed class LoginResponse {
    data class Success(val token: String) : LoginResponse()
    data class Requires2Fa(val tempToken: String) : LoginResponse()
    data class Error(val message: String) : LoginResponse()
}

fun mapLocalhostForEmulator(url: String): String {
    val isEmulator = Build.FINGERPRINT.startsWith("generic")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
    if (isEmulator) {
        return url.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")
    }
    return url
}

suspend fun testToken(serverUrl: String, token: String): Boolean = withContext(Dispatchers.IO) {
    val mappedUrl = mapLocalhostForEmulator(serverUrl)
    var formattedUrl = mappedUrl.trim()
    if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
        formattedUrl = "http://$formattedUrl"
    }
    formattedUrl = formattedUrl.removeSuffix("/")
    
    try {
        val url = URL("$formattedUrl/api/verify")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        val code = conn.responseCode
        code == 200
    } catch (e: Exception) {
        Log.e("ClyoCloud", "Token validation failed: ${e.message}")
        false
    }
}

suspend fun loginToCloud(serverUrl: String, user: String, pass: String): LoginResponse = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/login")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("username", user)
            put("password", pass)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val json = JSONObject(response.toString())
            if (json.has("requires2fa") && json.getBoolean("requires2fa")) {
                LoginResponse.Requires2Fa(json.getString("tempToken"))
            } else {
                LoginResponse.Success(json.getString("token"))
            }
        } else {
            LoginResponse.Error("Invalid username or password")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        LoginResponse.Error(e.message ?: "Connection error")
    } finally {
        conn.disconnect()
    }
}

suspend fun verify2fa(serverUrl: String, tempToken: String, code: String): String? = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/2fa/verify-login")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("tempToken", tempToken)
            put("code", code)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            JSONObject(response.toString()).getString("token")
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        conn.disconnect()
    }
}

// Fetch Favorites list
suspend fun fetchFavoritesList(serverUrl: String, token: String): List<CloudFile> = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/favorites")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            throw Exception("Server returned HTTP code ${conn.responseCode}")
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val arr = JSONArray(response.toString())
        val list = mutableListOf<CloudFile>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val metaObj = obj.optJSONObject("meta")
            val isPublic = metaObj?.optBoolean("isPublic", false) ?: false
            val isFavourite = metaObj?.optBoolean("isFavourite", false) ?: true
            val isWallpaper = metaObj?.optBoolean("isWallpaper", false) ?: false
            val description = if (metaObj?.has("description") == true) metaObj.getString("description") else null
            val previewImage = if (metaObj?.has("previewImage") == true) metaObj.getString("previewImage") else null
            val tagsList = mutableListOf<TagItem>()
            val tagsArr = metaObj?.optJSONArray("tags")
            if (tagsArr != null) {
                for (j in 0 until tagsArr.length()) {
                    val tagObj = tagsArr.getJSONObject(j)
                    tagsList.add(TagItem(tagObj.getString("name"), tagObj.getString("color")))
                }
            }
            
            val path = obj.getString("name") // full relative path, e.g. "Folder/Sub/Image.png"
            val displayName = path.substringAfterLast('/')

            list.add(
                CloudFile(
                    name = displayName,
                    size = obj.getLong("size"),
                    type = obj.getString("type"),
                    lastModified = obj.getLong("lastModified"),
                    isFolder = obj.getBoolean("isFolder"),
                    isPublic = isPublic,
                    isFavourite = isFavourite,
                    isWallpaper = isWallpaper,
                    description = description,
                    tags = tagsList,
                    previewImage = previewImage,
                    fullPath = path
                )
            )
        }
        list
    } finally {
        conn.disconnect()
    }
}

// Fetch Files list for folder browser
suspend fun fetchFilesList(serverUrl: String, token: String, path: String): List<CloudFile> = withContext(Dispatchers.IO) {
    val encodedPath = Uri.encode(path)
    val url = URL("$serverUrl/api/files?path=$encodedPath")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            throw Exception("Server returned HTTP code ${conn.responseCode}")
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val arr = JSONArray(response.toString())
        val list = mutableListOf<CloudFile>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val metaObj = obj.optJSONObject("meta")
            val isPublic = metaObj?.optBoolean("isPublic", false) ?: false
            val isFavourite = metaObj?.optBoolean("isFavourite", false) ?: false
            val isWallpaper = metaObj?.optBoolean("isWallpaper", false) ?: false
            val description = if (metaObj?.has("description") == true) metaObj.getString("description") else null
            val previewImage = if (metaObj?.has("previewImage") == true) metaObj.getString("previewImage") else null
            val tagsList = mutableListOf<TagItem>()
            val tagsArr = metaObj?.optJSONArray("tags")
            if (tagsArr != null) {
                for (j in 0 until tagsArr.length()) {
                    val tagObj = tagsArr.getJSONObject(j)
                    tagsList.add(TagItem(tagObj.getString("name"), tagObj.getString("color")))
                }
            }
            val fileName = obj.getString("name")
            val fullPathVal = if (path.isEmpty()) fileName else "$path/$fileName"
            list.add(
                CloudFile(
                    name = fileName,
                    size = obj.getLong("size"),
                    type = obj.getString("type"),
                    lastModified = obj.getLong("lastModified"),
                    isFolder = obj.getBoolean("isFolder"),
                    isPublic = isPublic,
                    isFavourite = isFavourite,
                    isWallpaper = isWallpaper,
                    description = description,
                    tags = tagsList,
                    previewImage = previewImage,
                    fullPath = fullPathVal
                )
            )
        }
        list
    } finally {
        conn.disconnect()
    }
}

// Fetch Media list for media tab
suspend fun fetchMediaList(
    serverUrl: String,
    token: String,
    search: String? = null,
    page: Int = 0,
    limit: Int = 0
): MediaListResult = withContext(Dispatchers.IO) {
    val queryParams = mutableListOf<String>()
    if (page > 0) queryParams.add("page=$page")
    if (limit > 0) queryParams.add("limit=$limit")
    if (!search.isNullOrBlank()) queryParams.add("search=${Uri.encode(search)}")
    
    val queryString = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
    val url = URL("$serverUrl/api/videos$queryString")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            return@withContext MediaListResult(emptyList(), 0)
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val list = mutableListOf<MediaItem>()
        var total = 0
        val jsonStr = response.toString().trim()
        if (jsonStr.startsWith("{")) {
            val root = JSONObject(jsonStr)
            total = root.optInt("total", 0)
            val itemsArr = root.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    list.add(parseMediaItem(itemsArr.getJSONObject(i)))
                }
            }
        } else if (jsonStr.startsWith("[")) {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(parseMediaItem(arr.getJSONObject(i)))
            }
            total = list.size
        }
        MediaListResult(list, total)
    } catch (e: Exception) {
        e.printStackTrace()
        MediaListResult(emptyList(), 0)
    } finally {
        conn.disconnect()
    }
}

// Fetch Shorts feed (shuffled random video/slideshow media items)
suspend fun fetchShortsFeed(serverUrl: String, token: String): List<MediaItem> = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/videos?shuffle=true&limit=200&page=1")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            return@withContext emptyList()
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val list = mutableListOf<MediaItem>()
        val jsonStr = response.toString().trim()
        if (jsonStr.startsWith("{")) {
            val root = JSONObject(jsonStr)
            val itemsArr = root.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    list.add(parseMediaItem(itemsArr.getJSONObject(i)))
                }
            }
        } else if (jsonStr.startsWith("[")) {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(parseMediaItem(arr.getJSONObject(i)))
            }
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    } finally {
        conn.disconnect()
    }
}

fun parseMediaItem(obj: JSONObject): MediaItem {
    val type = obj.optString("type", "video")
    val name = obj.getString("name")
    val fullPath = obj.getString("fullPath")
    val size = obj.optLong("size", 0L)
    val lastModified = obj.optLong("lastModified", 0L)

    val imagesList = mutableListOf<String>()
    val imagesArr = obj.optJSONArray("images")
    if (imagesArr != null) {
        for (i in 0 until imagesArr.length()) {
            imagesList.add(imagesArr.getString(i))
        }
    }

    val metaObj = obj.optJSONObject("meta")
    val isPublic = metaObj?.optBoolean("isPublic", false) ?: false
    val isFavourite = metaObj?.optBoolean("isFavourite", false) ?: false
    val description = if (metaObj?.has("description") == true) metaObj.getString("description") else null

    val tagsList = mutableListOf<TagItem>()
    val tagsArr = metaObj?.optJSONArray("tags")
    if (tagsArr != null) {
        for (i in 0 until tagsArr.length()) {
            val tagObj = tagsArr.getJSONObject(i)
            tagsList.add(TagItem(tagObj.getString("name"), tagObj.getString("color")))
        }
    }

    return MediaItem(
        type = type,
        name = name,
        fullPath = fullPath,
        images = imagesList,
        size = size,
        lastModified = lastModified,
        isPublic = isPublic,
        isFavourite = isFavourite,
        description = description,
        tags = tagsList
    )
}

// Fetch predefined tags from server
suspend fun fetchPredefinedTags(serverUrl: String, token: String): List<TagItem> = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/tags")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            return@withContext emptyList()
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val arr = JSONArray(response.toString())
        val list = mutableListOf<TagItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(TagItem(obj.getString("name"), obj.getString("color")))
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    } finally {
        conn.disconnect()
    }
}

// Update file metadata (public sharing, favorites, tags)
suspend fun updateFileMeta(
    serverUrl: String,
    token: String,
    filePath: String,
    isPublic: Boolean? = null,
    isFavourite: Boolean? = null,
    tags: List<TagItem>? = null,
    isWallpaper: Boolean? = null,
    previewImage: String? = null,
    description: String? = null,
    clearPreviewImage: Boolean = false
): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/file-meta")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("fullPath", filePath)
            if (isPublic != null) put("isPublic", isPublic)
            if (isFavourite != null) put("isFavourite", isFavourite)
            if (isWallpaper != null) put("isWallpaper", isWallpaper)
            if (description != null) put("description", description)
            if (clearPreviewImage) {
                put("previewImage", JSONObject.NULL)
            } else if (previewImage != null) {
                put("previewImage", previewImage)
            }
            if (tags != null) {
                val tagsArr = JSONArray()
                tags.forEach { tag ->
                    tagsArr.put(JSONObject().apply {
                        put("name", tag.name)
                        put("color", tag.color)
                    })
                }
                put("tags", tagsArr)
            }
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

// Create new folder in cloud
suspend fun createFolder(serverUrl: String, token: String, folderName: String, path: String): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/folder")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("name", folderName)
            put("path", path)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

// Delete file or folder from cloud
suspend fun deleteFileOrFolder(serverUrl: String, token: String, path: String): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/files")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "DELETE"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("path", path)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

// Download file directly to local Downloads folder using Android 10+ MediaStore API
suspend fun downloadFileToDevice(
    context: Context,
    serverUrl: String,
    token: String,
    filePath: String,
    fileName: String
): Boolean {
    val encodedSegments = filePath.split('/').map { Uri.encode(it) }
    val cleanPath = encodedSegments.joinToString("/")
    val downloadUrl = "$serverUrl/file/$cleanPath"
    return downloadUrlToDevice(context, token, downloadUrl, fileName)
}

// Upload file to server using multipart/form-data POST
suspend fun uploadFileToServer(
    context: Context,
    serverUrl: String,
    token: String,
    targetPath: String,
    fileUri: Uri,
    fileName: String,
    fileIndex: Int,
    totalFiles: Int,
    onProgress: (String, Int, Int, Int) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver

    // Get size
    var fileSize = 0L
    resolver.query(fileUri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex != -1) {
                fileSize = cursor.getLong(sizeIndex)
            }
        }
    }

    val url = URL("$serverUrl/api/upload")
    val conn = url.openConnection() as HttpURLConnection
    val boundary = "Boundary-${System.currentTimeMillis()}"
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.connectTimeout = 15000

    try {
        val outputStream = conn.outputStream
        val writer = OutputStreamWriter(outputStream, "UTF-8")

        // Write path field
        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"path\"\r\n\r\n")
        writer.write(targetPath)
        writer.write("\r\n")
        writer.flush()

        // Write files header
        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"files\"; filename=\"$fileName\"\r\n")
        val mimeType = resolver.getType(fileUri) ?: "application/octet-stream"
        writer.write("Content-Type: $mimeType\r\n\r\n")
        writer.flush()

        // Write file bytes with progress updates
        resolver.openInputStream(fileUri)?.use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesSent = 0L
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesSent += bytesRead
                if (fileSize > 0) {
                    val percent = ((totalBytesSent * 100) / fileSize).toInt()
                    onProgress(fileName, fileIndex, totalFiles, percent)
                }
            }
            outputStream.flush()
        }

        // End boundaries
        writer.write("\r\n--$boundary--\r\n")
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

// Trigger native Android Share Sheet
fun shareText(context: Context, text: String, title: String = "Share Link") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

// Fetch image bitmap helper
suspend fun fetchBitmapFromUrl(serverUrl: String, token: String, path: String, isThumbnail: Boolean): Bitmap? = withContext(Dispatchers.IO) {
    val endpoint = if (isThumbnail) "thumbnail" else "file"
    val encodedSegments = path.split('/').map { Uri.encode(it) }
    val cleanPath = encodedSegments.joinToString("/")
    val url = URL("$serverUrl/$endpoint/$cleanPath")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 15000
    conn.readTimeout = 20000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode == 200) {
            BitmapFactory.decodeStream(conn.inputStream)
        } else {
            Log.e("ClyoCloudAndroid", "HTTP error ${conn.responseCode} for: $url")
            null
        }
    } catch (e: Exception) {
        Log.e("ClyoCloudAndroid", "Error loading image: ${e.message}")
        null
    } finally {
        conn.disconnect()
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Custom icons that are missing in standard core library
val MoreVertIcon: ImageVector
    get() = ImageVector.Builder(
        name = "MoreVert",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 8f)
        curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f)
        curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
        curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f)
        curveTo(10f, 7.1f, 10.9f, 8f, 12f, 8f)
        close()
        moveTo(12f, 14f)
        curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
        curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
        curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
        curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
        close()
        moveTo(12f, 20f)
        curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f)
        curveTo(14f, 16.9f, 13.1f, 16f, 12f, 16f)
        curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f)
        curveTo(10f, 19.1f, 10.9f, 20f, 12f, 20f)
        close()
    }.build()

val CheckIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Check",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(9f, 16.17f)
        lineTo(4.83f, 12f)
        lineTo(3.41f, 13.41f)
        lineTo(9f, 19f)
        lineTo(21f, 7f)
        lineTo(19.59f, 5.59f)
        close()
    }.build()

val SortIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Sort",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(3f, 18f)
        lineTo(9f, 18f)
        lineTo(9f, 16f)
        lineTo(3f, 16f)
        close()
        moveTo(3f, 6f)
        lineTo(3f, 8f)
        lineTo(21f, 8f)
        lineTo(21f, 6f)
        close()
        moveTo(3f, 13f)
        lineTo(15f, 13f)
        lineTo(15f, 11f)
        lineTo(3f, 11f)
        close()
    }.build()

// New API helper functions for ClyoCloud Android

suspend fun renameFileOrFolder(
    serverUrl: String,
    token: String,
    oldPath: String,
    newName: String
): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/rename")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("oldPath", oldPath)
            put("newName", newName)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

suspend fun moveItems(
    serverUrl: String,
    token: String,
    paths: List<String>,
    destination: String
): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/move")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            val arr = JSONArray()
            paths.forEach { arr.put(it) }
            put("paths", arr)
            put("destination", destination)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

suspend fun fetchUserSettings(serverUrl: String, token: String): JSONObject? = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/user-settings")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            return@withContext null
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        JSONObject(response.toString())
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        conn.disconnect()
    }
}

suspend fun saveUserSettings(serverUrl: String, token: String, settings: JSONObject): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/user-settings")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(settings.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

suspend fun savePredefinedTags(serverUrl: String, token: String, tags: List<TagItem>): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/tags")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            val arr = JSONArray()
            tags.forEach { tag ->
                arr.put(JSONObject().apply {
                    put("name", tag.name)
                    put("color", tag.color)
                })
            }
            put("tags", arr)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

suspend fun downloadUrlToDevice(
    context: Context,
    token: String,
    downloadUrl: String,
    fileName: String
): Boolean = withContext(Dispatchers.IO) {
    val url = URL(downloadUrl)
    var conn: HttpURLConnection? = null
    var inputStream: java.io.InputStream? = null
    try {
        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.setRequestProperty("Authorization", "Bearer $token")

        if (conn.responseCode != 200) {
            return@withContext false
        }

        inputStream = conn.inputStream
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        inputStream?.close()
        conn?.disconnect()
    }
}

// Date formatter helper
fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

// --- Shorts Video Player Component ---
@Composable
fun ShortsVideoPlayer(
    videoUrl: String,
    token: String,
    isMuted: Boolean,
    loop: Boolean,
    isActive: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(loop) {
        exoPlayer.repeatMode = if (loop) {
            androidx.media3.common.Player.REPEAT_MODE_ALL
        } else {
            androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(isActive) {
        exoPlayer.playWhenReady = isActive
    }

    LaunchedEffect(videoUrl, token) {
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("ClyoCloudApp")
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
        val mediaSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(androidx.media3.common.MediaItem.fromUri(videoUrl))
        
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    onEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier
    )
}

// --- Shorts Slideshow Player Component ---
@Composable
fun ShortsSlideshowPlayer(
    serverUrl: String,
    token: String,
    images: List<String>,
    isActive: Boolean,
    autoscroll: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return
    val context = LocalContext.current
    var currentIndex by remember(images) { mutableStateOf(0) }
    
    LaunchedEffect(isActive, autoscroll, currentIndex) {
        if (isActive) {
            kotlinx.coroutines.delay(5000)
            if (currentIndex < images.size - 1) {
                currentIndex++
            } else {
                if (autoscroll) {
                    onEnded()
                } else {
                    currentIndex = 0
                }
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(images) {
                detectTapGestures(
                    onTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 3f) {
                            if (currentIndex > 0) currentIndex-- else currentIndex = images.size - 1
                        } else {
                            if (currentIndex < images.size - 1) currentIndex++ else currentIndex = 0
                        }
                    }
                )
            }
    ) {
        AsyncServerImage(
            serverUrl = serverUrl,
            token = token,
            relativePath = images[currentIndex],
            isThumbnail = false,
            contentDescription = "Slideshow image",
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 180.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            images.forEachIndexed { index, _ ->
                val selected = index == currentIndex
                Box(
                    modifier = Modifier
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

// --- Shorts Tab Content Composable ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsTabContent(
    serverUrl: String,
    token: String,
    username: String,
    shortsList: List<MediaItem>,
    isLoading: Boolean,
    predefinedTags: List<TagItem>,
    onPredefinedTagsChanged: (List<TagItem>) -> Unit,
    onRefreshFeed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMuted by remember { mutableStateOf(true) }
    var autoscroll by remember { mutableStateOf(true) }
    var showTagsDialogForMedia by remember { mutableStateOf<MediaItem?>(null) }
    val theme = LocalClyoTheme.current

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent1)
        }
        return
    }

    if (shortsList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("No items found in your Shorts feed.", color = Color(0xFFA0A0C0))
                Button(
                    onClick = onRefreshFeed,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent1)
                ) {
                    Text("Refresh", color = Color(0xFF0F0C1B))
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { shortsList.size })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val media = shortsList[pageIndex]
            val isActive = pagerState.currentPage == pageIndex

            Box(modifier = Modifier.fillMaxSize()) {
                if (media.type == "video") {
                    val streamUrl = "$serverUrl/stream/${Uri.encode(media.fullPath)}"
                    ShortsVideoPlayer(
                        videoUrl = streamUrl,
                        token = token,
                        isMuted = isMuted,
                        loop = !autoscroll,
                        isActive = isActive,
                        onEnded = {
                            if (autoscroll && pageIndex < shortsList.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pageIndex + 1)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (media.type == "slideshow") {
                    ShortsSlideshowPlayer(
                        serverUrl = serverUrl,
                        token = token,
                        images = media.images,
                        isActive = isActive,
                        autoscroll = autoscroll,
                        onEnded = {
                            if (autoscroll && pageIndex < shortsList.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pageIndex + 1)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Instagram Reels layout - Right Action Sidebar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Like/Favorite
                    var localFav by remember(media.fullPath) { mutableStateOf(media.isFavourite) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .combinedClickable(
                                    onClick = {
                                        localFav = !localFav
                                        scope.launch {
                                            updateFileMeta(serverUrl, token, media.fullPath, isFavourite = localFav)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = StarIcon,
                                contentDescription = "Favorite",
                                tint = if (localFav) Color(0xFFFFD700) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (localFav) "Liked" else "Like",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sound Mute Toggle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .combinedClickable(onClick = { isMuted = !isMuted }),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMuted) VolumeOffIcon else VolumeUpIcon,
                                contentDescription = "Mute Toggle",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMuted) "Muted" else "Sound",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // AutoScroll / Loop mode
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .combinedClickable(onClick = { autoscroll = !autoscroll }),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (autoscroll) AutoplayIcon else PlayIcon,
                                contentDescription = "Autoscroll Toggle",
                                tint = if (autoscroll) theme.accent1 else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (autoscroll) "Auto" else "Loop",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Exclude/Remove Folder
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .combinedClickable(
                                    onClick = {
                                        val folderPath = if (media.type == "slideshow") {
                                            media.fullPath
                                        } else {
                                            val parts = media.fullPath.split('/')
                                            if (parts.size > 1) parts.dropLast(1).joinToString("/") else ""
                                        }
                                        if (folderPath.isNotEmpty()) {
                                            scope.launch {
                                                val settings = fetchUserSettings(serverUrl, token)
                                                if (settings != null) {
                                                    val selectedPaths = mutableListOf<String>()
                                                    val arr = settings.optJSONArray("selectedShortsPaths")
                                                    if (arr != null) {
                                                        for (k in 0 until arr.length()) {
                                                            selectedPaths.add(arr.getString(k))
                                                        }
                                                    }
                                                    if (selectedPaths.contains(folderPath)) {
                                                        selectedPaths.remove(folderPath)
                                                        val updatedArr = JSONArray()
                                                        selectedPaths.forEach { updatedArr.put(it) }
                                                        settings.put("selectedShortsPaths", updatedArr)
                                                        if (saveUserSettings(serverUrl, token, settings)) {
                                                            Toast.makeText(context, "Folder removed from Shorts feed", Toast.LENGTH_SHORT).show()
                                                            onRefreshFeed()
                                                        } else {
                                                            Toast.makeText(context, "Failed to update settings", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Folder not in Shorts paths", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Failed to fetch settings", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Cannot remove root folder", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = DeleteIcon,
                                contentDescription = "Exclude Folder",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Exclude",
                            color = Color(0xFFEF5350),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Refresh Feed
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .combinedClickable(onClick = onRefreshFeed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = RefreshIcon,
                                contentDescription = "Refresh Feed",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Refresh",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Instagram Reels layout - Bottom-Left Info Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.7f)
                        .padding(start = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Profile/User row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Circular avatar with gradient
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(theme.accent1, theme.accent2)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username.take(1).uppercase(),
                                color = Color(0xFF0F0C1B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "@$username",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Filename
                    Text(
                        text = media.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Description (if any)
                    if (!media.description.isNullOrBlank()) {
                        Text(
                            text = media.description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Tags row
                    val currentMediaTags = media.tags
                    if (currentMediaTags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentMediaTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = tag.name,
                                        color = Color(android.graphics.Color.parseColor(tag.color)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Manage Tags capsule button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .combinedClickable(onClick = { showTagsDialogForMedia = media })
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(FileIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("Manage Tags", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showTagsDialogForMedia != null) {
        val mediaItem = showTagsDialogForMedia!!
        val currentTagsList = remember { mutableStateListOf<TagItem>().apply { addAll(mediaItem.tags) } }
        var newTagName by remember { mutableStateOf("") }
        val tagColors = listOf("#4B5EAA", "#BB86FC", "#03DAC6", "#FFB347", "#FF6B6B", "#6BCB77", "#FFD93D")
        var selectedTagColor by remember { mutableStateOf(tagColors.first()) }

        Dialog(onDismissRequest = { showTagsDialogForMedia = null }) {
            GlassCard(modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Manage Tags", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    
                    Text("Selected Tags", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTagsList.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .combinedClickable(onClick = { currentTagsList.remove(tag) })
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(tag.name, color = Color(android.graphics.Color.parseColor(tag.color)), fontSize = 11.sp)
                                    Icon(CloseIcon, contentDescription = "Remove", tint = Color(android.graphics.Color.parseColor(tag.color)), modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Text("Add Existing Tags", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        predefinedTags.filter { pt -> !currentTagsList.any { it.name == pt.name } }.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .combinedClickable(onClick = { currentTagsList.add(tag) })
                            ) {
                                Text(tag.name, color = Color(android.graphics.Color.parseColor(tag.color)), fontSize = 11.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Text("Create New Tag", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                    GlassTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = "Tag name",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tagColors.forEach { colorStr ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorStr)))
                                    .border(
                                        width = if (selectedTagColor == colorStr) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .combinedClickable(onClick = { selectedTagColor = colorStr })
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                val cleanTag = TagItem(newTagName.trim(), selectedTagColor)
                                if (!predefinedTags.any { it.name == cleanTag.name }) {
                                    val updatedPredefined = predefinedTags.toMutableList().apply { add(cleanTag) }
                                    onPredefinedTagsChanged(updatedPredefined)
                                    scope.launch {
                                        savePredefinedTags(serverUrl, token, updatedPredefined)
                                    }
                                }
                                if (!currentTagsList.any { it.name == cleanTag.name }) {
                                    currentTagsList.add(cleanTag)
                                }
                                newTagName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Text("Create & Add Tag", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showTagsDialogForMedia = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val savedList = currentTagsList.toList()
                                scope.launch {
                                    if (updateFileMeta(serverUrl, token, mediaItem.fullPath, tags = savedList)) {
                                        showTagsDialogForMedia = null
                                        onRefreshFeed()
                                    } else {
                                        Toast.makeText(context, "Failed to save tags", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                        ) {
                            Text("Apply", color = Color(0xFF0F0C1B))
                        }
                    }
                }
            }
        }
    }
}

// --- Media Details View Overlay Component ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaDetailsView(
    serverUrl: String,
    token: String,
    username: String,
    media: MediaItem,
    allVideos: List<MediaItem>,
    predefinedTags: List<TagItem>,
    onPredefinedTagsChanged: (List<TagItem>) -> Unit,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem, Boolean) -> Unit,
    onTogglePublic: (MediaItem, Boolean) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onExcludeParentFolder: (MediaItem) -> Unit,
    onVideoSelected: (MediaItem) -> Unit,
    onSaveDescription: (MediaItem, String) -> Unit,
    onUpdateTags: (MediaItem, List<TagItem>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isEditingDescription by remember(media.fullPath) { mutableStateOf(false) }
    var descriptionText by remember(media.fullPath) { mutableStateOf(media.description ?: "") }
    var showAddTagDialog by remember { mutableStateOf(false) }

    val relatedVideos = remember(media, allVideos) {
        val currentTags = media.tags.map { it.name.lowercase() }.toSet()
        allVideos
            .filter { it.fullPath != media.fullPath && it.type == "video" }
            .map { video ->
                val matchCount = video.tags.count { currentTags.contains(it.name.lowercase()) }
                Pair(video, matchCount)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0C1B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(CloseIcon, contentDescription = "Close", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = media.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    val streamUrl = "$serverUrl/stream/${Uri.encode(media.fullPath)}"
                    VideoPlayer(
                        videoUrl = streamUrl,
                        token = token,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Size:", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                        Text(formatFileSize(media.size), color = Color.White, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date Added:", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                        Text(formatDate(media.lastModified), color = Color.White, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Type:", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                        Text(media.type.replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.combinedClickable(onClick = { onToggleFavorite(media, !media.isFavourite) })
                    ) {
                        Icon(
                            imageVector = StarIcon,
                            contentDescription = "Favorite",
                            tint = if (media.isFavourite) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Favorite", color = Color.White, fontSize = 11.sp)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.combinedClickable(onClick = { onShare(media) })
                    ) {
                        Icon(ShareIcon, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Share Link", color = Color.White, fontSize = 11.sp)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.combinedClickable(onClick = { onDownload(media) })
                    ) {
                        Icon(DownloadIcon, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Download", color = Color.White, fontSize = 11.sp)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.combinedClickable(onClick = { onExcludeParentFolder(media) })
                    ) {
                        Icon(
                            imageVector = WarningIcon,
                            contentDescription = "Exclude Folder",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Exclude Folder", color = Color(0xFFEF5350), fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Description", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (!isEditingDescription) {
                            Text(
                                text = "Edit",
                                color = LocalClyoTheme.current.accent1,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.combinedClickable(onClick = { isEditingDescription = true })
                            )
                        }
                    }

                    if (isEditingDescription) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassTextField(
                                value = descriptionText,
                                onValueChange = { descriptionText = it },
                                label = "Edit description...",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    descriptionText = media.description ?: ""
                                    isEditingDescription = false
                                }) {
                                    Text("Cancel", color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onSaveDescription(media, descriptionText.trim())
                                        isEditingDescription = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                                ) {
                                    Text("Save", color = Color(0xFF0F0C1B))
                                }
                            }
                        }
                    } else {
                        Text(
                            text = media.description?.takeIf { it.isNotBlank() } ?: "No description added yet.",
                            color = if (media.description.isNullOrBlank()) Color(0xFFA0A0C0) else Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tags", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "+ Add Tag",
                            color = LocalClyoTheme.current.accent1,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.combinedClickable(onClick = { showAddTagDialog = true })
                        )
                    }

                    if (media.tags.isEmpty()) {
                        Text("No tags added.", color = Color(0xFFA0A0C0), fontSize = 13.sp)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            media.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .combinedClickable(onClick = {
                                            val listWithoutTag = media.tags.filter { it.name != tag.name }
                                            onUpdateTags(media, listWithoutTag)
                                        })
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = tag.name,
                                            color = Color(android.graphics.Color.parseColor(tag.color)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = CloseIcon,
                                            contentDescription = "Remove",
                                            tint = Color(android.graphics.Color.parseColor(tag.color)),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Related Media", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (relatedVideos.isEmpty()) {
                        Text("No related media found based on tags.", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(relatedVideos) { video ->
                                GlassCard(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(160.dp)
                                        .combinedClickable(onClick = { onVideoSelected(video) })
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .background(Color.Black)
                                        ) {
                                            AsyncServerImage(
                                                serverUrl = serverUrl,
                                                token = token,
                                                relativePath = video.fullPath,
                                                isThumbnail = true,
                                                contentDescription = video.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Text(
                                            text = video.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddTagDialog) {
        var newTagName by remember { mutableStateOf("") }
        val tagColors = listOf("#4B5EAA", "#BB86FC", "#03DAC6", "#FFB347", "#FF6B6B", "#6BCB77", "#FFD93D")
        var selectedTagColor by remember { mutableStateOf(tagColors.first()) }

        Dialog(onDismissRequest = { showAddTagDialog = false }) {
            GlassCard(modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Tags", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    Text("Select Existing Tag", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        predefinedTags.filter { pt -> !media.tags.any { it.name == pt.name } }.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .combinedClickable(onClick = {
                                        val listWithTag = media.tags.toMutableList().apply { add(tag) }
                                        onUpdateTags(media, listWithTag)
                                        showAddTagDialog = false
                                    })
                            ) {
                                Text(tag.name, color = Color(android.graphics.Color.parseColor(tag.color)), fontSize = 11.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Text("Create New Tag", color = Color(0xFFA0A0C0), fontSize = 12.sp)
                    GlassTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = "Tag name",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tagColors.forEach { colorStr ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorStr)))
                                    .border(
                                        width = if (selectedTagColor == colorStr) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .combinedClickable(onClick = { selectedTagColor = colorStr })
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                val cleanTag = TagItem(newTagName.trim(), selectedTagColor)
                                if (!predefinedTags.any { it.name == cleanTag.name }) {
                                    val updatedPredefined = predefinedTags.toMutableList().apply { add(cleanTag) }
                                    onPredefinedTagsChanged(updatedPredefined)
                                    scope.launch {
                                        savePredefinedTags(serverUrl, token, updatedPredefined)
                                    }
                                }
                                if (!media.tags.any { it.name == cleanTag.name }) {
                                    val listWithTag = media.tags.toMutableList().apply { add(cleanTag) }
                                    onUpdateTags(media, listWithTag)
                                }
                                showAddTagDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalClyoTheme.current.accent1)
                    ) {
                        Text("Create & Add Tag", color = Color(0xFF0F0C1B))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showAddTagDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
