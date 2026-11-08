package com.wizlabs.wizdipcharts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
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
import android.widget.Toast
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
        setContent { CappedFontScale { TankDipApp(tanks) } }
    }
}

/** Caps system font scale so the big numbers never overflow. */
@Composable
private fun CappedFontScale(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, minOf(d.fontScale, 1.15f)), content = content)
}

/* ---------- Palette: Wiz black & yellow, calm neutrals ---------- */

private class Pal(
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
)

private val Yellow = Color(0xFFFFC400)

private val LightPal = Pal(
    bg = Color(0xFFF6F6F3), card = Color.White, field = Color(0xFFEDEDE7), raised = Color.White,
    label = Color(0xFF111113), secondary = Color(0xFF5C5C63), tertiary = Color(0xFF8E8E95),
    separator = Color(0xFFE7E6E0), accent = Yellow, onAccent = Color(0xFF111113),
    accentSoft = Color(0xFFFFF4CC), error = Color(0xFFC2410C),
)
private val DarkPal = Pal(
    bg = Color(0xFF0B0B0C), card = Color(0xFF151517), field = Color(0xFF1D1D20), raised = Color(0xFF34343A),
    label = Color(0xFFF4F4F2), secondary = Color(0xFFB4B4BA), tertiary = Color(0xFF77777F),
    separator = Color(0xFF242428), accent = Yellow, onAccent = Color(0xFF111113),
    accentSoft = Color(0xFF2B2510), error = Color(0xFFFF8A65),
)

private val CardShape = RoundedCornerShape(20.dp)
private val volFmt = DecimalFormat("#,##0.0")
private val volFmt0 = DecimalFormat("#,##0")
private val dipFmt = DecimalFormat("0.0")
private val timeFmt get() = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateFmt get() = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

