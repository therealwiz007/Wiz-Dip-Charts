package com.wizlabs.wizdipcharts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.DecimalFormat

private const val PREFS = "tankdip_prefs"
private const val KEY_LAST_TANK = "last_chart_id"
private const val KEY_MY_CHARTS = "my_chart_ids"
private const val KEY_MY_CHARTS_CONFIGURED = "my_charts_configured"
private const val KEY_LAST_DIP = "last_dip_cm"
private const val KEY_PRODUCT_PREFIX = "product_name_"

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


/** Caps system font scale so a non-scrolling screen can never overflow. */
@Composable
private fun CappedFontScale(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, minOf(d.fontScale, 1.15f)), content = content)
}

/* ---------- Palette: calm neutrals, one brand-blue accent ---------- */

private class Pal(
    val bg: Color,
    val card: Color,
    val raised: Color,
    val label: Color,
    val secondary: Color,
    val separator: Color,
    val track: Color,
    val accent: Color,
    val accentSoft: Color,
    val error: Color,
)

private val LightPal = Pal(
    bg = Color(0xFFF5F6F8), card = Color.White, raised = Color.White,
    label = Color(0xFF0B1220), secondary = Color(0xFF6B7585), separator = Color(0xFFE6E9EE),
    track = Color(0xFFE9ECF1), accent = Color(0xFF0757A6), accentSoft = Color(0xFFE9F1FA),
    error = Color(0xFFC93C3C),
)
private val DarkPal = Pal(
    bg = Color(0xFF0A0F1A), card = Color(0xFF121A28), raised = Color(0xFF253146),
    label = Color(0xFFEEF2F8), secondary = Color(0xFF8B96A8), separator = Color(0xFF1E293B),
    track = Color(0xFF18212F), accent = Color(0xFF5BA3F5), accentSoft = Color(0xFF13233A),
    error = Color(0xFFFF7A7A),
)

private val CardShape = RoundedCornerShape(20.dp)
private val numberFmt = DecimalFormat("#,##0.0")

