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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.DecimalFormat

private const val PREFS = "tankdip_prefs"
private const val KEY_LAST_TANK = "last_tank_kl"
private const val KEY_LAST_DIP = "last_dip_cm"

private const val APP_ID = "com.wizlabs.wizdipcharts"

/** Replace with your real Play Store link once the app is published. */
private const val SHARE_LINK = "https://play.google.com/store/apps/details?id=$APP_ID"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val tanks = Calibration.load(this)
        setContent { TankDipApp(tanks) }
    }
}

/* ---------- iOS-style palette ---------- */

private class Pal(val bg: Color, val card: Color, val label: Color, val secondary: Color, val separator: Color, val blue: Color)

private val LightPal = Pal(
    bg = Color(0xFFF2F2F7), card = Color.White, label = Color.Black,
    secondary = Color(0xFF8E8E93), separator = Color(0xFFE5E5EA), blue = Color(0xFF007AFF),
)
private val DarkPal = Pal(
    bg = Color.Black, card = Color(0xFF1C1C1E), label = Color.White,
    secondary = Color(0xFF8E8E93), separator = Color(0xFF38383A), blue = Color(0xFF0A84FF),
)

private val numberFmt = DecimalFormat("#,##0.0")

@Composable
fun TankDipApp(tanks: List<Tank>) {
    val context = LocalContext.current
    val pal = if (isSystemInDarkTheme()) DarkPal else LightPal

    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var selectedKl by rememberSaveable { mutableStateOf(prefs.getInt(KEY_LAST_TANK, tanks.first().kl)) }
    val tank = tanks.firstOrNull { it.kl == selectedKl } ?: tanks.first()
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
                Text("Wiz Dip Charts", color = pal.label, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(24.dp))
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
        Box(Modifier.fillMaxSize().background(pal.bg).safeDrawingPadding()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = pal.blue)
                }
                Text("Wiz Dip Charts", color = pal.label, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))

                SegmentedControl(
                    options = listOf("Dip → Volume", "Volume → Dip"),
                    selectedIndex = if (reverse) 1 else 0,
                    pal = pal,
                ) { index ->
                    if ((index == 1) != reverse) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    reverse = index == 1
                }

                SectionLabel("TANK", pal)
                Card(pal) {
                    TankPicker(tanks, tank, pal) { picked ->
                        if (picked.kl != selectedKl) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedKl = picked.kl
                        prefs.edit().putInt(KEY_LAST_TANK, picked.kl).apply()
                    }
                }

                SectionLabel("SPECIFICATIONS", pal)
                Card(pal) {
                    SpecRow("Diameter", "${tank.diameterCm} cm", pal)
                    Divider(pal)
                    SpecRow("Length", "${tank.lengthCm} cm", pal)
                    Divider(pal)
                    SpecRow("P1", "${tank.p1Cm} cm", pal)
                    Divider(pal)
                    SpecRow("P2", "${tank.p2Cm} cm", pal)
                    Divider(pal)
                    SpecRow("Deadwood", "${tank.deadwoodLitres} L", pal)
                }

                if (!reverse) {
                    SectionLabel("DIP", pal)
                    Card(pal) {
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
                    }
                    Text(
                        "Whole or decimal dips. Max ${tank.maxDipCm} cm. Values between two cm rows are interpolated.",
                        color = pal.secondary, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp),
                    )

                    SectionLabel("VOLUME", pal)
                    ResultCard(Calibration.compute(tank, dipText), pal)
                } else {
                    SectionLabel("VOLUME", pal)
                    Card(pal) {
                        NumberField(
                            value = volumeText,
                            unit = "L",
                            maxLength = 9,
                            pal = pal,
                            onChange = { volumeText = it },
                        )
                    }
                    Text(
                        "Final volume in litres (after deadwood). The dip is interpolated between chart rows.",
                        color = pal.secondary, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp),
                    )

                    SectionLabel("DIP", pal)
                    DipResultCard(Calibration.dipForVolume(tank, volumeText), pal)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NumberField(value: String, unit: String, maxLength: Int, pal: Pal, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value,
            onValueChange = { new ->
                if (new.length <= maxLength && new.all { it.isDigit() || it == '.' || it == ',' }) onChange(new)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(color = pal.label, fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(pal.blue),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text("0.0", color = pal.secondary.copy(alpha = 0.5f),
                            fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                    }
                    inner()
                }
            },
        )
        Text(unit, color = pal.secondary, fontSize = 20.sp)
    }
}

@Composable
private fun SegmentedControl(options: List<String>, selectedIndex: Int, pal: Pal, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(pal.separator, RoundedCornerShape(10.dp))
            .padding(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .background(if (selected) pal.card else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = pal.label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = pal.blue)
        Spacer(Modifier.size(16.dp))
        Text(label, color = pal.label, fontSize = 17.sp)
    }
}

private val dipFmt = DecimalFormat("0.0")

@Composable
private fun DipResultCard(result: DipForVolumeResult, pal: Pal) {
    Card(pal) {
        when (result) {
            is DipForVolumeResult.Ok -> {
                Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dipFmt.format(result.dipCm), color = pal.label, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text("cm", color = pal.secondary, fontSize = 17.sp)
                }
                Divider(pal)
                SpecRow("Chart volume", "${numberFmt.format(result.chartVolume)} L", pal)
            }
            DipForVolumeResult.Empty -> Hint("Enter a volume to see the dip", pal)
            DipForVolumeResult.Invalid -> Hint("Enter a valid number", pal, error = true)
            is DipForVolumeResult.OutOfRange ->
                Hint("Above tank capacity (max ${numberFmt.format(result.maxVolumeLitres)} L)", pal, error = true)
        }
    }
}

@Composable
private fun ResultCard(result: DipResult, pal: Pal) {
    Card(pal) {
        when (result) {
            is DipResult.Ok -> {
                Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(numberFmt.format(result.netVolume), color = pal.label, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text("litres", color = pal.secondary, fontSize = 17.sp)
                }
                Divider(pal)
                SpecRow("Chart volume", "${numberFmt.format(result.chartVolume)} L", pal)
                Divider(pal)
                SpecRow("Deadwood", "− ${numberFmt.format(result.deadwood)} L", pal)
            }
            DipResult.Empty -> Hint("Enter a dip to see the volume", pal)
            DipResult.Invalid -> Hint("Enter a valid number", pal, error = true)
            is DipResult.OutOfRange -> Hint("Dip is above the tank maximum (${result.maxDipCm} cm)", pal, error = true)
        }
    }
}

@Composable
private fun Hint(text: String, pal: Pal, error: Boolean = false) {
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (error) Color(0xFFFF3B30) else pal.secondary, fontSize = 17.sp)
    }
}

@Composable
private fun TankPicker(tanks: List<Tank>, selected: Tank, pal: Pal, onPick: (Tank) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Tank size", color = pal.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text(selected.label, color = pal.blue, fontSize = 17.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = pal.blue)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = pal.card) {
            tanks.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.label, color = if (t.kl == selected.kl) pal.blue else pal.label, fontSize = 17.sp) },
                    onClick = { open = false; onPick(t) },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, pal: Pal) {
    Text(text, color = pal.secondary, fontSize = 13.sp,
        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 6.dp))
}

@Composable
private fun Card(pal: Pal, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(pal.card, RoundedCornerShape(12.dp))) { content() }
}

@Composable
private fun SpecRow(label: String, value: String, pal: Pal) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = pal.label, fontSize = 17.sp)
        Text(value, color = pal.secondary, fontSize = 17.sp)
    }
}

@Composable
private fun Divider(pal: Pal) {
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(pal.separator))
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
