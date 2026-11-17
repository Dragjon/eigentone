package io.github.dragjon.eigentone.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.ThemeMode

/** Every colour role, so a theme can only be complete. */
data class ThemeColors(
    val background: Color,
    /** The top and bottom bars, a step darker than the page so they read as fixed. */
    val bar: Color,
    /** Behind the status strip, so it stands apart from both the bars and the page. */
    val strip: Color,
    /** Things that are off, on the strip's green. */
    val stripDim: Color,
    /** Plain text on the strip's green, which stays dark in every theme. */
    val stripText: Color,
    /** Things that are on, on the strip's green. */
    val stripOn: Color,
    /** A warning on the strip's green. */
    val stripDanger: Color,
    /** A softer green under the strip when it's opened, so the details read as a drawer pulled out of it. */
    val stripPanel: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val tooltip: Color,
    /** Hairlines between rows. Decorative, so they sit below control contrast on purpose. */
    val line: Color,
    /** Control boundaries such as off switches and segments. At least 3:1 against every surface. */
    val outline: Color,
    val track: Color,
    val text: Color,
    val textDim: Color,
    /** Lit text, readouts and lines, which have to be read on the page. */
    val lit: Color,
    /** What lit controls are filled with: keys, segments, caps, rails and switches. */
    val litFill: Color,
    /** A lit control while held. */
    val litPressed: Color,
    /** Legends on [litFill]. */
    val onLit: Color,
    /** Orange rather than red, so a problem can't be mistaken for the right ear. */
    val danger: Color,
    val leftEar: Color,
    /** Also dashed on graphs, so colour isn't the only difference between the ears. */
    val rightEar: Color,
    /** Text in the strip's opened drawer, made for its tone. Unspecified means the strip's own text colours suit it. */
    val drawerText: Color = Color.Unspecified,
    val drawerDim: Color = Color.Unspecified,
    /**
     * The second accent, opposite the main colour, kept for the few things that should stand out most: the EQ curve,
     * the power switch (user decision). Unspecified means the main colour.
     */
    val accent: Color = Color.Unspecified,
    val onAccent: Color = Color.Unspecified,
)

/**
 * Modern near-black (user decision): neutral greys, with no brown or blue cast, so the amber, the ear colours and the green
 * strip are the only colour on screen. Amber means one thing: lit or active, like a glowing meter.
 */
val DarkColors = ThemeColors(
    background = Color(0xFF0F0F0F),
    bar = Color(0xFF090909),
    strip = Color(0xFF174035),
    stripDim = Color(0xFFBFD3C9),
    stripText = Color(0xFFEEEEEA),
    stripOn = Color(0xFFFFC24A),
    stripDanger = Color(0xFFFF9A5C),
    stripPanel = Color(0xFF1F4B3F),
    surface = Color(0xFF191919),
    surfaceHigh = Color(0xFF252525),
    tooltip = Color(0xFF292929),
    line = Color(0xFF2D2D2D),
    outline = Color(0xFF909090),
    track = Color(0xFF767676),
    text = Color(0xFFF2F2F2),
    textDim = Color(0xFFADADAD),
    lit = Color(0xFFFFC24A),
    litFill = Color(0xFFFFC24A),
    litPressed = Color(0xFFE0A53A),
    onLit = Color(0xFF2A1C00),
    danger = Color(0xFFFF9A5C),
    leftEar = Color(0xFF62B2F2),
    rightEar = Color(0xFFF06B5C),
)

/**
 * The same instrument in daylight, made modern (user decision): a white page with soft neutral-grey panels, bars only
 * a breath off white, and charcoal rather than black text, with no warm paper tones. Lit controls are filled with the
 * same bright amber as at night, under dark legends. Amber that has to be read as text on white would need to be so
 * dark it turns brown, so lit text and lines are a vivid orange instead, and warnings move to a deeper red-orange to
 * stay apart from it. The strip is a light amber tint with dark legends, and the ear colours deepen so they read as
 * ink on a chart, as an audiogram does on paper.
 */
val LightColors = ThemeColors(
    background = Color(0xFFFFFFFF),
    bar = Color(0xFFF7F7F7),
    strip = Color(0xFFFFE7C7),
    stripDim = Color(0xFF6B5640),
    stripText = Color(0xFF1A1206),
    stripOn = Color(0xFF8A4200),
    stripDanger = Color(0xFF8A1A00),
    stripPanel = Color(0xFFFFF2E1),
    surface = Color(0xFFF4F4F4),
    surfaceHigh = Color(0xFFEAEAEA),
    tooltip = Color(0xFFFFFFFF),
    line = Color(0xFFE5E5E5),
    outline = Color(0xFF868686),
    track = Color(0xFF8A8A8A),
    text = Color(0xFF1F1F1F),
    textDim = Color(0xFF5F5F5F),
    lit = Color(0xFFB35300),
    litFill = Color(0xFFFFB21A),
    litPressed = Color(0xFFF2A100),
    onLit = Color(0xFF241800),
    danger = Color(0xFFA82A00),
    leftEar = Color(0xFF0A5FAD),
    rightEar = Color(0xFFC4231A),
)

