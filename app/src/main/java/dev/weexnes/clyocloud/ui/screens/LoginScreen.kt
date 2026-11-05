package dev.weexnes.clyocloud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dev.weexnes.clyocloud.data.AuthApi
import dev.weexnes.clyocloud.data.CloudConnection
import dev.weexnes.clyocloud.data.LoginResult
import dev.weexnes.clyocloud.data.Session
import dev.weexnes.clyocloud.data.normalizeServerUrl
import dev.weexnes.clyocloud.ui.components.BodyText
import dev.weexnes.clyocloud.ui.components.ButtonKind
import dev.weexnes.clyocloud.ui.components.ClyoButton
import dev.weexnes.clyocloud.ui.components.ClyoDialog
import dev.weexnes.clyocloud.ui.components.ClyoTextField
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.PageTitle
import dev.weexnes.clyocloud.ui.components.SmallText
import dev.weexnes.clyocloud.ui.components.StatusLine
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoType
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LoginScreen() {
    val c = Clyo.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tempToken by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var scanned by remember { mutableStateOf<CloudConnection?>(null) }

    fun signIn() {
        val url = normalizeServerUrl(server)
        if (url.isEmpty() || username.isBlank() || password.isEmpty()) {
            error = "Enter the server address, username and password"
            return
        }
        loading = true
        error = null
        scope.launch {
            when (val r = AuthApi.login(url, username.trim(), password)) {
                is LoginResult.Success -> Session.save(context, CloudConnection(url, username.trim(), r.token))
                is LoginResult.Requires2Fa -> tempToken = r.tempToken
                is LoginResult.Error -> error = r.message
            }
            loading = false
        }
    }

    fun verifyCode() {
        val t = tempToken ?: return
        loading = true
        error = null
        scope.launch {
            val url = normalizeServerUrl(server)
            val token = AuthApi.verify2fa(url, t, code.trim())
            if (token != null) Session.save(context, CloudConnection(url, username.trim(), token))
            else error = "That code didn't work. Check your authenticator and try again."
            loading = false
        }
    }

    fun scanQr() {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                try {
                    val json = JSONObject(barcode.rawValue ?: "")
                    scanned = CloudConnection(normalizeServerUrl(json.getString("serverUrl")), json.getString("username"), json.getString("token"))
                } catch (_: Exception) {
                    error = "That QR code isn't a ClyoCloud login code. Open Settings → Mobile app on the website."
                }
            }
            .addOnFailureListener { error = "Scanning failed: ${it.localizedMessage}" }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ClyoCloud", style = ClyoType.title.copy(fontWeight = FontWeight.SemiBold), color = c.text)
            }
            Spacer(Modifier.height(48.dp))

            if (tempToken == null) {
                PageTitle("Sign in")
                Spacer(Modifier.height(8.dp))
                BodyText("Connect to your ClyoCloud server.")
                Spacer(Modifier.height(32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ClyoTextField(server, { server = it }, label = "Server", placeholder = "https://cloud.example.com", keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, mono = true)
                    ClyoTextField(username, { username = it }, label = "Username", imeAction = ImeAction.Next)
                    ClyoTextField(password, { password = it }, label = "Password", password = true, onImeAction = ::signIn)
                }
                error?.let {
                    Spacer(Modifier.height(16.dp))
                    StatusLine(it, c.danger)
                }
                Spacer(Modifier.height(24.dp))
                ClyoButton("Sign in", ::signIn, Modifier.fillMaxWidth(), kind = ButtonKind.Primary, loading = loading)

                Spacer(Modifier.height(32.dp))
                Hairline()
                Spacer(Modifier.height(24.dp))
                SmallText("Signed in on the website? Show the login QR code under Settings and scan it.")
                Spacer(Modifier.height(12.dp))
                ClyoButton("Scan QR code", ::scanQr, Modifier.fillMaxWidth(), icon = Icons.Outlined.QrCodeScanner, enabled = !loading)
            } else {
                PageTitle("Two-factor code")
                Spacer(Modifier.height(8.dp))
                BodyText("Enter the 6-digit code from your authenticator app.")
                Spacer(Modifier.height(32.dp))
                ClyoTextField(code, { code = it.filter(Char::isDigit).take(8) }, label = "Code", keyboardType = KeyboardType.NumberPassword, onImeAction = ::verifyCode, mono = true)
                error?.let {
                    Spacer(Modifier.height(16.dp))
                    StatusLine(it, c.danger)
                }
                Spacer(Modifier.height(24.dp))
                ClyoButton("Verify", ::verifyCode, Modifier.fillMaxWidth(), kind = ButtonKind.Primary, loading = loading, enabled = code.length >= 6)
                Spacer(Modifier.height(12.dp))
                ClyoButton("Back", { tempToken = null; code = ""; error = null }, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost)
            }
        }
    }

    scanned?.let { conn ->
        var checking by remember { mutableStateOf(false) }
        ClyoDialog(
            title = "Sign in as ${conn.username}?",
            onDismiss = { scanned = null },
            confirmText = "Sign in",
            confirmLoading = checking,
            onConfirm = {
                checking = true
                scope.launch {
                    when (AuthApi.verifyToken(conn.serverUrl, conn.token)) {
                        true -> Session.save(context, conn)
                        false -> error = "This login code has been revoked or expired. Create a new one on the website."
                        null -> error = "Could not reach ${conn.serverUrl}"
                    }
                    checking = false
                    scanned = null
                }
            },
        ) {
            SmallText("Server", color = c.text3)
            Text(conn.serverUrl, style = ClyoType.num, color = c.text)
        }
    }
}
