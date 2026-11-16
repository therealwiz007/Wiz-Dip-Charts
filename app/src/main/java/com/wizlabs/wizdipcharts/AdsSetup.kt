package com.wizlabs.wizdipcharts

import android.content.Context
import com.wizlabs.ads.AdUnits
import com.wizlabs.ads.AdsConfig
import com.wizlabs.ads.WizAds

/**
 * Ads for Dip Charts (WIZ_3APPS_MASTER_PLAN.md 4.5): one bottom banner on the main screen, nothing
 * in My charts or Find my chart. Switched off in this release (BuildConfig.WIZ_ADS_ENABLED=false),
 * so no consent form, no SDK init and no ad request ever happen.
 */
internal fun installAds(context: Context) = WizAds.install(
    context,
    AdsConfig(
        enabled = BuildConfig.WIZ_ADS_ENABLED,
        units = AdUnits(banner = BuildConfig.AD_BANNER.ifEmpty { null }),
        adScreens = setOf("main"),
        noAdScreens = setOf("manage", "find"),
        admobAppId = BuildConfig.ADMOB_APP_ID,
        debug = BuildConfig.DEBUG,
    ),
)
