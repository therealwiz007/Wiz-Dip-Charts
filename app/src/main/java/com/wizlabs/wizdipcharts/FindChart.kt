package com.wizlabs.wizdipcharts

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import kotlin.math.abs

/** The two dips a dealer reads off their own printed chart. */
private val FIND_DIPS = listOf(50, 100)
private const val EXACT_L = 1.0   // mean difference treated as the same chart
private const val CLOSE_L = 10.0  // still worth a look
private val litreFmt = DecimalFormat("#,##0.0")

/** Small "DW N/A" pill for charts whose source states no deadwood. */
@Composable
internal fun NoDwBadge(pal: Pal) {
    Text(
        "DW N/A", color = pal.secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp,
        modifier = Modifier.clip(CircleShape).background(pal.field).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

private fun parseLitres(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }

@Composable
internal fun FindChartScreen(
    tanks: List<Tank>,
    pal: Pal,
    isSaved: (Tank) -> Boolean,
    onDismiss: () -> Unit,
    onAdd: (Tank) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val capacities = remember(tanks) { tanks.map { it.kl }.distinct().sorted() }
    var kl by rememberSaveable { mutableStateOf<Int?>(null) }
    var at50 by rememberSaveable { mutableStateOf("") }
    var at100 by rememberSaveable { mutableStateOf("") }
    val readings = listOf(parseLitres(at50), parseLitres(at100))
    val ready = kl != null && readings.all { it != null }
    val results = remember(kl, readings) {
        if (!ready) emptyList() else Calibration.findMatches(tanks, kl!!, FIND_DIPS, readings.map { it!! })
    }

    Column(Modifier.fillMaxSize().background(pal.bg).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "Back", tint = pal.label) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Find my chart", color = pal.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Match your printed chart in seconds", color = pal.tertiary, fontSize = 13.sp)
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "intro") {
                Text(
                    "Pick your tank size, then type the litres printed on your chart at dip 50 cm and dip 100 cm. We compare them with every built-in chart.",
                    color = pal.secondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 4.dp).enterStagger(0),
                )
            }
            item(key = "kl") {
                Column(Modifier.enterStagger(1)) {
                    Text("TANK SIZE (KL)", color = pal.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        capacities.forEach { c ->
                            val on = c == kl
                            val bg by animateColorAsState(if (on) pal.accent else pal.card, tween(200), label = "kl")
                            val stroke by animateColorAsState(if (on) pal.accent else pal.separator, tween(200), label = "klStroke")
                            Text(
                                "$c", color = if (on) pal.onAccent else pal.label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                    .drawBehind { drawRect(bg); drawRoundRect(stroke, cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(1.dp.toPx())) }
                                    .pressClick { kl = c }.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
            item(key = "inputs") {
                Row(Modifier.enterStagger(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReadingField("At dip 50 cm", at50, pal, Modifier.weight(1f)) { at50 = it }
                    ReadingField("At dip 100 cm", at100, pal, Modifier.weight(1f)) { at100 = it }
                }
            }
            item(key = "status") {
                val status = when {
                    kl == null -> "Pick your tank size to start."
                    !ready -> "Enter both readings from your chart."
                    results.isEmpty() -> "No $kl KL charts are in the library yet."
                    results.first().error <= EXACT_L -> "Found it. Add the matching chart to My charts."
                    results.first().error <= CLOSE_L -> "Close match. Check the readings, then pick the closest chart."
                    else -> "No chart matches these readings. Check the numbers, or the chart may not be in the app yet."
                }
                AnimatedContent(status, transitionSpec = { (fadeIn(tween(200)) + slideInVertically { it / 3 }) togetherWith fadeOut(tween(120)) }, label = "findStatus") { st ->
                    Text(st, color = pal.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            itemsIndexed(results, key = { _, m -> m.tank.id }) { index, m ->
                MatchCard(index, m, readings.map { it ?: 0.0 }, isSaved(m.tank), pal, Modifier.enterStagger(index.coerceAtMost(6))) { onAdd(m.tank) }
            }
        }
    }
}

@Composable
private fun ReadingField(label: String, value: String, pal: Pal, modifier: Modifier, onChange: (String) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(pal.card).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label.uppercase(), color = pal.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = { new -> if (new.length <= 9 && new.all { it.isDigit() || it == '.' || it == ',' }) onChange(new) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                textStyle = TextStyle(color = pal.label, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                cursorBrush = SolidColor(if (pal.dark) Yellow else pal.label),
                modifier = Modifier.weight(1f),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text("0", color = pal.tertiary.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.SemiBold); inner() } },
            )
            Text("L", color = pal.tertiary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MatchCard(index: Int, m: Calibration.ChartMatch, readings: List<Double>, saved: Boolean, pal: Pal, modifier: Modifier, onAdd: () -> Unit) {
    val exact = m.error <= EXACT_L
    val close = m.error <= CLOSE_L
    val best = index == 0 && close
    val shape = RoundedCornerShape(22.dp)
    val stroke = if (best) Yellow else pal.separator
    Column(
        modifier.fillMaxWidth().clip(shape).background(pal.card)
            .drawBehind { drawRoundRect(stroke, cornerRadius = CornerRadius(22.dp.toPx()), style = Stroke((if (best) 2 else 1).dp.toPx())) }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (tag, tagBg, tagFg) = when {
                exact -> Triple("Exact match", Yellow, Ink)
                close -> Triple("Close", pal.accentSoft, pal.label)
                else -> Triple("Different chart", pal.field, pal.secondary)
            }
            Text(tag, color = tagFg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(tagBg).padding(horizontal = 9.dp, vertical = 3.dp))
            Spacer(Modifier.width(8.dp))
            if (!m.tank.hasDeadwood) NoDwBadge(pal)
            Spacer(Modifier.weight(1f))
            Text("±${litreFmt.format(m.error)} L", color = pal.tertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
        Text(m.tank.label, color = pal.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(m.tank.name.substringAfter(" · ", m.tank.name), color = pal.tertiary, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FIND_DIPS.forEachIndexed { i, dip ->
                val delta = m.volumes[i] - readings[i]
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(pal.field).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Dip $dip cm", color = pal.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("${litreFmt.format(m.volumes[i])} L", color = pal.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, style = TextStyle(fontFeatureSettings = "tnum"))
                    Text(
                        if (abs(delta) < 0.05) "same as yours" else "${if (delta > 0) "+" else "−"}${litreFmt.format(abs(delta))} L vs yours",
                        color = if (abs(delta) <= EXACT_L) pal.secondary else pal.tertiary, fontSize = 11.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val addShape = RoundedCornerShape(14.dp)
        Box(
            Modifier.fillMaxWidth().height(44.dp).clip(addShape).background(if (saved) pal.field else if (close) Yellow else pal.field)
                .pressClick(enabled = !saved, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(saved, transitionSpec = { (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith fadeOut(tween(90)) }, label = "added") { done ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (done) { Icon(Icons.Default.Check, null, tint = pal.label, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)) }
                    Text(
                        if (done) "In My charts" else "Use this chart",
                        color = if (done) pal.label else if (close) Ink else pal.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
