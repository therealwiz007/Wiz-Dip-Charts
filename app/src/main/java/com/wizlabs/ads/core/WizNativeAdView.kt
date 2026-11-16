package com.wizlabs.ads.core

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.wizlabs.ads.AdPalette

/**
 * Programmatic native ad row in Wiz style (no XML): icon, "Ad" badge, headline, body, call to
 * action. No MediaView, so it stays about 88 dp tall with no video and little memory.
 */
class WizNativeAdView(context: Context, private val palette: AdPalette) {
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density + 0.5f).toInt()

    val root = NativeAdView(context)
    private val icon = ImageView(context)
    private val headline = TextView(context)
    private val body = TextView(context)
    private val cta = TextView(context)
    private var bound: NativeAd? = null

    init {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(palette.surface); cornerRadius = dp(palette.cornerDp).toFloat(); setStroke(dp(1), palette.line)
            }
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val badge = TextView(context).apply {
            text = "Ad"
            setTextColor(palette.onAccent)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), dp(1), dp(6), dp(1))
            background = GradientDrawable().apply { setColor(palette.accent); cornerRadius = dp(6).toFloat() }
        }
        top.addView(badge)
        headline.apply {
            setTextColor(palette.text); setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f); typeface = Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        top.addView(headline, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
        texts.addView(top)
        body.apply { setTextColor(palette.muted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
        texts.addView(body)
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        cta.apply {
            setTextColor(palette.onAccent); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; minHeight = dp(40); setPadding(dp(14), 0, dp(14), 0)
            background = GradientDrawable().apply { setColor(palette.accent); cornerRadius = dp(20).toFloat() }
        }
        row.addView(cta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) })
        root.addView(row, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    fun bind(ad: NativeAd) {
        bound?.destroy()
        bound = ad
        headline.text = ad.headline.orEmpty()
        body.text = ad.body.orEmpty()
        cta.text = ad.callToAction.orEmpty()
        cta.visibility = if (ad.callToAction.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        val d = ad.icon?.drawable
        icon.setImageDrawable(d)
        icon.visibility = if (d == null) android.view.View.GONE else android.view.View.VISIBLE
        root.headlineView = headline
        root.bodyView = body
        root.callToActionView = cta
        root.iconView = icon
        root.registerNativeAd(ad, null)
    }

    fun destroy() {
        runCatching { bound?.destroy(); root.destroy() }
        bound = null
    }
}
