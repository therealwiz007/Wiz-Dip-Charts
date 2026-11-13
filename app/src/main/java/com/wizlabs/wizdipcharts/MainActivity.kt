package com.wizlabs.wizdipcharts

import androidx.compose.animation.SizeTransform
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS = "tankdip_prefs"
private const val KEY_LAST_TANK = "last_chart_id"
private const val KEY_MY_CHARTS = "my_chart_ids"
private const val KEY_MY_CHARTS_CONFIGURED = "my_charts_configured"
private const val KEY_LAST_DIP = "last_dip_cm"          // pre-1.5 single dip, migrated to the last tank
private const val KEY_PRODUCT_PREFIX = "product_name_"
private const val KEY_DIP_PREFIX = "dip_"               // per-tank dip text
private const val KEY_DIP_AT_PREFIX = "dip_at_"         // per-tank dip time (epoch ms)

private const val APP_ID = "com.wizlabs.wizdipcharts"

/** Play Store listing URL; works once the app listing is published. */
private const val SHARE_LINK = "https://play.google.com/store/apps/details?id=$APP_ID"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val tanks = Calibration.load(this)
        setContent { CappedFontScale { WizTheme { CompositionLocalProvider(LocalBackPeek provides remember { BackPeek() }) { TankDipApp(tanks) } } } }
    }
}

/** Caps system font scale so the big numbers never overflow. */
@Composable
private fun CappedFontScale(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, minOf(d.fontScale, 1.15f)), content = content)
}

/* ---------- Palette: Wiz black & yellow, calm neutrals ---------- */

internal class Pal(
    val bg: Color,
    val card: Color,
    val field: Color,
    val raised: Color,
    val label: Color,
    val secondary: Color,
    val tertiary: Color,
    val separator: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val error: Color,
    val dark: Boolean,
)

internal val Yellow = Color(0xFFFFC400)
internal val Ink = Color(0xFF111113)

private val LightPal = Pal(
    bg = Color(0xFFF6F6F3), card = Color.White, field = Color(0xFFEDEDE7), raised = Color.White,
    label = Ink, secondary = Color(0xFF5C5C63), tertiary = Color(0xFF8E8E95),
    separator = Color(0xFFE7E6E0), accent = Yellow, onAccent = Ink,
    accentSoft = Color(0xFFFFF4CC), error = Color(0xFFC2410C), dark = false,
)
private val DarkPal = Pal(
    bg = Color(0xFF0B0B0C), card = Color(0xFF151517), field = Color(0xFF1D1D20), raised = Color(0xFF34343A),
    label = Color(0xFFF4F4F2), secondary = Color(0xFFB4B4BA), tertiary = Color(0xFF77777F),
    separator = Color(0xFF242428), accent = Yellow, onAccent = Ink,
    accentSoft = Color(0xFF2B2510), error = Color(0xFFFF8A65), dark = true,
)

