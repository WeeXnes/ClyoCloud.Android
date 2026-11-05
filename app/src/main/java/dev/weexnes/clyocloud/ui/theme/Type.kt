package dev.weexnes.clyocloud.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.weexnes.clyocloud.R

// Geist and Geist Mono are variable fonts; each weight is an instance of the same file.
@OptIn(ExperimentalTextApi::class)
private fun geist(weight: FontWeight) = Font(
    R.font.geist, weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun geistMono(weight: FontWeight) = Font(
    R.font.geist_mono, weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Geist = FontFamily(
    geist(FontWeight.Normal), geist(FontWeight.Medium), geist(FontWeight.SemiBold), geist(FontWeight.Bold),
)

val GeistMono = FontFamily(
    geistMono(FontWeight.Normal), geistMono(FontWeight.Medium), geistMono(FontWeight.SemiBold),
)

/** Text styles from DESIGN.md. Colours are applied by the components, not here. */
object ClyoType {
    /** Page title: large, weight 500, tight tracking */
    val pageTitle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.03).em)
    /** Section title: 20sp, weight 500 */
    val sectionTitle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em)
    /** Row / item title */
    val title = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    /** Body text, 15sp line height 1.6 */
    val body = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp)
    /** Secondary small text */
    val small = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    /** Button label */
    val button = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp)
    /** Micro label: mono, 11sp, uppercase, wide tracking (apply uppercase in the composable) */
    val micro = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.08.em)
    /** Numbers: mono with tabular figures */
    val num = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum")
    /** Hero stat number */
    val hero = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val centered = body.copy(textAlign = TextAlign.Center)
}

val ClyoTypography = Typography(
    displayLarge = ClyoType.pageTitle,
    displayMedium = ClyoType.pageTitle,
    displaySmall = ClyoType.pageTitle,
    headlineLarge = ClyoType.pageTitle,
    headlineMedium = ClyoType.sectionTitle,
    headlineSmall = ClyoType.sectionTitle,
    titleLarge = ClyoType.sectionTitle,
    titleMedium = ClyoType.title,
    titleSmall = ClyoType.button,
    bodyLarge = ClyoType.body,
    bodyMedium = ClyoType.small,
    bodySmall = ClyoType.small,
    labelLarge = ClyoType.button,
    labelMedium = ClyoType.small,
    labelSmall = ClyoType.micro,
)