/**
 * The colours in use. These are read everywhere, including while drawing graphs, and reading one here
 * means that a theme change redraws whatever used it.
 */
object Palette {
    internal var colors by mutableStateOf(DarkColors)

    /** Whether the theme in use is dark, whichever palette it came from. */
    internal var dark by mutableStateOf(true)

    /** Whether the quieter colours are drawn nearer the text, for the colours made outside the theme below. */
    internal var highContrast = false

    /**
     * The colours worn while a colour slider is dragged, ahead of the rest of the app: the strip's and the lit
     * controls'. Only what uses those reads these, so they can follow the finger, where recolouring the whole app
     * took about 40 ms on a Galaxy A54 and made the slider stutter (tester feedback, user decision).
     */
    internal var stripColors by mutableStateOf<ThemeColors?>(null)
    private val live get() = stripColors ?: colors

    val Background get() = colors.background
    val Bar get() = colors.bar
    val Strip get() = live.strip
    val StripDim get() = live.stripDim
    val StripText get() = live.stripText
    val StripOn get() = live.stripOn
    val StripDanger get() = live.stripDanger
    val StripPanel get() = live.stripPanel
    val DrawerText get() = live.let { it.drawerText.takeOrElse { it.stripText } }
    val DrawerOn get() = live.let { it.drawerText.takeOrElse { it.stripOn } }
    val DrawerDim get() = live.let { it.drawerDim.takeOrElse { it.stripDim } }
    val Surface get() = colors.surface
    val SurfaceHigh get() = colors.surfaceHigh
    val Tooltip get() = colors.tooltip
    val Line get() = colors.line
    val Outline get() = colors.outline
    val Track get() = colors.track
    val Text get() = colors.text
    val TextDim get() = colors.textDim
    val Lit get() = live.lit
    val LitFill get() = live.litFill

    /** The selected fader's cap: bright at night, and the lit orange by day rather than a black block. */
    val SelectedCap get() = if (dark) colors.text else live.lit

    /** Lit text that has to stand out on a dark page, such as the levels above the faders: lighter at night. */
    val LitBright get() = live.lit.let { if (dark) androidx.compose.ui.graphics.lerp(it, Color.White, 0.45f) else it }

    /**
     * The unfilled part of every selector, slider and fader track. At night a touch of the theme colour; by day a
     * plain grey, since the tinted container looked off on white (user decision). See-through rather than solid, so
     * the panel or page beneath shows through, as it does through the bars (user decision).
     */
    val Track2 get() = if (dark) {
        // Over a panel, about as light as the solid tint it replaced.
        live.litFill.copy(alpha = TRACK_TINT_DARK)
    } else {
        // Light enough to sit gently on the white page as well as on a panel.
        live.text.copy(alpha = TRACK_TINT_LIGHT)
    }
    private const val TRACK_TINT_DARK = 0.2f
    private const val TRACK_TINT_LIGHT = 0.06f

    /** Floating notes, such as unsaved changes and messages: a tint of the theme colour rather than grey (user decision). */
    val Note get() = androidx.compose.ui.graphics.lerp(live.surfaceHigh, live.litFill, if (dark) 0.22f else 0.14f)
    val NoteText get() = colors.text

    /** The edge of an unlit key or toggle: clear enough to see on any panel, quieter than a control's outline. */
    val KeyBorder get() = androidx.compose.ui.graphics.lerp(colors.line, colors.outline, 0.55f)
    val LitPressed get() = live.litPressed
    val OnLit get() = live.onLit

    /** The second accent and the colour of what's on it. See [ThemeColors.accent]. */
    val Accent get() = live.accent.takeOrElse { live.litFill }
    val OnAccent get() = live.onAccent.takeOrElse { live.onLit }
    val Danger get() = colors.danger
    val LeftEar get() = colors.leftEar
    val RightEar get() = colors.rightEar

    /**
     * The light a lit control gives off: amber on the dark faceplate. Nothing in daylight, where a lit
     * control is dark ink and couldn't glow.
     */
    // None in Material You, where lit things are flat (user decision).
    val Glow get() = Color.Transparent
}

/**
 * The same colours with quiet text, hairlines and tracks drawn nearer the main text, for anyone who finds
 * the dim greys hard to read.
 */
