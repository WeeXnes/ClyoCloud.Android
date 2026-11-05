package dev.weexnes.clyocloud.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoType

val RadiusCard = RoundedCornerShape(12.dp)
val RadiusInput = RoundedCornerShape(10.dp)
val RadiusTag = RoundedCornerShape(6.dp)
val Pill = RoundedCornerShape(percent = 50)

/** 120ms colour transitions only (DESIGN.md "Motion"). */
private fun <T> quick() = tween<T>(120)

// ─── Type helpers ────────────────────────────────────────────────────────────

@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier, color: Color = Clyo.colors.text3) {
    Text(text.uppercase(), style = ClyoType.micro, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ClyoType.pageTitle, color = Clyo.colors.text, modifier = modifier)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ClyoType.sectionTitle, color = Clyo.colors.text, modifier = modifier)
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, color: Color = Clyo.colors.text2, maxLines: Int = Int.MAX_VALUE) {
    Text(text, style = ClyoType.body, color = color, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun SmallText(text: String, modifier: Modifier = Modifier, color: Color = Clyo.colors.text3, maxLines: Int = Int.MAX_VALUE) {
    Text(text, style = ClyoType.small, color = color, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun NumText(text: String, modifier: Modifier = Modifier, color: Color = Clyo.colors.text2, style: TextStyle = ClyoType.num) {
    Text(text, style = style, color = color, modifier = modifier, maxLines = 1)
}

// ─── Buttons ─────────────────────────────────────────────────────────────────

enum class ButtonKind { Primary, Secondary, Danger, Ghost }

/**
 * Pill button. Primary: white fill, black text (at most one per view). Secondary: hairline border.
 * Danger: like secondary with red text, fills red only while pressed. Ghost: no border.
 */
@Composable
fun ClyoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Secondary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    small: Boolean = false,
) {
    val c = Clyo.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading

    val targetBg = when {
        !active && kind == ButtonKind.Primary -> c.surface2
        !active -> Color.Transparent
        kind == ButtonKind.Primary -> if (pressed) c.invert.copy(alpha = 0.86f) else c.invert
        kind == ButtonKind.Danger -> if (pressed) c.danger else Color.Transparent
        else -> if (pressed) c.surface2 else Color.Transparent
    }
    val targetFg = when {
        !active -> c.text3
        kind == ButtonKind.Primary -> c.onInvert
        kind == ButtonKind.Danger -> if (pressed) c.onInvert else c.danger
        else -> c.text
    }
    val border = when {
        kind == ButtonKind.Ghost -> Color.Transparent
        !active -> c.line
        kind == ButtonKind.Primary -> c.invert
        else -> c.lineStrong
    }
    val bg by animateColorAsState(targetBg, quick(), label = "btnBg")
    val fg by animateColorAsState(targetFg, quick(), label = "btnFg")

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = if (small) 32.dp else 40.dp)
            .clip(Pill)
            .background(bg)
            .border(1.dp, border, Pill)
            .clickable(interactionSource = interaction, indication = null, enabled = active, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = fg)
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = ClyoType.button, color = fg, maxLines = 1)
    }
}

/** Round icon-only control: transparent, surface-2 while pressed. */
@Composable
fun ClyoIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Clyo.colors.text2,
    enabled: Boolean = true,
    size: Dp = 40.dp,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(size)) {
        Icon(icon, contentDescription = contentDescription, tint = if (enabled) tint else Clyo.colors.text3.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

// ─── Structure ───────────────────────────────────────────────────────────────

/** 60dp sticky header on the page background with one hairline below. */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Clyo.colors
    Column(modifier.fillMaxWidth().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(start = if (onBack != null) 4.dp else 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                ClyoIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack, tint = c.text)
                Spacer(Modifier.width(4.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = ClyoType.title.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) SmallText(subtitle, maxLines = 1)
            }
            actions()
        }
        Hairline()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Clyo.colors.line) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = color)
}

/** One hairline-bordered container; put rows inside and separate them with [Hairline]. */
@Composable
fun HairList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RadiusCard)
            .border(1.dp, Clyo.colors.line, RadiusCard),
        content = content,
    )
}

/** A row inside a [HairList]: title, optional hint, trailing content; surface-2 while pressed. */
@Composable
fun HairRow(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    titleColor: Color = Clyo.colors.text,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed && onClick != null) Clyo.colors.surface2 else Color.Transparent, quick(), label = "rowBg")
    Row(
        modifier
            .fillMaxWidth()
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = ClyoType.title, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hint != null) SmallText(hint, maxLines = 2)
        }
        trailing()
    }
}

/** Section eyebrow (micro label) with an optional action on the right. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        MicroLabel(text, Modifier.weight(1f))
        action?.invoke()
    }
}

/** Big number above a small label, no box (hero stats). */
@Composable
fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = ClyoType.hero, color = Clyo.colors.text, maxLines = 1)
        MicroLabel(label)
    }
}

// ─── Small elements ──────────────────────────────────────────────────────────