private fun capacity(t: Tank) = t.volumes[t.maxDipCm]
/** Name without the KL prefix, e.g. "625×211 DW: 18 L". */
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
    fun copy(text: String, toast: String) {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
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
            Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                AppMark()
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dip Charts", color = pal.label, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                    Text("Tank calibration · offline", color = pal.tertiary, fontSize = 13.sp)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More options", tint = pal.secondary) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = pal.card) {
                        MenuItem(Icons.Default.List, "My charts", pal) { menuOpen = false; showManageCharts = true }
                        MenuItem(Icons.Default.Share, "Share app", pal) { menuOpen = false; shareText(context, "Share app", "Wiz Dip Charts – quick tank calibration lookup from dip reading.\n$SHARE_LINK") }
                        MenuItem(Icons.Default.Star, "Rate app", pal) { menuOpen = false; rateApp(context) }
                    }
                }
            }

            /* ---- Tank chips ---- */
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visibleTanks.forEach { t ->
                    TankChip(t, productOf(t), t.id == tank.id, hasDip = stock.any { it.tank.id == t.id }, pal = pal) {
                        selectTank(t)
                        if (productOf(t).isEmpty() && chartsConfigured) { renameChartId = t.id; renameValue = "" }
                    }
                }
                Box(
                    Modifier.height(54.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, pal.separator, RoundedCornerShape(14.dp))
                        .clickable { showManageCharts = true }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Add, "Manage my charts", tint = pal.secondary, modifier = Modifier.size(20.dp)) }
            }

            /* ---- Selected tank details ---- */
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tank.label, color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "P1 ${tank.p1Cm} cm · P2 ${tank.p2Cm} cm · Max dip ${tank.maxDipCm} cm",
                        color = pal.tertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { renameChartId = tank.id; renameValue = productOf(tank) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, "Edit product name", tint = pal.tertiary, modifier = Modifier.size(18.dp))
                }
            }

            /* ---- Mode ---- */
            SegmentedControl(listOf("Dip → Volume", "Volume → Dip"), if (reverse) 1 else 0, pal) { index ->
                if ((index == 1) != reverse) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                reverse = index == 1
            }
            Spacer(Modifier.height(12.dp))

            /* ---- Input + result in one card ---- */
            Column(Modifier.fillMaxWidth().background(pal.card, CardShape).border(1.dp, pal.separator, CardShape)) {
                if (!reverse) {
                    NumberField(
                        label = "Dip", hint = "max ${tank.maxDipCm} cm", value = dipText, unit = "cm", maxLength = 7, pal = pal,
                        error = forward is DipResult.Invalid || forward is DipResult.OutOfRange,
                        onDone = { keyboard?.hide() }, onChange = ::setDip,
                    )
                    HLine(pal)
                    when (forward) {
                        is DipResult.Ok -> ResultBlock(
                            label = "Volume", value = volFmt.format(forward.netVolume), unit = "L",
                            fill = (forward.netVolume / capacity(tank)).toFloat(),
                            space = capacity(tank) - forward.netVolume, pal = pal,
                            onCopy = { copy(volFmt.format(forward.netVolume), "Copied") },
                            onShare = {
                                shareText(context, "Share reading", "${tankLine(tank)}\nDip ${dipText.replace(',', '.')} cm = ${volFmt.format(forward.netVolume)} L")
                            },
                        )
                        DipResult.Empty -> EmptyResult("Enter a dip to see the volume", pal)
                        DipResult.Invalid -> EmptyResult("Enter a valid number", pal, error = true)
                        is DipResult.OutOfRange -> EmptyResult("Above the tank maximum of ${forward.maxDipCm} cm", pal, error = true)
                    }
                } else {
                    NumberField(
                        label = "Volume", hint = "max ${volFmt0.format(capacity(tank))} L", value = volumeText, unit = "L", maxLength = 9, pal = pal,
                        error = back is DipForVolumeResult.Invalid || back is DipForVolumeResult.OutOfRange,
                        onDone = { keyboard?.hide() }, onChange = { volumeText = it },
                    )
                    HLine(pal)
                    when (back) {
                        is DipForVolumeResult.Ok -> ResultBlock(
                            label = "Dip", value = dipFmt.format(back.dipCm), unit = "cm",
                            fill = (back.chartVolume / capacity(tank)).toFloat(),
                            space = capacity(tank) - back.chartVolume, pal = pal,
                            onCopy = { copy(dipFmt.format(back.dipCm), "Copied") },
                            onShare = {
                                shareText(context, "Share reading", "${tankLine(tank)}\n${volFmt.format(back.chartVolume)} L = dip ${dipFmt.format(back.dipCm)} cm")
                            },
                        )
                        DipForVolumeResult.Empty -> EmptyResult("Enter a volume to see the dip", pal)
                        DipForVolumeResult.Invalid -> EmptyResult("Enter a valid number", pal, error = true)
                        is DipForVolumeResult.OutOfRange -> EmptyResult("Above tank capacity of ${volFmt0.format(back.maxVolumeLitres)} L", pal, error = true)
                    }
                }
            }

            /* ---- Today's stock ---- */
            if (stock.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                StockCard(
                    stock, tank.id, pal,
                    onPick = { t -> selectTank(t); reverse = false },
                    onShare = { shareText(context, "Share stock", stockReport(stock)) },
                    onCopy = { copy(stockReport(stock), "Stock copied") },
                    nameOf = ::tankLine,
                )
            }

            Text(
                "Chart volumes are shown as printed. Deadwood is listed for reference and is not deducted.",
                color = pal.tertiary, fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp),
            )
        }
    }

    if (showManageCharts) {
        ManageChartsDialog(tanks, selectedIds, productNames, pal, onDismiss = { showManageCharts = false }) {
            chartsConfigured = true
            val edit = prefs.edit().putStringSet(KEY_MY_CHARTS, selectedIds.toSet()).putBoolean(KEY_MY_CHARTS_CONFIGURED, true)
            productNames.forEach { (id, name) -> edit.putString(KEY_PRODUCT_PREFIX + id, name.trim()) }
            edit.apply()
            val next = tanks.firstOrNull { it.id == selectedChartId && (selectedIds.isEmpty() || it.id in selectedIds) }
                ?: tanks.firstOrNull { selectedIds.isEmpty() || it.id in selectedIds } ?: tanks.first()
            selectedChartId = next.id
            prefs.edit().putString(KEY_LAST_TANK, next.id).apply()
            showManageCharts = false
        }
    }
    renameChartId?.let { id ->
        val chart = tanks.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { renameChartId = null },
            containerColor = pal.card,
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
                        modifier = Modifier.fillMaxWidth().background(pal.field, RoundedCornerShape(12.dp)).padding(14.dp),
                        decorationBox = { inner -> Box { if (renameValue.isEmpty()) Text("e.g. Petrol, Diesel", color = pal.tertiary, fontSize = 17.sp); inner() } },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Petrol", "Diesel", "Premium").forEach { s ->
                            Text(
                                s, color = pal.secondary, fontSize = 13.sp,
                                modifier = Modifier.clip(CircleShape).background(pal.field).clickable { renameValue = s }.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    productNames[id] = renameValue.trim()
                    prefs.edit().putString(KEY_PRODUCT_PREFIX + id, renameValue.trim()).apply()
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
    Box(Modifier.size(36.dp).background(Yellow, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(20.dp)) {
            val ink = Color(0xFF111113)
            val w = size.width; val h = size.height
            val s = w * 0.11f
            drawRoundRect(ink, topLeft = Offset(w * 0.08f, h * 0.32f), size = Size(w * 0.84f, h * 0.56f), cornerRadius = CornerRadius(h * 0.26f), style = Stroke(s))
            drawLine(ink, Offset(w * 0.5f, h * 0.06f), Offset(w * 0.5f, h * 0.74f), strokeWidth = s, cap = StrokeCap.Round)
            drawLine(ink, Offset(w * 0.2f, h * 0.66f), Offset(w * 0.8f, h * 0.66f), strokeWidth = s * 0.8f, cap = StrokeCap.Round)
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
    val shape = RoundedCornerShape(14.dp)
    val bg = if (selected) pal.accent else pal.card
    val fg = if (selected) pal.onAccent else pal.label
    val sub = if (selected) pal.onAccent.copy(alpha = 0.65f) else pal.tertiary
    Row(
        Modifier.height(54.dp).clip(shape).background(bg).border(1.dp, if (selected) pal.accent else pal.separator, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
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
        if (hasDip) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(6.dp).background(if (selected) pal.onAccent else Yellow, CircleShape))
        }
    }
}

@Composable
private fun SegmentedControl(options: List<String>, selectedIndex: Int, pal: Pal, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(pal.field, RoundedCornerShape(14.dp)).padding(4.dp)) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (selected) pal.raised else Color.Transparent)
                    .clickable { onSelect(index) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (selected) pal.label else pal.secondary, fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
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
    Column(
        Modifier.fillMaxWidth().padding(6.dp).clip(RoundedCornerShape(15.dp))
            .background(if (focused) pal.field else Color.Transparent).padding(horizontal = 14.dp, vertical = 10.dp),
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
                textStyle = TextStyle(color = if (error) pal.error else pal.label, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
                cursorBrush = SolidColor(pal.label),
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text("0", color = pal.tertiary.copy(alpha = 0.5f), fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                        inner()
                    }
                },
            )
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Clear", tint = pal.tertiary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(unit, color = pal.tertiary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ResultBlock(
    label: String, value: String, unit: String, fill: Float, space: Double, pal: Pal,
    onCopy: () -> Unit, onShare: () -> Unit,
) {
    val f by animateFloatAsState(fill.coerceIn(0f, 1f), label = "fill")
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp)) {
        Text(label.uppercase(), color = pal.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = pal.label, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            Text(unit, color = pal.tertiary, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 11.dp))
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(pal.field)) {
            Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(Yellow))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("${(fill * 100).coerceIn(0f, 100f).let { DecimalFormat("0").format(it) }}% full", color = pal.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("Space ${volFmt0.format(space.coerceAtLeast(0.0))} L", color = pal.secondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Copy", null, pal, Modifier.weight(1f), onCopy)
            ActionButton("Share", Icons.Default.Share, pal, Modifier.weight(1f), onShare)
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector?, pal: Pal, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.height(44.dp).clip(shape).border(1.dp, pal.separator, shape).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = pal.label, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)) }
        else { CopyGlyph(pal.label); Spacer(Modifier.width(7.dp)) }
        Text(label, color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun EmptyResult(text: String, pal: Pal, error: Boolean = false) {
    Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(20.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, color = if (error) pal.error else pal.tertiary, fontSize = 15.sp)
    }
}