internal fun ThemeColors.contrasted(): ThemeColors {
    fun toward(from: Color, to: Color, amount: Float) = androidx.compose.ui.graphics.lerp(from, to, amount)
    return copy(
        textDim = toward(textDim, text, 0.5f),
        line = toward(line, outline, 0.6f),
        outline = toward(outline, text, 0.35f),
        track = toward(track, text, 0.3f),
        stripDim = toward(stripDim, stripText, 0.5f),
    )
}

/**
 * A colour being tried while a colour slider moves. The strip wears it at once, through [strip]; the whole app
 * wears it through [seed] once the finger pauses. It's saved as the setting once the finger lifts, and cleared.
 */
object ThemePreview {
    var seed by mutableStateOf<Long?>(null)

    /** Recolours the strip alone, or puts it back with the rest of the app when null. */
    fun strip(seed: Long?) {
        Palette.stripColors = seed?.let { themeColors(SeedScheme.scheme(it, Palette.dark), Palette.dark, Palette.highContrast) }
    }
}

/** Whether this choice means dark right now. Follows the device live, including a dark mode schedule. */
@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    // Follows the phone; MainActivity keeps Android's choice for the app on "follow the phone" whenever Device is set.
    ThemeMode.DEVICE -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private val Body = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

private val Engraved = FontFamily(
    Font(R.font.barlow_semicondensed_medium, FontWeight.Medium),
    Font(R.font.barlow_semicondensed_semibold, FontWeight.SemiBold),
)

val ReadoutFamily = FontFamily(Font(R.font.jetbrains_mono))

private val TypeBase = Typography(
    displaySmall = TextStyle(fontFamily = Engraved, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium),
    headlineMedium = TextStyle(fontFamily = Engraved, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontFamily = Engraved, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = Engraved, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 12.sp, lineHeight = 16.sp),
    // Centred in its line and trimmed, so a word sits in the middle of a pill button rather than low in it.
    labelLarge = TextStyle(
        fontFamily = Body,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
            alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
            trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
        ),
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    ),
    labelMedium = TextStyle(fontFamily = Engraved, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = Engraved, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

/**
 * Wrapped text breaks into lines of about even length, rather than leaving a word or two alone on the last line, such
 * as "back as it was / saved." (tester feedback). Android balances lines from version 13; before that it wraps as usual.
 */
private fun TextStyle.balanced() = copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading)

private val Type = TypeBase.copy(
    displaySmall = TypeBase.displaySmall.balanced(),
    headlineMedium = TypeBase.headlineMedium.balanced(),
    headlineSmall = TypeBase.headlineSmall.balanced(),
    titleLarge = TypeBase.titleLarge.balanced(),
    titleMedium = TypeBase.titleMedium.balanced(),
    titleSmall = TypeBase.titleSmall.balanced(),
    bodyLarge = TypeBase.bodyLarge.balanced(),
    bodyMedium = TypeBase.bodyMedium.balanced(),
    bodySmall = TypeBase.bodySmall.balanced(),
    labelLarge = TypeBase.labelLarge.balanced(),
    labelMedium = TypeBase.labelMedium.balanced(),
    labelSmall = TypeBase.labelSmall.balanced(),
)

/** A panel's engraved legend. */
val PanelLegend = TextStyle(
    fontFamily = Engraved,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.2.sp,
)

/** A tool's main measurement, large enough to read at arm's length. */
val BigReadout = TextStyle(fontFamily = ReadoutFamily, fontSize = 38.sp, lineHeight = 44.sp)

/**
 * Axis and point labels drawn on graphs: monospaced, at Material's smallest size, 11 sp. They were 10, below the
 * smallest size Material allows for text (MD3 audit).
 */
val ChartLabel = TextStyle(fontFamily = ReadoutFamily, fontSize = 11.sp, lineHeight = 14.sp)

/** A panel's engraved legend at its smallest, for a label beside a control. */
val PanelLegendSmall = PanelLegend.copy(fontSize = 11.sp, lineHeight = 16.sp)

/** Monospaced digits, so measurements stay still while their values change. */
val Readout = TextStyle(fontFamily = ReadoutFamily, fontSize = 13.sp, lineHeight = 18.sp)

/**
 * The app's one corner scale. Everything rounded takes its corners from here, Material's own components through
 * [Corners] and the app's own through these, so no corner is a size of its own (MD3 audit).
 */
internal object Corner {
    /** Small marks inside other things, such as the strip's little curve. */
    val tiny = RoundedCornerShape(4.dp)
    val extraSmall = RoundedCornerShape(8.dp)
    val small = RoundedCornerShape(12.dp)
    /** Panels. */
    val medium = RoundedCornerShape(16.dp)
    /** Keys and toggles. */
    val large = RoundedCornerShape(20.dp)
    /** The bars that hold keys: the medium corners of the key inside plus the 8 dp between them, so they nest. */
    val largeIncreased = RoundedCornerShape(24.dp)
    val extraLarge = RoundedCornerShape(28.dp)
}

