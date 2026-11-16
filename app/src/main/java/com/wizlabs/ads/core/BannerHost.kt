package com.wizlabs.ads.core

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Builds, loads and destroys one anchored adaptive banner (plain Views, so Compose and Views apps
 * share it). Next-Gen SDK callbacks arrive on a background thread: results are posted to main.
 * The container has zero height until an ad has loaded; a failed load stays collapsed.
 */
class BannerHost(private val activity: Activity) {
    val view: FrameLayout = FrameLayout(activity)
    private var adView: AdView? = null
    private val main = Handler(Looper.getMainLooper())

    fun load(unitId: String, widthDp: Int, onLoaded: (heightPx: Int) -> Unit, onFailed: () -> Unit) {
        if (unitId.isEmpty() || adView != null) return
        runCatching {
            val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
            val av = AdView(activity)
            adView = av
            view.addView(av, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            av.loadAd(BannerAdRequest.Builder(unitId, size).build(), object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    main.post { if (adView === av) onLoaded(ad.getAdSize().getHeightInPixels(activity)) }
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    main.post { if (adView === av) onFailed() }
                }
            })
        }.onFailure { onFailed() }
    }

    fun destroy() {
        main.removeCallbacksAndMessages(null)
        runCatching { adView?.destroy() }
        adView = null
        view.removeAllViews()
    }
}