@Composable
fun TankDipApp(tanks: List<Tank>) {
    val context = LocalContext.current
    val pal = if (isSystemInDarkTheme()) DarkPal else LightPal

    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val savedIds = remember { prefs.getStringSet(KEY_MY_CHARTS, emptySet())?.toSet() ?: emptySet() }
    val selectedIds = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(savedIds) } }
    val productNames = remember { androidx.compose.runtime.mutableStateMapOf<String, String>().apply { tanks.forEach { t -> put(t.id, prefs.getString(KEY_PRODUCT_PREFIX + t.id, "") ?: "") } } }
    var renameChartId by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var chartsConfigured by remember { mutableStateOf(prefs.getBoolean(KEY_MY_CHARTS_CONFIGURED, false)) }
    val visibleTanks = remember(tanks, selectedIds.toList(), chartsConfigured) {
        val filtered = if (chartsConfigured && selectedIds.isNotEmpty()) tanks.filter { it.id in selectedIds } else tanks
        filtered.ifEmpty { tanks }
    }
    var selectedChartId by rememberSaveable { mutableStateOf(prefs.getString(KEY_LAST_TANK, visibleTanks.first().id) ?: visibleTanks.first().id) }
    val tank = visibleTanks.firstOrNull { it.id == selectedChartId } ?: visibleTanks.first()
    var showManageCharts by remember { mutableStateOf(!chartsConfigured) }
    var dipText by rememberSaveable { mutableStateOf(prefs.getString(KEY_LAST_DIP, "") ?: "") }
    var volumeText by rememberSaveable { mutableStateOf("") }
    var reverse by rememberSaveable { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = pal.card) {
                Spacer(Modifier.height(48.dp))
                Text("Wiz Dip Charts", color = pal.label, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(1.dp).background(pal.separator))
                Spacer(Modifier.height(6.dp))
                DrawerItem(Icons.Default.Star, "My Charts", pal) {
                    scope.launch { drawerState.close() }
                    showManageCharts = true
                }
                DrawerItem(Icons.Default.Share, "Share app", pal) {
                    scope.launch { drawerState.close() }
                    shareApp(context)
                }
                DrawerItem(Icons.Default.Star, "Rate app", pal) {
                    scope.launch { drawerState.close() }
                    rateApp(context)
                }
            }
        },
    ) {
        // Non-scrollable by design. BoxWithConstraints sits inside safeDrawingPadding, so
        // maxHeight already excludes the keyboard; the spec strip is dropped only when
        // there is genuinely not enough room, which keeps input + result always visible.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(pal.bg)
                .safeDrawingPadding()
                .padding(horizontal = 18.dp),
        ) {
            val showSpecs = maxHeight >= 430.dp

            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = pal.label)
                    }
                    Spacer(Modifier.size(5.dp))
                    Text("Wiz Dip Charts", color = pal.label, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp)
                }

                SegmentedControl(
                    options = listOf("Dip → Volume", "Volume → Dip"),
                    selectedIndex = if (reverse) 1 else 0,
                    pal = pal,
                ) { index ->
                    if ((index == 1) != reverse) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    reverse = index == 1
                }

                Spacer(Modifier.height(12.dp))
                // Tank picker + specs merged into one card (saves two labels and a card gap).
                Card(pal) {
                    TankPicker(visibleTanks, tank, tanks.size, productNames, pal, onPick = { picked ->
                        if (picked.id != selectedChartId) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedChartId = picked.id
                        prefs.edit().putString(KEY_LAST_TANK, picked.id).apply()
                        if (productNames[picked.id].isNullOrBlank()) { renameChartId = picked.id; renameValue = "" }
                    }, onManage = { showManageCharts = true })
                    if (showSpecs) {
                        Divider(pal)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiniSpec("Diameter", "${fmtDim(tank.diameterCm)} cm", pal, Modifier.weight(1f))
                            MiniSpec("Length", "${fmtDim(tank.lengthCm)} cm", pal, Modifier.weight(1f))
                            MiniSpec("P1", "${tank.p1Cm} cm", pal, Modifier.weight(1f))
                            MiniSpec("P2", "${tank.p2Cm} cm", pal, Modifier.weight(1f))
                            MiniSpec("Deadwood", "${tank.deadwoodLitres} L", pal, Modifier.weight(1f))
                        }
                    }
                }

                if (!reverse) {
                    SectionLabel("DIP", pal)
                    NumberField(
                        value = dipText,
                        unit = "cm",
                        maxLength = 7,
                        pal = pal,
                        onChange = { new ->
                            dipText = new
                            prefs.edit().putString(KEY_LAST_DIP, new).apply()
                        },
                    )
                    Caption("Max ${tank.maxDipCm} cm · decimals are interpolated", pal)
                    Spacer(Modifier.height(10.dp))
                    ResultCard(Calibration.compute(tank, dipText), pal)
                } else {
                    SectionLabel("VOLUME", pal)
                    NumberField(
                        value = volumeText,
                        unit = "L",
                        maxLength = 9,
                        pal = pal,
                        onChange = { volumeText = it },
                    )
                    Caption("Chart volume in litres · dip is interpolated", pal)
                    Spacer(Modifier.height(10.dp))
                    DipResultCard(Calibration.dipForVolume(tank, volumeText), pal)
                }
            }
        }
        if (showManageCharts) {
            ManageChartsDialog(tanks, selectedIds, productNames, pal, onDismiss = { showManageCharts = false }) {
                chartsConfigured = true
                val edit = prefs.edit().putStringSet(KEY_MY_CHARTS, selectedIds.toSet()).putBoolean(KEY_MY_CHARTS_CONFIGURED, true)
                productNames.forEach { (id, name) -> edit.putString(KEY_PRODUCT_PREFIX + id, name.trim()) }
                edit.apply()
                val firstUnnamed = selectedIds.firstOrNull { productNames[it].isNullOrBlank() }
                if (firstUnnamed != null) { renameChartId = firstUnnamed; renameValue = "" }
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
                title = { Text(if (productNames[id].isNullOrBlank()) "Name the product" else "Edit product name", color = pal.label) },
                text = {
                    Column {
                        Text(chart?.name ?: "Selected tank", color = pal.secondary, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = renameValue,
                            onValueChange = { renameValue = it.take(32) },
                            singleLine = true,
                            textStyle = TextStyle(color = pal.label, fontSize = 17.sp),
                            cursorBrush = SolidColor(pal.accent),
                            modifier = Modifier.fillMaxWidth().background(pal.bg, RoundedCornerShape(10.dp)).padding(12.dp),
                            decorationBox = { inner -> Box { if (renameValue.isEmpty()) Text("e.g. Petrol, Diesel", color = pal.secondary); inner() } }
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { productNames[id] = renameValue.trim(); prefs.edit().putString(KEY_PRODUCT_PREFIX + id, renameValue.trim()).apply(); renameChartId = null }) { Text("Save", color = pal.accent) } },
                dismissButton = { TextButton(onClick = { renameChartId = null }) { Text("Later", color = pal.secondary) } }
            )
        }
    }
}

