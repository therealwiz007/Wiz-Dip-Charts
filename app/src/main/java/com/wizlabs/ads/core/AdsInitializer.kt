package com.wizlabs.ads.core

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.wizlabs.ads.AdsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lazy MobileAds.initialize on a background thread (Next-Gen SDK 1.6.0 takes the app ID in code).
 * Called by the first slot that is about to appear, never from onCreate. No-op while ads are off,
 * without consent, or without an app ID.
 */
object AdsInitializer {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    @Volatile private var started = false

    fun ensure(context: Context, config: AdsConfig, canRequestAds: Boolean) {
        if (!config.enabled || !canRequestAds || config.admobAppId.isEmpty() || started) return
        started = true
        val app = context.applicationContext
        Thread({
            runCatching {
                MobileAds.initialize(app, InitializationConfig.Builder(config.admobAppId).build()) { _ ->
                    _ready.value = true
                }
            }.onFailure { started = false }
        }, "wiz-ads-init").start()
    }
}