@Composable
private fun StockCard(
    stock: List<StockEntry>, selectedId: String, pal: Pal,
    onPick: (Tank) -> Unit, onShare: () -> Unit, onCopy: () -> Unit, nameOf: (Tank) -> String,
) {
    Column(Modifier.fillMaxWidth().background(pal.card, CardShape).border(1.dp, pal.separator, CardShape).padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Today's stock", color = pal.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onCopy) { Text("Copy", color = pal.secondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            TextButton(onClick = onShare) { Text("Share", color = pal.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        }
        stock.forEachIndexed { i, e ->
            if (i > 0) HLine(pal, inset = 16.dp)
            Row(
                Modifier.fillMaxWidth().clickable { onPick(e.tank) }.padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).background(if (e.tank.id == selectedId) Yellow else Color.Transparent, CircleShape))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(nameOf(e.tank), color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${dipFmt.format(e.dip)} cm · ${timeFmt.format(Date(e.at))}", color = pal.tertiary, fontSize = 12.sp)
                }
                Text("${volFmt0.format(e.volume)} L", color = pal.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun ManageChartsDialog(
    tanks: List<Tank>,
    selectedIds: SnapshotStateList<String>,
    productNames: SnapshotStateMap<String, String>,
    pal: Pal,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val checked = remember { mutableStateListOf<String>().apply { addAll(selectedIds) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = pal.card,
        title = { Text("My charts", color = pal.label, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.height(440.dp)) {
                Text("Pick the charts used at your outlet and name each product. Only these appear on the main screen.", color = pal.secondary, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.lazy.LazyColumn {
                    items(tanks.size) { index ->
                        val chart = tanks[index]
                        val isChecked = chart.id in checked
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { if (isChecked) checked.remove(chart.id) else checked.add(chart.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { value -> if (value) { if (chart.id !in checked) checked.add(chart.id) } else checked.remove(chart.id) },
                                colors = CheckboxDefaults.colors(checkedColor = Yellow, checkmarkColor = Color(0xFF111113), uncheckedColor = pal.tertiary),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(chart.label, color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("P1 ${chart.p1Cm} · P2 ${chart.p2Cm} · Max dip ${chart.maxDipCm} cm", color = pal.tertiary, fontSize = 12.sp)
                                if (isChecked) {
                                    BasicTextField(
                                        value = productNames[chart.id] ?: "",
                                        onValueChange = { productNames[chart.id] = it.take(32) },
                                        singleLine = true,
                                        textStyle = TextStyle(color = pal.label, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                        cursorBrush = SolidColor(pal.label),
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 8.dp).background(pal.field, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
                                        decorationBox = { inner -> Box { if ((productNames[chart.id] ?: "").isEmpty()) Text("Product name, e.g. Petrol", color = pal.tertiary, fontSize = 13.sp); inner() } },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { selectedIds.clear(); selectedIds.addAll(checked); onSave() }) {
                Text("Save", color = pal.label, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = pal.secondary) } },
    )
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