@Composable
private fun ManageChartsDialog(
    tanks: List<Tank>,
    selectedIds: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    productNames: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>,
    pal: Pal,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val checked = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(selectedIds) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = pal.card,
        title = { Text("My Charts", color = pal.label, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.height(420.dp)) {
                Text("Select the calibration charts used at your outlet. Only selected charts will appear in the tank selector.", color = pal.secondary, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.lazy.LazyColumn {
                    items(tanks.size) { index ->
                        val chart = tanks[index]
                        val isChecked = chart.id in checked
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (isChecked) checked.remove(chart.id) else checked.add(chart.id)
                            }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = { value ->
                                if (value) { if (chart.id !in checked) checked.add(chart.id) } else checked.remove(chart.id)
                            })
                            Column(Modifier.weight(1f)) {
                                Text(chart.name, color = pal.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("${chart.dimensionsLabel}  ·  P1 ${chart.p1Cm}  ·  P2 ${chart.p2Cm}", color = pal.secondary, fontSize = 11.sp)
                                if (isChecked) {
                                    BasicTextField(value = productNames[chart.id] ?: "", onValueChange = { productNames[chart.id] = it.take(32) }, singleLine = true, textStyle = TextStyle(color = pal.accent, fontSize = 12.sp), cursorBrush = SolidColor(pal.accent), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), decorationBox = { inner -> Box { if ((productNames[chart.id] ?: "").isEmpty()) Text("Product name (Petrol / Diesel / custom)", color = pal.secondary, fontSize = 11.sp); inner() } })
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { selectedIds.clear(); selectedIds.addAll(checked); onSave() }) {
                Text("Save selection", color = pal.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = pal.secondary) } },
    )
}

private fun fmtDim(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().trimEnd('0').trimEnd('.')

@Composable
private fun NumberField(value: String, unit: String, maxLength: Int, pal: Pal, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(pal.card, CardShape)
            .border(if (focused) 1.5.dp else 1.dp, if (focused) pal.accent else pal.separator, CardShape)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { new ->
                if (new.length <= maxLength && new.all { it.isDigit() || it == '.' || it == ',' }) onChange(new)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(color = pal.label, fontSize = 30.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp),
            cursorBrush = SolidColor(pal.accent),
            modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text("0.0", color = pal.secondary.copy(alpha = 0.4f),
                            fontSize = 30.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp)
                    }
                    inner()
                }
            },
        )
        Text(unit, color = pal.secondary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SegmentedControl(options: List<String>, selectedIndex: Int, pal: Pal, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(pal.track, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .background(if (selected) pal.raised else Color.Transparent, RoundedCornerShape(11.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) pal.label else pal.secondary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, pal: Pal, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = pal.accent)
        Spacer(Modifier.size(18.dp))
        Text(label, color = pal.label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

private val dipFmt = DecimalFormat("0.0")

@Composable
private fun DipResultCard(result: DipForVolumeResult, pal: Pal) {
    when (result) {
        is DipForVolumeResult.Ok ->
            HeroResult(dipFmt.format(result.dipCm), "cm", "CALCULATED FROM CALIBRATION CHART", pal)
        DipForVolumeResult.Empty -> Hint("Enter a volume to see the dip", pal)
        DipForVolumeResult.Invalid -> Hint("Enter a valid number", pal, error = true)
        is DipForVolumeResult.OutOfRange ->
            Hint("Above tank capacity (max ${numberFmt.format(result.maxVolumeLitres)} L)", pal, error = true)
    }
}

@Composable
private fun ResultCard(result: DipResult, pal: Pal) {
    when (result) {
        is DipResult.Ok ->
            HeroResult(numberFmt.format(result.netVolume), "litres", "CALIBRATION CHART VOLUME", pal)
        DipResult.Empty -> Hint("Enter a dip to see the volume", pal)
        DipResult.Invalid -> Hint("Enter a valid number", pal, error = true)
        is DipResult.OutOfRange -> Hint("Dip is above the tank maximum (${result.maxDipCm} cm)", pal, error = true)
    }
}

/** The answer: large, centred, on a softly tinted panel so it is the first thing the eye lands on. */
@Composable
private fun HeroResult(value: String, unit: String, caption: String, pal: Pal) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(pal.accentSoft, CardShape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = pal.accent, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.5).sp)
            Spacer(Modifier.size(8.dp))
            Text(unit, color = pal.secondary, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(caption, color = pal.secondary, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Hint(text: String, pal: Pal, error: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(pal.card, CardShape)
            .border(1.dp, pal.separator, CardShape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (error) pal.error else pal.secondary, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TankPicker(
    tanks: List<Tank>, selected: Tank, totalCharts: Int,
    productNames: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>,
    pal: Pal, onPick: (Tank) -> Unit, onManage: () -> Unit,
) {
    fun displayName(tank: Tank): String {
        val savedName = productNames[tank.id]?.trim().orEmpty()
        return if (savedName.isNotEmpty()) "$savedName - ${tank.kl} KL" else tank.label
    }
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { open = true }.padding(start = 18.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Calibration chart", color = pal.secondary, fontSize = 12.sp)
                Text(displayName(selected), color = pal.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(selected.dimensionsLabel, color = pal.secondary, fontSize = 11.sp)
            }
            Text("${tanks.size} available", color = pal.secondary, fontSize = 11.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose chart", tint = pal.accent)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = pal.card) {
            tanks.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(displayName(t), color = if (t.id == selected.id) pal.accent else pal.label, fontSize = 14.sp,
                                fontWeight = if (t.id == selected.id) FontWeight.SemiBold else FontWeight.Normal)
                            Text(t.dimensionsLabel, color = pal.secondary, fontSize = 11.sp)
                        }
                    },
                    onClick = { open = false; onPick(t) },
                )
            }
            DropdownMenuItem(
                text = { Text("Manage My Charts  ·  $totalCharts available", color = pal.accent, fontWeight = FontWeight.SemiBold) },
                onClick = { open = false; onManage() },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, pal: Pal) {
    Text(text, color = pal.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 4.dp))
}

@Composable
private fun Caption(text: String, pal: Pal) {
    Text(text, color = pal.secondary, fontSize = 12.sp, lineHeight = 17.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp))
}

@Composable
private fun Card(pal: Pal, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(pal.card, CardShape)
            .border(1.dp, pal.separator, CardShape),
    ) { content() }
}

@Composable
private fun MiniSpec(label: String, value: String, pal: Pal, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = pal.secondary, fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, color = pal.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun Divider(pal: Pal) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(pal.separator))
}

private fun rateApp(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$APP_ID"))
    try {
        context.startActivity(market)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHARE_LINK)))
    }
}

private fun shareApp(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Wiz Dip Charts")
        putExtra(Intent.EXTRA_TEXT, "Wiz Dip Charts – quick tank calibration lookup from dip reading.\n$SHARE_LINK")
    }
    context.startActivity(Intent.createChooser(send, "Share app"))
}