// Material's components take the same scale, so menus, chips, fields and dialogs are as soft as the rest.
private val Corners = Shapes(
    extraSmall = Corner.extraSmall,
    small = Corner.small,
    medium = Corner.medium,
    large = Corner.large,
    extraLarge = Corner.extraLarge,
)

/**
 * Material You's colours, taken from the wallpaper (user decision), in the app's own roles. The ears and warnings
 * keep their fixed colours, since what they mean can't change with the wallpaper.
 */
private fun fromScheme(s: androidx.compose.material3.ColorScheme, fixed: ThemeColors, dark: Boolean): ThemeColors {
    fun over(top: Color, alpha: Float, under: Color) = androidx.compose.ui.graphics.lerp(under, top, alpha)
    return fixed.copy(
        // The same arrangement in both themes (user decision): the page in Material's surface tone, and the bars in
        // its lowest, a shade apart from the page, near-black at night and white by day.
        background = s.surface,
        // Material's lowest tone: near-black framing the page at night, and white by day (user decisions).
        bar = s.surfaceContainerLowest,
        // The tab bar's own surface, so the two read as a pair at top and bottom, with the colour only on what's lit
        // inside it. Filled with the colour it looked like a bigger toggle, and tinted grey it looked muddy (user decision).
        strip = s.surfaceContainerLowest,
        stripDim = s.onSurfaceVariant,
        stripText = s.onSurface,
        stripOn = s.primary,
        // A lighter shade of the strip's own colour for its drawer.
        // At night the strip's colour is a pale tint, so its drawer goes a shade deeper; by day a shade lighter.
        // The drawer is the strip itself carried on down, in the same colour with the same text, set apart only by a
        // faint line and its shadow. A different tone flipped light and dark between the two, so they read as two
        // pieces rather than one bar opening (user decision).
        stripPanel = s.surfaceContainerLowest,
        // Half a step off the page in both themes: panels are felt rather than seen, as the graph's is, with the space
        // between them doing more of the grouping (user decision).
        surface = over(s.surfaceContainerLow, PANEL_STEP, s.surface),
        surfaceHigh = s.surfaceContainerHighest,
        // Menus and dialogs: a raised grey at night, and white with their shadow by day, rather than a dark grey on white.
        tooltip = if (dark) s.surfaceContainerHigh else s.surfaceContainerLowest,
        line = s.outlineVariant,
        outline = s.outline,
        track = s.outline,
        text = s.onSurface,
        textDim = s.onSurfaceVariant,
        lit = s.primary,
        litFill = s.primary,
        litPressed = over(s.onPrimary, 0.12f, s.primary),
        onLit = s.onPrimary,
    ).let { colours ->
        val (accent, onAccent) = SeedScheme.accentFor(s.primary, dark)
        colours.copy(accent = accent, onAccent = onAccent)
    }
}

@Composable
fun EigentoneTheme(dark: Boolean, seed: Long? = null, highContrast: Boolean = false, content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Colours from the wallpaper from Android 12, unless a colour of their own is picked. Before Android 12 there
    // are none to take, so Material You's colours are made from Eigentone's blue, or the colour picked, instead
    // of the old fixed palettes (tester feedback).
    val wallpaper = seed == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val scheme = androidx.compose.runtime.remember(seed, dark, wallpaper) {
        when {
            wallpaper && dark -> androidx.compose.material3.dynamicDarkColorScheme(context)
            wallpaper -> androidx.compose.material3.dynamicLightColorScheme(context)
            else -> SeedScheme.scheme(seed ?: SeedScheme.DEFAULT_SEED, dark)
        }
    }
    val colors = androidx.compose.runtime.remember(scheme, dark, highContrast) { themeColors(scheme, dark, highContrast) }
    // Set before anything below reads it, so a change can't show one frame of the old theme.
    if (Palette.colors != colors) Palette.colors = colors
    Palette.dark = dark
    Palette.highContrast = highContrast
    MaterialTheme(colorScheme = scheme, typography = Type, shapes = Corners, content = content)
}

/** How far a panel steps from the page towards Material's low container: half way, for quiet panels. */
private const val PANEL_STEP = 0.55f

/** The app's colours from a Material scheme, as the theme wears them. */
internal fun themeColors(scheme: androidx.compose.material3.ColorScheme, dark: Boolean, highContrast: Boolean): ThemeColors =
    fromScheme(scheme, if (dark) DarkColors else LightColors, dark).let { if (highContrast) it.contrasted() else it }
