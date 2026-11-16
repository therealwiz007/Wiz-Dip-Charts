package com.wizlabs.ads.core

import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.wizlabs.ads.AdsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds at most one loaded NativeAd (plan 2.4 rule 4). No retries in a loop. No-op while ads are off. */
object NativeAdPool {
    private val main = Handler(Looper.getMainLooper())
    private var ad: NativeAd? = null
    private var loading = false

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    fun preload(config: AdsConfig) {
        val unit = config.units.native.orEmpty()
        if (!config.enabled || unit.isEmpty() || loading || ad != null) return
        loading = true
        runCatching {
            val request = NativeAdRequest.Builder(unit, listOf(NativeAd.NativeAdType.NATIVE)).build()
            NativeAdLoader.load(request, object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    main.post { loading = false; ad?.destroy(); ad = nativeAd; _available.value = true }
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    main.post { loading = false }
                }
            })
        }.onFailure { loading = false }
    }

    /** Hands the ad to a view, which then owns it and must destroy it. */
    fun take(): NativeAd? {
        val a = ad
        ad = null
        _available.value = false
        return a
    }

    fun clear() {
        runCatching { ad?.destroy() }
        ad = null
        _available.value = false
    }
}