/** Material components (menus, dialogs, text handles) themed Wiz yellow with no tonal tint. */
@Composable
private fun WizTheme(content: @Composable () -> Unit) {
    val p = if (isSystemInDarkTheme()) DarkPal else LightPal
    val scheme = if (p.dark) {
        darkColorScheme(primary = Yellow, onPrimary = Ink, surface = p.card, onSurface = p.label, background = p.bg,
            surfaceTint = Color.Transparent, surfaceContainer = p.card, surfaceContainerHigh = p.card, onSurfaceVariant = p.secondary)
    } else {
        lightColorScheme(primary = Ink, onPrimary = Color.White, surface = p.card, onSurface = p.label, background = p.bg,
            surfaceTint = Color.Transparent, surfaceContainer = p.card, surfaceContainerHigh = p.card, onSurfaceVariant = p.secondary)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private val CardShape = RoundedCornerShape(24.dp)
private val Tabular = "tnum"
private val volFmt = DecimalFormat("#,##0.0")
private val volFmt0 = DecimalFormat("#,##0")
private val dipFmt = DecimalFormat("0.0")
private val timeFmt get() = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateFmt get() = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

private fun capacity(t: Tank) = t.volumes[t.maxDipCm]
/** Name without the KL prefix, e.g. "211×625 DW: 18 L". */
private fun chartTitle(t: Tank) = t.label.substringAfter(" - ", t.label)

/** One tank's entry in today's stock list. */
private class StockEntry(val tank: Tank, val product: String, val dip: Double, val volume: Double, val at: Long)

@Composable
fun TankDipApp(tanks: List<Tank>) {
    val context = LocalContext.current
    val pal = if (isSystemInDarkTheme()) DarkPal else LightPal
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val savedIds = remember { prefs.getStringSet(KEY_MY_CHARTS, emptySet())?.toSet() ?: emptySet() }
    val selectedIds = remember { mutableStateListOf<String>().apply { addAll(savedIds) } }
    val productNames = remember {
        mutableStateMapOf<String, String>().apply { tanks.forEach { t -> put(t.id, prefs.getString(KEY_PRODUCT_PREFIX + t.id, "") ?: "") } }
    }
    var chartsConfigured by remember { mutableStateOf(prefs.getBoolean(KEY_MY_CHARTS_CONFIGURED, false)) }
    val visibleTanks = remember(tanks, selectedIds.toList(), chartsConfigured) {
        val filtered = if (chartsConfigured && selectedIds.isNotEmpty()) tanks.filter { it.id in selectedIds } else tanks
        filtered.ifEmpty { tanks }
    }
    var selectedChartId by rememberSaveable { mutableStateOf(prefs.getString(KEY_LAST_TANK, visibleTanks.first().id) ?: visibleTanks.first().id) }
    val tank = visibleTanks.firstOrNull { it.id == selectedChartId } ?: visibleTanks.first()

    // Each tank remembers its own dip and when it was entered.
    val dips = remember {
        mutableStateMapOf<String, String>().apply {
            tanks.forEach { t -> prefs.getString(KEY_DIP_PREFIX + t.id, null)?.let { put(t.id, it) } }
            val legacy = prefs.getString(KEY_LAST_DIP, "").orEmpty()
            val lastId = prefs.getString(KEY_LAST_TANK, null)
            if (legacy.isNotBlank() && lastId != null && !containsKey(lastId)) put(lastId, legacy)
        }
    }
    val dipTimes = remember {
        mutableStateMapOf<String, Long>().apply { tanks.forEach { t -> prefs.getLong(KEY_DIP_AT_PREFIX + t.id, 0L).takeIf { it > 0 }?.let { put(t.id, it) } } }
    }

    var showManageCharts by remember { mutableStateOf(!chartsConfigured) }
    var showFindChart by rememberSaveable { mutableStateOf(false) }
    val peek = LocalBackPeek.current
    // Once the back transition has played, drop the peek so the next visit starts full size.
    LaunchedEffect(showManageCharts, showFindChart) { delay(650); if (peek != null && !peek.dragging) peek.reset() }
    var renameChartId by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var volumeText by rememberSaveable { mutableStateOf("") }
    var reverse by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    fun productOf(t: Tank) = productNames[t.id]?.trim().orEmpty()
    fun selectTank(t: Tank) {
        if (t.id != selectedChartId) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedChartId = t.id
        prefs.edit().putString(KEY_LAST_TANK, t.id).apply()
    }
    fun setDip(new: String) {
        dips[tank.id] = new
        val now = System.currentTimeMillis()
        val edit = prefs.edit().putString(KEY_DIP_PREFIX + tank.id, new)
        if (new.isBlank()) { dipTimes.remove(tank.id); edit.remove(KEY_DIP_AT_PREFIX + tank.id) }
        else { dipTimes[tank.id] = now; edit.putLong(KEY_DIP_AT_PREFIX + tank.id, now) }
        edit.apply()
    }

    val dipText = dips[tank.id].orEmpty()
    val forward = Calibration.compute(tank, dipText)
    val back = Calibration.dipForVolume(tank, volumeText)

    val stock = visibleTanks.mapNotNull { t ->
        val at = dipTimes[t.id] ?: return@mapNotNull null
        if (!DateUtils.isToday(at)) return@mapNotNull null
        val text = dips[t.id].orEmpty()
        val r = Calibration.compute(t, text) as? DipResult.Ok ?: return@mapNotNull null
        StockEntry(t, productOf(t), text.replace(',', '.').toDouble(), r.netVolume, at)
    }

    fun tankLine(t: Tank): String {
        val p = productOf(t)
        return if (p.isNotEmpty()) "$p · ${t.label}" else t.label
    }
    fun copy(text: String, toast: String?) {
        clipboard.setText(AnnotatedString(text))
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (toast != null) Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }
    fun share(title: String, text: String) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        shareText(context, title, text)
    }

    Box(Modifier.fillMaxSize().background(pal.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            /* ---- Top bar ---- */
            Row(Modifier.fillMaxWidth().height(64.dp).enterStagger(0), verticalAlignment = Alignment.CenterVertically) {
                AppMark()
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dip Charts", color = pal.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                    Text("Tank calibration · offline", color = pal.tertiary, fontSize = 13.sp)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More options", tint = pal.secondary) }
                    DropdownMenu(
                        expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = pal.card,
                        shape = RoundedCornerShape(18.dp), tonalElevation = 0.dp, shadowElevation = 8.dp,
                    ) {
                        MenuItem(Icons.Default.List, "My charts", pal) { menuOpen = false; showManageCharts = true }
                        MenuItem(Icons.Default.Search, "Find my chart", pal) { menuOpen = false; showFindChart = true }
                        MenuItem(Icons.Default.Share, "Share app", pal) { menuOpen = false; shareText(context, "Share app", "Wiz Dip Charts – quick tank calibration lookup from dip reading.\n$SHARE_LINK") }
                        MenuItem(Icons.Default.Star, "Rate app", pal) { menuOpen = false; rateApp(context) }
                    }
                }
            }

            /* ---- Tank chips ---- */
            Row(
                Modifier.fillMaxWidth().enterStagger(1).horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visibleTanks.forEach { t ->
                    TankChip(t, productOf(t), t.id == tank.id, hasDip = stock.any { it.tank.id == t.id }, pal = pal) {
                        selectTank(t)
                        if (productOf(t).isEmpty() && chartsConfigured) { renameChartId = t.id; renameValue = "" }
                    }
                }
                Box(
                    Modifier.height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, pal.separator, RoundedCornerShape(16.dp))
                        .pressClick { showManageCharts = true }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Add, "Manage my charts", tint = pal.secondary, modifier = Modifier.size(20.dp)) }
            }

            /* ---- Selected tank details (crossfades on tank change) ---- */
            AnimatedContent(
                targetState = tank,
                transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 3 }) togetherWith fadeOut(tween(140)) },
                contentKey = { it.id },
                label = "tankDetails",
                modifier = Modifier.enterStagger(2),
            ) { t ->
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.label, color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!t.hasDeadwood) { NoDwBadge(pal); Spacer(Modifier.width(6.dp)) }
                            Text(
                                "P1 ${t.p1Cm} cm · P2 ${t.p2Cm} cm · Max dip ${t.maxDipCm} cm",
                                color = pal.tertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = { renameChartId = t.id; renameValue = productOf(t) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Edit, "Edit product name", tint = pal.tertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            /* ---- Mode ---- */
            SegmentedControl(listOf("Dip → Volume", "Volume → Dip"), if (reverse) 1 else 0, pal, Modifier.enterStagger(3)) { index ->
                if ((index == 1) != reverse) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                reverse = index == 1
            }
            Spacer(Modifier.height(12.dp))

            /* ---- Input + live tank hero in one card ---- */
            AnimatedContent(
                targetState = reverse,
                transitionSpec = {
                    val dir = if (targetState) 1 else -1
                    (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { dir * it / 4 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(260)) { -dir * it / 4 } + fadeOut(tween(160)))
                },
                label = "mode",
                modifier = Modifier.enterStagger(4),
            ) { rev ->
                Column(Modifier.fillMaxWidth().background(pal.card, CardShape).border(1.dp, pal.separator, CardShape)) {
                    if (!rev) {
                        NumberField(
                            label = "Dip", hint = "max ${tank.maxDipCm} cm", value = dipText, unit = "cm", maxLength = 7, pal = pal,
                            error = forward is DipResult.Invalid || forward is DipResult.OutOfRange,
                            onDone = { keyboard?.hide() }, onChange = ::setDip,
                        )
                        HLine(pal, inset = 20.dp)
                        val ok = forward as? DipResult.Ok
                        val dipNum = dipText.trim().replace(',', '.').toDoubleOrNull()
                        ResultArea(
                            label = "Volume", unit = "L", value = ok?.netVolume, format = volFmt::format,
                            message = when (forward) {
                                is DipResult.Ok -> null
                                DipResult.Empty -> "Enter a dip to see the volume"
                                DipResult.Invalid -> "Enter a valid number"
                                is DipResult.OutOfRange -> "Above the tank maximum of ${forward.maxDipCm} cm"
                            },
                            error = forward is DipResult.Invalid || forward is DipResult.OutOfRange,
                            level = when {
                                ok != null && dipNum != null -> (dipNum / tank.maxDipCm).toFloat()
                                forward is DipResult.OutOfRange -> 1f
                                else -> 0f
                            },
                            fill = ok?.let { (it.netVolume / capacity(tank)).toFloat() },
                            space = ok?.let { capacity(tank) - it.netVolume },
                            pal = pal,
                            onCopy = { ok?.let { copy(volFmt.format(it.netVolume), null) } },
                            onShare = {
                                ok?.let { share("Share reading", "${tankLine(tank)}\nDip ${dipText.replace(',', '.')} cm = ${volFmt.format(it.netVolume)} L") }
                            },
                        )
                    } else {
                        NumberField(
                            label = "Volume", hint = "max ${volFmt0.format(capacity(tank))} L", value = volumeText, unit = "L", maxLength = 9, pal = pal,
                            error = back is DipForVolumeResult.Invalid || back is DipForVolumeResult.OutOfRange,
                            onDone = { keyboard?.hide() }, onChange = { volumeText = it },
                        )
                        HLine(pal, inset = 20.dp)
                        val ok = back as? DipForVolumeResult.Ok
                        ResultArea(
                            label = "Dip", unit = "cm", value = ok?.dipCm, format = dipFmt::format,
                            message = when (back) {
                                is DipForVolumeResult.Ok -> null
                                DipForVolumeResult.Empty -> "Enter a volume to see the dip"
                                DipForVolumeResult.Invalid -> "Enter a valid number"
                                is DipForVolumeResult.OutOfRange -> "Above tank capacity of ${volFmt0.format(back.maxVolumeLitres)} L"
                            },
                            error = back is DipForVolumeResult.Invalid || back is DipForVolumeResult.OutOfRange,
                            level = when {
                                ok != null -> (ok.dipCm / tank.maxDipCm).toFloat()
                                back is DipForVolumeResult.OutOfRange -> 1f
                                else -> 0f
                            },
                            fill = ok?.let { (it.chartVolume / capacity(tank)).toFloat() },
                            space = ok?.let { capacity(tank) - it.chartVolume },
                            pal = pal,
                            onCopy = { ok?.let { copy(dipFmt.format(it.dipCm), null) } },
                            onShare = {
                                ok?.let { share("Share reading", "${tankLine(tank)}\n${volFmt.format(it.chartVolume)} L = dip ${dipFmt.format(it.dipCm)} cm") }
                            },
                        )
                    }
                }
            }

            /* ---- Today's stock ---- */
            AnimatedVisibility(
                visible = stock.isNotEmpty(),
                enter = fadeIn(tween(260)) + expandVertically(tween(320, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(240)),
                modifier = Modifier.enterStagger(5),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    StockCard(
                        stock, tank.id, pal,
                        onPick = { t -> selectTank(t); reverse = false },
                        onShare = { share("Share stock", stockReport(stock)) },
                        onCopy = { copy(stockReport(stock), "Stock copied") },
                        nameOf = ::tankLine,
                    )
                }
            }

            Text(
                "Chart volumes are shown as printed. Deadwood is listed for reference and is not deducted.",
                color = pal.tertiary, fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp).enterStagger(6),
            )
        }

        /* ---- My charts: full-screen page that slides in ---- */
        AnimatedVisibility(
            visible = showManageCharts,
            enter = slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(220)),
        ) {
          Box(Modifier.backPeek("manage")) {
            ManageChartsScreen(
                tanks, selectedIds, productNames, pal,
                onDismiss = { showManageCharts = false },
                onFind = { showManageCharts = false; showFindChart = true },
            ) {
                chartsConfigured = true
                val edit = prefs.edit().putStringSet(KEY_MY_CHARTS, selectedIds.toSet()).putBoolean(KEY_MY_CHARTS_CONFIGURED, true)
                productNames.forEach { (id, name) -> edit.putString(KEY_PRODUCT_PREFIX + id, name.trim()) }
                edit.apply()
                val next = tanks.firstOrNull { it.id == selectedChartId && (selectedIds.isEmpty() || it.id in selectedIds) }
                    ?: tanks.firstOrNull { selectedIds.isEmpty() || it.id in selectedIds } ?: tanks.first()
                selectedChartId = next.id
                prefs.edit().putString(KEY_LAST_TANK, next.id).apply()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showManageCharts = false
            }
          }
        }

        /* ---- Find my chart: match the dealer's readings against every chart ---- */
        AnimatedVisibility(
            visible = showFindChart,
            enter = slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(220)),
        ) {
          Box(Modifier.backPeek("find")) {
            FindChartScreen(
                tanks, pal,
                isSaved = { t -> chartsConfigured && t.id in selectedIds },
                onDismiss = { showFindChart = false },
            ) { t ->
                if (!(chartsConfigured && t.id in selectedIds)) {
                    if (!chartsConfigured) selectedIds.clear()
                    selectedIds.add(t.id)
                }
                chartsConfigured = true
                prefs.edit().putStringSet(KEY_MY_CHARTS, selectedIds.toSet()).putBoolean(KEY_MY_CHARTS_CONFIGURED, true)
                    .putString(KEY_LAST_TANK, t.id).apply()
                selectedChartId = t.id
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
          }
        }
    }

    renameChartId?.let { id ->
        val chart = tanks.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { renameChartId = null },
            containerColor = pal.card,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(26.dp),
            title = { Text(if (productNames[id].isNullOrBlank()) "Name the product" else "Edit product name", color = pal.label, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(chart?.label ?: "Selected tank", color = pal.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    BasicTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it.take(32) },
                        singleLine = true,
                        textStyle = TextStyle(color = pal.label, fontSize = 17.sp),
                        cursorBrush = SolidColor(pal.label),
                        modifier = Modifier.fillMaxWidth().background(pal.field, RoundedCornerShape(14.dp)).padding(14.dp),
                        decorationBox = { inner -> Box { if (renameValue.isEmpty()) Text("e.g. Petrol, Diesel", color = pal.tertiary, fontSize = 17.sp); inner() } },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Petrol", "Diesel", "Premium").forEach { s ->
                            val on = renameValue.trim().equals(s, ignoreCase = true)
                            val bg by animateColorAsState(if (on) pal.accent else pal.field, label = "suggest")
                            Text(
                                s, color = if (on) pal.onAccent else pal.secondary, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.clip(CircleShape).drawBehind { drawRect(bg) }.pressClick { renameValue = s }.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    productNames[id] = renameValue.trim()
                    prefs.edit().putString(KEY_PRODUCT_PREFIX + id, renameValue.trim()).apply()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    renameChartId = null
                }) { Text("Save", color = pal.label, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { renameChartId = null }) { Text("Later", color = pal.secondary) } },
        )
    }
}