/** 6dp status or colour dot; always shown next to text. */
@Composable
fun Dot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(6.dp).clip(CircleShape).background(color))
}

/** Tag: colour dot next to the name inside a 6dp-radius hairline box, never a filled pill. */
@Composable
fun TagChip(name: String, colorHex: String?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val c = Clyo.colors
    Row(
        modifier
            .clip(RadiusTag)
            .border(1.dp, c.lineStrong, RadiusTag)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(parseHexColor(colorHex) ?: c.text3)
        Spacer(Modifier.width(6.dp))
        Text(name, style = ClyoType.small, color = c.text2, maxLines = 1)
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

/** Status: dot plus text, never colour alone. */
@Composable
fun StatusLine(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Spacer(Modifier.width(8.dp))
        SmallText(text, color = Clyo.colors.text2)
    }
}

fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ClyoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val c = Clyo.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onInvert,
            checkedTrackColor = c.invert,
            checkedBorderColor = c.invert,
            uncheckedThumbColor = c.text3,
            uncheckedTrackColor = c.bg,
            uncheckedBorderColor = c.lineStrong,
        ),
    )
}

@Composable
fun Spinner(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    CircularProgressIndicator(modifier = modifier.size(size), strokeWidth = 1.5.dp, color = Clyo.colors.text2, trackColor = Clyo.colors.line)
}

/** Thin progress bar: line track, white fill. */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val c = Clyo.colors
    Box(modifier.fillMaxWidth().height(4.dp).clip(Pill).background(c.surface2)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).clip(Pill).background(c.invert))
    }
}

/** One quiet line and, where useful, one action. */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BodyText(text, color = Clyo.colors.text3)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            ClyoButton(actionText, onAction, small = true)
        }
    }
}

/** Pill tabs in a hairline track; the active one sits on surface-2. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Clyo.colors
    Row(modifier.clip(Pill).border(1.dp, c.line, Pill).padding(3.dp)) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            val bg by animateColorAsState(if (active) c.surface2 else Color.Transparent, quick(), label = "seg")
            Box(
                Modifier
                    .clip(Pill)
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = ClyoType.button, color = if (active) c.text else c.text2, maxLines = 1)
            }
        }
    }
}

// ─── Inputs ──────────────────────────────────────────────────────────────────

/** Label above, surface background, line-strong border that turns text-coloured on focus. */
@Composable
fun ClyoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    mono: Boolean = false,
) {
    val c = Clyo.colors
    Column(modifier) {
        if (label != null) {
            MicroLabel(label, Modifier.padding(bottom = 6.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = (if (mono) ClyoType.num else ClyoType.title).copy(color = c.text),
            placeholder = placeholder?.let { { Text(it, style = ClyoType.title, color = c.text3) } },
            leadingIcon = leadingIcon?.let { { Icon(it, null, tint = c.text3, modifier = Modifier.size(18.dp)) } },
            trailingIcon = trailing,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            shape = RadiusInput,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                disabledContainerColor = c.surface,
                focusedBorderColor = c.text,
                unfocusedBorderColor = c.lineStrong,
                disabledBorderColor = c.line,
                cursorColor = c.text,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
                disabledTextColor = c.text3,
            ),
        )
    }
}

// ─── Overlays ────────────────────────────────────────────────────────────────

/** Dialog on the page background with a hairline border; one primary action at most. */
@Composable
fun ClyoDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmKind: ButtonKind = ButtonKind.Primary,
    confirmEnabled: Boolean = true,
    confirmLoading: Boolean = false,
    dismissText: String = "Cancel",
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val c = Clyo.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bg,
        tonalElevation = 0.dp,
        shape = RadiusCard,
        modifier = Modifier.border(1.dp, c.line, RadiusCard),
        title = { Text(title, style = ClyoType.sectionTitle, color = c.text) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) },
        confirmButton = {
            if (confirmText != null && onConfirm != null) {
                ClyoButton(confirmText, onConfirm, kind = confirmKind, enabled = confirmEnabled, loading = confirmLoading)
            }
        },
        dismissButton = { ClyoButton(dismissText, onDismiss, kind = ButtonKind.Ghost) },
    )
}

/** Bottom sheet with the page background and a hairline handle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClyoSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = Clyo.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bg,
        contentColor = c.text,
        tonalElevation = 0.dp,
        scrimColor = c.scrim,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = { Box(Modifier.padding(vertical = 10.dp).size(width = 36.dp, height = 4.dp).clip(Pill).background(c.lineStrong)) },
        modifier = Modifier.border(BorderStroke(1.dp, c.line), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), content = content)
    }
}

/** A plain action row for sheets and menus: icon, label, optional danger colour. */
@Composable
fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit, danger: Boolean = false, hint: String? = null) {
    val c = Clyo.colors
    HairRow(
        title = label,
        hint = hint,
        titleColor = if (danger) c.danger else c.text,
        leading = { Icon(icon, null, tint = if (danger) c.danger else c.text2, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
    )
}

val ScreenPadding = PaddingValues(horizontal = 16.dp)
