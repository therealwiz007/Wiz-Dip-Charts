package com.wizlabs.ads.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.wizlabs.ads.AdPalette
import com.wizlabs.ads.WIZ_ADS_SDK_LINKED
import com.wizlabs.ads.WizAds
import com.wizlabs.ads.core.AdFormat
import com.wizlabs.ads.core.AdsInitializer
import com.wizlabs.ads.core.BannerHost
import com.wizlabs.ads.core.NativeAdPool
import com.wizlabs.ads.core.WizNativeAdView

/**
 * Ad slots (plan 3.10). While ads are switched off they return before composing anything: no
 * view, no allocation, zero height, so the screens look exactly as before. When on, they render
 * nothing until an ad has loaded, hide while the keyboard is open, and destroy the ad on dispose.
 */
@Composable
fun WizBannerAd(screen: String, modifier: Modifier = Modifier, inline: Boolean = false) {
    val config = WizAds.configOrNull() ?: return
    if (!config.enabled || !WIZ_ADS_SDK_LINKED) return
    val visible by WizAds.adsVisible.collectAsState()
    val ready by AdsInitializer.ready.collectAsState()
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val allowed = visible && !imeOpen && WizAds.mayShow(AdFormat.BANNER, screen)
    if (config.debug && allowed) check(screen !in config.noAdScreens) { "banner requested on no-ad screen '$screen'" }
    if (!allowed) return
    val context = LocalContext.current
    LaunchedEffect(Unit) { WizAds.ensureInitialized(context) }
    if (!ready) return
    val activity = context.findActivity() ?: return
    val widthDp = LocalConfiguration.current.screenWidthDp
    var loaded by remember { mutableStateOf(false) }
    val host = remember(activity) { BannerHost(activity) }
    DisposableEffect(host) { onDispose { host.destroy() } }
    LaunchedEffect(host, widthDp) {
        host.load(config.units.banner.orEmpty(), widthDp, onLoaded = { loaded = true }, onFailed = { loaded = false })
    }
    AndroidView(factory = { host.view }, modifier = modifier.fillMaxWidth().animateContentSize(tween(200)))
}

@Composable
fun WizNativeAdCard(screen: String, palette: AdPalette, modifier: Modifier = Modifier) {
    val config = WizAds.configOrNull() ?: return
    if (!config.enabled || !WIZ_ADS_SDK_LINKED) return
    val visible by WizAds.adsVisible.collectAsState()
    val ready by AdsInitializer.ready.collectAsState()
    val available by NativeAdPool.available.collectAsState()
    val allowed = visible && WizAds.mayShow(AdFormat.NATIVE, screen)
    if (config.debug && allowed) check(screen !in config.noAdScreens) { "native ad requested on no-ad screen '$screen'" }
    if (!allowed) return
    val context = LocalContext.current
    LaunchedEffect(Unit) { WizAds.ensureInitialized(context) }
    LaunchedEffect(ready) { if (ready) NativeAdPool.preload(config) }
    if (!ready || !available) return
    val view = remember { WizNativeAdView(context, palette) }
    DisposableEffect(view) { onDispose { view.destroy() } }
    AndroidView(factory = { view.root.also { r -> NativeAdPool.take()?.let { view.bind(it) } } }, modifier = modifier.fillMaxWidth())
}

/**
 * Interstitial at a true end-of-task break (PDF Master and Photo Fit only, later). Always runs
 * [then] exactly once; 0.2 has no InterstitialSlot, so it runs it straight away.
 */
fun showInterstitialAtBreak(screen: String, then: () -> Unit) {
    if (!WIZ_ADS_SDK_LINKED || !WizAds.mayShow(AdFormat.INTERSTITIAL, screen)) { then(); return }
    then()  // later: InterstitialSlot.showAtBreak(activity, screen, then)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