/* ---------- Building blocks ---------- */

/** Small yellow tile with a tank and dip-rod glyph. */
@Composable
private fun AppMark() {
    Box(Modifier.size(38.dp).background(Yellow, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(21.dp)) {
            val w = size.width; val h = size.height
            val s = w * 0.11f
            drawRoundRect(Ink, topLeft = Offset(w * 0.08f, h * 0.32f), size = Size(w * 0.84f, h * 0.56f), cornerRadius = CornerRadius(h * 0.26f), style = Stroke(s))
            drawLine(Ink, Offset(w * 0.5f, h * 0.06f), Offset(w * 0.5f, h * 0.74f), strokeWidth = s, cap = StrokeCap.Round)
            drawLine(Ink, Offset(w * 0.2f, h * 0.66f), Offset(w * 0.8f, h * 0.66f), strokeWidth = s * 0.8f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, pal: Pal, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = pal.label, fontSize = 15.sp) },
        leadingIcon = { Icon(icon, null, tint = pal.secondary, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
    )
}

@Composable
private fun TankChip(t: Tank, product: String, selected: Boolean, hasDip: Boolean, pal: Pal, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val bg = animateColorAsState(if (selected) pal.accent else pal.card, tween(220), label = "chipBg")
    val stroke = animateColorAsState(if (selected) pal.accent else pal.separator, tween(220), label = "chipStroke")
    val fg = if (selected) pal.onAccent else pal.label
    val sub = if (selected) pal.onAccent.copy(alpha = 0.65f) else pal.tertiary
    Row(
        Modifier.height(56.dp).clip(shape)
            .drawBehind {
                drawRect(bg.value)
                drawRoundRect(stroke.value, cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
            .pressClick(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(if (product.isNotEmpty()) product else "${t.kl} KL", color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (product.isNotEmpty()) "${t.kl} KL" else chartTitle(t),
                color = sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 130.dp),
            )
        }
        AnimatedVisibility(hasDip, enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
            Row {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(7.dp).background(if (selected) pal.onAccent else Yellow, CircleShape))
            }
        }
    }
}

/** Two-way switch with a sliding pill drawn in the draw phase. */
@Composable
private fun SegmentedControl(options: List<String>, selectedIndex: Int, pal: Pal, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val pos = animateFloatAsState(selectedIndex.toFloat(), spring(dampingRatio = 0.72f, stiffness = 420f), label = "seg")
    val pill = pal.raised
    val dark = pal.dark
    Row(
        modifier.fillMaxWidth().background(pal.field, RoundedCornerShape(16.dp)).padding(4.dp)
            .drawBehind {
                val segW = size.width / options.size
                if (!dark) drawRoundRect(Color.Black.copy(alpha = 0.05f), Offset(segW * pos.value, 1.dp.toPx()), Size(segW, size.height), CornerRadius(13.dp.toPx()))
                drawRoundRect(pill, Offset(segW * pos.value, 0f), Size(segW, size.height), CornerRadius(13.dp.toPx()))
            },
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val color by animateColorAsState(if (selected) pal.label else pal.secondary, label = "segText")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).clickable { onSelect(index) }.padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = color, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String, hint: String, value: String, unit: String, maxLength: Int, pal: Pal, error: Boolean,
    onDone: () -> Unit, onChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = animateColorAsState(if (focused) pal.field else pal.card, tween(200), label = "fieldBg")
    val line = animateFloatAsState(if (focused) 1f else 0f, tween(320, easing = FastOutSlowInEasing), label = "fieldLine")
    val lineColor = if (error) pal.error else Yellow
    Column(
        Modifier.fillMaxWidth().padding(6.dp).clip(RoundedCornerShape(18.dp))
            .drawBehind {
                drawRect(bg.value)
                val lw = size.width * line.value
                if (lw > 0f) drawRect(lineColor, Offset((size.width - lw) / 2f, size.height - 3.dp.toPx()), Size(lw, 3.dp.toPx()))
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(label.uppercase(), color = pal.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
            Text(hint, color = if (error) pal.error else pal.tertiary, fontSize = 12.sp, fontWeight = if (error) FontWeight.SemiBold else FontWeight.Normal)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = { new -> if (new.length <= maxLength && new.all { it.isDigit() || it == '.' || it == ',' }) onChange(new) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                textStyle = TextStyle(color = if (error) pal.error else pal.label, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp, fontFeatureSettings = Tabular),
                cursorBrush = SolidColor(if (pal.dark) Yellow else pal.label),
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text("0", color = pal.tertiary.copy(alpha = 0.5f), fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                        inner()
                    }
                },
            )
            AnimatedVisibility(value.isNotEmpty(), enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Clear", tint = pal.tertiary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(unit, color = pal.tertiary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * The hero: headline number that counts to the exact result, the live tank, fill stats and
 * actions. When the count finishes the exact [value] is formatted, so results are unchanged.
 */
@Composable
private fun ResultArea(
    label: String, unit: String, value: Double?, format: (Double) -> String, message: String?, error: Boolean,
    level: Float, fill: Float?, space: Double?, pal: Pal, onCopy: () -> Unit, onShare: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp)) {
        Text(label.uppercase(), color = pal.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        AnimatedContent(
            targetState = message,
            transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 4 }) togetherWith fadeOut(tween(120)) },
            label = "result",
        ) { msg ->
            Box(Modifier.fillMaxWidth().heightIn(min = 66.dp), contentAlignment = Alignment.CenterStart) {
                if (msg == null && value != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        CountUpText(value, format, pal)
                        Spacer(Modifier.width(8.dp))
                        Text(unit, color = pal.tertiary, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 11.dp))
                    }
                } else {
                    Text(msg.orEmpty(), color = if (error) pal.error else pal.tertiary, fontSize = 15.sp, fontWeight = if (error) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        TankGauge(level, pal, error, Modifier.fillMaxWidth().height(118.dp))
        Row(Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 18.dp)) {
            val f = fill
            if (f != null && space != null) {
                Text("${(f * 100).coerceIn(0f, 100f).let { DecimalFormat("0").format(it) }}% full", color = pal.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("Space ${volFmt0.format(space.coerceAtLeast(0.0))} L", color = pal.secondary, fontSize = 13.sp)
            } else {
                Text("Empty tank · waiting for a reading", color = pal.tertiary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        val enabled = value != null && message == null
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CopyButton(enabled, pal, Modifier.weight(1f), onCopy)
            ActionButton("Share", Icons.Default.Share, enabled, primary = true, pal, Modifier.weight(1f), onShare)
        }
    }
}

/**
 * Big result number. Characters roll like an odometer and only the digits that changed move,
 * so typing a dip costs a few short animations instead of re-formatting the text every frame.
 */
@Composable
private fun CountUpText(target: Double, format: (Double) -> String, pal: Pal) {
    val style = TextStyle(color = pal.label, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp, fontFeatureSettings = Tabular)
    val text = format(target)
    if (!motionAllowed()) { Text(text, style = style, maxLines = 1); return }
    var prev by remember { mutableDoubleStateOf(target) }
    val up = target >= prev
    LaunchedEffect(target) { prev = target }
    Row {
        val n = text.length
        for (k in 0 until n) {
            // keyed by position from the right so units stay units when the length changes
            androidx.compose.runtime.key(n - k) {
                AnimatedContent(
                    text[k],
                    transitionSpec = {
                        val dir = if (up) 1 else -1
                        (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { dir * it / 2 } + fadeIn(tween(160))) togetherWith
                            (slideOutVertically(tween(160)) { -dir * it / 2 } + fadeOut(tween(120))) using SizeTransform(clip = false)
                    },
                    label = "digit",
                ) { c -> Text(c.toString(), style = style, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun CopyButton(enabled: Boolean, pal: Pal, modifier: Modifier, onCopy: () -> Unit) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1400); copied = false } }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.height(46.dp).graphicsLayer { alpha = if (enabled) 1f else 0.4f }.clip(shape).border(1.dp, pal.separator, shape)
            .pressClick(enabled = enabled) { onCopy(); copied = true },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(copied, transitionSpec = { (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith fadeOut(tween(90)) }, label = "copied") { done ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done) Icon(Icons.Default.Check, null, tint = pal.label, modifier = Modifier.size(17.dp)) else CopyGlyph(pal.label)
                Spacer(Modifier.width(7.dp))
                Text(if (done) "Copied" else "Copy", color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, enabled: Boolean, primary: Boolean, pal: Pal, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val bg = if (primary) pal.accent else Color.Transparent
    val fg = if (primary) pal.onAccent else pal.label
    Row(
        modifier.height(46.dp).graphicsLayer { alpha = if (enabled) 1f else 0.4f }.clip(shape).background(bg)
            .then(if (primary) Modifier else Modifier.border(1.dp, pal.separator, shape))
            .pressClick(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Two overlapping squares (material-icons-core has no copy icon). */
@Composable
private fun CopyGlyph(color: Color, size: Dp = 16.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.width; val sw = s * 0.11f; val r = CornerRadius(s * 0.14f)
        drawRoundRect(color, topLeft = Offset(s * 0.34f, s * 0.34f), size = Size(s * 0.6f, s * 0.6f), cornerRadius = r, style = Stroke(sw))
        drawRoundRect(color, topLeft = Offset(s * 0.06f, s * 0.06f), size = Size(s * 0.6f, s * 0.6f), cornerRadius = r, style = Stroke(sw))
    }
}

/** A tiny static capsule showing how full a tank is (stock list). */
@Composable
private fun MiniTank(fill: Float, pal: Pal) {
    val f = animateFloatAsState(fill.coerceIn(0f, 1f), tween(600, easing = FastOutSlowInEasing), label = "mini")
    Box(
        Modifier.size(width = 30.dp, height = 18.dp).drawBehind {
            val r = CornerRadius(size.height / 2f)
            drawRoundRect(pal.field, cornerRadius = r)
            val fh = size.height * f.value
            drawRoundRect(Brush.verticalGradient(listOf(Yellow, Color(0xFFFF9F0A))), Offset(0f, size.height - fh), Size(size.width, fh), r)
            drawRoundRect(pal.separator, cornerRadius = r, style = Stroke(1.dp.toPx()))
        },
    )
}

@Composable
private fun StockCard(
    stock: List<StockEntry>, selectedId: String, pal: Pal,
    onPick: (Tank) -> Unit, onShare: () -> Unit, onCopy: () -> Unit, nameOf: (Tank) -> String,
) {
    Column(Modifier.fillMaxWidth().background(pal.card, CardShape).border(1.dp, pal.separator, CardShape).padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Today's stock", color = pal.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onCopy) { Text("Copy", color = pal.secondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            TextButton(onClick = onShare) { Text("Share", color = pal.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        }
        stock.forEachIndexed { i, e ->
            if (i > 0) HLine(pal, inset = 18.dp)
            val sel = e.tank.id == selectedId
            val rowBg = animateColorAsState(if (sel) pal.accentSoft else Color.Transparent, tween(240), label = "stockRow")
            Row(
                Modifier.fillMaxWidth().drawBehind { drawRect(rowBg.value) }.clickable { onPick(e.tank) }.padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniTank((e.volume / capacity(e.tank)).toFloat(), pal)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(nameOf(e.tank), color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${dipFmt.format(e.dip)} cm · ${timeFmt.format(Date(e.at))}", color = pal.tertiary, fontSize = 12.sp)
                }
                Text("${volFmt0.format(e.volume)} L", color = pal.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, style = TextStyle(fontFeatureSettings = Tabular))
            }
        }
        val totals = productTotals(stock)
        if (totals.isNotEmpty()) {
            HLine(pal)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                totals.forEach { (product, litres) ->
                    Row(Modifier.clip(CircleShape).background(pal.accentSoft).padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Text("$product ", color = pal.secondary, fontSize = 13.sp)
                        Text("${volFmt0.format(litres)} L", color = pal.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Totals per named product, shown when more than one tank is in the list. */
private fun productTotals(stock: List<StockEntry>): List<Pair<String, Double>> {
    if (stock.size < 2) return emptyList()
    return stock.filter { it.product.isNotEmpty() }
        .groupBy { it.product.lowercase(Locale.ROOT) }
        .map { (_, list) -> list.first().product to list.sumOf { it.volume } }
}

private fun stockReport(stock: List<StockEntry>): String = buildString {
    append("Tank stock · ").append(dateFmt.format(Date())).append('\n')
    stock.forEach { e ->
        val name = if (e.product.isNotEmpty()) "${e.product} (${e.tank.label})" else e.tank.label
        append(name).append(": ").append(dipFmt.format(e.dip)).append(" cm = ").append(volFmt.format(e.volume)).append(" L\n")
    }
    val totals = productTotals(stock)
    if (totals.isNotEmpty()) {
        append('\n')
        totals.forEach { (p, l) -> append("Total ").append(p).append(": ").append(volFmt.format(l)).append(" L\n") }
    }
    append("\nWiz Dip Charts")
}

@Composable
private fun HLine(pal: Pal, inset: Dp = 0.dp) {
    Box(Modifier.fillMaxWidth().padding(horizontal = inset).height(1.dp).background(pal.separator))
}

/** Round check that pops in with a spring. */
@Composable
private fun PopCheck(checked: Boolean, pal: Pal) {
    val s = animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "check")
    Box(
        Modifier.size(26.dp).drawBehind {
            val r = size.minDimension / 2f
            drawCircle(pal.tertiary.copy(alpha = 0.6f), r - 1.dp.toPx(), style = Stroke(1.6.dp.toPx()))
            val k = s.value
            if (k > 0.01f) drawCircle(Yellow, r * k.coerceAtMost(1.1f))
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Check, null, tint = Ink,
            modifier = Modifier.size(16.dp).graphicsLayer { val k = s.value; scaleX = k; scaleY = k; alpha = k.coerceIn(0f, 1f) },
        )
    }
}

@Composable
private fun ManageChartsScreen(
    tanks: List<Tank>,
    selectedIds: SnapshotStateList<String>,
    productNames: SnapshotStateMap<String, String>,
    pal: Pal,
    onDismiss: () -> Unit,
    onFind: () -> Unit,
    onSave: () -> Unit,
) {
    val checked = remember { mutableStateListOf<String>().apply { addAll(selectedIds) } }
    WizBackHandler("manage", onBack = onDismiss)
    Column(Modifier.fillMaxSize().background(pal.bg).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "Back", tint = pal.label) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("My charts", color = pal.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                AnimatedContent(checked.size, transitionSpec = { (fadeIn() + slideInVertically { -it / 2 }) togetherWith fadeOut() }, label = "count") { n ->
                    Text(if (n == 0) "None picked · all charts show" else "$n picked", color = pal.tertiary, fontSize = 13.sp)
                }
            }
            Box(
                Modifier.padding(end = 10.dp).height(40.dp).clip(CircleShape).background(Yellow)
                    .pressClick { selectedIds.clear(); selectedIds.addAll(checked); onSave() }.padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
        Text(
            "Pick the charts used at your outlet and name each product. Only these appear on the main screen.",
            color = pal.secondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "find") {
                Row(
                    Modifier.fillMaxWidth().enterStagger(0).clip(RoundedCornerShape(20.dp)).background(pal.accentSoft)
                        .pressClick(scaleTo = 0.97f, onClick = onFind).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(34.dp).background(Yellow, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Search, null, tint = Ink, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Not sure which chart is yours?", color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Find my chart from two readings", color = pal.secondary, fontSize = 12.sp)
                    }
                }
            }
            items(tanks.size, key = { tanks[it].id }) { index ->
                val chart = tanks[index]
                val isChecked = chart.id in checked
                val stroke = animateColorAsState(if (isChecked) Yellow else pal.separator, tween(200), label = "rowStroke")
                Column(
                    Modifier.fillMaxWidth().enterStagger(index.coerceAtMost(8)).clip(RoundedCornerShape(20.dp)).background(pal.card)
                        .drawBehind { drawRoundRect(stroke.value, cornerRadius = CornerRadius(20.dp.toPx()), style = Stroke(1.5.dp.toPx())) }
                        .clickable { if (isChecked) checked.remove(chart.id) else checked.add(chart.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PopCheck(isChecked, pal)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chart.label, color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!chart.hasDeadwood) { NoDwBadge(pal); Spacer(Modifier.width(6.dp)) }
                                Text("P1 ${chart.p1Cm} · P2 ${chart.p2Cm} · Max dip ${chart.maxDipCm} cm", color = pal.tertiary, fontSize = 12.sp)
                            }
                        }
                    }
                    AnimatedVisibility(
                        isChecked,
                        enter = fadeIn(tween(200)) + expandVertically(tween(240)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
                    ) {
                        BasicTextField(
                            value = productNames[chart.id] ?: "",
                            onValueChange = { productNames[chart.id] = it.take(32) },
                            singleLine = true,
                            textStyle = TextStyle(color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(pal.label),
                            modifier = Modifier.fillMaxWidth().padding(start = 38.dp, top = 10.dp).background(pal.field, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                            decorationBox = { inner -> Box { if ((productNames[chart.id] ?: "").isEmpty()) Text("Product name, e.g. Petrol", color = pal.tertiary, fontSize = 14.sp); inner() } },
                        )
                    }
                }
            }
        }
    }
}

private fun rateApp(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$APP_ID"))
    try {
        context.startActivity(market)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHARE_LINK)))
    }
}

private fun shareText(context: Context, title: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Wiz Dip Charts")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, title))
}
